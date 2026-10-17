package library.tools;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import spoon.Launcher;
import spoon.reflect.CtModel;
import spoon.reflect.code.CtConstructorCall;
import spoon.reflect.code.CtExecutableReferenceExpression;
import spoon.reflect.code.CtExpression;
import spoon.reflect.code.CtFieldAccess;
import spoon.reflect.code.CtInvocation;
import spoon.reflect.declaration.CtAnnotation;
import spoon.reflect.declaration.CtAnonymousExecutable;
import spoon.reflect.declaration.CtClass;
import spoon.reflect.declaration.CtConstructor;
import spoon.reflect.declaration.CtElement;
import spoon.reflect.declaration.CtExecutable;
import spoon.reflect.declaration.CtField;
import spoon.reflect.declaration.CtMethod;
import spoon.reflect.declaration.CtSealable;
import spoon.reflect.declaration.CtType;
import spoon.reflect.declaration.CtTypeMember;
import spoon.reflect.declaration.CtTypeParameter;
import spoon.reflect.declaration.ModifierKind;
import spoon.reflect.reference.CtArrayTypeReference;
import spoon.reflect.reference.CtExecutableReference;
import spoon.reflect.reference.CtTypeReference;
import spoon.reflect.visitor.filter.TypeFilter;

/**
 * Main を起点に、競プロ提出用の 1 ファイル Java ソースを生成する。
 *
 * <p>このクラスが行う変換は原則として次の二つに限定する。
 *
 * <ul>
 * <li>tree-shaking: 到達不能な型・メソッド・フィールドを除去する。</li>
 * <li>link: 複数 package のソースを unnamed package の 1 ファイルへまとめるために、
 *     package 除去・top-level 型名衝突の解消・public の調整を行う。</li>
 * </ul>
 *
 * <p>non-static inner class の static 化など、プログラムの意味を変え得る最適化は行わない。
 */
public class MergeFiles {

	private static final Set<String> PRIMITIVE_TYPE_NAMES = Set.of(
			"boolean", "byte", "short", "int", "long", "char", "float", "double", "void");

	/**
	 * 通常の到達可能性解析では落ちるメソッドを、明示的に出力へ残すためのアノテーション。
	 * 出力時にはアノテーション自体は削除される。
	 */
	@java.lang.annotation.Retention(java.lang.annotation.RetentionPolicy.RUNTIME)
	@java.lang.annotation.Target(java.lang.annotation.ElementType.METHOD)
	public static @interface IncludeToMerge {
	}

	/**
	 * Main クラスを起点に競プロ用 1 ファイルを生成する。
	 *
	 * @throws IOException 入出力エラーが発生した場合
	 */
	// 未テスト
	public static void export() throws IOException {
		exportImpl("Main", "solver/src/main/java/Main.java", Paths.get("out", "Main.java").toString(), false);
	}

	/**
	 * コメントおよび余分な空白を除去するモードで、Main クラスを起点に競プロ用 1 ファイルを生成する。
	 *
	 * @throws IOException 入出力エラーが発生した場合
	 */
	// 未テスト
	public static void exportRemovingSpaceAndComment() throws IOException {
		exportImpl("Main", "solver/src/main/java/Main.java", Paths.get("out", "Main.java").toString(), true);
	}

	private static Path projectRoot = null;

	private static Path getProjectRoot() {
		if (projectRoot != null)
			return projectRoot;
		Path cur = Paths.get("").toAbsolutePath();
		while (cur != null) {
			if (Files.exists(cur.resolve("src/main/java/library"))
					&& Files.exists(cur.resolve("solver/src/main/java"))) {
				projectRoot = cur;
				return projectRoot;
			}
			cur = cur.getParent();
		}
		projectRoot = Paths.get("").toAbsolutePath();
		return projectRoot;
	}

	/**
	 * 指定されたクラスを起点に競プロ用 1 ファイルを生成する。
	 *
	 * @param entryClass エントリーポイントとなるクラス名
	 * @param mainFilePath メインファイルのパス
	 * @param outputFilePath 出力ファイルのパス
	 * @throws IOException 入出力エラーが発生した場合
	 */
	// 未テスト
	public static void export(String entryClass, String mainFilePath, String outputFilePath) throws IOException {
		exportImpl(entryClass, mainFilePath, outputFilePath, false);
	}

	/**
	 * コメントおよび余分な空白を除去するモードで、指定されたクラスを起点に競プロ用 1 ファイルを生成する。
	 *
	 * @param entryClass エントリーポイントとなるクラス名
	 * @param mainFilePath メインファイルのパス
	 * @param outputFilePath 出力ファイルのパス
	 * @throws IOException 入出力エラーが発生した場合
	 */
	// 未テスト
	public static void exportRemovingSpaceAndComment(String entryClass, String mainFilePath, String outputFilePath)
			throws IOException {
		exportImpl(entryClass, mainFilePath, outputFilePath, true);
	}

	/** 到達可能性解析の結果。解析完了後は、この集合を基準にのみ削除を行う。 */
	private static class Reachability {
		/** モデルに未収録で、到達コードが参照するソース。 */
		final Set<Path> missingSources = new LinkedHashSet<>();
		/** プロジェクト内の型名からソースへの索引。 */
		Map<String, Set<Path>> sourcesByQualifiedName = Map.of();
		/** 現在のモデルへ投入したソース。 */
		Set<Path> modeledSources = Set.of();
		final Set<CtExecutable<?>> executables = new HashSet<>();
		final Set<CtField<?>> fields = new HashSet<>();
		final Set<CtType<?>> types = new HashSet<>();
		final Set<CtExecutable<?>> scheduledExecutables = new HashSet<>();
		final Deque<CtExecutable<?>> executableStack = new ArrayDeque<>();
	}

	/** 初期投入ファイルと、追加投入に使う型名索引。 */
	private static class SourceSelection {
		/** 初期投入ファイル。 */
		final List<Path> initialFiles;
		/** プロジェクト内の型名からソースへの索引。 */
		final Map<String, Set<Path>> sourcesByQualifiedName;

		/**
		 * 初期ファイル F と型名索引 Q を保持する。計算量: O(1)。
		 *
		 * @param initialFiles F
		 * @param sourcesByQualifiedName Q
		 */
		// 未テスト
		SourceSelection(List<Path> initialFiles, Map<String, Set<Path>> sourcesByQualifiedName) {
			this.initialFiles = initialFiles;
			this.sourcesByQualifiedName = sourcesByQualifiedName;
		}
	}

	/**
	 * mainFile の識別子集合 I と型名集合 T(f) に対し、I ∩ T(f) ≠ ∅ かつ
	 * 同一 package・import・完全修飾名で可視の f を初期投入する。
	 * 後続の到達解析が参照する型のソースは型名索引から追加する。
	 * Unicode escape を含むソースがある場合は全ソースを初期投入する。
	 * 計算量: O(L + M)。L は全ソースの文字数、M は候補ファイルと識別子の対応数。
	 *
	 * @param mainFile エントリーファイル
	 * @return 初期投入ファイルとプロジェクト型名の索引
	 * @throws IOException ソースの読み取りに失敗した場合
	 */
	// 未テスト
	private static SourceSelection selectInputFiles(Path mainFile) throws IOException {
		Path root = getProjectRoot();
		List<Path> sourceFiles = new ArrayList<>();
		for (Path sourceRoot : List.of(root.resolve("solver/src/main/java"), root.resolve("src/main/java"))) {
			try (var paths = Files.walk(sourceRoot)) {
				paths.filter(Files::isRegularFile)
						.filter(path -> path.toString().endsWith(".java"))
						.forEach(sourceFiles::add);
			}
		}
		Path normalizedMain = mainFile.toAbsolutePath().normalize();
		if (!sourceFiles.contains(normalizedMain)) {
			sourceFiles.add(normalizedMain);
		}
		sourceFiles.sort(Path::compareTo);

		Map<Path, Set<String>> identifiersByFile = new HashMap<>();
		Map<String, List<Path>> filesByTypeName = new HashMap<>();
		Map<String, Set<Path>> sourcesByQualifiedName = new HashMap<>();
		Map<Path, String> packageByFile = new HashMap<>();
		Map<Path, Set<String>> importPrefixesByFile = new HashMap<>();
		Map<Path, Set<String>> wildcardImportsByFile = new HashMap<>();
		Map<Path, Map<String, Set<String>>> qualifiedPrefixesByFile = new HashMap<>();
		String identifier = "[\\p{javaJavaIdentifierStart}][\\p{javaJavaIdentifierPart}]*";
		String qualifiedIdentifier = identifier + "(?:\\." + identifier + ")*";
		Pattern packageDirective = Pattern.compile("\\bpackage\\s+(" + qualifiedIdentifier + ")\\s*;");
		Pattern importDirective = Pattern.compile(
				"\\bimport\\s+(?:static\\s+)?(" + qualifiedIdentifier + ")(\\.\\*)?\\s*;");
		Pattern qualifiedOccurrence = Pattern.compile(identifier + "(?:\\." + identifier + ")+");
		Pattern typeDeclaration = Pattern.compile(
				"(?<!\\.)\\b(?:class|interface|enum|record)\\s+([\\p{javaJavaIdentifierStart}][\\p{javaJavaIdentifierPart}]*)");
		boolean hasUnicodeEscape = false;
		for (Path sourceFile : sourceFiles) {
			String source = Files.readString(sourceFile);
			for (int i = 0; i + 1 < source.length(); i++) {
				if (source.charAt(i) == '\\' && source.charAt(i + 1) == 'u') {
					hasUnicodeEscape = true;
					break;
				}
			}
			List<String> tokens = javaIdentifiers(source);
			identifiersByFile.put(sourceFile, new HashSet<>(tokens));
			String code = removeCommentsAndWhitespace(source);
			Matcher packageMatch = packageDirective.matcher(code);
			packageByFile.put(sourceFile, packageMatch.find() ? packageMatch.group(1) : "");
			Set<String> importPrefixes = new HashSet<>();
			Set<String> wildcardImports = new HashSet<>();
			Matcher imports = importDirective.matcher(code);
			while (imports.find()) {
				String imported = imports.group(1);
				if (imports.group(2) != null)
					wildcardImports.add(imported);
				for (String prefix = imported;;) {
					importPrefixes.add(prefix);
					int dot = prefix.lastIndexOf('.');
					if (dot < 0)
						break;
					prefix = prefix.substring(0, dot);
				}
			}
			importPrefixesByFile.put(sourceFile, importPrefixes);
			wildcardImportsByFile.put(sourceFile, wildcardImports);
			Map<String, Set<String>> qualifiedPrefixes = new HashMap<>();
			Matcher qualifiedMatches = qualifiedOccurrence.matcher(code);
			while (qualifiedMatches.find()) {
				String qualifiedName = qualifiedMatches.group();
				int dot = qualifiedName.lastIndexOf('.');
				qualifiedPrefixes.computeIfAbsent(qualifiedName.substring(dot + 1), k -> new HashSet<>())
						.add(qualifiedName.substring(0, dot));
			}
			qualifiedPrefixesByFile.put(sourceFile, qualifiedPrefixes);
			String fileName = sourceFile.getFileName().toString();
			String baseName = fileName.substring(0, fileName.length() - 5);
			filesByTypeName.computeIfAbsent(baseName, k -> new ArrayList<>())
					.add(sourceFile);
			String sourcePackage = packageByFile.get(sourceFile);
			sourcesByQualifiedName.computeIfAbsent(sourcePackage.isEmpty() ? baseName : sourcePackage + "." + baseName,
					k -> new LinkedHashSet<>()).add(sourceFile);
			Matcher declarations = typeDeclaration.matcher(code);
			while (declarations.find()) {
				String name = declarations.group(1);
				filesByTypeName.computeIfAbsent(name, k -> new ArrayList<>()).add(sourceFile);
				sourcesByQualifiedName.computeIfAbsent(sourcePackage.isEmpty() ? name : sourcePackage + "." + name,
						k -> new LinkedHashSet<>()).add(sourceFile);
			}
		}
		if (hasUnicodeEscape) {
			// Unicode escape は字句解析より先に展開されるため、候補を絞らない。
			return new SourceSelection(sourceFiles, sourcesByQualifiedName);
		}

		Set<Path> selected = new LinkedHashSet<>();
		Deque<Path> queue = new ArrayDeque<>();
		selected.add(normalizedMain);
		queue.add(normalizedMain);
		Path directiveFile = root.resolve("src/main/java/library/tools/MergeFiles.java");
		while (!queue.isEmpty()) {
			Path source = queue.removeFirst();
			if (!source.equals(normalizedMain))
				continue;
			if (source.equals(directiveFile))
				continue;
			for (String name : identifiersByFile.get(source)) {
				for (Path candidate : filesByTypeName.getOrDefault(name, List.of())) {
					String sourcePackage = packageByFile.get(source);
					String candidatePackage = packageByFile.get(candidate);
					String candidateQualifiedName = candidatePackage.isEmpty() ? name : candidatePackage + "." + name;
					boolean visible = sourcePackage.equals(candidatePackage)
							|| importPrefixesByFile.get(source).contains(candidateQualifiedName)
							|| wildcardImportsByFile.get(source).contains(candidatePackage);
					if (!visible && !candidatePackage.isEmpty()) {
						for (String prefix : qualifiedPrefixesByFile.get(source).getOrDefault(name, Set.of())) {
							if (prefix.equals(candidatePackage) || prefix.startsWith(candidatePackage + ".")) {
								visible = true;
								break;
							}
						}
					}
					if (!visible)
						continue;
					if (selected.add(candidate)) {
						queue.addLast(candidate);
					}
				}
			}
		}
		return new SourceSelection(new ArrayList<>(selected), sourcesByQualifiedName);
	}

	/**
	 * source のコード部分にある Java 識別子を出現順に返す。計算量: O(|source|)。
	 * package と通常の import は除き、static import は宣言元の型を見落とさないため含める。
	 *
	 * @param source Java ソース
	 * @return 識別子列
	 */
	// 未テスト
	private static List<String> javaIdentifiers(String source) {
		List<String> tokens = new ArrayList<>();
		List<String> directiveTokens = new ArrayList<>();
		int directive = 0;
		for (int i = 0; i < source.length();) {
			char c = source.charAt(i);
			if (c == ';' && directive != 0) {
				if (directive == 1 && !directiveTokens.isEmpty() && directiveTokens.get(0).equals("static")) {
					tokens.addAll(directiveTokens);
				}
				directiveTokens.clear();
				directive = 0;
				i++;
				continue;
			}
			if (c == '/' && i + 1 < source.length() && source.charAt(i + 1) == '/') {
				i += 2;
				while (i < source.length() && source.charAt(i) != '\n' && source.charAt(i) != '\r')
					i++;
				continue;
			}
			if (c == '/' && i + 1 < source.length() && source.charAt(i + 1) == '*') {
				i += 2;
				while (i + 1 < source.length()
						&& !(source.charAt(i) == '*' && source.charAt(i + 1) == '/'))
					i++;
				i = Math.min(source.length(), i + 2);
				continue;
			}
			if (c == '"' && i + 2 < source.length() && source.charAt(i + 1) == '"'
					&& source.charAt(i + 2) == '"') {
				i += 3;
				while (i < source.length()) {
					if (source.charAt(i) == '\\' && i + 1 < source.length()) {
						i += 2;
					} else if (i + 2 < source.length() && source.charAt(i) == '"'
							&& source.charAt(i + 1) == '"' && source.charAt(i + 2) == '"') {
						i += 3;
						break;
					} else {
						i++;
					}
				}
				continue;
			}
			if (c == '"' || c == '\'') {
				char quote = c;
				i++;
				while (i < source.length()) {
					if (source.charAt(i) == '\\' && i + 1 < source.length()) {
						i += 2;
					} else if (source.charAt(i++) == quote) {
						break;
					}
				}
				continue;
			}
			int cp = source.codePointAt(i);
			if (!Character.isJavaIdentifierStart(cp)) {
				i += Character.charCount(cp);
				continue;
			}
			int start = i;
			i += Character.charCount(cp);
			while (i < source.length()) {
				cp = source.codePointAt(i);
				if (!Character.isJavaIdentifierPart(cp))
					break;
				i += Character.charCount(cp);
			}
			String token = source.substring(start, i);
			if (directive != 0) {
				directiveTokens.add(token);
			} else if (token.equals("import")) {
				directive = 1;
			} else if (token.equals("package")) {
				directive = 2;
			} else {
				tokens.add(token);
			}
		}
		return tokens;
	}

	/**
	 * sources を一時ディレクトリに複製し、そのディレクトリを返す。
	 * 計算量: O(B)。B は選択されたソースの総バイト数。
	 *
	 * @param sources Spoon に渡すソース
	 * @return 一時ディレクトリ
	 * @throws IOException 複製に失敗した場合
	 */
	// 未テスト
	private static Path stageSources(List<Path> sources) throws IOException {
		Path root = getProjectRoot();
		Path targetRoot = root.resolve("target").toAbsolutePath().normalize();
		Files.createDirectories(targetRoot);
		Path stage = Files.createTempDirectory(targetRoot, "mergefiles-sources-");
		try {
			for (Path source : sources) {
				Path relative = source.startsWith(root) ? root.relativize(source)
						: Path.of("external", source.getFileName().toString());
				Path stagedFile = stage.resolve(relative).normalize();
				if (!stagedFile.startsWith(stage))
						throw new IOException("Invalid source path: " + source);
				Files.createDirectories(stagedFile.getParent());
				Files.copy(source, stagedFile);
			}
			return stage;
		} catch (IOException | RuntimeException e) {
			deleteStagedSources(stage);
			throw e;
		}
	}

	/**
	 * stage 以下の一時複製を削除する。計算量: O(F)。F は stage 内のファイル数。
	 *
	 * @param stage stageSources が返したディレクトリ
	 * @throws IOException 削除に失敗した場合
	 */
	// 未テスト
	private static void deleteStagedSources(Path stage) throws IOException {
		Path targetRoot = getProjectRoot().resolve("target").toAbsolutePath().normalize();
		Path normalizedStage = stage.toAbsolutePath().normalize();
		if (!normalizedStage.getParent().equals(targetRoot) || !normalizedStage.startsWith(targetRoot))
			throw new IOException("Invalid staging directory: " + stage);
		try (var paths = Files.walk(normalizedStage)) {
			for (Path path : paths.sorted((a, b) -> b.compareTo(a)).toList()) {
				Files.deleteIfExists(path);
			}
		}
	}

	/**
	 * 指定されたクラスを起点に競プロ用 1 ファイルを生成する内部実装。
	 *
	 * <p>処理順は次の通り。
	 *
	 * <ol>
	 * <li>元 AST を変更せずに到達可能性を解析する。</li>
	 * <li>到達不能 member を削除する。</li>
	 * <li>MergeFiles 自身の build directive を削除する。</li>
	 * <li>1 ファイル化に必要な名前衝突解決・package 除去だけを行う。</li>
	 * <li>最後に一度だけ文字列化する。</li>
	 * </ol>
	 */
	private static void exportImpl(String entryClass, String mainFilePath, String outputFilePath,
			boolean removeCommentsAndWhitespace) throws IOException {
		SourceSelection sourceSelection = selectInputFiles(getProjectRoot().resolve(mainFilePath));
		Set<Path> selected = new LinkedHashSet<>(sourceSelection.initialFiles);
		Launcher launcher;
		CtType<?> mainClass;
		Reachability reachable;
		while (true) {
			launcher = buildSelectedModel(selected, removeCommentsAndWhitespace);
			CtModel model = launcher.getModel();
			Map<String, CtType<?>> typeByQualifiedName = new HashMap<>();
			for (CtType<?> type : model.getAllTypes()) {
				typeByQualifiedName.put(type.getQualifiedName(), type);
			}
			mainClass = model.getAllTypes().stream()
					.filter(t -> t.getSimpleName().equals(entryClass))
					.findFirst()
					.orElseThrow(() -> new RuntimeException("Entry class " + entryClass + " not found in model."));
			// 1. Analyze. ここでは AST を変更しない。
			reachable = analyzeReachability(mainClass, typeByQualifiedName,
					sourceSelection.sourcesByQualifiedName, selected);
			if (!selected.addAll(reachable.missingSources))
				break;
		}

		// 2. Tree-shaking. 解析結果に従って削除するだけ。
		for (CtType<?> type : topLevelTypes(reachable.types)) {
			pruneTypeRecursively(type, reachable);
		}

		// export() 系の build directive と IncludeToMerge は、提出コードからだけ除去する。
		for (CtType<?> type : topLevelTypes(reachable.types)) {
			stripBuildDirectives(type);
		}

		// 3. Link. 1 ファイル化のために不可避な変換だけを行う。
		List<CtTypeReference<?>> reachableReferences = new ArrayList<>();
		for (CtType<?> type : topLevelTypes(reachable.types)) {
			reachableReferences.addAll(type.getElements(new TypeFilter<CtTypeReference<?>>(CtTypeReference.class)));
		}
		resolveTopLevelNameCollisions(reachableReferences, reachable.types, mainClass);
		moveReachableTopLevelTypesToUnnamedPackage(launcher, reachableReferences, reachable.types);

		for (CtType<?> type : topLevelTypes(reachable.types)) {
			if (type != mainClass) {
				type.removeModifier(ModifierKind.PUBLIC);
			}
		}

		// autoImports=false のまま出力する。
		// JDK/external 型は fully-qualified name で出るので、import の衝突処理や文字列置換が不要になる。
		String merged = emitTypes(reachable.types, mainClass);

		String outputStr;
		if (removeCommentsAndWhitespace) {
			outputStr = removeCommentsAndWhitespace(merged);
		} else {
			String originalCode = Files.readString(getProjectRoot().resolve(mainFilePath));
			StringBuilder commented = new StringBuilder("\n// --- Original Code ---\n");
			for (String line : originalCode.split("\n", -1)) {
				commented.append("// ").append(line).append("\n");
			}
			outputStr = merged + commented;
		}

		Path outPath = getProjectRoot().resolve(outputFilePath);
		if (outPath.getParent() != null)
			Files.createDirectories(outPath.getParent());
		Files.writeString(outPath, outputStr);
	}

	/**
	 * S を投入した Spoon モデルを返す。計算量: O(Σ_(f∈S)|f| + B(S))。
	 * B(S) は Spoon による S の解析時間を表す。
	 *
	 * @param selected 投入するソース集合
	 * @param withoutComments コメントを読み込まない場合 true
	 * @return S から作られたモデルを保持する Launcher
	 * @throws IOException ソースのステージングに失敗した場合
	 */
	// 未テスト
	private static Launcher buildSelectedModel(Set<Path> selected, boolean withoutComments) throws IOException {
		Launcher launcher = new Launcher();
		launcher.getEnvironment().setPreserveLineNumbers(false);
		launcher.getEnvironment().setComplianceLevel(21);
		launcher.getEnvironment().setNoClasspath(true);
		launcher.getEnvironment().setAutoImports(false);
		if (withoutComments)
			launcher.getEnvironment().setCommentEnabled(false);
		Path stage = stageSources(new ArrayList<>(selected));
		try {
			Path stagedSolverRoot = stage.resolve("solver/src/main/java");
			Path stagedLibraryRoot = stage.resolve("src/main/java");
			if (Files.exists(stagedSolverRoot))
				launcher.addInputResource(stagedSolverRoot.toString());
			if (Files.exists(stagedLibraryRoot))
				launcher.addInputResource(stagedLibraryRoot.toString());
			for (Path source : selected) {
				if (!source.startsWith(getProjectRoot().resolve("solver/src/main/java"))
						&& !source.startsWith(getProjectRoot().resolve("src/main/java"))) {
					Path relative = source.startsWith(getProjectRoot()) ? getProjectRoot().relativize(source)
							: Path.of("external", source.getFileName().toString());
					launcher.addInputResource(stage.resolve(relative).toString());
				}
			}
			launcher.buildModel();
		} finally {
			deleteStagedSources(stage);
		}
		return launcher;
	}

	/**
	 * 元 AST を一切変更せず、entry class から必要な declaration を集める。
	 */
	private static Reachability analyzeReachability(CtType<?> mainClass, Map<String, CtType<?>> typeByQualifiedName,
			Map<String, Set<Path>> sourcesByQualifiedName, Set<Path> modeledSources) {
		Reachability ret = new Reachability();
		ret.sourcesByQualifiedName = sourcesByQualifiedName;
		ret.modeledSources = modeledSources;

		addTypeAndRequiredMethods(mainClass, ret, typeByQualifiedName);

		// main/run は明示的な entry point とする。
		for (CtMethod<?> method : mainClass.getMethods()) {
			if (method.getSimpleName().equals("main") || method.getSimpleName().equals("run")) {
				scheduleExecutable(method, ret);
			}
		}

		while (!ret.executableStack.isEmpty()) {
			CtExecutable<?> exec = ret.executableStack.pop();
			if (!(exec instanceof CtTypeMember member))
				continue;
			CtType<?> declaringType = member.getDeclaringType();
			if (declaringType == null || isExternalTypeName(declaringType.getQualifiedName())
					|| declaringType.getQualifiedName().equals("library.tools.MergeFiles")) {
				continue;
			}
			if (!ret.executables.add(exec))
				continue;

			if (exec instanceof CtMethod<?> method) {
				scheduleOverriddenMethods(method, ret);
			}

			addTypeAndRequiredMethods(declaringType, ret, typeByQualifiedName);
			scanElementDependencies(exec, ret, typeByQualifiedName);
		}

		return ret;
	}

	private static void scheduleOverriddenMethods(CtMethod<?> method, Reachability reachable) {
		CtType<?> declaringType = method.getDeclaringType();
		if (declaringType == null)
			return;
		for (CtType<?> superType : getAllSuperTypes(declaringType)) {
			for (CtMethod<?> superMethod : superType.getMethods()) {
				if (isOverriding(method, superMethod)) {
					scheduleExecutable(superMethod, reachable);
				}
			}
		}
	}

	/**
	 * executable / field initializer 内の型・呼び出し・フィールド参照を解析する。
	 * 計算量: O(E + D)。E は対象要素の子孫数、D は参照解決と新たに到達した宣言の解析時間。
	 */
	private static void scanElementDependencies(CtElement element,
			Reachability reachable, Map<String, CtType<?>> typeByQualifiedName) {
		for (CtElement child : element.getElements(new TypeFilter<CtElement>(CtElement.class))) {
			if (isInsideBuildDirective(child))
				continue;
			if (child instanceof CtTypeReference<?> ref) {
				markTypeReferenceReachable(ref, reachable, typeByQualifiedName);
			} else if (child instanceof CtExecutableReferenceExpression<?, ?> methodRef) {
				addExecutableIfReachable(methodRef.getExecutable(), reachable, typeByQualifiedName);
			} else if (child instanceof CtInvocation<?> invocation) {
				addExecutableIfReachable(invocation.getExecutable(), reachable, typeByQualifiedName);
			} else if (child instanceof CtConstructorCall<?> constructorCall) {
				addExecutableIfReachable(constructorCall.getExecutable(), reachable, typeByQualifiedName);
				markTypeReferenceReachable(constructorCall.getType(), reachable, typeByQualifiedName);
			} else if (child instanceof CtFieldAccess<?> fieldAccess && fieldAccess.getVariable() != null) {
				markTypeReferenceReachable(fieldAccess.getVariable().getDeclaringType(), reachable, typeByQualifiedName);
				try {
					markFieldAndInitializerReachable(fieldAccess.getVariable().getDeclaration(), reachable, typeByQualifiedName);
				} catch (Exception e) {
					// noClasspath では declaration を解決できないことがある。
				}
			}
		}
	}

	private static void scheduleExecutable(CtExecutable<?> exec, Reachability reachable) {
		if (exec != null && reachable.scheduledExecutables.add(exec)) {
			reachable.executableStack.push(exec);
		}
	}

	private static void addExecutableIfReachable(CtExecutableReference<?> execRef,
			Reachability reachable, Map<String, CtType<?>> typeByQualifiedName) {
		if (execRef == null)
			return;
		markTypeReferenceReachable(execRef.getDeclaringType(), reachable, typeByQualifiedName);
		CtExecutable<?> exec = resolveExecutable(execRef, typeByQualifiedName);
		if (exec != null) {
			scheduleExecutable(exec, reachable);
		}
	}

	private static void markFieldAndInitializerReachable(CtField<?> field,
			Reachability reachable, Map<String, CtType<?>> typeByQualifiedName) {
		if (field == null || field.getDeclaringType() == null)
			return;
		if (!reachable.fields.add(field))
			return;

		addTypeAndRequiredMethods(field.getDeclaringType(), reachable, typeByQualifiedName);
		markTypeReferenceReachable(field.getType(), reachable, typeByQualifiedName);

		for (CtAnnotation<?> annotation : field.getAnnotations()) {
			scanElementDependencies(annotation, reachable, typeByQualifiedName);
		}

		CtExpression<?> init = field.getDefaultExpression();
		if (init != null) {
			scanElementDependencies(init, reachable, typeByQualifiedName);
		}
	}

	private static void scheduleConcreteImplementation(CtType<?> type, CtMethod<?> superMethod, Reachability reachable) {
		for (CtType<?> cur = type; cur != null; cur = cur.getSuperclass() != null ? cur.getSuperclass().getTypeDeclaration() : null) {
			for (CtMethod<?> m : cur.getMethods()) {
				if (!m.isAbstract() && isOverriding(m, superMethod)) {
					scheduleExecutable(m, reachable);
					return;
				}
			}
		}
	}

	/**
	 * 型を出力対象へ加える。
	 *
	 * <p>型が残る以上コンパイルに必要になる constructor / initializer / override 系メソッドは
	 * 保守的に残す。tree-shaking は「確実に不要」と判断できる通常メソッド・フィールドを主に落とす。
	 * sealed 型の permits に現れる型も出力対象に含める。
	 * 計算量: 到達型全体で O(D + H)。D は走査される宣言・参照数、H は継承メソッドの比較回数。
	 */
	private static void addTypeAndRequiredMethods(CtType<?> type,
			Reachability reachable, Map<String, CtType<?>> typeByQualifiedName) {
		if (type == null || isInvalidMergeType(type))
			return;
		if (!reachable.types.add(type))
			return;

		// nested type を残すなら owner も必要。
		if (type.getDeclaringType() != null) {
			addTypeAndRequiredMethods(type.getDeclaringType(), reachable, typeByQualifiedName);
		}

		markTypeReferenceReachable(type.getSuperclass(), reachable, typeByQualifiedName);
		for (CtTypeReference<?> iface : type.getSuperInterfaces()) {
			markTypeReferenceReachable(iface, reachable, typeByQualifiedName);
		}
		if (type instanceof CtSealable sealable) {
			for (CtTypeReference<?> permittedType : sealable.getPermittedTypes()) {
				markTypeReferenceReachable(permittedType, reachable, typeByQualifiedName);
			}
		}

		// class-level annotation / type parameter bound は declaration の一部なので残す。
		for (CtAnnotation<?> annotation : type.getAnnotations()) {
			scanElementDependencies(annotation, reachable, typeByQualifiedName);
		}
		for (CtTypeParameter typeParameter : type.getFormalCtTypeParameters()) {
			scanElementDependencies(typeParameter, reachable, typeByQualifiedName);
		}

		// constructor を一部だけ削ると implicit super() 等を壊しやすいので、型が残るなら全て残す。
		if (type instanceof CtClass<?> clazz) {
			for (CtConstructor<?> constructor : clazz.getConstructors()) {
				scheduleExecutable(constructor, reachable);
			}
		}

		// static / instance initializer は型の declaration の一部として残す。
		for (CtTypeMember member : type.getTypeMembers()) {
			if (member instanceof CtAnonymousExecutable anonymousExecutable) {
				scheduleExecutable(anonymousExecutable, reachable);
			}
		}

		for (CtType<?> superType : getAllSuperTypes(type)) {
			for (CtMethod<?> superMethod : superType.getMethods()) {
				if (superMethod.isAbstract()) {
					scheduleConcreteImplementation(type, superMethod, reachable);
				}
			}
		}

		boolean preserveAllMembers = type.isInterface()
				|| type instanceof spoon.reflect.declaration.CtRecord
				|| type instanceof spoon.reflect.declaration.CtEnum;

		if (preserveAllMembers) {
			for (CtMethod<?> method : type.getMethods()) {
				scheduleExecutable(method, reachable);
			}
			for (CtField<?> field : type.getFields()) {
				markFieldAndInitializerReachable(field, reachable, typeByQualifiedName);
			}
		} else {
			for (CtMethod<?> method : type.getMethods()) {
				if (method.getAnnotation(IncludeToMerge.class) != null
						|| method.getAnnotation(Override.class) != null
						|| implementsAbstractMethod(type, method)
						|| isSpecialMethod(type, method)
						|| overridesNonJdkMethod(type, method)) {
					scheduleExecutable(method, reachable);
				}
			}
		}
	}

	private static void markTypeReferenceReachable(CtTypeReference<?> ref,
			Reachability reachable, Map<String, CtType<?>> typeByQualifiedName) {
		if (ref == null)
			return;
		while (ref instanceof CtArrayTypeReference<?> arrayRef) {
			ref = arrayRef.getComponentType();
		}
		for (CtTypeReference<?> typeArg : ref.getActualTypeArguments()) {
			markTypeReferenceReachable(typeArg, reachable, typeByQualifiedName);
		}
		if (isInvalidTypeReference(ref))
			return;
		if (scheduleMissingSource(ref, reachable))
			return;
		CtType<?> refType = resolveType(ref, typeByQualifiedName);
		if (refType != null && !isInvalidMergeType(refType)) {
			addTypeAndRequiredMethods(refType, reachable, typeByQualifiedName);
		}
	}

	/**
	 * Reachability に従って通常の class member を削る。
	 * declaration の static 性・owner・継承関係などは変更しない。
	 */
	private static void pruneTypeRecursively(CtType<?> type, Reachability reachable) {
		boolean preserveAllMembers = type.isInterface()
				|| type instanceof spoon.reflect.declaration.CtRecord
				|| type instanceof spoon.reflect.declaration.CtEnum;

		if (!preserveAllMembers) {
			for (CtMethod<?> method : new HashSet<>(type.getMethods())) {
				if (!reachable.executables.contains(method)
						&& method.getAnnotation(IncludeToMerge.class) == null) {
					method.delete();
				}
			}
			for (CtField<?> field : new HashSet<>(type.getFields())) {
				if (!reachable.fields.contains(field)) {
					field.delete();
				}
			}
		}

		for (CtType<?> nested : new HashSet<>(type.getNestedTypes())) {
			if (!reachable.types.contains(nested)) {
				nested.delete();
			} else {
				pruneTypeRecursively(nested, reachable);
			}
		}
	}

	/**
	 * build directive 呼び出しだけを AST 上で除去する。
	 */
	private static void stripBuildDirectives(CtType<?> type) {
		List<CtInvocation<?>> invocations = type.getElements(new TypeFilter<CtInvocation<?>>(CtInvocation.class));
		for (CtInvocation<?> invocation : new ArrayList<>(invocations)) {
			if (isBuildDirectiveInvocation(invocation)) {
				invocation.delete();
			}
		}

		List<CtAnnotation<?>> annotations = type.getElements(new TypeFilter<CtAnnotation<?>>(CtAnnotation.class));
		for (CtAnnotation<?> annotation : new ArrayList<>(annotations)) {
			CtTypeReference<?> annotationType = annotation.getAnnotationType();
			if (annotationType != null && isIncludeToMergeTypeName(annotationType.getQualifiedName())) {
				annotation.delete();
			}
		}
	}

	/** build directive i なら true を返す。計算量: O(1)。 */
	// 未テスト
	private static boolean isBuildDirectiveInvocation(CtInvocation<?> invocation) {
		CtExecutableReference<?> exec = invocation.getExecutable();
		if (exec == null || exec.getDeclaringType() == null)
			return false;
		String qName = exec.getDeclaringType().getQualifiedName();
		String name = exec.getSimpleName();
		return (isMergeFilesTypeName(qName)
				&& (name.equals("export") || name.equals("exportRemovingSpaceAndComment")))
				|| (("library.tools.JavaToCpp".equals(qName) || "JavaToCpp".equals(qName))
						&& name.equals("export"));
	}

	/** e が build directive 呼び出しの部分木に属するなら true を返す。計算量: O(h)。h は親階層数。 */
	// 未テスト
	private static boolean isInsideBuildDirective(CtElement element) {
		for (CtElement current = element; current != null;
				current = current.isParentInitialized() ? current.getParent() : null) {
			if (current instanceof CtInvocation<?> invocation && isBuildDirectiveInvocation(invocation))
				return true;
		}
		return false;
	}

	private static boolean isMergeFilesTypeName(String qName) {
		return qName != null
				&& (qName.equals("library.tools.MergeFiles") || qName.equals("MergeFiles"));
	}

	private static boolean isIncludeToMergeTypeName(String qName) {
		return qName != null
				&& (qName.equals("library.tools.MergeFiles.IncludeToMerge")
						|| qName.equals("library.tools.MergeFiles$IncludeToMerge")
						|| qName.equals("MergeFiles.IncludeToMerge")
						|| qName.equals("MergeFiles$IncludeToMerge"));
	}

	/**
	 * unnamed package にまとめたときだけ生じる top-level simple name 衝突を解消する。
	 * declaration と、それを指す CtTypeReference を同時に rename する。
	 */
	private static void resolveTopLevelNameCollisions(List<CtTypeReference<?>> reachableReferences, Set<CtType<?>> reachableTypes,
			CtType<?> entryType) {
		Map<String, List<CtType<?>>> bySimpleName = new HashMap<>();
		Set<String> usedNames = new HashSet<>();

		for (CtType<?> type : topLevelTypes(reachableTypes)) {
			bySimpleName.computeIfAbsent(type.getSimpleName(), k -> new ArrayList<>()).add(type);
			usedNames.add(type.getSimpleName());
		}

		for (List<CtType<?>> types : bySimpleName.values()) {
			if (types.size() <= 1)
				continue;

			types.sort((a, b) -> a.getQualifiedName().compareTo(b.getQualifiedName()));
			CtType<?> keep = types.contains(entryType) ? entryType : types.get(0);

			for (CtType<?> type : types) {
				if (type == keep)
					continue;
				String oldQualifiedName = type.getQualifiedName();
				String base = collisionName(oldQualifiedName);
				String newName = base;
				for (int suffix = 2; usedNames.contains(newName); suffix++) {
					newName = base + suffix;
				}
				usedNames.add(newName);
				tr("Resolving top-level collision: " + oldQualifiedName + " -> " + newName);
				renameTopLevelType(reachableReferences, type, oldQualifiedName, newName);
			}
		}
	}

	private static String collisionName(String qualifiedName) {
		StringBuilder ret = new StringBuilder();
		for (String part : qualifiedName.split("\\.")) {
			if (part.isEmpty())
				continue;
			ret.append(Character.toUpperCase(part.charAt(0))).append(part.substring(1));
		}
		return ret.toString();
	}

	private static void renameTopLevelType(List<CtTypeReference<?>> reachableReferences, CtType<?> type,
			String oldQualifiedName, String newSimpleName) {
		List<CtTypeReference<?>> refs = new ArrayList<>();
		for (CtTypeReference<?> ref : reachableReferences) {
			CtTypeReference<?> topRef = topLevelReference(ref);
			if (referencesTopLevelType(topRef, type, oldQualifiedName)) {
				refs.add(topRef);
			}
		}

		type.setSimpleName(newSimpleName);
		for (CtTypeReference<?> ref : refs) {
			ref.setSimpleName(newSimpleName);
		}
	}

	/** 参照 ref が nested type なら、その一番外側の型参照を返す。 */
	private static CtTypeReference<?> topLevelReference(CtTypeReference<?> ref) {
		CtTypeReference<?> ret = ref;
		while (ret != null && ret.getDeclaringType() != null) {
			ret = ret.getDeclaringType();
		}
		return ret;
	}

	private static boolean referencesTopLevelType(CtTypeReference<?> ref, CtType<?> type,
			String oldQualifiedName) {
		if (ref == null)
			return false;
		try {
			if (ref.getTypeDeclaration() == type)
				return true;
		} catch (Exception e) {
			// noClasspath fallback below
		}
		return oldQualifiedName.equals(ref.getQualifiedName());
	}

	/** reachable な top-level 型とその参照だけを unnamed package へ移す。 */
	private static void moveReachableTopLevelTypesToUnnamedPackage(Launcher launcher, List<CtTypeReference<?>> reachableReferences,
			Set<CtType<?>> reachableTypes) {
		List<CtType<?>> topLevelTypes = topLevelTypes(reachableTypes);
		Set<CtType<?>> topLevelTypeSet = new HashSet<>(topLevelTypes);
		Set<String> qualifiedNames = new HashSet<>();
		for (CtType<?> type : topLevelTypes) {
			qualifiedNames.add(type.getQualifiedName());
		}

		for (CtTypeReference<?> ref : reachableReferences) {
			CtTypeReference<?> topRef = topLevelReference(ref);
			if (topRef == null)
				continue;

			boolean internal = qualifiedNames.contains(topRef.getQualifiedName());
			if (!internal) {
				try {
					internal = topLevelTypeSet.contains(topRef.getTypeDeclaration());
				} catch (Exception e) {
					// noClasspath: qualifiedName fallback only
				}
			}
			if (internal) {
				topRef.setPackage(null);
			}
		}

		for (CtType<?> type : topLevelTypes) {
			if (type.getPackage() != null && !type.getPackage().isUnnamedPackage()) {
				type.delete();
				launcher.getFactory().Package().getRootPackage().addType(type);
			}
		}
	}

	private static List<CtType<?>> topLevelTypes(Set<CtType<?>> types) {
		List<CtType<?>> ret = new ArrayList<>();
		for (CtType<?> type : types) {
			if (!isInvalidMergeType(type)
					&& type.getDeclaringType() == null
					&& !type.isLocalType()
					&& !type.isAnonymous()) {
				ret.add(type);
			}
		}
		return ret;
	}

	private static String emitTypes(Set<CtType<?>> reachableTypes, CtType<?> entryType) {
		List<CtType<?>> sorted = topLevelTypes(reachableTypes);
		sorted.sort((a, b) -> {
			if (a == entryType)
				return -1;
			if (b == entryType)
				return 1;
			return a.getSimpleName().compareTo(b.getSimpleName());
		});

		StringBuilder merged = new StringBuilder();
		for (CtType<?> type : sorted) {
			merged.append(type.toString()).append("\n\n");
		}
		return merged.toString();
	}

	/**
	 * qualified name に対応するモデル内の型を返す。
	 * 計算量: 索引で見つかる場合 O(1)（平均）、それ以外は Spoon の宣言解決に依存する。
	 */
	// 未テスト
	private static CtType<?> resolveType(CtTypeReference<?> ref, Map<String, CtType<?>> typeByQualifiedName) {
		if (ref == null)
			return null;
		CtType<?> type = typeByQualifiedName.get(ref.getQualifiedName());
		if (type != null)
			return type;
		return ref.getTypeDeclaration();
	}

	/**
	 * 参照 q に対応するプロジェクトソースがモデルに無いとき、そのファイルを追加予定集合へ入れる。
	 * nested 型 q については最長の既知 prefix に対応するファイルを選ぶ。
	 * 計算量: O(|q|^2 + K)。K は一致するソース数。
	 *
	 * @param ref 型参照
	 * @param reachable 到達解析の状態
	 * @return モデルに無いプロジェクトソースが見つかった場合 true
	 */
	// 未テスト
	private static boolean scheduleMissingSource(CtTypeReference<?> ref, Reachability reachable) {
		String name = ref.getQualifiedName();
		if (name == null || isExternalTypeName(name) || name.equals("library.tools.MergeFiles"))
			return false;
		name = name.replace('$', '.');
		for (String prefix = name;;) {
			Set<Path> sources = reachable.sourcesByQualifiedName.get(prefix);
			if (sources != null) {
				boolean missing = false;
				for (Path source : sources) {
					if (!reachable.modeledSources.contains(source)) {
						reachable.missingSources.add(source);
						missing = true;
					}
				}
				return missing;
			}
			int dot = prefix.lastIndexOf('.');
			if (dot < 0)
				return false;
			prefix = prefix.substring(0, dot);
		}
	}

	private static CtExecutable<?> resolveExecutable(CtExecutableReference<?> ref, Map<String, CtType<?>> typeByQualifiedName) {
		if (ref == null)
			return null;
		CtExecutable<?> declaration = ref.getDeclaration();
		if (declaration != null)
			return declaration;

		CtTypeReference<?> declaringTypeRef = ref.getDeclaringType();
		if (declaringTypeRef == null)
			return null;
		CtType<?> declaringType = resolveType(declaringTypeRef, typeByQualifiedName);
		if (declaringType == null)
			return null;

		// noClasspath fallback。完全な overload resolution は Spoon に任せ、ここでは最後の救済だけ行う。
		for (CtMethod<?> method : declaringType.getAllMethods()) {
			if (method.getSimpleName().equals(ref.getSimpleName())
					&& method.getParameters().size() == ref.getParameters().size()) {
				return method;
			}
		}
		for (CtConstructor<?> constructor : declaringType.getElements(new TypeFilter<CtConstructor<?>>(CtConstructor.class))) {
			if (constructor.getParameters().size() == ref.getParameters().size()) {
				return constructor;
			}
		}
		return null;
	}

	private static List<CtType<?>> getAllSuperTypes(CtType<?> type) {
		List<CtType<?>> res = new ArrayList<>();
		Deque<CtTypeReference<?>> stack = new ArrayDeque<>();
		if (type.getSuperclass() != null)
			stack.push(type.getSuperclass());
		stack.addAll(type.getSuperInterfaces());
		Set<String> visited = new HashSet<>();
		while (!stack.isEmpty()) {
			CtTypeReference<?> ref = stack.pop();
			if (!visited.add(ref.getQualifiedName()))
				continue;
			CtType<?> t = ref.getTypeDeclaration();
			if (t == null)
				continue;
			res.add(t);
			if (t.getSuperclass() != null)
				stack.push(t.getSuperclass());
			stack.addAll(t.getSuperInterfaces());
		}
		return res;
	}

	static boolean implementsAbstractMethod(CtType<?> type, CtMethod<?> method) {
		if (method.isAbstract() || method.hasModifier(ModifierKind.PRIVATE))
			return false;
		for (CtType<?> superType : getAllSuperTypes(type)) {
			for (CtMethod<?> superMethod : superType.getMethods()) {
				if (superMethod.isAbstract() && isOverriding(method, superMethod))
					return true;
			}
		}
		return false;
	}

	private static boolean overridesNonJdkMethod(CtType<?> type, CtMethod<?> method) {
		if (method.hasModifier(ModifierKind.PRIVATE) || method.hasModifier(ModifierKind.STATIC))
			return false;
		for (CtType<?> superType : getAllSuperTypes(type)) {
			if (isExternalTypeName(superType.getQualifiedName()))
				continue;
			for (CtMethod<?> superMethod : superType.getMethods()) {
				if (isOverriding(method, superMethod))
					return true;
			}
		}
		return false;
	}

	private static boolean isSpecialMethod(CtType<?> type, CtMethod<?> method) {
		String name = method.getSimpleName();
		if (name.equals("iterator") && method.getParameters().isEmpty())
			return isSubtypeOf(type, Iterable.class);
		if (name.equals("compareTo") && method.getParameters().size() == 1)
			return isSubtypeOf(type, Comparable.class);
		if ((name.equals("hasNext") || name.equals("next") || name.equals("nextInt"))
				&& method.getParameters().isEmpty()) {
			return isSubtypeOf(type, java.util.Iterator.class)
					|| isSubtypeOf(type, java.util.PrimitiveIterator.class);
		}
		return false;
	}

	private static boolean isSubtypeOf(CtType<?> type, Class<?> superType) {
		try {
			return type.getReference().isSubtypeOf(type.getFactory().Type().createReference(superType));
		} catch (Exception e) {
			return false;
		}
	}

	private static boolean isTypeCompatible(CtTypeReference<?> t1, CtTypeReference<?> t2) {
		if (t1 == null || t2 == null)
			return false;
		while (t1 instanceof CtArrayTypeReference<?> arr1)
			t1 = arr1.getComponentType();
		while (t2 instanceof CtArrayTypeReference<?> arr2)
			t2 = arr2.getComponentType();
		if (t1.getQualifiedName().equals(t2.getQualifiedName()))
			return true;
		try {
			if (t1.getTypeDeclaration() instanceof CtTypeParameter)
				return true;
		} catch (Exception e) {
		}
		try {
			if (t2.getTypeDeclaration() instanceof CtTypeParameter)
				return true;
		} catch (Exception e) {
		}
		return false;
	}

	private static boolean isOverriding(CtMethod<?> method, CtMethod<?> superMethod) {
		if (method == superMethod)
			return false;
		try {
			if (method.isOverriding(superMethod))
				return true;
		} catch (Exception e) {
		}
		if (!method.getSimpleName().equals(superMethod.getSimpleName()))
			return false;
		if (method.getParameters().size() != superMethod.getParameters().size())
			return false;
		for (int i = 0; i < method.getParameters().size(); i++) {
			if (!isTypeCompatible(method.getParameters().get(i).getType(), superMethod.getParameters().get(i).getType()))
				return false;
		}
		if (superMethod.hasModifier(ModifierKind.PRIVATE) || superMethod.hasModifier(ModifierKind.STATIC))
			return false;
		if (superMethod.hasModifier(ModifierKind.PUBLIC) || superMethod.hasModifier(ModifierKind.PROTECTED))
			return true;

		CtType<?> type1 = method.getDeclaringType();
		CtType<?> type2 = superMethod.getDeclaringType();
		if (type1 == null || type2 == null)
			return false;
		String p1 = type1.getPackage() != null ? type1.getPackage().getQualifiedName() : "";
		String p2 = type2.getPackage() != null ? type2.getPackage().getQualifiedName() : "";
		return p1.equals(p2);
	}

	private static boolean isInvalidMergeType(CtType<?> type) {
		if (type == null || type instanceof CtTypeParameter)
			return true;
		String qName = type.getQualifiedName();
		if (qName == null || qName.equals("library.tools.MergeFiles") || isExternalTypeName(qName))
			return true;
		return isInvalidTypeName(qName) || isInvalidTypeName(type.getSimpleName());
	}

	private static boolean isInvalidTypeReference(CtTypeReference<?> ref) {
		if (ref == null)
			return true;
		CtTypeReference<?> component = ref;
		while (component instanceof CtArrayTypeReference<?> arrayRef)
			component = arrayRef.getComponentType();
		return isInvalidTypeName(component.getQualifiedName());
	}

	private static boolean isInvalidTypeName(String name) {
		return name == null
				|| PRIMITIVE_TYPE_NAMES.contains(name)
				|| name.equals("null")
				|| name.contains("[")
				|| name.contains("]");
	}

	/**
	 * プロジェクト内で merge する型ではない型名。
	 * 外部ライブラリもここでは削除せず、単に merge 対象外として扱う。
	 */
	private static boolean isExternalTypeName(String qName) {
		if (qName == null)
			return true;
		return qName.startsWith("java.")
				|| qName.startsWith("javax.")
				|| qName.startsWith("sun.")
				|| qName.startsWith("jdk.")
				|| qName.startsWith("org.graphstream.")
				|| qName.startsWith("org.knowm.xchart.")
				|| qName.startsWith("spoon.")
				|| qName.startsWith("cc.redberry.rings.");
	}

	/**
	 * 必要に応じてスペースを挿入する。
	 */
	// 未テスト
	private static void appendPendingSpace(StringBuilder sb, char c) {
		if (sb.length() > 0) {
			char prev = sb.charAt(sb.length() - 1);
			boolean needSpace = false;
			if (Character.isJavaIdentifierPart(prev) && Character.isJavaIdentifierPart(c)) {
				needSpace = true;
			} else if (prev == '+' && c == '+') {
				needSpace = true;
			} else if (prev == '-' && c == '-') {
				needSpace = true;
			}
			if (needSpace) {
				sb.append(' ');
			}
		}
	}

	/**
	 * Java ソースコードからすべてのコメントおよび余分な空白を除去する。
	 *
	 * @param code 変換元の Java ソースコード文字列
	 * @return コメントと余分な空白が除去された Java ソースコード文字列
	 */
	// 未テスト
	public static String removeCommentsAndWhitespace(String code) {
		StringBuilder sb = new StringBuilder();
		int len = code.length();
		int i = 0;
		boolean pendingSpace = false;
		boolean beginningOfLine = true;

		final int NORMAL = 0;
		final int STRING = 1;
		final int CHAR = 2;
		final int TEXT_BLOCK = 3;
		final int BLOCK_COMMENT = 4;
		final int LINE_COMMENT = 5;

		int state = NORMAL;

		while (i < len) {
			char c = code.charAt(i);

			if (state == BLOCK_COMMENT) {
				if (c == '*' && i + 1 < len && code.charAt(i + 1) == '/') {
					state = NORMAL;
					i += 2;
					if (!beginningOfLine) {
						pendingSpace = true;
					}
				} else {
					i++;
				}
				continue;
			}

			if (state == LINE_COMMENT) {
				if (c == '\n' || c == '\r') {
					state = NORMAL;
				} else {
					i++;
					continue;
				}
			}

			if (state == TEXT_BLOCK) {
				if (c == '"' && i + 2 < len && code.substring(i, i + 3).equals("\"\"\"")) {
					sb.append("\"\"\"");
					state = NORMAL;
					i += 3;
					beginningOfLine = false;
				} else if (c == '\\' && i + 1 < len) {
					sb.append('\\').append(code.charAt(i + 1));
					i += 2;
				} else {
					sb.append(c);
					i++;
				}
				continue;
			}

			if (state == STRING) {
				if (c == '"') {
					sb.append('"');
					state = NORMAL;
					i++;
					beginningOfLine = false;
				} else if (c == '\\' && i + 1 < len) {
					sb.append('\\').append(code.charAt(i + 1));
					i += 2;
				} else {
					sb.append(c);
					i++;
				}
				continue;
			}

			if (state == CHAR) {
				if (c == '\'') {
					sb.append('\'');
					state = NORMAL;
					i++;
					beginningOfLine = false;
				} else if (c == '\\' && i + 1 < len) {
					sb.append('\\').append(code.charAt(i + 1));
					i += 2;
				} else {
					sb.append(c);
					i++;
				}
				continue;
			}

			if (c == '/' && i + 1 < len && code.charAt(i + 1) == '*') {
				state = BLOCK_COMMENT;
				i += 2;
				continue;
			}
			if (c == '/' && i + 1 < len && code.charAt(i + 1) == '/') {
				state = LINE_COMMENT;
				i += 2;
				continue;
			}
			if (c == '"' && i + 2 < len && code.substring(i, i + 3).equals("\"\"\"")) {
				if (pendingSpace) {
					appendPendingSpace(sb, c);
					pendingSpace = false;
				}
				state = TEXT_BLOCK;
				sb.append("\"\"\"");
				i += 3;
				continue;
			}
			if (c == '"') {
				if (pendingSpace) {
					appendPendingSpace(sb, c);
					pendingSpace = false;
				}
				state = STRING;
				sb.append('"');
				i++;
				continue;
			}
			if (c == '\'') {
				if (pendingSpace) {
					appendPendingSpace(sb, c);
					pendingSpace = false;
				}
				state = CHAR;
				sb.append('\'');
				i++;
				continue;
			}

			if (Character.isWhitespace(c)) {
				if (!beginningOfLine) {
					pendingSpace = true;
				}
				i++;
				continue;
			}

			if (pendingSpace) {
				appendPendingSpace(sb, c);
				pendingSpace = false;
			}

			sb.append(c);
			beginningOfLine = false;
			i++;
		}

		return sb.toString().trim();
	}

	static boolean DEBUG = false;

	static void tr(Object... objects) {
		if (DEBUG)
			System.out.println(Arrays.deepToString(objects));
	}
}
