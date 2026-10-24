package library.tools;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import spoon.Launcher;
import spoon.reflect.CtModel;
import spoon.reflect.code.CtArrayAccess;
import spoon.reflect.code.CtAssert;
import spoon.reflect.code.CtAssignment;
import spoon.reflect.code.CtBinaryOperator;
import spoon.reflect.code.CtBlock;
import spoon.reflect.code.CtBreak;
import spoon.reflect.code.CtCase;
import spoon.reflect.code.CtCatch;
import spoon.reflect.code.CtConditional;
import spoon.reflect.code.CtConstructorCall;
import spoon.reflect.code.CtContinue;
import spoon.reflect.code.CtComment;
import spoon.reflect.code.CtDo;
import spoon.reflect.code.CtExpression;
import spoon.reflect.code.CtExecutableReferenceExpression;
import spoon.reflect.code.CtFieldAccess;
import spoon.reflect.code.CtFor;
import spoon.reflect.code.CtForEach;
import spoon.reflect.code.CtIf;
import spoon.reflect.code.CtInvocation;
import spoon.reflect.code.CtLambda;
import spoon.reflect.code.CtLiteral;
import spoon.reflect.code.CtLocalVariable;
import spoon.reflect.code.CtNewArray;
import spoon.reflect.code.CtNewClass;
import spoon.reflect.code.CtOperatorAssignment;
import spoon.reflect.code.CtReturn;
import spoon.reflect.code.CtStatement;
import spoon.reflect.code.CtSwitch;
import spoon.reflect.code.CtSynchronized;
import spoon.reflect.code.CtSuperAccess;
import spoon.reflect.code.CtThisAccess;
import spoon.reflect.code.CtThrow;
import spoon.reflect.code.CtTry;
import spoon.reflect.code.CtTypeAccess;
import spoon.reflect.code.CtUnaryOperator;
import spoon.reflect.code.CtVariableAccess;
import spoon.reflect.code.CtWhile;
import spoon.reflect.declaration.CtClass;
import spoon.reflect.declaration.CtConstructor;
import spoon.reflect.declaration.CtElement;
import spoon.reflect.declaration.CtField;
import spoon.reflect.declaration.CtMethod;
import spoon.reflect.declaration.CtParameter;
import spoon.reflect.declaration.CtType;
import spoon.reflect.declaration.CtTypeParameter;
import spoon.reflect.declaration.ModifierKind;
import spoon.reflect.reference.CtArrayTypeReference;
import spoon.reflect.reference.CtFieldReference;
import spoon.reflect.reference.CtTypeReference;
import spoon.reflect.reference.CtTypeParameterReference;
import spoon.reflect.visitor.filter.TypeFilter;

/**
 * {@link MergeFiles#export()} が生成した単一 Java ソースを AtCoder 提出用 C++17 ソースへ変換する。
 * 対応外の Java 構文は、その位置を含む {@link IllegalArgumentException} とする。
 */
public final class JavaToCpp {
    /** C++ の予約語と衝突する Java 識別子。 */
    private static final Set<String> CPP_KEYWORDS = Set.of("delete", "template", "typename", "operator",
            "namespace", "concept", "requires", "and", "or", "xor", "compl", "not", "new", "union");

    /** インスタンス化しない。計算量: O(1)。 */
    // 未テスト
    private JavaToCpp() {
    }

    /**
     * {@code args[0]} の Java ソースを変換し、{@code args[1]} へ UTF-8 で書く。
     * 引数を省略した場合はそれぞれ {@code out/Main.java}, {@code out/main.cpp} とする。
     * 計算量: O(L)。L は入力ソースと生成ソースの長さの和。
     *
     * @param args 入力パス、出力パス
     * @throws IOException 入出力に失敗した場合
     */
    // 未テスト
    public static void main(String[] args) throws IOException {
        Path input = args.length >= 1 ? Paths.get(args[0]) : Paths.get("out", "Main.java");
        Path output = args.length >= 2 ? Paths.get(args[1]) : Paths.get("out", "main.cpp");
        export(input, output);
    }

    /**
     * {@code out/Main.java} を {@code out/main.cpp} へ変換する。
     * 計算量: O(L)。L は入力ソースと生成ソースの長さの和。
     *
     * @throws IOException 入出力に失敗した場合
     */
    // 未テスト
    public static void export() throws IOException {
        export(Paths.get("out", "Main.java"), Paths.get("out", "main.cpp"));
    }

    /**
     * 単一 Java ソースのプログラム P を、同じ標準入出力を持つ C++17 プログラムへ変換する。
     * 計算量: O(L)。L は入力ソースと生成ソースの長さの和。
     *
     * @param inputJava 入力 Java ソース
     * @param outputCpp 出力 C++ ソース
     * @throws IOException 入出力に失敗した場合
     */
    // 未テスト
    public static void export(Path inputJava, Path outputCpp) throws IOException {
        String cpp = convert(inputJava);
        Path parent = outputCpp.toAbsolutePath().getParent();
        if (parent != null) Files.createDirectories(parent);
        Files.writeString(outputCpp, cpp, StandardCharsets.UTF_8);
    }

    /**
     * 単一 Java ソースのプログラム P を、同じ標準入出力を持つ C++17 プログラム文字列へ変換する。
     * 計算量: O(L)。L は入力ソースと生成ソースの長さの和。
     *
     * @param inputJava 入力 Java ソース
     * @return C++17 ソース
     */
    // 未テスト
    public static String convert(Path inputJava) {
        Launcher launcher = new Launcher();
        launcher.getEnvironment().setNoClasspath(true);
        launcher.getEnvironment().setComplianceLevel(21);
        launcher.addInputResource(inputJava.toString());
        CtModel model = launcher.buildModel();
        return new Emitter(model).emit();
    }

    /** C++ ソースを構築する変換状態。 */
    private static final class Emitter {
        /** 解析対象モデル。 */
        private final CtModel model;
        /** 単一ソースに宣言された型の単純名。 */
        private final Set<String> declaredTypes = new HashSet<>();
        /** C++入力ランタイムへ置換するソース内型名。 */
        private final Set<String> runtimeScannerTypes = new HashSet<>();
        /** C++出力ランタイムへ置換するソース内型名。 */
        private final Set<String> runtimeWriterTypes = new HashSet<>();
        /** 出力バッファ。 */
        private final StringBuilder out = new StringBuilder();
        /** 全型定義後に本体を出力するメソッド。 */
        private final List<CtMethod<?>> deferredMethods = new java.util.ArrayList<>();
        /** 現在本体を出力している型。 */
        private CtType<?> currentType;
        /** 現在出力中の無名関数メソッド。 */
        private CtMethod<?> currentAnonymousMethod;
        /** static 初期化子または static メソッドの出力中なら true。 */
        private boolean currentStaticContext;
        /** 現在の字下げ段数。 */
        private int indent;

        /**
         * モデル M の変換状態を作る。計算量: O(T)。T は宣言型数。
         *
         * @param model M
         */
        // 未テスト
        Emitter(CtModel model) {
            this.model = model;
            for (CtType<?> type : model.getElements(new TypeFilter<>(CtType.class))) {
                registerType(type);
            }
        }

        /** 型 t とその入れ子型を登録する。計算量: O(|t|)。 */
        // 未テスト
        private void registerType(CtType<?> type) {
            declaredTypes.add(type.getSimpleName());
            declaredTypes.add(type.getQualifiedName());
            String name = simpleName(type.getSimpleName());
            boolean readsTokens = type.getMethods().stream().anyMatch(m -> m.getSimpleName().equals("nextInt"));
            boolean ownsInput = type.getFields().stream().anyMatch(f ->
                    f.getType().getQualifiedName().equals("java.io.InputStream"));
            if (name.equals("FastScanner") || (readsTokens && ownsInput)) {
                runtimeScannerTypes.add(name);
                runtimeScannerTypes.add(type.getSimpleName());
            }
            if (name.equals("MyPrintWriter") || (type instanceof CtClass<?> cls
                    && cls.getSuperclass() != null
                    && cls.getSuperclass().getQualifiedName().equals("java.io.PrintWriter"))) {
                runtimeWriterTypes.add(name);
                runtimeWriterTypes.add(type.getSimpleName());
            }
            for (CtType<?> nested : type.getNestedTypes()) registerType(nested);
        }

        /**
         * モデル全体を C++17 ソースへ変換する。計算量: O(L)。L は生成ソース長。
         *
         * @return C++17 ソース
         */
        // 未テスト
        private List<CtType<?>> reachableTopLevelTypes(List<CtType<?>> topLevelTypes) {
            CtType<?> mainType = topLevelTypes.stream().filter(t -> t.getSimpleName().equals("Main")).findFirst().orElse(null);
            if (mainType == null) return topLevelTypes;

            Set<String> visitedNames = new HashSet<>();
            java.util.Queue<CtType<?>> queue = new java.util.ArrayDeque<>();

            visitedNames.add("Main");
            queue.add(mainType);

            while (!queue.isEmpty()) {
                CtType<?> current = queue.poll();
                for (CtTypeReference<?> ref : current.getElements(new TypeFilter<>(CtTypeReference.class))) {
                    String simpleName = simpleName(ref.getSimpleName());
                    for (CtType<?> candidate : topLevelTypes) {
                        String candName = candidate.getSimpleName();
                        if (!visitedNames.contains(candName) && candName.equals(simpleName)) {
                            visitedNames.add(candName);
                            queue.add(candidate);
                        }
                    }
                }
            }

            Set<String> topNames = topLevelTypes.stream().map(CtType::getSimpleName).collect(java.util.stream.Collectors.toSet());
            return topLevelTypes.stream().filter(t -> {
                if (visitedNames.contains(t.getSimpleName())) return true;
                for (CtTypeReference<?> ref : t.getElements(new TypeFilter<>(CtTypeReference.class))) {
                    String q = ref.getQualifiedName();
                    String simple = simpleName(ref.getSimpleName());
                    if (ref.getTypeDeclaration() == null && !topNames.contains(simple)
                            && !q.startsWith("java.") && !q.startsWith("javax.") && !isRuntimeType(simple)
                            && !q.equals("void") && !q.equals("boolean") && !q.equals("byte") && !q.equals("short")
                            && !q.equals("int") && !q.equals("long") && !q.equals("float") && !q.equals("double") && !q.equals("char")) {
                        return false;
                    }
                }
                return true;
            }).toList();
        }

        String emit() {
            out.append(PRELUDE1).append(PRELUDE2);
            List<CtType<?>> types = model.getAllTypes().stream()
                    .filter(t -> t.getDeclaringType() == null)
                    .filter(t -> !t.isLocalType() && !t.isAnonymous() && t.getParent(CtBlock.class) == null)
                    .filter(t -> !isRuntimeType(t.getSimpleName()))
                    .toList();
            types = reachableTopLevelTypes(types);
            types = dependencyOrder(types);
            for (CtType<?> type : types) emitForward(type);
            out.append('\n');
            for (CtType<?> type : types) emitType(type);
            for (CtMethod<?> method : deferredMethods) emitDeferredMethod(method);
            out.append("\nint main() {\n    ios::sync_with_stdio(false);\n    cin.tie(nullptr);\n");
            if (types.stream().noneMatch(t -> t.getSimpleName().equals("Main"))) {
                throw unsupported(model.getRootPackage(), "Main class is required");
            }
            out.append("    Main::javaMain({});\n    return 0;\n}\n");
            return out.toString();
        }

        /** 宣言型を参照先が参照元より前になる順に返す。循環部分は名前順とする。計算量: O(T^2 + R)。 */
        // 未テスト
        private List<CtType<?>> dependencyOrder(List<CtType<?>> input) {
            List<CtType<?>> remaining = new java.util.ArrayList<>(input);
            List<CtType<?>> result = new java.util.ArrayList<>();
            remaining.sort(Comparator.comparing((CtType<?> t) -> t.getSimpleName().equals("Main"))
                    .thenComparing(CtType::getSimpleName));
            while (!remaining.isEmpty()) {
                List<CtType<?>> candidates = remaining.stream().filter(t -> inheritanceDependencies(t).stream()
                        .noneMatch(d -> remaining.stream().anyMatch(r -> r.getSimpleName().equals(d)))).toList();
                if (candidates.isEmpty()) candidates = remaining;
                CtType<?> next = candidates.stream().min(Comparator.comparingInt(t -> (int) dependencies(t).stream()
                        .filter(d -> remaining.stream().anyMatch(r -> r.getSimpleName().equals(d))).count()))
                        .orElse(candidates.get(0));
                result.add(next);
                remaining.remove(next);
            }
            return result;
        }

        /** 型 t の継承先である最上位宣言型名を返す。計算量: O(B)。B は直接基底型数。 */
        // 未テスト
        private Set<String> inheritanceDependencies(CtType<?> t) {
            Set<String> result = new HashSet<>();
            if (t instanceof CtClass<?> cls && cls.getSuperclass() != null) {
                result.add(topLevelName(cls.getSuperclass()));
            }
            for (CtTypeReference<?> base : t.getSuperInterfaces()) result.add(topLevelName(base));
            result.remove(t.getSimpleName());
            result.removeIf(n -> !declaredTypes.contains(n));
            return result;
        }

        /** 型参照 t の最上位単純名を返す。計算量: O(|name(t)|)。 */
        // 未テスト
        private String topLevelName(CtTypeReference<?> t) {
            String q = t.getQualifiedName();
            int packageEnd = q.lastIndexOf('.');
            String local = packageEnd < 0 ? q : q.substring(packageEnd + 1);
            int nested = local.indexOf('$');
            return nested < 0 ? local : local.substring(0, nested);
        }

        /** 型 t が参照する最上位宣言型名を返す。計算量: O(|t|)。 */
        // 未テスト
        private Set<String> dependencies(CtType<?> t) {
            Set<String> result = new HashSet<>();
            for (CtTypeReference<?> reference : t.getElements(new TypeFilter<>(CtTypeReference.class))) {
                String top = topLevelName(reference);
                if (!top.equals(t.getSimpleName()) && declaredTypes.contains(top)) result.add(top);
            }
            return result;
        }

        /** 型 T の前方宣言を出力する。計算量: O(P)。P は型パラメータ数。 */
        // 未テスト
        private void emitForward(CtType<?> type) {
            if (!type.getFormalCtTypeParameters().isEmpty()) {
                out.append("template <");
                for (int i = 0; i < type.getFormalCtTypeParameters().size(); i++) {
                    if (i > 0) out.append(", ");
                    out.append("class ").append(id(type.getFormalCtTypeParameters().get(i).getSimpleName()));
                }
                out.append(">\n");
            }
            out.append("struct ").append(id(type.getSimpleName())).append(";\n");
        }

        /** 型 T の定義を出力する。計算量: O(|T|)。 */
        // 未テスト
        private void emitType(CtType<?> type) {
            CtType<?> previousType = currentType;
            currentType = type;
            emitTemplate(type.getFormalCtTypeParameters());
            out.append("struct ").append(id(type.getSimpleName()));
            List<String> cppBases = new java.util.ArrayList<>();
            if (!type.isLocalType()) {
                cppBases.add("public std::enable_shared_from_this<" + id(type.getSimpleName()) + templateArgs(type.getFormalCtTypeParameters()) + ">");
            }
            if (type instanceof CtClass<?> cls && cls.getSuperclass() != null
                    && !cls.getSuperclass().getQualifiedName().equals("java.lang.Object")
                    && !cls.getSuperclass().getQualifiedName().equals("java.lang.Record")) {
                String baseStr = rawCustomType(cls.getSuperclass());
                if (!baseStr.contains("<") && !cls.getSuperclass().getActualTypeArguments().isEmpty()) {
                    StringBuilder sb = new StringBuilder(baseStr).append('<');
                    List<CtTypeReference<?>> args = cls.getSuperclass().getActualTypeArguments();
                    for (int i = 0; i < args.size(); i++) {
                        if (i > 0) sb.append(", ");
                        sb.append(type(args.get(i)));
                    }
                    baseStr = sb.append('>').toString();
                }
                cppBases.add("public " + baseStr);
            }
            for (CtTypeReference<?> superInterface : type.getSuperInterfaces()) {
                if (isDeclaredType(superInterface)) {
                    String baseStr = rawCustomType(superInterface);
                    if (!baseStr.contains("<") && !superInterface.getActualTypeArguments().isEmpty()) {
                        StringBuilder sb = new StringBuilder(baseStr).append('<');
                        List<CtTypeReference<?>> args = superInterface.getActualTypeArguments();
                        for (int i = 0; i < args.size(); i++) {
                            if (i > 0) sb.append(", ");
                            CtTypeReference<?> arg = args.get(i);
                            if (arg.getQualifiedName().startsWith(type.getQualifiedName() + "$")) {
                                sb.append("JObject");
                            } else {
                                sb.append(type(arg));
                            }
                        }
                        sb.append('>');
                        baseStr = sb.toString();
                    }
                    cppBases.add("public virtual " + baseStr);
                }
            }
            if (!cppBases.isEmpty()) {
                out.append(" : ").append(String.join(", ", cppBases));
            }
            out.append(" {\n");
            indent++;
            if (type instanceof CtClass<?> cls && cls.getSuperclass() != null
                    && !cls.getSuperclass().getQualifiedName().equals("java.lang.Object")
                    && !cls.getSuperclass().getQualifiedName().equals("java.lang.Record")
                    && isDeclaredType(cls.getSuperclass())) {
                String baseName = rawCustomType(cls.getSuperclass());
                CtType<?> baseType = cls.getSuperclass().getTypeDeclaration();
                if (baseType != null) {
                    Set<String> thisMethodNames = type.getMethods().stream().map(CtMethod::getSimpleName).collect(java.util.stream.Collectors.toSet());
                    Set<String> baseMethodNames = baseType.getMethods().stream().map(CtMethod::getSimpleName).collect(java.util.stream.Collectors.toSet());
                    for (String mName : thisMethodNames) {
                        if (baseMethodNames.contains(mName)) {
                            line("using " + baseName + "::" + id(mName) + ";");
                        }
                    }
                }
            }
            if (isNonStaticNested(type.getReference())) {
                line(id(type.getDeclaringType().getSimpleName()) + "* outer;");
            }
            for (CtType<?> nested : type.getNestedTypes()) {
                if (!isRuntimeType(nested.getSimpleName()) && !nested.isLocalType()) {
                    writeIndent();
                    emitTemplate(nested.getFormalCtTypeParameters());
                    out.append("struct ").append(id(nested.getSimpleName())).append(";\n");
                }
            }
            for (CtType<?> nested : type.getNestedTypes()) {
                if (!isRuntimeType(nested.getSimpleName())) emitType(nested);
            }
            for (CtField<?> field : type.getFields()) emitField(field);
            if (type instanceof CtClass<?> cls) {
                for (CtConstructor<?> constructor : cls.getConstructors()) emitConstructor(constructor, type);
            }
            for (CtMethod<?> method : type.getMethods()) {
                if (isPrimitiveIteratorMethod(method)) emitCppIterator(type);
                else emitMethod(method);
            }
            CtMethod<?> iterMethod = type.getMethods().stream().filter(m -> m.getSimpleName().equals("iterator") && m.getParameters().isEmpty()).findFirst().orElse(null);
            if (iterMethod != null) {
                String iterType = type(iterMethod.getType());
                line("struct CppIterator {");
                indent++;
                line(iterType + " it;");
                line("using ValueType = std::decay_t<decltype(it.next())>;");
                line("ValueType current_val{};");
                line("bool active = false;");
                line("CppIterator() = default;");
                line("explicit CppIterator(const " + iterType + "& iter) : it(iter) {");
                indent++;
                line("if (it.hasNext()) { current_val = it.next(); active = true; }");
                indent--;
                line("}");
                line("bool operator!=(const CppIterator&) const { return active; }");
                line("void operator++() {");
                indent++;
                line("if (it.hasNext()) { current_val = it.next(); }");
                line("else { active = false; }");
                indent--;
                line("}");
                line("ValueType operator*() const { return current_val; }");
                indent--;
                line("};");
                line("CppIterator begin() { return CppIterator(this->iterator()); }");
                line("CppIterator end() { return CppIterator(); }");
                line("CppIterator begin() const { return CppIterator(const_cast<std::decay_t<decltype(*this)>*>(this)->iterator()); }");
                line("CppIterator end() const { return CppIterator(); }");
            }
            emitInheritedInterfaceBridges(type);
            indent--;
            line("};");
            out.append('\n');
            currentType = previousType;
        }

        /** 型パラメータ列 P の型引数文字列表記（例: "<T, U>"）を返す。計算量: O(|P|)。 */
        // 未テスト
        private String templateArgs(List<CtTypeParameter> parameters) {
            if (parameters.isEmpty()) return "";
            StringBuilder sb = new StringBuilder("<");
            for (int i = 0; i < parameters.size(); i++) {
                if (i > 0) sb.append(", ");
                sb.append(typeParameterName(parameters.get(i)));
            }
            return sb.append(">").toString();
        }

        /** 型パラメータ列 P を出力する。計算量: O(|P|)。 */
        // 未テスト
        private void emitTemplate(List<CtTypeParameter> parameters) {
            if (parameters.isEmpty()) return;
            out.append("template <");
            for (int i = 0; i < parameters.size(); i++) {
                if (i > 0) out.append(", ");
                out.append("class ").append(typeParameterName(parameters.get(i)));
            }
            out.append(">\n");
        }

        /** フィールド f を出力する。計算量: O(|f|)。 */
        // 未テスト
        private void emitField(CtField<?> field) {
            StringBuilder s = new StringBuilder();
            boolean emittedStatic = field.hasModifier(ModifierKind.STATIC)
                    || field.getDeclaringType().getSimpleName().equals("Main");
            if (emittedStatic) {
                s.append("inline static ");
            }
            s.append(type(field.getType())).append(' ').append(fieldName(field.getReference()));
            boolean previousStaticContext = currentStaticContext;
            currentStaticContext = emittedStatic;
            if (field.getDefaultExpression() != null) {
                String value = initializer(field.getType(), field.getDefaultExpression());
                if (emittedStatic) value = value.replace("this->", "");
                s.append(" = ").append(value);
            }
            currentStaticContext = previousStaticContext;
            s.append(';');
            line(s.toString());
        }

        /** コンストラクタ c を出力する。計算量: O(|c|)。 */
        // 未テスト
        private void emitConstructor(CtConstructor<?> constructor, CtType<?> owner) {
            StringBuilder s = new StringBuilder(id(owner.getSimpleName())).append('(');
            if (isNonStaticNested(owner.getReference())) {
                s.append(id(owner.getDeclaringType().getSimpleName())).append("* outer_");
                if (!constructor.getParameters().isEmpty()) s.append(", ");
            }
            appendParameters(s, constructor.getParameters());
            s.append(')');
            List<String> initializers = new java.util.ArrayList<>();
            if (isNonStaticNested(owner.getReference())) initializers.add("outer(outer_)");
            if (!constructor.getBody().getStatements().isEmpty()
                    && constructor.getBody().getStatement(0) instanceof CtInvocation<?> invocation
                    && invocation.getExecutable().getSimpleName().equals("<init>")
                    && invocation.getExecutable().getDeclaringType() != null
                    && !invocation.getExecutable().getDeclaringType().getQualifiedName().equals("java.lang.Object")
                    && !invocation.getExecutable().getDeclaringType().getQualifiedName().equals("java.lang.Record")
                    && (invocation.getTarget() instanceof CtSuperAccess<?>
                    || !simpleName(invocation.getExecutable().getDeclaringType().getSimpleName())
                    .equals(simpleName(owner.getSimpleName())))) {
                String baseTypeStr = rawCustomType(invocation.getExecutable().getDeclaringType());
                if (owner instanceof CtClass<?> cls && cls.getSuperclass() != null && !cls.getSuperclass().getActualTypeArguments().isEmpty()) {
                    StringBuilder sb = new StringBuilder(baseTypeStr).append('<');
                    List<CtTypeReference<?>> superArgs = cls.getSuperclass().getActualTypeArguments();
                    for (int idx = 0; idx < superArgs.size(); idx++) {
                        if (idx > 0) sb.append(", ");
                        sb.append(type(superArgs.get(idx)));
                    }
                    sb.append('>');
                    baseTypeStr = sb.toString();
                } else if (!owner.getFormalCtTypeParameters().isEmpty()) {
                    baseTypeStr += templateArgs(owner.getFormalCtTypeParameters());
                }
                initializers.add(baseTypeStr + arguments(invocation.getArguments()));
            }
            if (!initializers.isEmpty()) s.append(" : ").append(String.join(", ", initializers));
            s.append(' ');
            writeIndent();
            out.append(s);
            emitBlock(constructor.getBody(), false, constructor.getParameters());
            out.append('\n');
        }

        /** メソッド m を出力する。計算量: O(|m|)。 */
        // 未テスト
        private void emitMethod(CtMethod<?> method) {
            StringBuilder s = new StringBuilder();
            if (!method.getFormalCtTypeParameters().isEmpty()) {
                writeIndent();
                emitTemplate(method.getFormalCtTypeParameters());
            }
            if (method.hasModifier(ModifierKind.STATIC)) s.append("static ");
            else s.append("virtual ");
            String methodName = method.getDeclaringType().getSimpleName().equals("Main")
                    && method.getSimpleName().equals("main") ? "javaMain" : id(method.getSimpleName());
            s.append(type(method.getType())).append(' ').append(methodName).append('(');
            appendParameters(s, method.getParameters());
            s.append(')');
            if (method.getSimpleName().equals("iterator") && method.getParameters().isEmpty()) {
                s.append(" const");
            }
            if (method.getBody() == null && method.getParameters().isEmpty()
                    && !method.getType().getQualifiedName().equals("void")
                    && method.getDeclaringType().getFields().stream()
                            .anyMatch(f -> f.getSimpleName().equals(method.getSimpleName()))) {
                s.append(" { return ").append(id(method.getSimpleName())).append("_; }");
                line(s.toString());
                return;
            }
            if (method.getBody() == null) {
                s.append(" = 0;");
                line(s.toString());
                return;
            }
            if (method.getDeclaringType().getDeclaringType() == null
                    && !method.getDeclaringType().isLocalType()
                    && method.getDeclaringType().getParent(CtBlock.class) == null
                    && method.getDeclaringType().getFormalCtTypeParameters().isEmpty()
                    && method.getFormalCtTypeParameters().isEmpty()) {
                s.append(';');
                line(s.toString());
                deferredMethods.add(method);
                return;
            }
            boolean previousStaticContext = currentStaticContext;
            currentStaticContext = method.hasModifier(ModifierKind.STATIC);
            writeIndent();
            out.append(s).append(' ');
            emitBlock(method.getBody(), false, method.getParameters());
            out.append('\n');
            currentStaticContext = previousStaticContext;
        }

        /** Java の基底クラス実装で満たされるインターフェースメソッドを C++ 上で中継する。計算量: O(I M^2)。 */
        // 未テスト
        private void emitInheritedInterfaceBridges(CtType<?> type) {
            if (!(type instanceof CtClass<?> cls) || cls.getSuperclass() == null) return;
            CtType<?> superclass = cls.getSuperclass().getTypeDeclaration();
            if (superclass == null) return;
            for (CtTypeReference<?> interfaceReference : type.getSuperInterfaces()) {
                CtType<?> interfaceType = interfaceReference.getTypeDeclaration();
                if (interfaceType == null) continue;
                for (CtMethod<?> required : interfaceType.getMethods()) {
                    if (required.getBody() != null || !type.getMethodsByName(required.getSimpleName()).isEmpty()) continue;
                    CtMethod<?> implementation = superclass.getMethodsByName(required.getSimpleName()).stream()
                            .filter(m -> m.getParameters().size() == required.getParameters().size()).findFirst().orElse(null);
                    if (implementation == null) continue;
                    StringBuilder s = new StringBuilder("virtual ").append(type(required.getType())).append(' ')
                            .append(id(required.getSimpleName())).append('(');
                    appendParameters(s, implementation.getParameters());
                    s.append(") { ");
                    if (!required.getType().getQualifiedName().equals("void")) s.append("return ");
                    s.append(rawCustomType(cls.getSuperclass())).append("::").append(id(required.getSimpleName())).append('(');
                    for (int i = 0; i < implementation.getParameters().size(); i++) {
                        if (i > 0) s.append(", ");
                        CtParameter<?> p = implementation.getParameters().get(i);
                        String parameterType = type(p.getType());
                        s.append(id(p.getSimpleName()));
                        if (isPassByReferenceType(parameterType)) s.append("_const");
                    }
                    s.append("); }");
                    line(s.toString());
                }
            }
        }

        /** 非テンプレートメソッド m のクラス外定義を出力する。計算量: O(|m|)。 */
        // 未テスト
        private void emitDeferredMethod(CtMethod<?> method) {
            CtType<?> previousType = currentType;
            boolean previousStaticContext = currentStaticContext;
            currentType = null;
            String returnType = type(method.getType());
            currentType = method.getDeclaringType();
            currentStaticContext = method.hasModifier(ModifierKind.STATIC);
            StringBuilder s = new StringBuilder(returnType).append(' ')
                    .append(id(method.getDeclaringType().getSimpleName())).append("::");
            String methodName = method.getDeclaringType().getSimpleName().equals("Main")
                    && method.getSimpleName().equals("main") ? "javaMain" : id(method.getSimpleName());
            s.append(methodName).append('(');
            appendParameters(s, method.getParameters());
            s.append(')');
            if (method.getSimpleName().equals("iterator") && method.getParameters().isEmpty()) {
                s.append(" const");
            }
            s.append(' ');
            out.append(s);
            emitBlock(method.getBody(), false, method.getParameters());
            out.append("\n\n");
            currentType = previousType;
            currentStaticContext = previousStaticContext;
        }

        private static final Set<String> CPP_PRIMITIVE_TYPES = Set.of(
                "void", "bool", "int8_t", "int16_t", "int", "long long", "float", "double", "char", "nullptr_t"
        );

        private boolean isPassByReferenceType(String parameterType) {
            return !CPP_PRIMITIVE_TYPES.contains(parameterType);
        }

        /** パラメータ列 P を s へ加える。計算量: O(|P|)。 */
        // 未テスト
        private void appendParameters(StringBuilder s, List<CtParameter<?>> parameters) {
            for (int i = 0; i < parameters.size(); i++) {
                if (i > 0) s.append(", ");
                CtParameter<?> p = parameters.get(i);
                String parameterType = type(p.getType());
                if (isPassByReferenceType(parameterType)) {
                    s.append("const ").append(parameterType).append("& ").append(id(p.getSimpleName())).append("_const");
                } else {
                    s.append(parameterType).append(' ').append(id(p.getSimpleName()));
                }
            }
        }

        /** ブロック b を出力する。計算量: O(|b|)。 */
        // 未テスト
        private void emitBlock(CtBlock<?> block, boolean indentOpening, List<CtParameter<?>> parameters) {
            if (indentOpening) writeIndent();
            out.append("{\n");
            indent++;
            if (parameters != null) {
                for (CtParameter<?> p : parameters) {
                    String parameterType = type(p.getType());
                    if (isPassByReferenceType(parameterType)) {
                        writeIndent();
                        out.append("auto& ").append(id(p.getSimpleName())).append(" = const_cast<").append(parameterType)
                           .append("&>(").append(id(p.getSimpleName())).append("_const);\n");
                    }
                }
            }
            for (CtStatement statement : block.getStatements()) emitStatement(statement);
            indent--;
            writeIndent();
            out.append('}');
        }

        private void emitBlock(CtBlock<?> block, boolean indentOpening) {
            emitBlock(block, indentOpening, null);
        }

        /** 文 s を出力する。計算量: O(|s|)。 */
        // 未テスト
        private List<CtLocalVariable<?>> findPatternVariables(CtElement element) {
            List<CtLocalVariable<?>> list = new java.util.ArrayList<>();
            if (element == null) return list;
            if (element instanceof spoon.reflect.code.CtTypePattern pattern) {
                if (pattern.getVariable() != null) list.add(pattern.getVariable());
            }
            for (CtElement child : element.getDirectChildren()) {
                list.addAll(findPatternVariables(child));
            }
            return list;
        }

        private void emitStatement(CtStatement s) {
            if (s instanceof CtComment) {
                return;
            } else if (s instanceof CtBlock<?> block) {
                emitBlock(block, true);
                out.append('\n');
            } else if (s instanceof CtLocalVariable<?> v) {
                CtMethod<?> parentMethod = v.getParent(CtMethod.class);
                boolean isReferencedInLocalType = false;
                if (parentMethod != null) {
                    for (CtType<?> localType : parentMethod.getElements(new TypeFilter<>(CtType.class))) {
                        boolean matches = localType.getElements(new TypeFilter<>(CtVariableAccess.class)).stream()
                                .anyMatch(va -> va.getVariable() != null && va.getVariable().getSimpleName().equals(v.getSimpleName()));
                        if (matches) {
                            isReferencedInLocalType = true;
                            break;
                        }
                    }
                }
                if (isReferencedInLocalType) {
                    line("static " + type(v.getType()) + " " + id(v.getSimpleName()) + ";");
                    if (v.getDefaultExpression() != null) {
                        line(id(v.getSimpleName()) + " = " + initializer(v.getType(), v.getDefaultExpression()) + ";");
                    }
                } else {
                    String varType = (v.getType() != null && (v.getType().isAnonymous() || v.isInferred() || v.getType().getSimpleName().contains("$"))) ? "auto" : type(v.getType());
                    line(varType + " " + id(v.getSimpleName())
                            + (v.getDefaultExpression() == null ? "" : " = " + initializer(v.getType(), v.getDefaultExpression())) + ";");
                }
            } else if (s instanceof CtAssert<?> ass) {
                line("assert(" + expr(ass.getAssertExpression()) + ");");
            } else if (s instanceof CtIf branch) {
                List<CtLocalVariable<?>> patternVars = findPatternVariables(branch);
                if (!patternVars.isEmpty()) {
                    writeIndent();
                    out.append("{\n");
                    indent++;
                    for (CtLocalVariable<?> v : patternVars) {
                        line(type(v.getType()) + " " + id(v.getSimpleName()) + "{};");
                    }
                    writeIndent();
                    out.append("if (").append(expr(branch.getCondition())).append(") ");
                    emitControlled(branch.getThenStatement());
                    if (branch.getElseStatement() != null) {
                        out.append(" else ");
                        emitControlled(branch.getElseStatement());
                    }
                    out.append('\n');
                    indent--;
                    writeIndent();
                    out.append("}\n");
                    return;
                }
                writeIndent();
                out.append("if (").append(expr(branch.getCondition())).append(") ");
                emitControlled(branch.getThenStatement());
                if (branch.getElseStatement() != null) {
                    out.append(" else ");
                    emitControlled(branch.getElseStatement());
                }
                out.append('\n');
            } else if (s instanceof CtFor loop) {
                writeIndent();
                out.append("for (");
                for (int i = 0; i < loop.getForInit().size(); i++) {
                    if (i > 0) out.append(", ");
                    CtStatement init = loop.getForInit().get(i);
                    if (i > 0 && init instanceof CtLocalVariable<?> variable
                            && loop.getForInit().get(0) instanceof CtLocalVariable<?> first
                            && type(first.getType()).equals(type(variable.getType()))) {
                        out.append(localInitializerWithoutType(variable));
                    } else {
                        out.append(statementInline(init));
                    }
                }
                out.append("; ").append(loop.getExpression() == null ? "" : expr(loop.getExpression())).append("; ");
                for (int i = 0; i < loop.getForUpdate().size(); i++) {
                    if (i > 0) out.append(", ");
                    out.append(statementInline(loop.getForUpdate().get(i)));
                }
                out.append(") ");
                emitControlled(loop.getBody());
                out.append('\n');
            } else if (s instanceof CtForEach loop) {
                writeIndent();
                String iterable = expr(loop.getExpression());
                if (loop.getExpression().getType() != null && isDeclaredType(loop.getExpression().getType())) {
                    iterable = "*(" + iterable + ")";
                }
                out.append("for (").append(type(loop.getVariable().getType())).append(' ')
                        .append(id(loop.getVariable().getSimpleName())).append(" : ").append(iterable).append(") ");
                emitControlled(loop.getBody());
                out.append('\n');
            } else if (s instanceof CtWhile loop) {
                writeIndent();
                out.append("while (").append(expr(loop.getLoopingExpression())).append(") ");
                emitControlled(loop.getBody());
                out.append('\n');
            } else if (s instanceof CtDo loop) {
                writeIndent();
                out.append("do ");
                emitControlled(loop.getBody());
                out.append(" while (").append(expr(loop.getLoopingExpression())).append(");\n");
            } else if (s instanceof CtReturn<?> ret) {
                CtMethod<?> method = ret.getParent(CtMethod.class);
                line("return" + (ret.getReturnedExpression() == null ? "" : " "
                        + initializer(method == null ? ret.getReturnedExpression().getType() : method.getType(),
                        ret.getReturnedExpression())) + ";");
            } else if (s instanceof CtBreak) {
                line("break;");
            } else if (s instanceof CtContinue) {
                line("continue;");
            } else if (s instanceof CtInvocation<?> invocation) {
                if (!invocation.getExecutable().getSimpleName().equals("<init>")) line(expr(invocation) + ";");
            } else if (s instanceof CtAssignment<?, ?> assignment) {
                line(expr(assignment) + ";");
            } else if (s instanceof CtUnaryOperator<?> unary) {
                line(expr(unary) + ";");
            } else if (s instanceof CtThrow thr) {
                CtExpression<?> thrown = thr.getThrownExpression();
                if (thrown instanceof CtConstructorCall<?> constructor) {
                    line("throw runtime_error(" + quote(constructor.getType().getSimpleName()) + ");");
                } else {
                    line("throw runtime_error(jstr(" + expr(thrown) + "));");
                }
            } else if (s instanceof CtSwitch<?> sw) {
                emitSwitch(sw);
            } else if (s instanceof CtTry attempt) {
                emitTry(attempt);
            } else if (s instanceof CtSynchronized sync) {
                emitStatement(sync.getBlock());
            } else if (s instanceof CtType<?> localType) {
                emitType(localType);
            } else {
                throw unsupported(s, "statement " + s.getClass().getSimpleName());
            }
        }

        /** 制御文の本体 s を出力する。計算量: O(|s|)。 */
        // 未テスト
        private void emitControlled(CtStatement s) {
            if (s == null) {
                out.append("{}");
                return;
            }
            if (s instanceof CtBlock<?> block) emitBlock(block, false);
            else {
                out.append("{\n");
                indent++;
                emitStatement(s);
                indent--;
                writeIndent();
                out.append('}');
            }
        }

        /** switch 文 s を出力する。計算量: O(|s|)。 */
        // 未テスト
        private void emitSwitch(CtSwitch<?> s) {
            writeIndent();
            out.append("switch (").append(expr(s.getSelector())).append(") {\n");
            indent++;
            for (CtCase<?> c : s.getCases()) {
                writeIndent();
                if (c.getCaseExpressions().isEmpty()) out.append("default:\n");
                else for (CtExpression<?> label : c.getCaseExpressions()) {
                    out.append("case ").append(expr(label)).append(":\n");
                }
                indent++;
                for (CtStatement statement : c.getStatements()) emitStatement(statement);
                indent--;
            }
            indent--;
            line("}");
        }

        /** try 文 t を出力する。計算量: O(|t|)。 */
        // 未テスト
        private void emitTry(CtTry t) {
            writeIndent();
            out.append("try ");
            emitBlock(t.getBody(), false);
            for (CtCatch catcher : t.getCatchers()) {
                out.append(" catch (const exception& ").append(id(catcher.getParameter().getSimpleName())).append(") ");
                emitBlock(catcher.getBody(), false);
            }
            out.append('\n');
            if (t.getFinalizer() != null) for (CtStatement s : t.getFinalizer().getStatements()) emitStatement(s);
        }

        /** for の節にある文 s を返す。計算量: O(|s|)。 */
        // 未テスト
        private String statementInline(CtStatement s) {
            if (s instanceof CtLocalVariable<?> v) {
                return type(v.getType()) + " " + id(v.getSimpleName())
                        + (v.getDefaultExpression() == null ? "" : " = " + initializer(v.getType(), v.getDefaultExpression()));
            }
            if (s instanceof CtExpression<?> e) return expr(e);
            String text = s.toString();
            return text.endsWith(";") ? text.substring(0, text.length() - 1) : text;
        }

        /** for 初期化子 v を型名なしで返す。計算量: O(|v|)。 */
        // 未テスト
        private String localInitializerWithoutType(CtLocalVariable<?> v) {
            return id(v.getSimpleName()) + (v.getDefaultExpression() == null ? ""
                    : " = " + initializer(v.getType(), v.getDefaultExpression()));
        }

        /** 式 e を C++ 式へ変換する。計算量: O(|e|)。 */
        // 未テスト
        private String expr(CtExpression<?> e) {
            if (e == null) return "";
            if (!e.getTypeCasts().isEmpty()) {
                CtTypeReference<?> cast = e.getTypeCasts().get(e.getTypeCasts().size() - 1);
                CtExpression<?> copy = e.clone();
                copy.getTypeCasts().clear();
                if (cast instanceof CtArrayTypeReference<?> arrayCast
                        && arrayCast.getComponentType() instanceof CtTypeParameterReference
                        && copy instanceof CtNewArray<?> newArray && !newArray.getDimensionExpressions().isEmpty()) {
                    return vectorAllocation(cast, newArray.getDimensionExpressions(), 0);
                }
                if (e.getType() != null && e.getType().getQualifiedName().equals("java.lang.Object")) {
                    return "any_cast<" + type(cast) + ">(" + expr(copy) + ")";
                }
                String castType = type(cast);
                if (castType.startsWith("shared_ptr<") && castType.endsWith(">")) {
                    String innerType = castType.substring("shared_ptr<".length(), castType.length() - 1);
                    return "static_pointer_cast<" + innerType + ">(" + expr(copy) + ")";
                }
                return "static_cast<" + castType + ">(" + expr(copy) + ")";
            }
            if (e instanceof CtLiteral<?> literal) return literal(literal);
            if (e instanceof CtFieldAccess<?> field) return field(field);
            if (e instanceof CtThisAccess<?>) return "this->shared_from_this()";
            if (e instanceof CtSuperAccess<?>) return "this";
            if (e instanceof CtVariableAccess<?> variable) {
                if (variable.getVariable() instanceof CtFieldReference<?> field) return fieldName(field);
                return id(variable.getVariable().getSimpleName());
            }
            if (e instanceof CtTypeAccess<?> access) return typeAccess(access.getAccessedType());
            if (e instanceof CtArrayAccess<?, ?> access) return expr(access.getTarget()) + "[" + expr(access.getIndexExpression()) + "]";
            if (e instanceof CtOperatorAssignment<?, ?> assignment) {
                if (assignment.getAssigned().getType() != null
                        && assignment.getAssigned().getType().getQualifiedName().equals("boolean")
                        && (assignment.getKind() == spoon.reflect.code.BinaryOperatorKind.BITOR
                        || assignment.getKind() == spoon.reflect.code.BinaryOperatorKind.BITAND)) {
                    String assigned = expr(assignment.getAssigned());
                    return "(" + assigned + " = (" + assigned + " " + binarySymbol(assignment.getKind()) + " "
                            + expr(assignment.getAssignment()) + "))";
                }
                return "(" + expr(assignment.getAssigned()) + " " + binarySymbol(assignment.getKind()) + "= "
                        + expr(assignment.getAssignment()) + ")";
            }
            if (e instanceof CtAssignment<?, ?> assignment) {
                return "(" + expr(assignment.getAssigned()) + " = "
                        + initializer(assignment.getAssigned().getType(), assignment.getAssignment()) + ")";
            }
            if (e instanceof CtBinaryOperator<?> binary) {
                String nullComparison = nullArrayComparison(binary);
                if (nullComparison != null) return nullComparison;
                if ((binary.getKind() == spoon.reflect.code.BinaryOperatorKind.EQ
                        || binary.getKind() == spoon.reflect.code.BinaryOperatorKind.NE)
                        && (binary.getLeftHandOperand() instanceof CtThisAccess<?>
                        || binary.getRightHandOperand() instanceof CtThisAccess<?>)) {
                    CtExpression<?> other = binary.getLeftHandOperand() instanceof CtThisAccess<?>
                            ? binary.getRightHandOperand() : binary.getLeftHandOperand();
                    if (other != null && isDeclaredType(other.getType())) {
                        String equality = "(" + expr(other) + ".get() == this)";
                        return binary.getKind() == spoon.reflect.code.BinaryOperatorKind.NE
                                ? "(!" + equality + ")" : equality;
                    }
                }
                if ((binary.getKind() == spoon.reflect.code.BinaryOperatorKind.EQ
                        || binary.getKind() == spoon.reflect.code.BinaryOperatorKind.NE)
                        && (binary.getLeftHandOperand() instanceof CtThisAccess<?>
                        || binary.getRightHandOperand() instanceof CtThisAccess<?>)
                        && (isObjectExpression(binary.getLeftHandOperand()) || isObjectExpression(binary.getRightHandOperand()))) {
                    return binary.getKind() == spoon.reflect.code.BinaryOperatorKind.NE ? "true" : "false";
                }
                if (binary.getKind() == spoon.reflect.code.BinaryOperatorKind.INSTANCEOF) {
                    CtExpression<?> rhs = binary.getRightHandOperand();
                    if (rhs instanceof CtTypeAccess<?> access) {
                        return "jisInstance<" + type(access.getAccessedType()) + ">(" + expr(binary.getLeftHandOperand()) + ")";
                    }
                    if (rhs instanceof spoon.reflect.code.CtTypePattern pattern) {
                        CtLocalVariable<?> v = pattern.getVariable();
                        if (v != null) {
                            return "jisInstance(" + expr(binary.getLeftHandOperand()) + ", " + id(v.getSimpleName()) + ")";
                        }
                        return "jisInstance<" + type(pattern.getType()) + ">(" + expr(binary.getLeftHandOperand()) + ")";
                    }
                    return "jisInstance<" + type(rhs.getType()) + ">(" + expr(binary.getLeftHandOperand()) + ")";
                }
            if (binary.getKind() == spoon.reflect.code.BinaryOperatorKind.MOD
                    && (isDoubleType(binary.getType()) || isDoubleType(binary.getLeftHandOperand().getType()) || isDoubleType(binary.getRightHandOperand().getType()))) {
                return "fmod(" + expr(binary.getLeftHandOperand()) + ", " + expr(binary.getRightHandOperand()) + ")";
            }
                if (binary.getKind() == spoon.reflect.code.BinaryOperatorKind.PLUS && binary.getType() != null
                        && binary.getType().getQualifiedName().equals("java.lang.String")) {
                    return "jconcat(" + expr(binary.getLeftHandOperand()) + ", " + expr(binary.getRightHandOperand()) + ")";
                }
                return "(" + expr(binary.getLeftHandOperand()) + " " + binarySymbol(binary.getKind()) + " "
                        + expr(binary.getRightHandOperand()) + ")";
            }
            if (e instanceof CtUnaryOperator<?> unary) return unary(unary);
            if (e instanceof CtConditional<?> conditional) {
                CtExpression<?> thenE = conditional.getThenExpression();
                CtExpression<?> elseE = conditional.getElseExpression();
                boolean thenIsString = isJavaString(thenE.getType())
                        || (thenE instanceof CtLiteral<?> lit && lit.getValue() instanceof String);
                boolean elseIsString = isJavaString(elseE.getType())
                        || (elseE instanceof CtLiteral<?> lit && lit.getValue() instanceof String);
                boolean condIsString = isJavaString(conditional.getType());
                if (thenIsString || elseIsString || condIsString) {
                    String thenStr = thenIsString ? expr(thenE) : "jstr(" + expr(thenE) + ")";
                    String elseStr = elseIsString ? expr(elseE) : "jstr(" + expr(elseE) + ")";
                    return "(" + expr(conditional.getCondition()) + " ? " + thenStr + " : " + elseStr + ")";
                }
                return "(" + expr(conditional.getCondition()) + " ? "
                        + initializer(conditional.getType(), thenE)
                        + " : " + initializer(conditional.getType(), elseE) + ")";
            }
            if (e instanceof CtConstructorCall<?> constructor) return constructor(constructor);
            if (e instanceof CtNewArray<?> array) return newArray(array);
            if (e instanceof CtInvocation<?> invocation) return invocation(invocation);
            if (e instanceof CtLambda<?> lambda) return lambda(lambda);
            if (e instanceof CtExecutableReferenceExpression<?, ?> reference) return executableReference(reference);
            throw unsupported(e, "expression " + e.getClass().getSimpleName());
        }

        /** リテラル v を返す。計算量: O(|v|)。 */
        // 未テスト
        private String literal(CtLiteral<?> v) {
            Object value = v.getValue();
            if (value == null) {
                if (v.getType() instanceof CtArrayTypeReference<?>) return type(v.getType()) + "{}";
                return "nullptr";
            }
            if (value instanceof String text) return "JString(" + quote(text) + ")";
            if (value instanceof Character c) {
                char ch = c;
                return switch (ch) {
                    case '\'' -> "'\\''";
                    case '\\' -> "'\\\\'";
                    case '\n' -> "'\\n'";
                    case '\r' -> "'\\r'";
                    case '\t' -> "'\\t'";
                    case '\0' -> "'\\0'";
                    default -> "'" + ch + "'";
                };
            }
            if (value instanceof Long) return value + "LL";
            if (value instanceof Float) return value + "f";
            return value.toString();
        }

        /** 文字列 s の C++ リテラルを返す。計算量: O(|s|)。 */
        // 未テスト
        private String quote(String s) {
            return "\"" + s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n")
                    .replace("\r", "\\r").replace("\t", "\\t") + "\"";
        }

        /** フィールドアクセス f を返す。計算量: O(|f|)。 */
        // 未テスト
        private String field(CtFieldAccess<?> f) {
            String name = f.getVariable().getSimpleName();
            if (f.getTarget() instanceof CtTypeAccess<?> access
                    && access.getAccessedType().getQualifiedName().equals("java.lang.System")) {
                if (name.equals("in")) return "JInputStream()";
                if (name.equals("out") || name.equals("err")) return "JPrintWriter()";
            }
            if (name.equals("length") && f.getTarget() != null) {
                return "static_cast<int>(" + expr(f.getTarget()) + ".size())";
            }
            CtField<?> declaration = f.getVariable().getDeclaration();
            if (declaration != null && declaration.hasModifier(ModifierKind.STATIC)) {
                if (f.getTarget() == null || f.getTarget() instanceof CtThisAccess<?>) {
                    return id(declaration.getDeclaringType().getSimpleName()) + "::" + fieldName(f.getVariable());
                }
            }
            if (declaration != null && currentType != null && currentType.getDeclaringType() != null
                    && declaration.getDeclaringType() == currentType.getDeclaringType()
                    && declaration.getDeclaringType() != currentType) {
                if (declaration.hasModifier(ModifierKind.STATIC) || currentStaticContext || currentType.hasModifier(ModifierKind.STATIC)) {
                    return id(declaration.getDeclaringType().getSimpleName()) + "::" + fieldName(f.getVariable());
                }
                return "outer->" + fieldName(f.getVariable());
            }
            if (f.getTarget() == null) return fieldName(f.getVariable());
            if (f.getTarget() instanceof CtThisAccess<?>) {
                return currentStaticContext ? fieldName(f.getVariable()) : "this->" + fieldName(f.getVariable());
            }
            String separator = f.getTarget() instanceof CtTypeAccess<?> ? "::" : memberSeparator(f.getTarget());
            String target = expr(f.getTarget());
            if (target.isEmpty() && separator.equals("->")) target = "this";
            return target + separator + fieldName(f.getVariable());
        }

        /** フィールド参照 f の衝突しない C++ 名を返す。計算量: O(M)。M は所有型のメソッド数。 */
        // 未テスト
        private String fieldName(CtFieldReference<?> f) {
            CtField<?> declaration = f.getDeclaration();
            if (declaration == null && currentType != null) {
                declaration = currentType.getField(f.getSimpleName());
            }
            if (declaration == null && f.getDeclaringType() != null && f.getDeclaringType().getTypeDeclaration() != null) {
                declaration = f.getDeclaringType().getTypeDeclaration().getField(f.getSimpleName());
            }
            boolean hasMethodConflict = false;
            if (declaration != null) {
                hasMethodConflict = !declaration.getDeclaringType().getMethodsByName(f.getSimpleName()).isEmpty();
            } else if (f.getDeclaringType() != null && f.getDeclaringType().getTypeDeclaration() != null) {
                hasMethodConflict = !f.getDeclaringType().getTypeDeclaration().getMethodsByName(f.getSimpleName()).isEmpty();
            } else if (currentType != null) {
                hasMethodConflict = !currentType.getMethodsByName(f.getSimpleName()).isEmpty();
            }
            if (hasMethodConflict || model.getAllTypes().stream().anyMatch(t -> !t.getMethodsByName(f.getSimpleName()).isEmpty())) {
                return id(f.getSimpleName()) + "_";
            }
            return id(f.getSimpleName());
        }

        /** e の静的型が Object なら true を返す。計算量: O(1)。 */
        // 未テスト
        private boolean isObjectExpression(CtExpression<?> e) {
            return e != null && e.getType() != null && e.getType().getQualifiedName().equals("java.lang.Object");
        }

        /** 呼出し i を返す。計算量: O(|i|)。 */
        // 未テスト
        private String invocation(CtInvocation<?> i) {
            String qualified = i.getExecutable().getDeclaringType() == null ? ""
                    : i.getExecutable().getDeclaringType().getQualifiedName();
            String name = i.getExecutable().getSimpleName();
            if (currentAnonymousMethod != null && name.equals(currentAnonymousMethod.getSimpleName())
                    && (i.getTarget() == null || i.getTarget() instanceof CtThisAccess<?>)) {
                return "__self(__self" + (i.getArguments().isEmpty() ? ")" : ", " + arguments(i.getArguments()).substring(1));
            }
            if (i.getTarget() instanceof CtSuperAccess<?> && i.getExecutable().getDeclaringType() != null) {
                return rawCustomType(i.getExecutable().getDeclaringType()) + "::" + id(name) + arguments(i.getArguments());
            }
            if ((qualified.equals("java.util.List") || simpleTargetName(i.getTarget()).equals("List")) && name.equals("of")) {
                return "jlistOf" + arguments(i.getArguments());
            }
            if ((qualified.equals("java.util.Set") || simpleTargetName(i.getTarget()).equals("Set")) && name.equals("of")) {
                return "jsetOf" + arguments(i.getArguments());
            }
            if ((qualified.equals("java.lang.System") || qualified.equals("java.io.PrintStream"))
                    && (name.equals("println") || name.equals("print"))) {
                return (name.equals("println") ? "jprintln" : "jprint") + arguments(i.getArguments());
            }
            if ((qualified.equals("java.lang.System") || qualified.equals("java.io.PrintStream"))
                    && (name.equals("printf") || name.equals("format"))) {
                return "jprintf" + arguments(i.getArguments());
            }
            if ((qualified.equals("java.lang.System") || qualified.equals("java.io.PrintStream"))
                    && name.equals("flush")) {
                return "cout.flush()";
            }
            if (qualified.equals("java.lang.System") && name.equals("arraycopy")) {
                return "jarraycopy" + arguments(i.getArguments());
            }
            if (qualified.equals("java.lang.System") && name.equals("currentTimeMillis")) {
                return "jcurrentTimeMillis()";
            }
            if (qualified.equals("java.lang.System") && name.equals("exit")) {
                return "exit" + arguments(i.getArguments());
            }
            if ((qualified.equals("java.lang.System") || simpleTargetName(i.getTarget()).equals("System")) && name.equals("gc")) {
                return "(void)0";
            }
            if (name.equals("printStackTrace")) {
                return "(void)0";
            }
            if (qualified.equals("java.lang.Math")) return mathInvocation(name, i.getArguments());
            if (qualified.equals("java.util.Arrays")) return arraysInvocation(name, i.getArguments());
            if (qualified.equals("java.util.Comparator")
                    && (name.equals("comparing") || name.equals("comparingInt") || name.equals("comparingLong"))) {
                return "jcomparing" + arguments(i.getArguments());
            }
            if ((simpleName(qualified).equals("ArrayUtils")
                    || i.getTarget() instanceof CtTypeAccess<?> access
                    && simpleName(access.getAccessedType().getSimpleName()).equals("ArrayUtils"))
                    && (name.equals("min") || name.equals("max"))
                    && i.getArguments().size() == 2) {
                return name + arguments(i.getArguments());
            }
            if ((qualified.equals("java.lang.String") || simpleTargetName(i.getTarget()).equals("String"))
                    && name.equals("format")) {
                return "jformat" + arguments(i.getArguments());
            }
            if (qualified.equals("java.lang.String") && name.equals("valueOf")) {
                return "jstr(" + expr(i.getArguments().get(0)) + ")";
            }
            if (qualified.equals("java.lang.String") && name.equals("repeat") && i.getTarget() != null) {
                return "jrepeat(" + expr(i.getTarget()) + ", " + expr(i.getArguments().get(0)) + ")";
            }
            if (qualified.equals("java.lang.String") && name.equals("toCharArray") && i.getTarget() != null) {
                return "jchars(" + expr(i.getTarget()) + ")";
            }
            if (qualified.equals("java.lang.String") && name.equals("charAt") && i.getTarget() != null) {
                return expr(i.getTarget()) + "[" + expr(i.getArguments().get(0)) + "]";
            }
            if (name.equals("compareTo") && i.getTarget() != null && i.getArguments().size() == 1) {
                return "jcompareTo(" + expr(i.getTarget()) + ", " + expr(i.getArguments().get(0)) + ")";
            }
            if (qualified.equals("java.lang.Character") && name.equals("toUpperCase")
                    && i.getArguments().size() == 1) {
                return "jtoUpperCase(" + expr(i.getArguments().get(0)) + ")";
            }
            if (qualified.equals("java.lang.String") && name.equals("toUpperCase")
                    && i.getTarget() != null && i.getArguments().isEmpty()) {
                return "jtoUpperCase(" + expr(i.getTarget()) + ")";
            }
            if (qualified.equals("java.lang.Character") && name.equals("toLowerCase")
                    && i.getArguments().size() == 1) {
                return "jtoLowerCase(" + expr(i.getArguments().get(0)) + ")";
            }
            if (qualified.equals("java.lang.String") && name.equals("toLowerCase")
                    && i.getTarget() != null && i.getArguments().isEmpty()) {
                return "jtoLowerCase(" + expr(i.getTarget()) + ")";
            }
            if (qualified.equals("java.lang.String") && name.equals("equalsIgnoreCase")
                    && i.getTarget() != null && i.getArguments().size() == 1) {
                return "jequalsIgnoreCase(" + expr(i.getTarget()) + ", " + expr(i.getArguments().get(0)) + ")";
            }
            if (name.equals("replaceAll") && i.getTarget() != null && i.getArguments().size() == 2) {
                return "jreplaceAll(" + expr(i.getTarget()) + ", " + expr(i.getArguments().get(0)) + ", " + expr(i.getArguments().get(1)) + ")";
            }
            if (name.equals("replace") && i.getTarget() != null && i.getArguments().size() == 2) {
                return "jreplace(" + expr(i.getTarget()) + ", " + expr(i.getArguments().get(0)) + ", " + expr(i.getArguments().get(1)) + ")";
            }
            if (name.equals("startsWith") && i.getTarget() != null && i.getArguments().size() == 1) {
                return "jstartsWith(" + expr(i.getTarget()) + ", " + expr(i.getArguments().get(0)) + ")";
            }
            if (name.equals("endsWith") && i.getTarget() != null && i.getArguments().size() == 1) {
                return "jendsWith(" + expr(i.getTarget()) + ", " + expr(i.getArguments().get(0)) + ")";
            }
            if (name.equals("compare") && i.getTarget() != null && i.getArguments().size() == 2) {
                if (i.getTarget() instanceof CtTypeAccess<?>) {
                    return expr(i.getTarget()) + "::" + id(name) + arguments(i.getArguments());
                }
                return expr(i.getTarget()) + arguments(i.getArguments());
            }
            if (qualified.equals("java.lang.String") && name.equals("getBytes") && i.getTarget() != null) {
                return "jchars(" + expr(i.getTarget()) + ")";
            }
            if (name.equals("isEmpty") && i.getTarget() != null && i.getArguments().isEmpty()
                    && (isJavaString(i.getTarget().getType()) || isContainerType(i.getTarget().getType()))) {
                return expr(i.getTarget()) + ".empty()";
            }
            if (qualified.equals("java.lang.String") && name.equals("split") && i.getTarget() != null) {
                return "jsplit(" + expr(i.getTarget()) + ", " + expr(i.getArguments().get(0)) + ")";
            }
            if ((qualified.equals("java.lang.String") || i.getTarget() != null && isJavaString(i.getTarget().getType())) && name.equals("indexOf") && i.getTarget() != null) {
                if (i.getArguments().size() == 1) {
                    return "jindexOf(" + expr(i.getTarget()) + ", " + expr(i.getArguments().get(0)) + ")";
                } else if (i.getArguments().size() == 2) {
                    return "jindexOf(" + expr(i.getTarget()) + ", " + expr(i.getArguments().get(0)) + ", " + expr(i.getArguments().get(1)) + ")";
                }
            }
            if ((qualified.equals("java.lang.String") || i.getTarget() != null && isJavaString(i.getTarget().getType())) && name.equals("lastIndexOf") && i.getTarget() != null) {
                if (i.getArguments().size() == 1) {
                    return "jlastIndexOf(" + expr(i.getTarget()) + ", " + expr(i.getArguments().get(0)) + ")";
                } else if (i.getArguments().size() == 2) {
                    return "jlastIndexOf(" + expr(i.getTarget()) + ", " + expr(i.getArguments().get(0)) + ", " + expr(i.getArguments().get(1)) + ")";
                }
            }
            if (qualified.equals("java.lang.String") && name.equals("substring") && i.getTarget() != null) {
                if (i.getArguments().size() == 1) {
                    return "jsubstring(" + expr(i.getTarget()) + ", " + expr(i.getArguments().get(0)) + ")";
                } else if (i.getArguments().size() == 2) {
                    return "jsubstring(" + expr(i.getTarget()) + ", " + expr(i.getArguments().get(0)) + ", " + expr(i.getArguments().get(1)) + ")";
                }
            }
            if (qualified.equals("java.util.Objects") && name.equals("hash")) {
                return "jhash" + arguments(i.getArguments());
            }
            if (qualified.equals("java.util.Objects") && name.equals("hashCode")) {
                return "static_cast<int>(jhash(" + expr(i.getArguments().get(0)) + "))";
            }
            if (qualified.equals("java.util.Objects") && name.equals("equals")) {
                return "(" + expr(i.getArguments().get(0)) + " == " + expr(i.getArguments().get(1)) + ")";
            }
            if (name.equals("equals") && i.getArguments().size() == 1 && i.getTarget() != null
                    && supportsCppValueEquality(i.getTarget().getType())) {
                return "(" + expr(i.getTarget()) + " == " + expr(i.getArguments().get(0)) + ")";
            }
            if (i.getTarget() != null && isContainerType(i.getTarget().getType()) && name.equals("toArray")) {
                return "jtoArray(" + expr(i.getTarget()) + ")";
            }
            if (name.equals("iterator") && i.getTarget() != null && i.getType() != null
                    && i.getType().getQualifiedName().equals("java.util.Iterator")) {
                return "JIterator(" + expr(i.getTarget()) + ")";
            }
            if ((qualified.equals("java.lang.Integer") || qualified.equals("java.lang.Long"))
                    && (name.equals("intValue") || name.equals("longValue")) && i.getTarget() != null) {
                return expr(i.getTarget());
            }
            if (name.equals("getClass")) {
                if (i.getTarget() != null && isObjectExpression(i.getTarget())) return expr(i.getTarget()) + ".type()";
                return "typeid(shared_ptr<" + id(currentType.getSimpleName()) + ">)";
            }
            if (i.getTarget() != null && i.getTarget().getType() instanceof CtArrayTypeReference<?>) {
                if (name.equals("clone")) return expr(i.getTarget());
                if (name.equals("toString")) return "jstr(" + expr(i.getTarget()) + ")";
            }
            if (i.getTarget() != null && i.getTarget().getType() instanceof CtTypeParameterReference
                    && name.equals("toString")) {
                return "jstr(" + expr(i.getTarget()) + ")";
            }
            if (qualified.equals("java.util.Map$Entry") && i.getTarget() != null) {
                if (name.equals("getKey")) return expr(i.getTarget()) + ".first";
                if (name.equals("getValue")) return expr(i.getTarget()) + ".second";
            }
            if (qualified.equals("java.lang.Thread") && name.equals("setDefaultUncaughtExceptionHandler")) return "jnoop()";
            if (qualified.equals("java.lang.Runtime") && name.equals("getRuntime")) return "JRuntime()";
            String target = i.getTarget() == null || i.getTarget() instanceof CtThisAccess<?> ? "" : expr(i.getTarget());
            if (target.isEmpty() && !i.getExecutable().isStatic()
                    && i.getExecutable().getDeclaringType() != null
                    && isDeclaredType(i.getExecutable().getDeclaringType())) {
                target = outerTarget(i.getExecutable().getDeclaringType());
            }
            if ((target.isEmpty() || target.equals("this")) && currentType != null && !currentType.getMethodsByName(name).isEmpty()) {
                target = "this";
            } else if ((target.isEmpty() || target.equals("this")) && currentType != null && currentType.getDeclaringType() != null
                    && currentType.getMethodsByName(name).isEmpty()
                    && !currentType.getDeclaringType().getMethodsByName(name).isEmpty()) {
                if (i.getExecutable().isStatic() || currentType.getDeclaringType().getMethodsByName(name).stream().anyMatch(m -> m.isStatic())) {
                    target = id(currentType.getDeclaringType().getSimpleName());
                } else {
                    target = "outer";
                }
            }
            if (i.getTarget() instanceof CtTypeAccess<?> access && isDeclaredType(access.getAccessedType())
                    && access.getAccessedType().getActualTypeArguments().isEmpty() && i.getType() != null
                    && simpleName(access.getAccessedType().getSimpleName()).equals(simpleName(i.getType().getSimpleName()))
                    && !i.getType().getActualTypeArguments().isEmpty()) {
                target = rawCustomType(i.getType());
            }
            if (runtimeWriterTypes.contains(simpleTargetName(i.getTarget())) && name.equals("getInstance")) return "JPrintWriter()";
            if (runtimeScannerTypes.contains(simpleTargetName(i.getTarget())) && name.equals("getInstance")) return "JScanner()";
            if (isFunctionalInvocation(qualified, name) && i.getTarget() != null) {
                return target + arguments(i.getArguments());
            }
            if ((i.getTarget() == null || i.getTarget() instanceof CtThisAccess<?>)
                    && currentType != null && currentType.getDeclaringType() != null
                    && !currentType.hasModifier(ModifierKind.STATIC)) {
                boolean declaredHere = currentType.getMethods().stream().anyMatch(method ->
                        method.getSimpleName().equals(name)
                        && method.getParent(CtType.class) == currentType);
                if (!declaredHere) target = "outer";
            }
            String separator = i.getTarget() instanceof CtTypeAccess<?> ? "::" : memberSeparator(i.getTarget());
            if (target.equals("this")) separator = "->";
            List<CtTypeReference<?>> typeArgs = !i.getActualTypeArguments().isEmpty()
                    ? i.getActualTypeArguments()
                    : i.getExecutable().getActualTypeArguments();
            if (typeArgs.isEmpty() && i.getType() != null) {
                if (i.getType() instanceof CtArrayTypeReference<?> arrayRef && !arrayRef.getComponentType().getActualTypeArguments().isEmpty()) {
                    typeArgs = arrayRef.getComponentType().getActualTypeArguments();
                } else if (!i.getType().getActualTypeArguments().isEmpty()) {
                    typeArgs = i.getType().getActualTypeArguments();
                }
            }
            String typeArgsStr = "";
            if (!typeArgs.isEmpty() && i.getExecutable().getDeclaration() instanceof CtMethod<?> method
                    && !method.getFormalCtTypeParameters().isEmpty()) {
                StringBuilder sb = new StringBuilder("<");
                for (int idx = 0; idx < typeArgs.size(); idx++) {
                    if (idx > 0) sb.append(", ");
                    sb.append(type(typeArgs.get(idx)));
                }
                sb.append(">");
                typeArgsStr = sb.toString();
            }
            return (target.isEmpty() ? "" : target + separator) + id(name) + typeArgsStr + invocationArguments(i);
        }

        /** 呼出し i の引数を可変長引数を含めて返す。計算量: O(A)。 */
        // 未テスト
        private String invocationArguments(CtInvocation<?> i) {
            CtElement declaration = i.getExecutable().getDeclaration();
            CtMethod<?> method = declaration instanceof CtMethod<?> m ? m : null;
            if (method != null && !method.getParameters().isEmpty()
                    && method.getParameters().get(method.getParameters().size() - 1).isVarArgs()) {
                int fixed = method.getParameters().size() - 1;
                CtTypeReference<?> varArgType = method.getParameters().get(fixed).getType();
                String elemType = varArgType instanceof CtArrayTypeReference<?> arrayRef ? type(arrayRef.getComponentType()) : "auto";
                StringBuilder s = new StringBuilder("(");
                for (int j = 0; j < fixed; j++) {
                    if (j > 0) s.append(", ");
                    CtTypeReference<?> paramType = method.getParameters().get(j).getType();
                    s.append(initializer(paramType, i.getArguments().get(j)));
                }
                if (fixed > 0 && !i.getArguments().isEmpty()) s.append(", ");
                s.append("vector<").append(elemType).append(">{");
                for (int j = fixed; j < i.getArguments().size(); j++) {
                    if (j > fixed) s.append(", ");
                    s.append(expr(i.getArguments().get(j)));
                }
                return s.append("})").toString();
            }

            StringBuilder s = new StringBuilder("(");
            for (int j = 0; j < i.getArguments().size(); j++) {
                if (j > 0) s.append(", ");
                CtTypeReference<?> paramType = (method != null && j < method.getParameters().size())
                        ? method.getParameters().get(j).getType()
                        : (j < i.getExecutable().getParameters().size() ? i.getExecutable().getParameters().get(j) : null);
                s.append(paramType != null ? initializer(paramType, i.getArguments().get(j)) : argumentExpr(i.getArguments().get(j)));
            }
            return s.append(')').toString();
        }

        /** Math の呼出しを返す。計算量: O(A)。A は引数式の長さ。 */
        // 未テスト
        private String mathInvocation(String name, List<CtExpression<?>> args) {
            if (name.equals("random")) return "JRandom().nextDouble()";
            String mapped = switch (name) {
                case "min", "max", "signum", "floorDiv", "ceilDiv", "floorMod", "clamp",
                        "addExact", "subtractExact", "multiplyExact", "toIntExact", "negateExact" -> "j" + name;
                case "abs", "sqrt", "cbrt", "pow", "floor", "ceil", "round", "log", "log10", "log2", "exp",
                        "sin", "cos", "tan", "asin", "acos", "atan", "atan2", "hypot", "rint" -> "std::" + name;
                default -> throw new IllegalArgumentException("Unsupported Math method: " + name);
            };
            return mapped + arguments(args);
        }

        private String constructorArguments(CtConstructorCall<?> c) {
            CtConstructor<?> declaration = c.getExecutable().getDeclaration() instanceof CtConstructor<?> cons ? cons : null;
            if (declaration == null && c.getType() != null) {
                String simpleName = simpleName(c.getType().getSimpleName());
                CtType<?> typeDecl = model.getAllTypes().stream()
                        .filter(t -> t.getSimpleName().equals(simpleName))
                        .findFirst().orElse(null);
                if (typeDecl instanceof CtClass<?> classDecl) {
                    declaration = classDecl.getConstructors().stream()
                            .filter(cons -> cons.getParameters().size() == c.getArguments().size()
                                    || (!cons.getParameters().isEmpty() && cons.getParameters().get(cons.getParameters().size() - 1).isVarArgs())
                                    || (cons.getParameters().size() == 1 && cons.getParameters().get(0).getType() instanceof CtArrayTypeReference<?>))
                            .findFirst().orElse(null);
                }
            }
            boolean isVarArgCall = declaration != null && !declaration.getParameters().isEmpty()
                    && (declaration.getParameters().get(declaration.getParameters().size() - 1).isVarArgs()
                    || (declaration.getParameters().size() == 1 && declaration.getParameters().get(0).getType() instanceof CtArrayTypeReference<?> && c.getArguments().size() > 1));
            if (isVarArgCall) {
                List<CtParameter<?>> params = declaration != null ? declaration.getParameters() : null;
                int fixed = params != null ? params.size() - 1 : Math.max(0, c.getExecutable().getParameters().size() - 1);
                CtTypeReference<?> varArgType = params != null ? params.get(fixed).getType() : (!c.getExecutable().getParameters().isEmpty() ? c.getExecutable().getParameters().get(fixed) : null);
                String elemType = varArgType instanceof CtArrayTypeReference<?> arrayRef
                        ? type(arrayRef.getComponentType()) : "int";
                StringBuilder s = new StringBuilder("(");
                for (int j = 0; j < fixed; j++) {
                    if (j > 0) s.append(", ");
                    CtTypeReference<?> paramType = params.get(j).getType();
                    s.append(initializer(paramType, c.getArguments().get(j)));
                }
                if (fixed > 0 && !c.getArguments().isEmpty()) s.append(", ");
                s.append("vector<").append(elemType).append(">{");
                for (int j = fixed; j < c.getArguments().size(); j++) {
                    if (j > fixed) s.append(", ");
                    s.append(expr(c.getArguments().get(j)));
                }
                return s.append("})").toString();
            }
            List<? extends CtTypeReference<?>> paramTypes = declaration != null
                    ? declaration.getParameters().stream().map(CtParameter::getType).toList()
                    : c.getExecutable().getParameters();
            StringBuilder s = new StringBuilder("(");
            for (int j = 0; j < c.getArguments().size(); j++) {
                if (j > 0) s.append(", ");
                CtTypeReference<?> paramType = j < paramTypes.size() ? paramTypes.get(j) : null;
                s.append(paramType != null ? initializer(paramType, c.getArguments().get(j)) : argumentExpr(c.getArguments().get(j)));
            }
            return s.append(')').toString();
        }

        /** Arrays の呼出しを返す。計算量: O(A)。A は引数式の長さ。 */
        // 未テスト
        private String arraysInvocation(String name, List<CtExpression<?>> args) {
            return switch (name) {
                case "asList" -> "jlistOf" + arguments(args);
                case "sort" -> "jsort" + arguments(args);
                case "fill" -> "jfill" + arguments(args);
                case "copyOf" -> "jcopyOf" + arguments(args);
                case "copyOfRange" -> "jcopyOfRange" + arguments(args);
                case "setAll" -> "jsetAll" + arguments(args);
                case "parallelPrefix" -> "jparallelPrefix" + arguments(args);
                case "binarySearch" -> "jbsearch" + arguments(args);
                case "compare" -> "jcompare" + arguments(args);
                case "mismatch" -> "jmismatch" + arguments(args);
                case "stream" -> "JStream" + arguments(args);
                case "hashCode", "deepHashCode" -> "static_cast<int>(jhash(" + expr(args.get(0)) + "))";
                case "equals", "deepEquals" -> "(" + expr(args.get(0)) + " == " + expr(args.get(1)) + ")";
                case "toString", "deepToString" -> "jstr(" + expr(args.get(0)) + ")";
                default -> throw new IllegalArgumentException("Unsupported Arrays method: " + name);
            };
        }

        /** 引数列 A を返す。計算量: O(A)。 */
        // 未テスト
        private String arguments(List<? extends CtExpression<?>> args) {
            StringBuilder s = new StringBuilder("(");
            for (int j = 0; j < args.size(); j++) {
                if (j > 0) s.append(", ");
                s.append(argumentExpr(args.get(j)));
            }
            return s.append(')').toString();
        }

        /** 関数引数 e を型推論可能な C++ 式として返す。計算量: O(|e|)。 */
        // 未テスト
        private String argumentExpr(CtExpression<?> e) {
            return expr(e);
        }

        /**
         * 式 elem の期待される C++ 型を文脈から試行的に取得する。
         *
         * @param elem 対象の構文要素
         * @return 期待される型参照。取得不能な場合は null
         * 計算量: O(1)
         */
        // 未テスト
        private CtTypeReference<?> getExpectedType(CtElement elem) {
            if (elem == null) return null;
            CtElement parent = elem.getParent();
            if (parent instanceof CtLocalVariable<?> v) return v.getType();
            if (parent instanceof CtField<?> f) return f.getType();
            if (parent instanceof CtAssignment<?, ?> a) return a.getAssigned().getType();
            if (parent instanceof CtReturn<?> r) {
                CtMethod<?> m = r.getParent(CtMethod.class);
                return m != null ? m.getType() : null;
            }
            if (parent instanceof CtArrayAccess<?, ?> aw) return aw.getType();
            return null;
        }

        /** new 式 c を返す。計算量: O(|c|)。 */
        // 未テスト
        private String constructor(CtConstructorCall<?> c) {
            String q = c.getType().getQualifiedName();
            String simple = simpleName(c.getType().getSimpleName());
            if (q.equals("java.util.Scanner") || runtimeScannerTypes.contains(simple)) return "JScanner()";
            if (q.equals("java.io.PrintWriter") || runtimeWriterTypes.contains(simple)) return "JPrintWriter()";
            if (q.equals("java.io.InputStream") || q.equals("java.io.ByteArrayInputStream")
                    || q.equals("java.io.BufferedInputStream") || q.equals("java.io.BufferedReader")
                    || q.equals("java.io.InputStreamReader") || q.equals("java.io.Reader")
                    || q.equals("java.io.StringReader") || q.equals("java.io.FileInputStream")
                    || q.equals("java.io.DataInputStream")) return "JInputStream" + arguments(c.getArguments());
            if (q.equals("java.io.BufferedWriter") || q.equals("java.io.OutputStreamWriter")
                    || q.equals("java.io.Writer") || q.equals("java.io.FileWriter")
                    || q.equals("java.io.FileOutputStream") || q.equals("java.io.DataOutputStream")
                    || q.equals("java.io.PrintStream") || q.equals("java.io.BufferedOutputStream")
                    || q.equals("java.io.OutputStream") || q.equals("java.io.ByteArrayOutputStream")) {
                return "JPrintWriter" + (c.getArguments().isEmpty() ? "()" : arguments(c.getArguments()));
            }
            if (q.equals("java.util.BitSet") || simple.equals("BitSet")) return "JBitSet" + arguments(c.getArguments());
            if (q.equals("java.lang.Thread")) return "JThread" + arguments(c.getArguments());
            if (q.equals("java.util.Random")) return "JRandom" + arguments(c.getArguments());
            if (q.equals("java.util.Date") || simple.equals("Date")) return "JDate" + arguments(c.getArguments());
            if (q.equals("java.util.Iterator") || simple.equals("Iterator")) return type(c.getType()) + arguments(c.getArguments());
            if (q.equals("java.util.StringTokenizer") || simple.equals("StringTokenizer")) return "JStringTokenizer" + arguments(c.getArguments());
            if (q.equals("java.math.BigInteger") || simple.equals("BigInteger")) return "JBigInteger" + arguments(c.getArguments());
            if (q.equals("java.math.BigDecimal") || simple.equals("BigDecimal")) return "JBigDecimal" + arguments(c.getArguments());
            if (q.equals("java.lang.String") || simple.equals("String")) return "JString" + arguments(c.getArguments());
            if (q.equals("java.lang.StringBuilder")) return "JStringBuilder" + arguments(c.getArguments());
            if (q.startsWith("java.util.") && switch (simple) {
                case "List", "ArrayList", "LinkedList", "Vector", "Set", "HashSet", "LinkedHashSet", "TreeSet", "Map", "HashMap", "LinkedHashMap", "TreeMap",
                        "Deque", "ArrayDeque", "Queue", "PriorityQueue", "Stack" -> true;
                default -> false;
            }) {
                CtTypeReference<?> targetType = c.getType();
                if (targetType != null && targetType.getActualTypeArguments().isEmpty()) {
                    CtTypeReference<?> expected = getExpectedType(c);
                    if (expected != null && !expected.getActualTypeArguments().isEmpty()) {
                        targetType = expected;
                    }
                }
                return type(targetType) + arguments(c.getArguments());
            }
            if (c instanceof CtNewClass<?> anonymous) {
                if (q.equals("java.util.Comparator") || functionalType(c.getType()) != null || q.startsWith("java.util.function.")) {
                    return anonymousFunctional(anonymous);
                }
                if (c.getType() != null && c.getType().getTypeDeclaration() != null && (c.getType().getTypeDeclaration().isInterface() || c.getType().getTypeDeclaration().hasModifier(ModifierKind.ABSTRACT))) {
                    return anonymousFunctional(anonymous);
                }
            }
            String args = constructorArguments(c);
            if (isNonStaticNested(c.getType())) {
                String outer = outerTarget(c.getType().getDeclaringType());
                args = "(" + outer + (c.getArguments().isEmpty() ? "" : ", "
                        + args.substring(1, args.length() - 1)) + ")";
            }
            String rawType = rawCustomType(c.getType());
            if (c.getType() != null && c.getType().getActualTypeArguments().isEmpty()
                    && c.getType().getTypeDeclaration() != null
                    && !c.getType().getTypeDeclaration().getFormalCtTypeParameters().isEmpty()) {
                rawType = "decltype(" + rawType + args + ")";
            }
            return "make_shared<" + rawType + ">" + args;
        }

        /** t が非 static のメンバー型なら true を返す。計算量: O(1)。 */
        // 未テスト
        private boolean isNonStaticNested(CtTypeReference<?> t) {
            CtType<?> declaration = t == null ? null : t.getTypeDeclaration();
            return declaration != null && declaration.getDeclaringType() != null
                    && !declaration.hasModifier(ModifierKind.STATIC)
                    && !declaration.getClass().getSimpleName().contains("Record");
        }

        /** 現在型から所有型 t を指す C++ 式を返す。計算量: O(depth(currentType))。 */
        // 未テスト
        private String outerTarget(CtTypeReference<?> t) {
            if (currentStaticContext) return "nullptr";
            if (currentType == null || t == null) return "this";
            CtType<?> cursor = currentType;
            String result = "this";
            while (cursor != null && !simpleName(cursor.getSimpleName()).equals(simpleName(t.getSimpleName()))) {
                if (cursor.getDeclaringType() == null) return "this";
                result = result.equals("this") ? "outer" : result + "->outer";
                cursor = cursor.getDeclaringType();
            }
            return result;
        }

        /** 宣言型 t を考慮して初期化式 e を返す。計算量: O(|e| + rank(t))。 */
        // 未テスト
        private String initializer(CtTypeReference<?> t, CtExpression<?> e) {
            if (isNullLiteral(e)) {
                if (t == null || t instanceof CtTypeParameterReference) return "{}";
                String typeStr = type(t);
                return typeStr.contains(" ") ? "(" + typeStr + "){}" : typeStr + "{}";
            }
            if (e instanceof CtNewArray<?> array && array.getElements().isEmpty()
                    && !array.getDimensionExpressions().isEmpty() && t instanceof CtArrayTypeReference<?>) {
                return vectorAllocation(t, array.getDimensionExpressions(), 0);
            }
            return expr(e);
        }

        private boolean isDoubleType(CtTypeReference<?> t) {
            if (t == null) return false;
            String q = t.getQualifiedName();
            return q.equals("double") || q.equals("java.lang.Double") || q.equals("float") || q.equals("java.lang.Float");
        }

        private boolean hasRecursiveCall(CtMethod<?> method) {
            if (method.getBody() == null) return false;
            return !method.getBody().getElements(new TypeFilter<>(CtInvocation.class)).stream()
                    .filter(i -> i.getExecutable().getSimpleName().equals(method.getSimpleName()))
                    .filter(i -> i.getTarget() == null || i.getTarget() instanceof CtThisAccess<?>)
                    .toList().isEmpty();
        }

        private String anonymousFunctional(CtNewClass<?> c) {
            CtClass<?> anonymous = c.getAnonymousClass();
            if (anonymous == null) throw unsupported(c, "anonymous class");
            List<CtMethod<?>> methods = anonymous.getMethods().stream()
                    .filter(m -> !m.isImplicit() && !m.getSimpleName().equals("equals"))
                    .toList();
            if (methods.size() == 1 && (functionalType(c.getType()) != null || c.getType().getQualifiedName().startsWith("java.util.function.") || c.getType().getQualifiedName().equals("java.util.Comparator"))) {
                CtMethod<?> method = methods.get(0);
                String capture = c.getParent(CtMethod.class) == null ? "[]" : "[&]";
                StringBuilder s = new StringBuilder(capture).append("(");
                appendParameters(s, method.getParameters());
                s.append(") ");

                boolean recursive = hasRecursiveCall(method);
                CtMethod<?> previousAnonymousMethod = currentAnonymousMethod;
                if (recursive) {
                    currentAnonymousMethod = method;
                }

                int length = out.length();
                emitBlock(method.getBody(), false);
                String block = out.substring(length);
                out.setLength(length);

                if (recursive) {
                    currentAnonymousMethod = previousAnonymousMethod;
                    StringBuilder rec = new StringBuilder("{\n");
                    rec.append("    auto __self = [&](auto&& __self, ");
                    appendParameters(rec, method.getParameters());
                    rec.append(") -> ").append(type(method.getType())).append(" ");
                    rec.append(block).append(";\n");
                    rec.append("    return __self(__self");
                    for (CtParameter<?> p : method.getParameters()) {
                        rec.append(", ").append(id(p.getSimpleName()));
                    }
                    rec.append(");\n}");
                    return s.append(rec).toString();
                }

                return s.append(block).toString();
            }

            String outerRef = currentStaticContext || currentType == null ? "nullptr" : "this";
            String capture = currentStaticContext || currentType == null ? "[]" : "[this]";
            StringBuilder s = new StringBuilder(capture).append("() {\n");
            s.append("    auto outer_ptr = ").append(outerRef).append(";\n");
            s.append("    struct __Anon : public ").append(rawCustomType(c.getType())).append(" {\n");
            s.append("        decltype(outer_ptr) outer;\n");
            s.append("        __Anon(decltype(outer_ptr) o) : outer(o) {}\n");
            CtType<?> previousType = currentType;
            for (CtMethod<?> m : methods) {
                currentType = anonymous;
                s.append("        ").append(type(m.getType())).append(" ").append(id(m.getSimpleName())).append("(");
                appendParameters(s, m.getParameters());
                s.append(") override ");
                int length = out.length();
                emitBlock(m.getBody(), false, m.getParameters());
                String block = out.substring(length);
                out.setLength(length);
                s.append(block).append("\n");
            }
            currentType = previousType;
            s.append("    };\n");
            s.append("    return static_pointer_cast<").append(rawCustomType(c.getType())).append(">(make_shared<__Anon>(outer_ptr));\n");
            s.append("}()");
            return s.toString();
        }

        /** 型アクセス t の C++ 表記を返す。計算量: O(|name(t)|)。 */
        // 未テスト
        private String typeAccess(CtTypeReference<?> t) {
            return switch (t.getQualifiedName()) {
                case "java.util.BitSet", "BitSet" -> "JBitSet";
                case "java.awt.geom.Line2D", "Line2D" -> "Line2D";
                case "java.util.Collections", "Collections" -> "JCollections";
                case "java.util.Comparator", "Comparator" -> "JComparatorUtils";
                case "java.lang.Math", "Math" -> "JMath";
                case "java.lang.Long" -> "JLong";
                case "java.lang.Integer" -> "JInteger";
                case "java.lang.Double" -> "JDouble";
                case "java.lang.Number", "Number" -> "JNumber";
                case "java.lang.Character" -> "JCharacter";
                case "java.lang.Runtime" -> "JRuntime";
                case "java.lang.Thread" -> "JThread";
                case "java.math.BigInteger", "BigInteger" -> "JBigInteger";
                case "java.math.BigDecimal", "BigDecimal" -> "JBigDecimal";
                case "java.util.stream.IntStream" -> "JIntStream";
                case "java.util.stream.LongStream" -> "JLongStream";
                default -> rawCustomType(t);
            };
        }

        /** 配列と null の比較 b を C++ 式へ変換し、該当しなければ null を返す。計算量: O(|b|)。 */
        // 未テスト
        private String nullArrayComparison(CtBinaryOperator<?> b) {
            if (b.getKind() != spoon.reflect.code.BinaryOperatorKind.EQ
                    && b.getKind() != spoon.reflect.code.BinaryOperatorKind.NE) return null;
            CtExpression<?> left = b.getLeftHandOperand();
            CtExpression<?> right = b.getRightHandOperand();
            CtExpression<?> array = isNullLiteral(left) ? right : isNullLiteral(right) ? left : null;
            if (array == null) return null;
            if (array.getType() instanceof CtTypeParameterReference) {
                return (b.getKind() == spoon.reflect.code.BinaryOperatorKind.NE ? "!" : "")
                        + "jisNull(" + expr(array) + ")";
            }
            if (isContainerType(array.getType())) {
                return (b.getKind() == spoon.reflect.code.BinaryOperatorKind.NE ? "!" : "")
                        + expr(array) + ".empty()";
            }
            if (array.getType() != null && array.getType().getQualifiedName().equals("java.lang.Object")) {
                return (b.getKind() == spoon.reflect.code.BinaryOperatorKind.NE ? "" : "!")
                        + expr(array) + ".has_value()";
            }
            if (array.getType() instanceof CtArrayTypeReference<?>) {
                return (b.getKind() == spoon.reflect.code.BinaryOperatorKind.NE ? "!" : "")
                        + expr(array) + ".empty()";
            }
            return (b.getKind() == spoon.reflect.code.BinaryOperatorKind.NE ? "(!" : "(")
                    + "jisNull(" + expr(array) + "))";
        }

        /** e が null リテラルなら true を返す。計算量: O(1)。 */
        // 未テスト
        private boolean isNullLiteral(CtExpression<?> e) {
            return e instanceof CtLiteral<?> literal && literal.getValue() == null;
        }

        /** t が値表現へ変換される Java コレクション型なら true を返す。計算量: O(1)。 */
        // 未テスト
        private boolean isContainerType(CtTypeReference<?> t) {
            if (t == null) return false;
            String q = t.getQualifiedName();
            return q.startsWith("java.util.") && switch (simpleName(q)) {
                case "List", "ArrayList", "LinkedList", "Vector", "Set", "HashSet", "LinkedHashSet", "TreeSet", "Map", "HashMap", "LinkedHashMap", "TreeMap",
                        "Deque", "ArrayDeque", "Queue", "PriorityQueue", "Stack" -> true;
                default -> false;
            };
        }

        /** t が Java の String 型なら true を返す。計算量: O(1)。 */
        // 未テスト
        private boolean isJavaString(CtTypeReference<?> t) {
            return t != null && t.getQualifiedName().equals("java.lang.String");
        }

        /** t が プリミティブまたはそのラッパー型なら true を返す。計算量: O(1)。 */
        // 未テスト
        private boolean isPrimitiveOrWrapper(CtTypeReference<?> t) {
            if (t == null) return false;
            if (t.isPrimitive()) return true;
            return switch (t.getQualifiedName()) {
                case "java.lang.Boolean", "java.lang.Byte", "java.lang.Short", "java.lang.Integer",
                        "java.lang.Long", "java.lang.Character", "java.lang.Float", "java.lang.Double" -> true;
                default -> false;
            };
        }

        /** t の equals が C++ 値等価で表現できるなら true を返す。計算量: O(1)。 */
        // 未テスト
        private boolean supportsCppValueEquality(CtTypeReference<?> t) {
            if (t == null) return false;
            if (isContainerType(t) || isJavaString(t)) return true;
            return switch (t.getQualifiedName()) {
                case "java.util.BitSet", "java.lang.Boolean", "java.lang.Byte", "java.lang.Short", "java.lang.Integer",
                        "java.lang.Long", "java.lang.Character", "java.lang.Float", "java.lang.Double" -> true;
                default -> false;
            };
        }

        /** メソッド参照 r の C++ 表記を返す。計算量: O(|r|)。 */
        // 未テスト
        private String executableReference(CtExecutableReferenceExpression<?, ?> r) {
            CtExpression<?> target = r.getTarget();
            String name = r.getExecutable().getSimpleName();
            if (name.equals("<init>")) {
                if (target instanceof CtTypeAccess<?> access && access.getAccessedType() instanceof CtArrayTypeReference<?> arrayRef) {
                    return "[](int n) { return vector<" + type(arrayRef.getComponentType()) + ">(n); }";
                }
                if (target instanceof CtTypeAccess<?> access) {
                    return "[](auto&&... args) { return make_shared<" + rawCustomType(access.getAccessedType()) + ">(std::forward<decltype(args)>(args)...); }";
                }
            }
            if (target instanceof CtTypeAccess<?> access) {
                boolean isStatic = r.getExecutable().isStatic();
                if (!isStatic) {
                    String accessedType = access.getAccessedType().getQualifiedName();
                    if (accessedType.equals("java.lang.Integer") && name.equals("intValue")) {
                        return "[](const auto& x, auto&&...) { return static_cast<int>(x); }";
                    }
                    if (accessedType.equals("java.lang.Long") && name.equals("longValue")) {
                        return "[](const auto& x, auto&&...) { return static_cast<long long>(x); }";
                    }
                    if (accessedType.equals("java.lang.Double") && name.equals("doubleValue")) {
                        return "[](const auto& x, auto&&...) { return static_cast<double>(x); }";
                    }
                    if (accessedType.equals("java.lang.Float") && name.equals("floatValue")) {
                        return "[](const auto& x, auto&&...) { return static_cast<float>(x); }";
                    }
                    if (accessedType.equals("java.lang.Boolean") && name.equals("booleanValue")) {
                        return "[](const auto& x, auto&&...) { return static_cast<bool>(x); }";
                    }
                    if (accessedType.equals("java.lang.Byte") && name.equals("byteValue")) {
                        return "[](const auto& x, auto&&...) { return static_cast<int8_t>(x); }";
                    }
                    if (accessedType.equals("java.lang.Short") && name.equals("shortValue")) {
                        return "[](const auto& x, auto&&...) { return static_cast<int16_t>(x); }";
                    }
                    if (accessedType.equals("java.lang.Character") && name.equals("charValue")) {
                        return "[](const auto& x, auto&&...) { return static_cast<char>(x); }";
                    }
                    if (name.equals("length")) return "[](const auto& x) { return x.length(); }";
                    return "[](const auto& x, auto&&... args) { return x." + id(name) + "(std::forward<decltype(args)>(args)...); }";
                } else {
                    String targetStr = typeAccess(access.getAccessedType());
                    return "[](auto&&... args) { return " + targetStr + "::" + id(name) + "(std::forward<decltype(args)>(args)...); }";
                }
            }
            if (target instanceof CtThisAccess<?> || target == null) {
                return "[this](auto&&... args) { return this->" + id(name) + "(std::forward<decltype(args)>(args)...); }";
            }
            String separator = memberSeparator(target);
            return "[obj=" + expr(target) + "](auto&&... args) mutable { return obj" + separator + id(name) + "(std::forward<decltype(args)>(args)...); }";
        }

        /** 対象式 t の単純型名を返す。計算量: O(1)。 */
        // 未テスト
        private String simpleTargetName(CtExpression<?> t) {
            if (t instanceof CtTypeAccess<?> access) return simpleName(access.getAccessedType().getSimpleName());
            return t != null && t.getType() != null ? simpleName(t.getType().getSimpleName()) : "";
        }

        /** 関数型 q の抽象メソッド名 n ならtrueを返す。計算量: O(1)。 */
        // 未テスト
        private boolean isFunctionalInvocation(String q, String n) {
            return q.startsWith("java.util.function.") && switch (n) {
                case "test", "apply", "applyAsInt", "applyAsLong", "applyAsDouble", "accept", "getAsInt", "getAsLong", "get" -> true;
                default -> false;
            };
        }

        /** 配列生成式 a を返す。計算量: O(|a|)。 */
        // 未テスト
        private String newArray(CtNewArray<?> a) {
            if (!a.getElements().isEmpty()) {
                StringBuilder s = new StringBuilder();
                if (a.getType() != null) {
                    s.append(type(a.getType()));
                }
                s.append('{');
                for (int i = 0; i < a.getElements().size(); i++) {
                    if (i > 0) s.append(", ");
                    s.append(expr(a.getElements().get(i)));
                }
                return s.append('}').toString();
            }
            List<CtExpression<Integer>> dimensions = a.getDimensionExpressions();
            if (dimensions.isEmpty()) return "{}";
            return vectorAllocation(a.getType(), dimensions, 0);
        }

        /** 配列型 t の dimensions[d..] を確保する式を返す。計算量: O(rank(t))。 */
        // 未テスト
        private String vectorAllocation(CtTypeReference<?> t, List<? extends CtExpression<?>> dimensions, int d) {
            if (!(t instanceof CtArrayTypeReference<?> array)) throw unsupported(t, "array type");
            String result = "vector<" + type(array.getComponentType()) + ">(" + expr(dimensions.get(d));
            if (d + 1 < dimensions.size()) result += ", " + vectorAllocation(array.getComponentType(), dimensions, d + 1);
            return result + ")";
        }

        /** ラムダ l が参照する変数を値捕捉する C++ ラムダを返す。計算量: O(|l| + c)。c は参照変数数。 */
        // 未テスト
        private String lambda(CtLambda<?> l) {
            StringBuilder s = new StringBuilder("[=](");
            appendParameters(s, l.getParameters());
            s.append(") ");
            if (l.getBody() != null) {
                StringBuilder saved = new StringBuilder(out);
                int length = out.length();
                emitBlock(l.getBody(), false);
                String block = out.substring(length);
                out.setLength(length);
                if (saved.length() != length) throw new AssertionError();
                return s.append(block).toString();
            }
            return s.append("{ return ").append(expr(l.getExpression())).append("; }").toString();
        }

        /** 単項式 u を返す。計算量: O(|u|)。 */
        // 未テスト
        private String unary(CtUnaryOperator<?> u) {
            String operand = expr(u.getOperand());
            return switch (u.getKind()) {
                case NOT -> "(!" + operand + ")";
                case NEG -> "(-" + operand + ")";
                case POS -> "(+" + operand + ")";
                case COMPL -> "(~" + operand + ")";
                case PREINC -> "(++" + operand + ")";
                case PREDEC -> "(--" + operand + ")";
                case POSTINC -> "(" + operand + "++)";
                case POSTDEC -> "(" + operand + "--)";
            };
        }

        /** 二項演算子 k の C++ 表記を返す。計算量: O(1)。 */
        // 未テスト
        private String binarySymbol(spoon.reflect.code.BinaryOperatorKind k) {
            return switch (k) {
                case OR -> "||"; case AND -> "&&"; case BITOR -> "|"; case BITXOR -> "^"; case BITAND -> "&";
                case EQ -> "=="; case NE -> "!="; case LT -> "<"; case GT -> ">"; case LE -> "<="; case GE -> ">=";
                case SL -> "<<"; case SR -> ">>"; case USR -> ">>"; case PLUS -> "+"; case MINUS -> "-";
                case MUL -> "*"; case DIV -> "/"; case MOD -> "%"; case INSTANCEOF -> throw new IllegalArgumentException("instanceof is unsupported");
            };
        }

        /** 型 t の C++ 表記を返す。計算量: O(rank(t) + typeArguments(t))。 */
        // 未テスト
        private String type(CtTypeReference<?> t) {
            if (t == null) return "void";
            if (t instanceof CtArrayTypeReference<?> array) return "vector<" + type(array.getComponentType()) + ">";
            String q = t.getQualifiedName();
            if (runtimeScannerTypes.contains(simpleName(t.getSimpleName()))) return "JScanner";
            if (runtimeWriterTypes.contains(simpleName(t.getSimpleName()))) return "JPrintWriter";
            String primitive = switch (q) {
                case "void" -> "void"; case "boolean", "java.lang.Boolean" -> "bool";
                case "byte" -> "int8_t"; case "short" -> "int16_t"; case "int", "java.lang.Integer" -> "int";
                case "long", "java.lang.Long" -> "long long"; case "char", "java.lang.Character" -> "char";
                case "float", "java.lang.Float" -> "float"; case "double", "java.lang.Double" -> "double";
                case "java.lang.String" -> "JString"; case "java.lang.Object" -> "JObject";
                case "java.lang.Number", "Number" -> "JNumber";
                case "java.lang.StringBuilder" -> "JStringBuilder";
                case "java.util.StringTokenizer", "StringTokenizer" -> "JStringTokenizer";
                case "java.util.Scanner" -> "JScanner"; case "java.io.PrintWriter" -> "JPrintWriter";
                case "java.util.Iterator" -> "JIterator<" + (t.getActualTypeArguments().isEmpty() ? "JObject" : type(t.getActualTypeArguments().get(0))) + ">";
                case "java.util.PrimitiveIterator$OfInt", "java.util.PrimitiveIterator.OfInt", "PrimitiveIterator.OfInt", "PrimitiveIterator$OfInt" -> "PrimitiveIterator::OfInt";
                case "java.util.PrimitiveIterator$OfLong", "java.util.PrimitiveIterator.OfLong", "PrimitiveIterator.OfLong", "PrimitiveIterator$OfLong" -> "PrimitiveIterator::OfLong";
                case "java.util.PrimitiveIterator$OfDouble", "java.util.PrimitiveIterator.OfDouble", "PrimitiveIterator.OfDouble", "PrimitiveIterator$OfDouble" -> "PrimitiveIterator::OfDouble";
                case "java.io.InputStream", "java.io.ByteArrayInputStream", "java.io.BufferedInputStream",
                        "java.io.BufferedReader", "java.io.InputStreamReader", "java.io.Reader",
                        "java.io.StringReader", "java.io.FileInputStream", "java.io.DataInputStream" -> "JInputStream";
                case "java.io.BufferedWriter", "java.io.OutputStreamWriter", "java.io.Writer",
                        "java.io.FileWriter", "java.io.FileOutputStream", "java.io.DataOutputStream",
                        "java.io.PrintStream", "java.io.BufferedOutputStream", "java.io.OutputStream",
                        "java.io.ByteArrayOutputStream" -> "JPrintWriter";
                case "java.util.Comparator" -> {
                    if (t.getActualTypeArguments().isEmpty()) yield "function<int(JObject, JObject)>";
                    CtTypeReference<?> arg = t.getActualTypeArguments().get(0);
                    if (arg instanceof CtTypeParameterReference tpr && tpr.getDeclaration() == null) yield "nullptr_t";
                    yield "function<int(" + type(arg) + ", " + type(arg) + ")>";
                }
                case "java.lang.Comparable" -> "shared_ptr<Comparable<" + (t.getActualTypeArguments().isEmpty() ? "JObject" : type(t.getActualTypeArguments().get(0))) + ">>";
                case "java.lang.Runnable" -> "Runnable";
                case "java.lang.AutoCloseable", "java.io.Closeable" -> "AutoCloseable";
                case "java.io.Serializable" -> "Serializable";
                case "java.lang.Cloneable" -> "Cloneable";
                case "java.lang.Thread" -> "JThread"; case "java.util.Random", "java.util.random.RandomGenerator" -> "JRandom";
                case "java.util.Date", "Date" -> "JDate";
                case "java.lang.Iterable" -> t.getActualTypeArguments().isEmpty() ? "JIterable<JObject>" : "JIterable<" + type(t.getActualTypeArguments().get(0)) + ">";
                case "java.util.BitSet", "BitSet" -> "JBitSet";
                case "java.math.BigInteger", "BigInteger" -> "JBigInteger";
                case "java.math.BigDecimal", "BigDecimal" -> "JBigDecimal";
                case "java.util.stream.IntStream" -> "JIntStream"; case "java.util.stream.LongStream" -> "JLongStream";
                case "library.tools.FastScanner", "FastScanner" -> "JScanner";
                case "library.tools.MyPrintWriter", "MyPrintWriter" -> "JPrintWriter";
                default -> null;
            };
            if (primitive != null) return primitive;
            String functional = functionalType(t);
            if (functional != null) return functional;
            if (q.equals("java.util.Map$Entry")) {
                List<CtTypeReference<?>> a = t.getActualTypeArguments();
                return "std::pair<" + type(a.get(0)) + ", " + type(a.get(1)) + ">";
            }
            String container = switch (q) {
                case "java.util.List", "java.util.ArrayList", "java.util.LinkedList", "java.util.Vector" -> "JList";
                case "java.util.Set", "java.util.HashSet", "java.util.LinkedHashSet", "java.util.TreeSet" -> "JSet";
                case "java.util.Map", "java.util.HashMap", "java.util.LinkedHashMap", "java.util.TreeMap" -> "JMap";
                case "java.util.Deque", "java.util.ArrayDeque", "java.util.Queue" -> "JDeque";
                case "java.util.Stack" -> "JStack";
                case "java.util.PriorityQueue" -> "JPriorityQueue";
                default -> null;
            };
            if (container != null) {
                StringBuilder s = new StringBuilder(container).append('<');
                List<CtTypeReference<?>> arguments = t.getActualTypeArguments();
                if (arguments.isEmpty()) {
                    s.append(container.equals("JMap") ? "JObject, JObject" : "JObject");
                }
                for (int i = 0; i < arguments.size(); i++) {
                    if (i > 0) s.append(", ");
                    s.append(type(arguments.get(i)));
                }
                return s.append('>').toString();
            }
            if (t instanceof spoon.reflect.reference.CtWildcardReference wildcard) {
                return wildcard.getBoundingType() != null ? type(wildcard.getBoundingType()) : "JObject";
            }
            if (t instanceof CtTypeParameterReference parameter) return typeParameterName(parameter);
            if (isDeclaredType(t)) {
                String raw = rawCustomType(t);
                if (!raw.startsWith("J") && !raw.startsWith("std::")) {
                    raw = "struct " + raw;
                }
                return "shared_ptr<" + raw + ">";
            }
            throw unsupported(t, "type " + q);
        }

        /** 参照型 t のポインタでない C++ 型名を返す。計算量: O(typeArguments(t))。 */
        // 未テスト
        private String rawCustomType(CtTypeReference<?> t) {
            String q = t.getQualifiedName();
            String mapped = switch (q) {
                case "java.util.PriorityQueue", "PriorityQueue" -> "JPriorityQueue";
                case "java.util.List", "List" -> "JList";
                case "java.util.Set", "Set" -> "JSet";
                case "java.util.Map", "Map" -> "JMap";
                case "java.util.Deque", "Deque", "ArrayDeque" -> "JDeque";
                case "java.util.Stack", "Stack" -> "JStack";
                default -> null;
            };
            if (mapped != null) return mapped;
            String name = t.getQualifiedName();
            int packageEnd = name.lastIndexOf('.');
            name = packageEnd < 0 ? name : name.substring(packageEnd + 1);
            String[] rawNested = name.split("\\$");
            List<String> nestedList = new java.util.ArrayList<>();
            for (String part : rawNested) {
                String clean = part.replaceFirst("^\\d+", "");
                if (!clean.isEmpty()) nestedList.add(clean);
            }
            if (nestedList.isEmpty()) nestedList.add(name);
            String[] nested = nestedList.toArray(new String[0]);
            StringBuilder s = new StringBuilder();
            int start = 0;
            if (currentType != null && nested.length > 1) {
                CtType<?> outer = currentType;
                while (outer.getDeclaringType() != null) outer = outer.getDeclaringType();
                if (outer.getSimpleName().equals(nested[0])) {
                    start = 1;
                }
            }
            for (int i = start; i < nested.length; i++) {
                if (i > start) s.append("::");
                s.append(id(nested[i]));
                if (i == 0 && nested.length > 1 && !t.getActualTypeArguments().isEmpty()
                        && model.getAllTypes().stream().anyMatch(declared -> declared.getSimpleName().equals(nested[0])
                        && !declared.getFormalCtTypeParameters().isEmpty())) {
                    appendTypeArguments(s, t.getActualTypeArguments());
                }
            }
            if (!t.getActualTypeArguments().isEmpty()) {
                appendTypeArguments(s, t.getActualTypeArguments());
            }
            return s.toString();
        }

        /** 型引数列 arguments を s に加える。計算量: O(|arguments|)。 */
        // 未テスト
        private void appendTypeArguments(StringBuilder s, List<CtTypeReference<?>> arguments) {
            s.append('<');
            for (int i = 0; i < arguments.size(); i++) {
                if (i > 0) s.append(", ");
                s.append(type(arguments.get(i)));
            }
            s.append('>');
        }

        /** 対象 t に対するメンバー選択演算子を返す。計算量: O(1)。 */
        // 未テスト
        private String memberSeparator(CtExpression<?> t) {
            if (t == null || t.getType() == null) return ".";
            CtTypeReference<?> type = t.getType();
            return isDeclaredType(type) && !isRuntimeType(type.getSimpleName()) ? "->" : ".";
        }

        /** t が変換対象ソースで宣言された型なら true を返す。計算量: O(1)。 */
        // 未テスト
        private boolean isDeclaredType(CtTypeReference<?> t) {
            String q = t.getQualifiedName();
            return declaredTypes.contains(t.getSimpleName()) || declaredTypes.contains(simpleName(t.getSimpleName()))
                    || declaredTypes.contains(q) || q.contains("$") && declaredTypes.contains(simpleName(q));
        }

        /** Java の内部型区切りを除いた単純名を返す。計算量: O(|name|)。 */
        // 未テスト
        private String simpleName(String name) {
            int dollar = name.lastIndexOf('$');
            int dot = name.lastIndexOf('.');
            int separator = Math.max(dollar, dot);
            String simple = separator < 0 ? name : name.substring(separator + 1);
            return simple.replaceFirst("^\\d+", "");
        }

        /** 型パラメータ p の外側テンプレートと衝突しない名前を返す。計算量: O(depth(p))。 */
        // 未テスト
        private String typeParameterName(CtTypeParameter p) {
            CtType<?> owner = p.getParent(CtType.class);
            CtMethod<?> methodOwner = p.getParent(CtMethod.class);
            if (methodOwner != null) {
                CtType<?> type = methodOwner.getDeclaringType();
                if (type != null && type.getFormalCtTypeParameters().stream().anyMatch(tp -> tp.getSimpleName().equals(p.getSimpleName()))) {
                    return id(p.getSimpleName()) + "Method";
                }
            }
            return id(p.getSimpleName()) + (owner != null && owner.getDeclaringType() != null ? "Inner" : "");
        }

        /** 型パラメータ参照 p の宣言と一致する C++ 名を返す。計算量: O(depth(p))。 */
        // 未テスト
        private String typeParameterName(CtTypeParameterReference p) {
            CtTypeParameter declaration = p.getDeclaration();
            return declaration == null ? id(p.getSimpleName()) : typeParameterName(declaration);
        }

        /** 識別子 s を C++ で衝突しない表記にする。計算量: O(1)。 */
        // 未テスト
        private String id(String s) {
            String clean = s.replaceFirst("^\\d+", "");
            if (clean.isEmpty()) clean = "Obj" + s;
            return CPP_KEYWORDS.contains(clean) ? clean + "_" : clean;
        }

        /** 1 行 s を現在の字下げで出力する。計算量: O(|s|)。 */
        // 未テスト
        private void line(String s) {
            writeIndent();
            out.append(s).append('\n');
        }

        /** 現在の字下げを出力する。計算量: O(indent)。 */
        // 未テスト
        private void writeIndent() {
            out.append("    ".repeat(indent));
        }

        /** ランタイムへ置換する型名かを返す。計算量: O(1)。 */
        // 未テスト
        private boolean isRuntimeType(String name) {
            String simple = simpleName(name);
            return runtimeScannerTypes.contains(name) || runtimeScannerTypes.contains(simple)
                    || runtimeWriterTypes.contains(name) || runtimeWriterTypes.contains(simple);
        }

        /** Java関数型 t のstd::function表記、対象外ならnullを返す。計算量: O(typeArguments(t))。 */
        // 未テスト
        private String functionalType(CtTypeReference<?> t) {
            String q = t.getQualifiedName();
            List<CtTypeReference<?>> a = t.getActualTypeArguments();
            return switch (q) {
                case "java.util.function.Predicate" -> "function<bool(" + type(a.get(0)) + ")>";
                case "java.util.function.BiPredicate" -> "function<bool(" + type(a.get(0)) + ", " + type(a.get(1)) + ")>";
                case "java.util.function.Consumer" -> "function<void(" + type(a.get(0)) + ")>";
                case "java.util.function.BiConsumer" -> "function<void(" + type(a.get(0)) + ", " + type(a.get(1)) + ")>";
                case "java.util.function.Supplier" -> "function<" + type(a.get(0)) + "()>";
                case "java.util.function.Function" -> "function<" + type(a.get(1)) + "(" + type(a.get(0)) + ")>";
                case "java.util.function.BiFunction" -> "function<" + type(a.get(2)) + "(" + type(a.get(0)) + ", " + type(a.get(1)) + ")>";
                case "java.util.function.BinaryOperator" -> "function<" + type(a.get(0)) + "(" + type(a.get(0)) + ", " + type(a.get(0)) + ")>";
                case "java.util.function.UnaryOperator" -> "function<" + type(a.get(0)) + "(" + type(a.get(0)) + ")>";
                case "java.util.function.DoubleBinaryOperator" -> "function<double(double, double)>";
                case "java.util.function.DoubleUnaryOperator" -> "function<double(double)>";
                case "java.util.function.LongUnaryOperator" -> "function<long long(long long)>";
                case "java.util.function.LongBinaryOperator" -> "function<long long(long long, long long)>";
                case "java.util.function.IntBinaryOperator" -> "function<int(int, int)>";
                case "java.util.function.IntUnaryOperator" -> "function<int(int)>";
                case "java.util.function.IntToLongFunction" -> "function<long long(int)>";
                case "java.util.function.IntToDoubleFunction" -> "function<double(int)>";
                case "java.util.function.LongToDoubleFunction" -> "function<double(long long)>";
                case "java.util.function.LongToIntFunction" -> "function<int(long long)>";
                case "java.util.function.IntFunction" -> "function<" + type(a.get(0)) + "(int)>";
                case "java.util.function.LongFunction" -> "function<" + type(a.get(0)) + "(long long)>";
                case "java.util.function.ObjIntConsumer" -> "function<void(" + type(a.get(0)) + ", int)>";
                case "java.util.function.ObjLongConsumer" -> "function<void(" + type(a.get(0)) + ", long long)>";
                default -> null;
            };
        }

        /** m が PrimitiveIterator を返す iterator メソッドなら true を返す。計算量: O(1)。 */
        // 未テスト
        private boolean isPrimitiveIteratorMethod(CtMethod<?> m) {
            String q = m.getType().getQualifiedName();
            return m.getSimpleName().equals("iterator") && q.startsWith("java.util.PrimitiveIterator$");
        }

        /** get(i) と要素数フィールドを用いる C++ range-for アダプターを型 t に出力する。計算量: O(F)。 */
        // 未テスト
        private void emitCppIterator(CtType<?> t) {
            CtField<?> limitField = t.getFields().stream()
                    .filter(f -> f.getSimpleName().equals("len") || f.getSimpleName().equals("tail")
                            || f.getSimpleName().equals("size"))
                    .findFirst().orElseThrow(() -> unsupported(t, "PrimitiveIterator length"));
            line("struct CppIterator {");
            indent++;
            line(id(t.getSimpleName()) + "* owner;");
            line("int index;");
            line("bool operator!=(const CppIterator& other) const { return index != other.index; }");
            line("void operator++() { ++index; }");
            line("auto operator*() const { return owner->get(index); }");
            indent--;
            line("};");
            line("CppIterator begin() { return {this, 0}; }");
            line("CppIterator end() { return {this, " + fieldName(limitField.getReference()) + "}; }");
        }

        /** 要素 e の位置を含む非対応例外を返す。計算量: O(1)。 */
        // 未テスト
        private IllegalArgumentException unsupported(CtElement e, String what) {
            String position = e != null && e.getPosition().isValidPosition() ? e.getPosition().toString() : "unknown position";
            return new IllegalArgumentException("Unsupported Java " + what + " at " + position);
        }
    }

    /** C++ 側の Java 競プロ互換ランタイム。 */
    private static final String PRELUDE1 = """
            #include <bits/stdc++.h>
            using namespace std;

            template<class T, class = void> struct jhas_key : false_type {};
            template<class T> struct jhas_key<T, void_t<decltype(declval<T>()->key)>> : true_type {};
            template<class T> struct jhas_key_obj : false_type {};
            template<class T> struct jhas_key<T, void_t<decltype(declval<T>().key)>> : true_type {};

            using JString = std::string;
            using JObject = std::any;

            struct JNumber {
                long long l_val = 0;
                double d_val = 0.0;
                bool is_fp = false;

                JNumber() = default;
                JNumber(long long v) : l_val(v), d_val(static_cast<double>(v)), is_fp(false) {}
                JNumber(int v) : l_val(v), d_val(v), is_fp(false) {}
                JNumber(double v) : l_val(static_cast<long long>(v)), d_val(v), is_fp(true) {}
                JNumber(float v) : l_val(static_cast<long long>(v)), d_val(v), is_fp(true) {}
                JNumber(int8_t v) : l_val(v), d_val(v), is_fp(false) {}
                JNumber(int16_t v) : l_val(v), d_val(v), is_fp(false) {}
                JNumber(const JObject& obj) {
                    if (!obj.has_value()) { l_val = 0; d_val = 0; is_fp = false; }
                    else if (auto p = std::any_cast<int>(&obj)) { l_val = *p; d_val = *p; is_fp = false; }
                    else if (auto p = std::any_cast<long long>(&obj)) { l_val = *p; d_val = *p; is_fp = false; }
                    else if (auto p = std::any_cast<double>(&obj)) { d_val = *p; l_val = static_cast<long long>(*p); is_fp = true; }
                    else if (auto p = std::any_cast<float>(&obj)) { d_val = *p; l_val = static_cast<long long>(*p); is_fp = true; }
                }

                long long longValue() const { return is_fp ? static_cast<long long>(d_val) : l_val; }
                int intValue() const { return static_cast<int>(longValue()); }
                double doubleValue() const { return is_fp ? d_val : static_cast<double>(l_val); }
                float floatValue() const { return static_cast<float>(doubleValue()); }
                int8_t byteValue() const { return static_cast<int8_t>(longValue()); }
                int16_t shortValue() const { return static_cast<int16_t>(longValue()); }

                bool operator==(const JNumber& o) const {
                    if (is_fp || o.is_fp) return doubleValue() == o.doubleValue();
                    return longValue() == o.longValue();
                }
                bool operator!=(const JNumber& o) const { return !(*this == o); }
                bool operator<(const JNumber& o) const {
                    if (is_fp || o.is_fp) return doubleValue() < o.doubleValue();
                    return longValue() < o.longValue();
                }
                friend ostream& operator<<(ostream& os, const JNumber& n) {
                    if (n.is_fp) os << n.d_val;
                    else os << n.l_val;
                    return os;
                }
            };

            template<class T, class U, class = enable_if_t<(is_same_v<decay_t<T>, JObject> || is_same_v<decay_t<U>, JObject>) && !(is_same_v<decay_t<T>, JObject> && is_same_v<decay_t<U>, JObject>)>>
            bool operator==(const T& a, const U& b) {
                if constexpr (is_same_v<decay_t<U>, JObject>) return !b.has_value();
                else return !a.has_value();
            }
            template<class T, class U, class = enable_if_t<(is_same_v<decay_t<T>, JObject> || is_same_v<decay_t<U>, JObject>) && !(is_same_v<decay_t<T>, JObject> && is_same_v<decay_t<U>, JObject>)>>
            bool operator!=(const T& a, const U& b) {
                return !(a == b);
            }
            inline bool operator==(const JObject& a, const JObject& b) {
                if (!a.has_value() && !b.has_value()) return true;
                if (!a.has_value() || !b.has_value()) return false;
                return &a == &b;
            }
            inline bool operator!=(const JObject& a, const JObject& b) { return !(a == b); }

            template<class T> bool jisInstance(const JObject& x) {
                return x.has_value() && (x.type() == typeid(T) || std::any_cast<T>(&x) != nullptr);
            }
            template<class T> bool jisInstance(const JObject& x, T& out) {
                if (x.has_value()) {
                    if (auto p = std::any_cast<T>(&x)) { out = *p; return true; }
                }
                return false;
            }
            template<class T, class U> bool jisInstance(const std::shared_ptr<U>& x) {
                if constexpr (std::is_base_of_v<T, U>) return x != nullptr;
                else return std::dynamic_pointer_cast<T>(x) != nullptr;
            }
            template<class T, class U> bool jisInstance(const std::shared_ptr<U>& x, std::shared_ptr<T>& out) {
                if (auto p = std::dynamic_pointer_cast<T>(x)) { out = p; return true; }
                return false;
            }
            inline std::vector<bool>::reference operator^=(std::vector<bool>::reference ref, bool val) { ref = ref ^ val; return ref; }
            inline std::vector<bool>::reference operator&=(std::vector<bool>::reference ref, bool val) { ref = ref & val; return ref; }
            inline std::vector<bool>::reference operator|=(std::vector<bool>::reference ref, bool val) { ref = ref | val; return ref; }

            template<class T> bool jisNull(const T&) { return false; }
            template<class T> bool jisNull(const vector<T>& x) { return x.empty(); }
            template<class T> bool jisNull(const shared_ptr<T>& x) { return !x; }
            inline bool jisNull(const JObject& x) { return !x.has_value(); }
            struct JStringTokenizer;
            inline bool jisNull(const JStringTokenizer& x);
            inline string jstr(const JObject&) { return "<object>"; }
            template<class T> string jstr(const T& x) { ostringstream o; o << x; return o.str(); }
            template<class T> string jstr(const vector<T>& a) {
                string s = "[";
                for (size_t i = 0; i < a.size(); ++i) s += (i ? ", " : "") + jstr(a[i]);
                return s + "]";
            }
            template<class A, class B> JString jconcat(const A& a, const B& b) { return jstr(a) + jstr(b); }
            inline JString jrepeat(const JString& s, int n) { JString r; while (n-- > 0) r += s; return r; }
            inline bool jstartsWith(const JString& s, const JString& prefix) {
                return s.rfind(prefix, 0) == 0;
            }
            inline bool jendsWith(const JString& s, const JString& suffix) {
                if (suffix.size() > s.size()) return false;
                return s.compare(s.size() - suffix.size(), suffix.size(), suffix) == 0;
            }
            inline JString jreplace(JString s, const JString& target, const JString& replacement) {
                if (target.empty()) return s;
                size_t pos = 0;
                while ((pos = s.find(target, pos)) != JString::npos) {
                    s.replace(pos, target.length(), replacement);
                    pos += replacement.length();
                }
                return s;
            }
            inline JString jreplace(JString s, char target, char replacement) {
                for (char& c : s) {
                    if (c == target) c = replacement;
                }
                return s;
            }
            inline JString jreplaceAll(JString s, const JString& regex, const JString& replacement) {
                try {
                    return regex_replace(s, std::regex(regex), replacement);
                } catch (...) {
                    return jreplace(s, regex, replacement);
                }
            }
            inline vector<char> jchars(const JString& s) { return vector<char>(s.begin(), s.end()); }
            inline vector<JObject> jlistOf() { return {}; }
            template<class T> auto jlistOf(const vector<T>& v) { return v; }
            template<class T, class... U> auto jlistOf(T first, U... rest) {
                using V = common_type_t<T, U...>;
                return vector<V>{static_cast<V>(first), static_cast<V>(rest)...};
            }
            template<class... T> auto jsetOf(T... args) { return JSet(jlistOf(args...)); }
            inline char jtoUpperCase(char c) { return static_cast<char>(::toupper(static_cast<unsigned char>(c))); }
            inline JString jtoUpperCase(JString s) {
                for (char& c : s) c = static_cast<char>(::toupper(static_cast<unsigned char>(c)));
                return s;
            }
            inline char jtoLowerCase(char c) { return static_cast<char>(::tolower(static_cast<unsigned char>(c))); }
            inline JString jtoLowerCase(JString s) {
                for (char& c : s) c = static_cast<char>(::tolower(static_cast<unsigned char>(c)));
                return s;
            }
            inline bool jequalsIgnoreCase(const JString& a, const JString& b) {
                if (a.size() != b.size()) return false;
                for (size_t i = 0; i < a.size(); ++i) {
                    if (::tolower(static_cast<unsigned char>(a[i])) != ::tolower(static_cast<unsigned char>(b[i]))) return false;
                }
                return true;
            }
            struct JInputStream {
                vector<char> buffer;
                size_t pos = 0;
                size_t marked_pos = 0;
                bool is_bytes = false;

                JInputStream() = default;
                JInputStream(const vector<char>& b) : buffer(b), is_bytes(true) {}
                JInputStream(const vector<int8_t>& b) : buffer(b.begin(), b.end()), is_bytes(true) {}
                template<class... Args>
                JInputStream(const JInputStream& other, Args&&...) : buffer(other.buffer), pos(other.pos), marked_pos(other.marked_pos), is_bytes(other.is_bytes) {}

                int read() {
                    if (pos < buffer.size()) {
                        return static_cast<unsigned char>(buffer[pos++]);
                    }
                    if (is_bytes) return -1;
                    int ch = cin.get();
                    if (ch == EOF) return -1;
                    buffer.push_back(static_cast<char>(ch));
                    pos++;
                    return ch;
                }

                int read(vector<int8_t>& b) {
                    return read(b, 0, static_cast<int>(b.size()));
                }

                int read(vector<char>& b) {
                    return read(b, 0, static_cast<int>(b.size()));
                }

                template<typename T>
                int read(vector<T>& b, int off, int length) {
                    int cnt = 0;
                    for (int i = 0; i < length; ++i) {
                        int ch = read();
                        if (ch == -1) break;
                        b[off + i] = static_cast<T>(ch);
                        cnt++;
                    }
                    return cnt == 0 ? -1 : cnt;
                }

                void mark(int) {
                    marked_pos = pos;
                }

                void reset() {
                    pos = marked_pos;
                }

                int available() const {
                    return is_bytes ? max(0, static_cast<int>(buffer.size()) - static_cast<int>(pos)) : 0;
                }

                void close() {}

                long long skip(long long n) {
                    long long skipped = 0;
                    while (skipped < n && read() != -1) skipped++;
                    return skipped;
                }

                JString readLine() {
                    JString s;
                    while (true) {
                        int ch = read();
                        if (ch == -1) return s;
                        if (ch == '\\r') {
                            int next_ch = read();
                            if (next_ch != '\\n' && next_ch != -1) {
                                // Ignore or handle non-lf after cr
                            }
                            break;
                        }
                        if (ch == '\\n') break;
                        s.push_back(static_cast<char>(ch));
                    }
                    return s;
                }
            };
            inline int jindexOf(const JString& s, const JString& target, int fromIndex = 0) {
                if (fromIndex < 0) fromIndex = 0;
                if ((size_t)fromIndex > s.length()) return -1;
                auto pos = s.find(target, fromIndex);
                return pos == JString::npos ? -1 : (int)pos;
            }
            inline int jindexOf(const JString& s, char ch, int fromIndex = 0) {
                if (fromIndex < 0) fromIndex = 0;
                if ((size_t)fromIndex > s.length()) return -1;
                auto pos = s.find(ch, fromIndex);
                return pos == JString::npos ? -1 : (int)pos;
            }
            inline int jlastIndexOf(const JString& s, const JString& target) {
                auto pos = s.rfind(target);
                return pos == JString::npos ? -1 : (int)pos;
            }
            inline int jlastIndexOf(const JString& s, const JString& target, int fromIndex) {
                if (fromIndex < 0) return -1;
                auto pos = s.rfind(target, fromIndex);
                return pos == JString::npos ? -1 : (int)pos;
            }
            inline int jlastIndexOf(const JString& s, char ch) {
                auto pos = s.rfind(ch);
                return pos == JString::npos ? -1 : (int)pos;
            }
            inline int jlastIndexOf(const JString& s, char ch, int fromIndex) {
                if (fromIndex < 0) return -1;
                auto pos = s.rfind(ch, fromIndex);
                return pos == JString::npos ? -1 : (int)pos;
            }
            inline JString jsubstring(const JString& s, int begin) { return s.substr(begin); }
            inline JString jsubstring(const JString& s, int begin, int end) { return s.substr(begin, max(0, end - begin)); }
            inline vector<JString> jsplit(const JString& s, const JString& regex) {
                vector<JString> result;
                std::regex re(regex);
                std::sregex_token_iterator it(s.begin(), s.end(), re, -1), end;
                for (; it != end; ++it) result.push_back(*it);
                return result;
            }
            inline size_t jhash() { return 1; }
            template<class T> size_t jhashValue(const T& x) { return hash<T>{}(x); }
            template<class T> size_t jhashValue(const vector<T>& values) {
                size_t result = 1;
                for (const auto& value : values) result = result * 31 + jhashValue(value);
                return result;
            }
            template<class T, class... U> size_t jhash(const T& x, const U&... rest) {
                return jhashValue(x) * 31 + jhash(rest...);
            }
            struct JStringBuilder {
                JString value;
                JStringBuilder() = default;
                explicit JStringBuilder(const JString& initial) : value(initial) {}
                template<class T> JStringBuilder& append(const T& x) { value += jstr(x); return *this; }
                JStringBuilder& appendCodePoint(int cp) { value += static_cast<char>(cp); return *this; }
                JStringBuilder& reverse() { std::reverse(value.begin(), value.end()); return *this; }
                int length() const { return static_cast<int>(value.size()); }
                void setLength(int newLength) { value.resize(newLength); }
                char charAt(int index) const { return value[index]; }
                void setCharAt(int index, char ch) { value[index] = ch; }
                JStringBuilder& deleteCharAt(int index) { value.erase(index, 1); return *this; }
                JStringBuilder& delete_(int start, int end) { value.erase(start, end - start); return *this; }
                JString substring(int start) const { return value.substr(start); }
                JString substring(int start, int end) const { return value.substr(start, end - start); }
                JString toString() const { return value; }
                friend ostream& operator<<(ostream& os, const JStringBuilder& sb) {
                    return os << sb.value;
                }
            };
            inline void jnoop() {}
            inline void jprint() {}
            template<class T, class... U> void jprint(const T& x, const U&... rest) { cout << jstr(x); jprint(rest...); }
            inline void jprintln() { cout << '\\n'; }
            template<class T, class... U> void jprintln(const T& x, const U&... rest) { cout << jstr(x); jprint(rest...); cout << '\\n'; }
            template<class... Args> void jprintf(const char* fmt, Args&&... args) {
                printf(fmt, forward<Args>(args)...);
            }
            template<class... Args> void jprintf(const JString& fmt, Args&&... args) {
                printf(fmt.c_str(), forward<Args>(args)...);
            }

            template<class... Args> JString jformat(const char* fmt, Args&&... args) {
                int sz = snprintf(nullptr, 0, fmt, forward<Args>(args)...);
                if (sz <= 0) return "";
                vector<char> buf(sz + 1);
                snprintf(buf.data(), buf.size(), fmt, forward<Args>(args)...);
                return JString(buf.data(), sz);
            }
            template<class... Args> JString jformat(const JString& fmt, Args&&... args) {
                return jformat(fmt.c_str(), forward<Args>(args)...);
            }
            template<class L, class... Args>
            typename enable_if<!is_convertible_v<L, JString> && !is_convertible_v<L, const char*>, JString>::type
            jformat(const L&, const char* fmt, Args&&... args) {
                return jformat(fmt, forward<Args>(args)...);
            }
            template<class L, class... Args>
            typename enable_if<!is_convertible_v<L, JString> && !is_convertible_v<L, const char*>, JString>::type
            jformat(const L&, const JString& fmt, Args&&... args) {
                return jformat(fmt.c_str(), forward<Args>(args)...);
            }

            template<class T> auto jsignum(T x) {
                if constexpr (std::is_floating_point_v<T>) {
                    if (std::isnan(x)) return x;
                    return x > T(0) ? T(1) : (x < T(0) ? T(-1) : T(0));
                } else {
                    return (x > T(0)) - (x < T(0));
                }
            }
            template<class A, class B> auto jmin(const A& a, const B& b) {
                using C = common_type_t<A, B>;
                return std::min<C>(a, b);
            }
            template<class A, class B> auto jmax(const A& a, const B& b) {
                using C = common_type_t<A, B>;
                return std::max<C>(a, b);
            }
            template<class A, class B> auto jfloorDiv(A a, B b) {
                using C = common_type_t<A, B>;
                C x = a, y = b;
                C res = x / y;
                C rem = x % y;
                if ((rem != 0) && ((x ^ y) < 0)) --res;
                return res;
            }
            template<class A, class B> auto jceilDiv(A a, B b) {
                using C = common_type_t<A, B>;
                C x = a, y = b;
                C res = x / y;
                C rem = x % y;
                if ((rem != 0) && ((x ^ y) > 0)) ++res;
                return res;
            }
            template<class A, class B> auto jfloorMod(A a, B b) {
                using C = common_type_t<A, B>;
                C x = a, y = b;
                C rem = x % y;
                if ((rem != 0) && ((x ^ y) < 0)) rem += y;
                return rem;
            }
            template<class V, class L, class H> auto jclamp(V v, L l, H h) {
                return v < l ? l : (v > h ? h : v);
            }
            template<class A, class B> auto jaddExact(A a, B b) { return a + b; }
            template<class A, class B> auto jsubtractExact(A a, B b) { return a - b; }
            template<class A, class B> auto jmultiplyExact(A a, B b) { return a * b; }
            template<class T> auto jtoIntExact(T x) { return static_cast<int>(x); }
            template<class T> auto jnegateExact(T x) { return -x; }
            template<class A, class B> int jcompareValue(const A& a, const B& b) {
                return (a > b) - (a < b);
            }
            template<class T> int jcompare(const vector<T>& a, const vector<T>& b) {
                if (lexicographical_compare(a.begin(), a.end(), b.begin(), b.end())) return -1;
                if (lexicographical_compare(b.begin(), b.end(), a.begin(), a.end())) return 1;
                return 0;
            }

            template<class A, class B> int jcompareTo(const A& a, const B& b);
            template<class A, class B> bool jequals(const A& a, const B& b);

            template<class F> struct JComparator {
                F key;
                template<class T> int operator()(const T& a, const T& b) const {
                    if constexpr (is_invocable_v<F, const T&, const T&>) {
                        return key(a, b);
                    } else {
                        auto x = key(a), y = key(b);
                        return jcompareTo(x, y);
                    }
                }
                template<class G> auto thenComparing(G other) const;
                template<class G> auto thenComparingInt(G key) const { return thenComparing(key); }
                template<class G> auto thenComparingLong(G key) const { return thenComparing(key); }
                template<class G> auto thenComparingDouble(G key) const { return thenComparing(key); }
            };
            template<class A, class B> struct JChainedComparator {
                A first;
                B second;
                template<class T> int operator()(const T& a, const T& b) const {
                    int result = first(a, b);
                    return result != 0 ? result : second(a, b);
                }
                template<class G> auto thenComparing(G other) const;
                template<class G> auto thenComparingInt(G key) const { return thenComparing(key); }
                template<class G> auto thenComparingLong(G key) const { return thenComparing(key); }
                template<class G> auto thenComparingDouble(G key) const { return thenComparing(key); }
            };
            template<class F> template<class G> auto JComparator<F>::thenComparing(G other) const {
                return JChainedComparator<JComparator<F>, JComparator<G>>{*this, JComparator<G>{other}};
            }
            template<class A, class B> template<class G> auto JChainedComparator<A, B>::thenComparing(G other) const {
                return JChainedComparator<JChainedComparator<A, B>, JComparator<G>>{*this, JComparator<G>{other}};
            }
            template<class F> auto jcomparing(F key) { return JComparator<F>{key}; }

            template<class T> struct JOptional {
                bool has = false;
                T val = {};
                bool isPresent() const { return has; }
                T get() const { return val; }
                long long getAsLong() const { return static_cast<long long>(val); }
                int getAsInt() const { return static_cast<int>(val); }
                double getAsDouble() const { return static_cast<double>(val); }
                T orElse(T def) const { return has ? val : def; }
            };

            template<class T> struct JStream {
                vector<T> values;
                explicit JStream(const vector<T>& source) : values(source) {}
                static JStream range(T startInclusive, T endExclusive) {
                    vector<T> res;
                    if (startInclusive < endExclusive) {
                        res.reserve(endExclusive - startInclusive);
                        for (T i = startInclusive; i < endExclusive; ++i) res.push_back(i);
                    }
                    return JStream(res);
                }
                static JStream rangeClosed(T startInclusive, T endInclusive) {
                    vector<T> res;
                    if (startInclusive <= endInclusive) {
                        res.reserve(endInclusive - startInclusive + 1);
                        for (T i = startInclusive; i <= endInclusive; ++i) res.push_back(i);
                    }
                    return JStream(res);
                }
                JStream& distinct() {
                    sort(values.begin(), values.end());
                    values.erase(unique(values.begin(), values.end()), values.end());
                    return *this;
                }
                JStream& sorted() { sort(values.begin(), values.end()); return *this; }
                template<class F> JStream& filter(F predicate) {
                    values.erase(remove_if(values.begin(), values.end(), [&](const T& x) { return !predicate(x); }), values.end());
                    return *this;
                }
                template<class F> auto map(F mapper) const {
                    using R = decltype(mapper(declval<T>()));
                    vector<R> res;
                    res.reserve(values.size());
                    for (const auto& x : values) res.push_back(mapper(x));
                    return JStream<R>(res);
                }
                template<class F> auto mapToLong(F mapper) const { return map(mapper); }
                template<class F> auto mapToInt(F mapper) const { return map(mapper); }
                template<class F> auto mapToDouble(F mapper) const { return map(mapper); }
                template<class F> auto mapToObj(F mapper) const { return map(mapper); }
                template<class F> void forEach(F action) const {
                    for (const auto& x : values) action(x);
                }
                T sum() const {
                    T res = 0;
                    for (const auto& x : values) res += x;
                    return res;
                }
                JOptional<T> max() const {
                    if (values.empty()) return JOptional<T>{false, T{}};
                    T res = values[0];
                    for (const auto& x : values) if (x > res) res = x;
                    return JOptional<T>{true, res};
                }
                template<class Comp> JOptional<T> max(Comp comp) const {
                    if (values.empty()) return JOptional<T>{false, T{}};
                    T res = values[0];
                    for (const auto& x : values) if (comp(res, x) < 0) res = x;
                    return JOptional<T>{true, res};
                }
                JOptional<T> min() const {
                    if (values.empty()) return JOptional<T>{false, T{}};
                    T res = values[0];
                    for (const auto& x : values) if (x < res) res = x;
                    return JOptional<T>{true, res};
                }
                template<class Comp> JOptional<T> min(Comp comp) const {
                    if (values.empty()) return JOptional<T>{false, T{}};
                    T res = values[0];
                    for (const auto& x : values) if (comp(res, x) > 0) res = x;
                    return JOptional<T>{true, res};
                }
                template<class F> JOptional<T> reduce(F accumulator) const {
                    if (values.empty()) return JOptional<T>{false, T{}};
                    T res = values[0];
                    for (size_t i = 1; i < values.size(); ++i) res = accumulator(res, values[i]);
                    return JOptional<T>{true, res};
                }
                template<class U, class F> U reduce(U identity, F accumulator) const {
                    U res = identity;
                    for (const auto& x : values) res = accumulator(res, x);
                    return res;
                }
                template<class U, class F1, class F2> U reduce(U identity, F1 accumulator, F2 combiner) const {
                    U res = identity;
                    for (const auto& x : values) res = accumulator(res, x);
                    return res;
                }
                template<class F> bool allMatch(F predicate) const {
                    for (const auto& x : values) if (!predicate(x)) return false;
                    return true;
                }
                template<class F> bool anyMatch(F predicate) const {
                    for (const auto& x : values) if (predicate(x)) return true;
                    return false;
                }
                template<class F> bool noneMatch(F predicate) const {
                    for (const auto& x : values) if (predicate(x)) return false;
                    return true;
                }
                long long count() const { return (long long)values.size(); }
                JStream<T> boxed() const { return *this; }
                vector<T> toArray() const { return values; }
                template<class Generator> auto toArray(Generator&& g) const {
                    auto res = g((int)values.size());
                    for (size_t i = 0; i < values.size(); ++i) res[i] = values[i];
                    return res;
                }
            };
            using JIntStream = JStream<int>;
            using JLongStream = JStream<long long>;

            template<class T> struct JList : vector<T> {
                using vector<T>::vector;
                JList(const vector<T>& source) : vector<T>(source) {}
                JList(vector<T>&& source) : vector<T>(move(source)) {}
                template<class U> JList(const JList<U>& source) {
                    for (const auto& value : source) {
                        if constexpr (is_same_v<U, JObject>) this->push_back(any_cast<T>(value));
                        else this->push_back(static_cast<T>(value));
                    }
                }
                template<class Container, class = enable_if_t<!is_convertible_v<Container, std::size_t> && !is_same_v<decay_t<Container>, JList> && !is_same_v<decay_t<Container>, vector<T>>>>
                JList(const Container& container) {
                    for (const auto& item : container) this->push_back(item);
                }
                bool add(const T& x) { this->push_back(x); return true; }
                void add(int i, const T& x) { this->insert(this->begin() + i, x); }
                T get(int i) const { return (*this)[i]; }
                T set(int i, const T& x) { T old = (*this)[i]; (*this)[i] = x; return old; }
                int size() const { return (int)vector<T>::size(); }
                bool isEmpty() const { return this->empty(); }
                T remove(int index) {
                    T old = (*this)[index];
                    this->erase(this->begin() + index);
                    return old;
                }
                template<class Arg>
                bool remove(const Arg& x) {
                    if constexpr (is_integral_v<Arg> && !is_same_v<decay_t<Arg>, bool>) {
                        if constexpr (is_integral_v<T> && !is_same_v<decay_t<T>, bool>) {
                            auto it = std::find(this->begin(), this->end(), static_cast<T>(x));
                            if (it != this->end()) { this->erase(it); return true; }
                            return false;
                        } else {
                            auto it = std::find_if(this->begin(), this->end(), [&](const T& item) {
                                if constexpr (is_convertible_v<Arg, T>) return item == x;
                                else if constexpr (jhas_key<T>::value) return item->key == x;
                                else if constexpr (jhas_key_obj<T>::value) return item.key == x;
                                else return false;
                            });
                            if (it != this->end()) { this->erase(it); return true; }
                            return false;
                        }
                    } else {
                        auto it = std::find_if(this->begin(), this->end(), [&](const T& item) {
                            if constexpr (is_convertible_v<Arg, T>) return item == x;
                            else if constexpr (jhas_key<T>::value) return item->key == x;
                            else if constexpr (jhas_key_obj<T>::value) return item.key == x;
                            else return false;
                        });
                        if (it != this->end()) { this->erase(it); return true; }
                        return false;
                    }
                }
                template<class C> bool addAll(const C& c) {
                    for (const auto& x : c) this->push_back(x);
                    return true;
                }
                template<class Arg>
                bool contains(const Arg& x) const {
                    return std::find_if(this->begin(), this->end(), [&](const T& item) {
                        return jequals(item, x);
                    }) != this->end();
                }
                int indexOf(const T& x) const {
                    auto it = std::find(this->begin(), this->end(), x);
                    return it == this->end() ? -1 : (int)distance(this->begin(), it);
                }
                int lastIndexOf(const T& x) const {
                    auto it = std::find(this->rbegin(), this->rend(), x);
                    return it == this->rend() ? -1 : (int)(this->size() - 1 - distance(this->rbegin(), it));
                }
                template<class Comp> void sort(Comp comp) {
                    if constexpr (is_same_v<decay_t<Comp>, nullptr_t>) {
                        std::sort(this->begin(), this->end());
                    } else {
                        using R = invoke_result_t<Comp, const T&, const T&>;
                        std::sort(this->begin(), this->end(), [&](const auto& a, const auto& b) {
                            if constexpr (is_same_v<R, bool>) return comp(a, b);
                            else return comp(a, b) < 0;
                        });
                    }
                }
                void sort(nullptr_t) {
                    std::sort(this->begin(), this->end());
                }
                void sort() {
                    std::sort(this->begin(), this->end());
                }
                JList<T> subList(int fromIndex, int toIndex) const {
                    if (fromIndex < 0) fromIndex = 0;
                    if (toIndex > (int)this->size()) toIndex = (int)this->size();
                    if (fromIndex >= toIndex) return JList<T>{};
                    return JList<T>(this->begin() + fromIndex, this->begin() + toIndex);
                }
                JStream<T> stream() const { return JStream<T>(*this); }
            };
""";
    private static final String PRELUDE2 = """
            template<class T, class = void> struct jhas_compareTo : false_type {};
            template<class T> struct jhas_compareTo<T, void_t<decltype(declval<T>().compareTo(declval<T>()))>> : true_type {};

            template<class T, class = void> struct jhas_ptr_compareTo : false_type {};
            template<class T> struct jhas_ptr_compareTo<T, void_t<decltype(declval<T>()->compareTo(declval<T>()))>> : true_type {};

            template<class A, class B>
            bool jequals(const A& a, const B& b) {
                if constexpr (is_same_v<decay_t<A>, decay_t<B>>) {
                    if constexpr (jhas_ptr_compareTo<A>::value) {
                        if (a == b) return true;
                        if (a && b) return a->compareTo(b) == 0;
                        return false;
                    } else if constexpr (jhas_compareTo<A>::value) {
                        return a.compareTo(b) == 0;
                    } else {
                        return a == b;
                    }
                } else if constexpr (is_convertible_v<B, A>) {
                    return a == static_cast<A>(b);
                } else if constexpr (is_convertible_v<A, B>) {
                    return static_cast<B>(a) == b;
                } else {
                    return false;
                }
            }

            template<class A, class B>
            int jcompareTo(const A& a, const B& b) {
                if constexpr (jhas_ptr_compareTo<A>::value) {
                    return a->compareTo(b);
                } else if constexpr (jhas_compareTo<A>::value) {
                    return a.compareTo(b);
                } else {
                    return (a > b) - (a < b);
                }
            }

            template<class T> struct JLess {
                bool operator()(const T& a, const T& b) const {
                    if constexpr (jhas_ptr_compareTo<T>::value) {
                        return a->compareTo(b) < 0;
                    } else if constexpr (jhas_compareTo<T>::value) {
                        return a.compareTo(b) < 0;
                    } else {
                        return a < b;
                    }
                }
            };

            template<class T> struct JSet : set<T, function<bool(const T&, const T&)>> {
                using Base = set<T, function<bool(const T&, const T&)>>;
                JSet() : Base(JLess<T>{}) {}
                JSet(const Base& o) : Base(o) {}
                JSet(Base&& o) : Base(move(o)) {}
                template<class C, class = enable_if_t<!is_same_v<decay_t<C>, JSet> && !is_same_v<decay_t<C>, Base> && !is_same_v<decay_t<C>, vector<T>>>>
                explicit JSet(C comp) : Base([comp](const T& a, const T& b) {
                    using R = invoke_result_t<C, const T&, const T&>;
                    if constexpr (is_same_v<R, bool>) return comp(a, b);
                    else return comp(a, b) < 0;
                }) {}
                JSet(const vector<T>& v) : Base(JLess<T>{}) {
                    for (const auto& x : v) this->insert(x);
                }
                JSet(initializer_list<T> init) : Base(JLess<T>{}) {
                    for (const auto& x : init) this->insert(x);
                }
                bool add(const T& x) { return this->insert(x).second; }
                bool remove(const T& x) { return this->erase(x) != 0; }
                template<class Arg>
                bool contains(const Arg& x) const {
                    return std::find_if(this->begin(), this->end(), [&](const T& item) {
                        return jequals(item, x);
                    }) != this->end();
                }
                T first() const { return *this->begin(); }
                T last() const { return *this->rbegin(); }
                T pollFirst() {
                    if (this->empty()) return T{};
                    auto it = this->begin();
                    T res = *it;
                    this->erase(it);
                    return res;
                }
                T pollLast() {
                    if (this->empty()) return T{};
                    auto it = prev(this->end());
                    T res = *it;
                    this->erase(it);
                    return res;
                }
                T lower(const T& x) const {
                    auto it = this->lower_bound(x);
                    if (it != this->begin()) return *prev(it);
                    return T{};
                }
                T floor(const T& x) const {
                    auto it = this->upper_bound(x);
                    if (it != this->begin()) return *prev(it);
                    return T{};
                }
                T ceiling(const T& x) const {
                    auto it = this->lower_bound(x);
                    if (it != this->end()) return *it;
                    return T{};
                }
                T higher(const T& x) const {
                    auto it = this->upper_bound(x);
                    if (it != this->end()) return *it;
                    return T{};
                }
                int size() const { return (int)Base::size(); }
                bool isEmpty() const { return this->empty(); }
                JStream<T> stream() const { return JStream<T>(vector<T>(this->begin(), this->end())); }
            };
            template<class K, class V> struct JMap : map<K, V, function<bool(const K&, const K&)>> {
                using Base = map<K, V, function<bool(const K&, const K&)>>;
                JMap() : Base(JLess<K>{}) {}
                JMap(const Base& o) : Base(o) {}
                JMap(Base&& o) : Base(move(o)) {}
                template<class K2, class V2>
                JMap(const JMap<K2, V2>& source) : Base(JLess<K>{}) {
                    for (const auto& [k, v] : source) {
                        if constexpr (is_same_v<K2, JObject> && is_same_v<V2, JObject>) {
                            this->insert({any_cast<K>(k), any_cast<V>(v)});
                        } else if constexpr (is_same_v<K2, JObject>) {
                            this->insert({any_cast<K>(k), static_cast<V>(v)});
                        } else if constexpr (is_same_v<V2, JObject>) {
                            this->insert({static_cast<K>(k), any_cast<V>(v)});
                        } else {
                            this->insert({static_cast<K>(k), static_cast<V>(v)});
                        }
                    }
                }
                template<class K2, class V2>
                JMap& operator=(const JMap<K2, V2>& source) {
                    this->clear();
                    for (const auto& [k, v] : source) {
                        if constexpr (is_same_v<K2, JObject> && is_same_v<V2, JObject>) {
                            this->insert({any_cast<K>(k), any_cast<V>(v)});
                        } else if constexpr (is_same_v<K2, JObject>) {
                            this->insert({any_cast<K>(k), static_cast<V>(v)});
                        } else if constexpr (is_same_v<V2, JObject>) {
                            this->insert({static_cast<K>(k), any_cast<V>(v)});
                        } else {
                            this->insert({static_cast<K>(k), static_cast<V>(v)});
                        }
                    }
                    return *this;
                }
                template<class C, class = enable_if_t<!is_same_v<decay_t<C>, JMap> && !is_same_v<decay_t<C>, Base>>>
                explicit JMap(C comp) : Base([comp](const K& a, const K& b) {
                    using R = invoke_result_t<C, const K&, const K&>;
                    if constexpr (is_same_v<R, bool>) return comp(a, b);
                    else return comp(a, b) < 0;
                }) {}
                template<class K2, class V2> V put(const K2& k, const V2& v) { K key = static_cast<K>(k); V val = static_cast<V>(v); V old{}; auto it = this->find(key); if (it != this->end()) old = it->second; (*this)[key] = val; return old; }
                template<class K2> V get(const K2& k) const { auto it = this->find(static_cast<K>(k)); return it == this->end() ? V{} : it->second; }
                template<class K2> V remove(const K2& k) { K key = static_cast<K>(k); V old{}; auto it = this->find(key); if (it != this->end()) { old = it->second; this->erase(it); } return old; }
                template<class K2, class V2> V getOrDefault(const K2& k, const V2& v) const { auto it = this->find(static_cast<K>(k)); return it == this->end() ? static_cast<V>(v) : it->second; }
                template<class K2> bool containsKey(const K2& k) const { return this->count(static_cast<K>(k)); }
                template<class F> V compute(const K& k, F&& remappingFunction) {
                    V oldVal = this->count(k) ? (*this)[k] : V{};
                    V newVal = remappingFunction(k, oldVal);
                    (*this)[k] = newVal;
                    return newVal;
                }
                template<class F> V merge(const K& k, const V& v, F&& remappingFunction) {
                    auto it = this->find(k);
                    if (it == this->end()) {
                        (*this)[k] = v;
                        return v;
                    } else {
                        V nv = remappingFunction(it->second, v);
                        (*this)[k] = nv;
                        return nv;
                    }
                }
                template<class F> V computeIfAbsent(const K& k, F&& mappingFunction) {
                    auto it = this->find(k);
                    if (it != this->end()) return it->second;
                    V v = mappingFunction(k);
                    (*this)[k] = v;
                    return v;
                }
                template<class F> V computeIfPresent(const K& k, F&& remappingFunction) {
                    auto it = this->find(k);
                    if (it == this->end()) return V{};
                    V v = remappingFunction(k, it->second);
                    (*this)[k] = v;
                    return v;
                }
                V putIfAbsent(const K& k, const V& v) {
                    auto it = this->find(k);
                    if (it != this->end()) return it->second;
                    (*this)[k] = v;
                    return v;
                }
                JSet<std::pair<K, V>> entrySet() const {
                    JSet<std::pair<K, V>> result;
                    for (const auto& entry : *this) result.add(entry);
                    return result;
                }
                JSet<K> keySet() const {
                    JSet<K> result;
                    for (const auto& entry : *this) result.add(entry.first);
                    return result;
                }
                JList<V> values() const {
                    JList<V> result;
                    for (const auto& entry : *this) result.push_back(entry.second);
                    return result;
                }
                int size() const { return (int)Base::size(); }
                bool isEmpty() const { return this->empty(); }
                K firstKey() const { return this->begin()->first; }
                K lastKey() const { return this->rbegin()->first; }
                std::pair<K, V> pollFirstEntry() {
                    if (this->empty()) return {};
                    auto it = this->begin();
                    std::pair<K, V> res = *it;
                    this->erase(it);
                    return res;
                }
                std::pair<K, V> pollLastEntry() {
                    if (this->empty()) return {};
                    auto it = prev(this->end());
                    std::pair<K, V> res = *it;
                    this->erase(it);
                    return res;
                }
                std::pair<K, V> firstEntry() const {
                    if (this->empty()) return {};
                    return *this->begin();
                }
                std::pair<K, V> lastEntry() const {
                    if (this->empty()) return {};
                    return *prev(this->end());
                }
                JStream<std::pair<K, V>> stream() const {
                    vector<std::pair<K, V>> v(this->begin(), this->end());
                    return JStream<std::pair<K, V>>(v);
                }
            };
            template<class T> struct JDeque : deque<T> {
                using deque<T>::deque;
                bool add(const T& x) { this->push_back(x); return true; }
                template<class Container> bool addAll(const Container& c) {
                    for (const auto& item : c) this->push_back(item);
                    return true;
                }
                void addFirst(const T& x) { this->push_front(x); }
                void addLast(const T& x) { this->push_back(x); }
                T pollFirst() { T x = this->front(); this->pop_front(); return x; }
                T pollLast() { T x = this->back(); this->pop_back(); return x; }
                T poll() { return pollFirst(); }
                T peek() const { return this->front(); }
                T peekFirst() const { return this->front(); }
                T peekLast() const { return this->back(); }
                T element() const { return this->front(); }
                T remove() { return pollFirst(); }
                T removeFirst() { return pollFirst(); }
                T removeLast() { return pollLast(); }
                T getFirst() const { return this->front(); }
                void push(const T& x) { this->push_front(x); }
                T pop() { return pollFirst(); }
                T getLast() const { return this->back(); }
                bool offer(const T& x) { return add(x); }
                bool offerFirst(const T& x) { addFirst(x); return true; }
                bool offerLast(const T& x) { addLast(x); return true; }
                template<class Arg>
                bool contains(const Arg& x) const {
                    return std::find_if(this->begin(), this->end(), [&](const T& item) {
                        return jequals(item, x);
                    }) != this->end();
                }
                int size() const { return (int)deque<T>::size(); }
                bool isEmpty() const { return this->empty(); }
                JStream<T> stream() const { return JStream<T>(vector<T>(this->begin(), this->end())); }
            };
            template<class T> struct JStack : vector<T> {
                using vector<T>::vector;
                bool add(const T& x) { this->push_back(x); return true; }
                T push(const T& x) { this->push_back(x); return x; }
                T pop() { T x = this->back(); this->pop_back(); return x; }
                T peek() const { return this->back(); }
                int size() const { return (int)vector<T>::size(); }
                bool isEmpty() const { return this->empty(); }
                JStream<T> stream() const { return JStream<T>(*this); }
            };
            template<class P, class = void> struct jhas_next : false_type {};
            template<class P> struct jhas_next<P, void_t<decltype(declval<P>()->next())>> : true_type {};

            template<class P, class = void> struct jhas_nextInt : false_type {};
            template<class P> struct jhas_nextInt<P, void_t<decltype(declval<P>()->nextInt())>> : true_type {};

            template<class P, class = void> struct jhas_nextLong : false_type {};
            template<class P> struct jhas_nextLong<P, void_t<decltype(declval<P>()->nextLong())>> : true_type {};

            template<class P, class = void> struct jhas_nextDouble : false_type {};
            template<class P> struct jhas_nextDouble<P, void_t<decltype(declval<P>()->nextDouble())>> : true_type {};

            template<class T> struct JIterator {
                function<bool()> fn_hasNext;
                function<T()> fn_next;
                vector<T> items;
                size_t pos = 0;
                bool valid = false;

                JIterator() = default;
                JIterator(const vector<T>& v) : items(v), valid(true) {
                    fn_hasNext = [this]() { return pos < items.size(); };
                    fn_next = [this]() { return items[pos++]; };
                }
                template<class Ptr, class = enable_if_t<!is_same_v<decay_t<Ptr>, vector<T>> && !is_same_v<decay_t<Ptr>, JIterator>>>
                JIterator(Ptr ptr) : valid(bool(ptr)) {
                    if (ptr) {
                        fn_hasNext = [ptr]() { return ptr->hasNext(); };
                        fn_next = [ptr]() {
                            if constexpr (jhas_next<Ptr>::value) {
                                return ptr->next();
                            } else if constexpr (jhas_nextInt<Ptr>::value) {
                                return ptr->nextInt();
                            } else if constexpr (jhas_nextLong<Ptr>::value) {
                                return ptr->nextLong();
                            } else if constexpr (jhas_nextDouble<Ptr>::value) {
                                return ptr->nextDouble();
                            } else {
                                return T{};
                            }
                        };
                    }
                }
                bool hasNext() const {
                    if (fn_hasNext) return fn_hasNext();
                    return pos < items.size();
                }
                T next() {
                    if (fn_next) return fn_next();
                    return items[pos++];
                }
                bool operator==(nullptr_t) const { return !valid && !fn_hasNext; }
                bool operator!=(nullptr_t) const { return valid || bool(fn_hasNext); }
            };
            template<class T> struct JIterable {
                function<JIterator<T>()> supplier;
                JIterable() = default;
                template<class F, class = enable_if_t<!is_same_v<decay_t<F>, vector<T>> && !is_same_v<decay_t<F>, JIterable>>>
                JIterable(F f) {
                    supplier = [f]() -> JIterator<T> {
                        return JIterator<T>(f());
                    };
                }
                JIterable(const vector<T>& v) : supplier([v]() { return JIterator<T>(v); }) {}
                JIterator<T> iterator() const { return supplier ? supplier() : JIterator<T>(); }
                struct CppIterator {
                    JIterator<T> it;
                    optional<T> current_val;
                    CppIterator() = default;
                    explicit CppIterator(JIterator<T> iter) : it(iter) {
                        if (it.hasNext()) current_val = it.next();
                    }
                    bool operator!=(const CppIterator&) const { return current_val.has_value(); }
                    void operator++() {
                        if (it.hasNext()) current_val = it.next();
                        else current_val.reset();
                    }
                    T operator*() const { return *current_val; }
                };
                CppIterator begin() { return CppIterator(this->iterator()); }
                CppIterator end() { return CppIterator(); }
                CppIterator begin() const { return CppIterator(iterator()); }
                CppIterator end() const { return CppIterator(); }
            };

            struct PrimitiveIterator {
                struct OfInt : JIterator<int> {};
                struct OfLong : JIterator<long long> {};
                struct OfDouble : JIterator<double> {};
            };
            using JPrimitiveIterator = PrimitiveIterator;

            struct JArray {
                template<class T> static int getLength(const vector<T>& v) { return static_cast<int>(v.size()); }
                static int getLength(const JObject& obj) {
                    if (!obj.has_value()) return 0;
                    try { return static_cast<int>(std::any_cast<vector<int>>(obj).size()); } catch(...) {}
                    try { return static_cast<int>(std::any_cast<vector<long long>>(obj).size()); } catch(...) {}
                    try { return static_cast<int>(std::any_cast<vector<JObject>>(obj).size()); } catch(...) {}
                    return 0;
                }
                template<class T> static auto get(const vector<T>& v, int index) { return v[index]; }
                static JObject get(const JObject& obj, int index) {
                    try { return std::any_cast<vector<int>>(obj)[index]; } catch(...) {}
                    try { return std::any_cast<vector<long long>>(obj)[index]; } catch(...) {}
                    try { return std::any_cast<vector<JObject>>(obj)[index]; } catch(...) {}
                    return {};
                }
            };
            using Array = JArray;

            template<class C> auto jtoArray(const C& values) {
                using T = typename C::value_type;
                return vector<T>(values.begin(), values.end());
            }

            template<class T> bool jgreater_fn(const T& a, const T& b) {
                if constexpr (jhas_ptr_compareTo<T>::value) {
                    return a->compareTo(b) > 0;
                } else if constexpr (jhas_compareTo<T>::value) {
                    return a.compareTo(b) > 0;
                } else {
                    return a > b;
                }
            }

            template<class T> struct JGreater {
                bool operator()(const T& a, const T& b) const { return jgreater_fn(a, b); }
            };

            template<class T> struct JPriorityQueue {
                function<bool(const T&, const T&)> comp = JGreater<T>{};
                priority_queue<T, vector<T>, function<bool(const T&, const T&)>> q;

                JPriorityQueue() : q(comp) {}
                template<class C>
                explicit JPriorityQueue(C c) : comp([c](const T& a, const T& b) { return c(a, b) > 0; }), q(comp) {}
                template<class C>
                JPriorityQueue(int, C c) : comp([c](const T& a, const T& b) { return c(a, b) > 0; }), q(comp) {}
                explicit JPriorityQueue(int) : q(comp) {}

                bool add(const T& x) { q.push(x); return true; }
                bool offer(const T& x) { q.push(x); return true; }
                T poll() { T x = q.top(); q.pop(); return x; }
                T peek() const { return q.top(); }
                int size() const { return (int)q.size(); }
                bool isEmpty() const { return q.empty(); }
                bool empty() const { return q.empty(); }
                void clear() { q = priority_queue<T, vector<T>, function<bool(const T&, const T&)>>(comp); }
                template<class Arg>
                bool remove(const Arg& x) {
                    vector<T> v;
                    bool removed = false;
                    while (!q.empty()) {
                        T item = q.top();
                        q.pop();
                        if (!removed && jequals(item, x)) {
                            removed = true;
                        } else {
                            v.push_back(item);
                        }
                    }
                    for (const auto& item : v) q.push(item);
                    return removed;
                }
            };
            struct System {
                static void gc() {}
            };
            struct JStringTokenizer {
                vector<JString> tokens;
                size_t pos = 0;
                bool valid = true;

                JStringTokenizer() : valid(false) {}
                JStringTokenizer(nullptr_t) : valid(false) {}
                JStringTokenizer(const JString& str) : JStringTokenizer(str, JString(" \\t\\n\\r\\f"), false) {}
                JStringTokenizer(const JString& str, const JString& delim) : JStringTokenizer(str, delim, false) {}
                JStringTokenizer(const JString& str, const JString& delim, bool returnDelims) : valid(true) {
                    string cur;
                    for (char c : str) {
                        if (delim.find(c) != string::npos) {
                            if (!cur.empty()) {
                                tokens.push_back(cur);
                                cur.clear();
                            }
                            if (returnDelims) {
                                tokens.push_back(string(1, c));
                            }
                        } else {
                            cur.push_back(c);
                        }
                    }
                    if (!cur.empty()) tokens.push_back(cur);
                }
                bool hasMoreTokens() const { return valid && pos < tokens.size(); }
                bool hasMoreElements() const { return hasMoreTokens(); }
                JString nextToken() { return hasMoreTokens() ? tokens[pos++] : ""; }
                JString nextToken(const JString& delim) {
                    JString rest;
                    for (size_t i = pos; i < tokens.size(); ++i) {
                        if (i > pos) rest += " ";
                        rest += tokens[i];
                    }
                    *this = JStringTokenizer(rest, delim);
                    return nextToken();
                }
                JString nextElement() { return nextToken(); }
                int countTokens() const { return valid ? static_cast<int>(tokens.size() - pos) : 0; }
                bool operator==(nullptr_t) const { return !valid; }
                bool operator!=(nullptr_t) const { return valid; }
            };
            inline bool jisNull(const JStringTokenizer& x) { return !x.valid; }
            struct JScanner {
                JString next() { JString x; cin >> x; return x; }
                JString nextLine() { JString s; string line; if (getline(cin >> ws, line)) s = line; return s; }
                int nextInt() { int x; cin >> x; return x; }
                long long nextLong() { long long x; cin >> x; return x; }
                double nextDouble() { double x; cin >> x; return x; }
                bool hasNext() { return bool(cin >> ws) && cin.peek() != EOF; }
                vector<int> nextInts(int n) { vector<int> a(n); for (auto& x : a) cin >> x; return a; }
                vector<vector<int>> nextInts(int h, int w) {
                    vector<vector<int>> a(h, vector<int>(w));
                    for (auto& row : a) for (auto& x : row) cin >> x;
                    return a;
                }
                vector<long long> nextLongs(int n) { vector<long long> a(n); for (auto& x : a) cin >> x; return a; }
                vector<vector<char>> nextChars(int h, int w) {
                    vector<vector<char>> a(h, vector<char>(w));
                    for (auto& row : a) { JString s; cin >> s; row.assign(s.begin(), s.end()); }
                    return a;
                }
                void close() {}
            };
            inline long long jcurrentTimeMillis() {
                return chrono::duration_cast<chrono::milliseconds>(chrono::system_clock::now().time_since_epoch()).count();
            }
            struct JDate {
                long long time_ms = 0;
                JDate() : time_ms(jcurrentTimeMillis()) {}
                explicit JDate(long long ms) : time_ms(ms) {}
                long long getTime() const { return time_ms; }
            };
            struct JBigInteger {
                __int128 val = 0;
                JBigInteger() = default;
                JBigInteger(__int128 v) : val(v) {}
                JBigInteger(long long v) : val(v) {}
                JBigInteger(int v) : val(v) {}
                JBigInteger(const JString& s) {
                    val = 0;
                    bool neg = false;
                    size_t start = 0;
                    if (!s.empty() && s[0] == '-') { neg = true; start = 1; }
                    for (size_t i = start; i < s.size(); ++i) {
                        if (isdigit(static_cast<unsigned char>(s[i]))) val = val * 10 + (s[i] - '0');
                    }
                    if (neg) val = -val;
                }
                static JBigInteger valueOf(long long v) { return JBigInteger(v); }
                template<class R> static JBigInteger probablePrime(int bitLength, R&) {
                    uint64_t v = (1ULL << (bitLength - 1)) | 1ULL;
                    return JBigInteger(static_cast<long long>(v));
                }
                static const JBigInteger ZERO;
                static const JBigInteger ONE;
                static const JBigInteger TWO;
                static const JBigInteger TEN;

                JBigInteger add(const JBigInteger& o) const { return JBigInteger(val + o.val); }
                JBigInteger subtract(const JBigInteger& o) const { return JBigInteger(val - o.val); }
                JBigInteger multiply(const JBigInteger& o) const { return JBigInteger(val * o.val); }
                JBigInteger divide(const JBigInteger& o) const { return JBigInteger(val / o.val); }
                JBigInteger remainder(const JBigInteger& o) const { return JBigInteger(val % o.val); }
                JBigInteger mod(const JBigInteger& o) const {
                    __int128 m = val % o.val;
                    if (m < 0) m += (o.val < 0 ? -o.val : o.val);
                    return JBigInteger(m);
                }
                JBigInteger pow(int exp) const {
                    __int128 res = 1, base = val;
                    while (exp > 0) {
                        if (exp & 1) res *= base;
                        base *= base;
                        exp >>= 1;
                    }
                    return JBigInteger(res);
                }
                JBigInteger shiftLeft(int n) const { return JBigInteger(val << n); }
                JBigInteger shiftRight(int n) const { return JBigInteger(val >> n); }
                bool testBit(int n) const { return ((val >> n) & 1) != 0; }
                JBigInteger setBit(int n) const { return JBigInteger(val | ((__int128)1 << n)); }
                JBigInteger clearBit(int n) const { return JBigInteger(val & ~((__int128)1 << n)); }
                JBigInteger flipBit(int n) const { return JBigInteger(val ^ ((__int128)1 << n)); }
                JBigInteger or_(const JBigInteger& o) const { return JBigInteger(val | o.val); }
                JBigInteger and_(const JBigInteger& o) const { return JBigInteger(val & o.val); }
                JBigInteger xor_(const JBigInteger& o) const { return JBigInteger(val ^ o.val); }
                JBigInteger not_() const { return JBigInteger(~val); }
                JBigInteger andNot(const JBigInteger& o) const { return JBigInteger(val & ~o.val); }
                JBigInteger abs() const { return JBigInteger(val < 0 ? -val : val); }
                JBigInteger negate() const { return JBigInteger(-val); }
                JBigInteger gcd(const JBigInteger& o) const {
                    __int128 a = val < 0 ? -val : val, b = o.val < 0 ? -o.val : o.val;
                    while (b) { a %= b; std::swap(a, b); }
                    return JBigInteger(a);
                }
                int bitCount() const { return __builtin_popcountll(static_cast<uint64_t>(val)) + __builtin_popcountll(static_cast<uint64_t>(val >> 64)); }
                int bitLength() const {
                    __int128 v = val < 0 ? -val : val;
                    int len = 0;
                    while (v > 0) { len++; v >>= 1; }
                    return len;
                }
                int signum() const { return (val > 0) - (val < 0); }
                int compareTo(const JBigInteger& o) const { return (val > o.val) - (val < o.val); }
                bool equals(const JBigInteger& o) const { return val == o.val; }
                bool isProbablePrime(int certainty = 10) const {
                    if (val <= 1) return false;
                    if (val <= 3) return true;
                    if (val % 2 == 0 || val % 3 == 0) return false;
                    auto power = [](__int128 base, __int128 exp, __int128 mod) {
                        base %= mod;
                        __int128 res = 1;
                        while (exp > 0) {
                            if (exp % 2 == 1) res = (res * base) % mod;
                            base = (base * base) % mod;
                            exp /= 2;
                        }
                        return res;
                    };
                    auto millerRabin = [&](__int128 n, int a) {
                        if (n % a == 0) return false;
                        __int128 d = n - 1;
                        while (d % 2 == 0) d /= 2;
                        __int128 x = power(a, d, n);
                        if (x == 1 || x == n - 1) return true;
                        while (d != n - 1) {
                            x = (x * x) % n;
                            d *= 2;
                            if (x == n - 1) return true;
                            if (x == 1) return false;
                        }
                        return false;
                    };
                    static const int bases[] = {2, 3, 5, 7, 11, 13, 17, 19, 23, 29, 31, 37};
                    for (int a : bases) {
                        if (val <= a) break;
                        if (!millerRabin(val, a)) return false;
                    }
                    return true;
                }
                JBigInteger nextProbablePrime() const {
                    if (val < 2) return JBigInteger(2);
                    JBigInteger p(val + 1);
                    if (p.val > 2 && p.val % 2 == 0) p.val++;
                    while (!p.isProbablePrime()) {
                        p.val += 2;
                    }
                    return p;
                }
                long long longValue() const { return static_cast<long long>(val); }
                int intValue() const { return static_cast<int>(val); }
                double doubleValue() const { return static_cast<double>(val); }
                JString toString() const {
                    if (val == 0) return "0";
                    __int128 v = val;
                    bool neg = false;
                    if (v < 0) { neg = true; v = -v; }
                    string s;
                    while (v > 0) { s.push_back('0' + (v % 10)); v /= 10; }
                    if (neg) s.push_back('-');
                    std::reverse(s.begin(), s.end());
                    return s;
                }
                friend ostream& operator<<(ostream& os, const JBigInteger& b) {
                    os << b.toString();
                    return os;
                }
            };
            inline const JBigInteger JBigInteger::ZERO = JBigInteger(0);
            inline const JBigInteger JBigInteger::ONE = JBigInteger(1);
            inline const JBigInteger JBigInteger::TWO = JBigInteger(2);
            inline const JBigInteger JBigInteger::TEN = JBigInteger(10);
            struct JBigDecimal {
                double val = 0.0;
                JBigDecimal() = default;
                JBigDecimal(double v) : val(v) {}
                JBigDecimal(long long v) : val(static_cast<double>(v)) {}
                JBigDecimal(int v) : val(v) {}
                JBigDecimal(const JString& s) {
                    try { val = stod(s); } catch(...) { val = 0.0; }
                }
                static JBigDecimal valueOf(double v) { return JBigDecimal(v); }
                static JBigDecimal valueOf(long long v) { return JBigDecimal(static_cast<double>(v)); }
                static constexpr int ROUND_UP = 0;
                static constexpr int ROUND_DOWN = 1;
                static constexpr int ROUND_CEILING = 2;
                static constexpr int ROUND_FLOOR = 3;
                static constexpr int ROUND_HALF_UP = 4;
                static constexpr int ROUND_HALF_DOWN = 5;
                static constexpr int ROUND_HALF_EVEN = 6;
                static constexpr int ROUND_UNNECESSARY = 7;

                static const JBigDecimal ZERO;
                static const JBigDecimal ONE;
                static const JBigDecimal TEN;

                JBigDecimal add(const JBigDecimal& o) const { return JBigDecimal(val + o.val); }
                JBigDecimal subtract(const JBigDecimal& o) const { return JBigDecimal(val - o.val); }
                JBigDecimal multiply(const JBigDecimal& o) const { return JBigDecimal(val * o.val); }
                JBigDecimal divide(const JBigDecimal& o) const { return JBigDecimal(val / (o.val == 0 ? 1 : o.val)); }
                JBigDecimal divide(const JBigDecimal& o, int, int = 0) const { return divide(o); }
                JBigDecimal pow(int n) const { return JBigDecimal(std::pow(val, n)); }
                JBigDecimal abs() const { return JBigDecimal(std::abs(val)); }
                JBigDecimal negate() const { return JBigDecimal(-val); }
                JBigDecimal setScale(int, int = 0) const { return *this; }
                int compareTo(const JBigDecimal& o) const { return (val > o.val) - (val < o.val); }
                bool equals(const JBigDecimal& o) const { return val == o.val; }
                double doubleValue() const { return val; }
                long long longValue() const { return static_cast<long long>(val); }
                int intValue() const { return static_cast<int>(val); }
                JString toString() const { return std::to_string(val); }
                friend ostream& operator<<(ostream& os, const JBigDecimal& b) { return os << b.val; }
            };
            inline const JBigDecimal JBigDecimal::ZERO = JBigDecimal(0);
            inline const JBigDecimal JBigDecimal::ONE = JBigDecimal(1);
            inline const JBigDecimal JBigDecimal::TEN = JBigDecimal(10);
            struct JPrintWriter {
                JPrintWriter() = default;
                explicit JPrintWriter(const JInputStream&) {}
                explicit JPrintWriter(nullptr_t) {}
                template<class T> void print(const T& x) { jprint(x); }
                template<class T> void println(const T& x) { jprintln(x); }
                template<class T> void print(const vector<T>& a) { print(a, " "); }
                template<class T> void println(const vector<T>& a) { println(a, " "); }
                template<class T> void print(const vector<T>& a, const JString& separator) {
                    for (size_t i = 0; i < a.size(); ++i) { if (i) jprint(separator); jprint(a[i]); }
                }
                template<class T> void println(const vector<T>& a, const JString& separator) {
                    for (size_t i = 0; i < a.size(); ++i) { if (i) jprint(separator); jprint(a[i]); }
                    jprintln();
                }
                void println() { jprintln(); }
                void printlnYesNo(bool x) { jprintln(x ? "Yes" : "No"); }
                template<class... Args> void printf(Args&&... args) { jprintf(forward<Args>(args)...); }
                template<class T> void write(const T& x) { jprint(x); }
                void write(int c) { cout.put(static_cast<char>(c)); }
                void write(const JString& s, int off, int len) { cout << s.substr(off, len); }
                void flush() { cout.flush(); }
                void close() { cout.flush(); }
            };
            struct JRuntime {
                long long totalMemory() const { return numeric_limits<long long>::max(); }
                long long maxMemory() const { return numeric_limits<long long>::max(); }
                long long freeMemory() const { return numeric_limits<long long>::max(); }
            };
            struct JLong {
                static constexpr long long MAX_VALUE = numeric_limits<long long>::max();
                static constexpr long long MIN_VALUE = numeric_limits<long long>::min();
                static int compare(long long a, long long b) { return (a > b) - (a < b); }
                static int signum(long long x) { return (x > 0) - (x < 0); }
                static long long sum(long long a, long long b) { return a + b; }
                static long long max(long long a, long long b) { return std::max(a, b); }
                static long long min(long long a, long long b) { return std::min(a, b); }
                static int bitCount(long long x) { return __builtin_popcountll(static_cast<unsigned long long>(x)); }
                static int numberOfLeadingZeros(long long x) { return x ? __builtin_clzll((unsigned long long)x) : 64; }
                static int numberOfTrailingZeros(long long x) { return x ? __builtin_ctzll((unsigned long long)x) : 64; }
                static long long highestOneBit(long long x) { return x ? (1ULL << (63 - __builtin_clzll(static_cast<unsigned long long>(x)))) : 0LL; }
                static long long lowestOneBit(long long x) { return x & -x; }
                static long long reverse(long long x) {
                    uint64_t u = static_cast<uint64_t>(x);
                    u = ((u & 0x5555555555555555ULL) << 1) | ((u & 0xAAAAAAAAAAAAAAAAULL) >> 1);
                    u = ((u & 0x3333333333333333ULL) << 2) | ((u & 0xCCCCCCCCCCCCCCCCULL) >> 2);
                    u = ((u & 0x0F0F0F0F0F0F0F0FULL) << 4) | ((u & 0xF0F0F0F0F0F0F0F0ULL) >> 4);
                    u = ((u & 0x00FF00FF00FF00FFULL) << 8) | ((u & 0xFF00FF00FF00FF00ULL) >> 8);
                    u = ((u & 0x0000FFFF0000FFFFULL) << 16) | ((u & 0xFFFF0000FFFF0000ULL) >> 16);
                    u = (u << 32) | (u >> 32);
                    return static_cast<long long>(u);
                }
                static long long reverseBytes(long long x) {
                    return static_cast<long long>(__builtin_bswap64(static_cast<uint64_t>(x)));
                }
                static JString toBinaryString(long long x) {
                    if (x == 0) return "0";
                    uint64_t u = static_cast<uint64_t>(x);
                    string s;
                    while (u > 0) {
                        s.push_back((u & 1) ? '1' : '0');
                        u >>= 1;
                    }
                    std::reverse(s.begin(), s.end());
                    return s;
                }
                static JString toHexString(long long x) {
                    if (x == 0) return "0";
                    uint64_t u = static_cast<uint64_t>(x);
                    stringstream ss;
                    ss << std::hex << u;
                    return ss.str();
                }
                static JString toOctalString(long long x) {
                    if (x == 0) return "0";
                    uint64_t u = static_cast<uint64_t>(x);
                    stringstream ss;
                    ss << std::oct << u;
                    return ss.str();
                }
                static JString toString(long long x, int radix = 10) {
                    if (radix == 10) return std::to_string(x);
                    if (radix == 2) return toBinaryString(x);
                    if (radix == 16) return toHexString(x);
                    if (radix == 8) return toOctalString(x);
                    return std::to_string(x);
                }
                static long long parseLong(const JString& x, int radix = 10) { return stoll(x, nullptr, radix); }
                static long long valueOf(const JString& x, int radix = 10) { return stoll(x, nullptr, radix); }
                static long long valueOf(long long x) { return x; }
            };
            struct JInteger {
                static constexpr int MAX_VALUE = numeric_limits<int>::max();
                static constexpr int MIN_VALUE = numeric_limits<int>::min();
                static int compare(int a, int b) { return (a > b) - (a < b); }
                static int signum(int x) { return (x > 0) - (x < 0); }
                static int sum(int a, int b) { return a + b; }
                static int max(int a, int b) { return std::max(a, b); }
                static int min(int a, int b) { return std::min(a, b); }
                static int bitCount(int x) { return __builtin_popcount((unsigned int)x); }
                static int numberOfLeadingZeros(int x) { return x ? __builtin_clz((unsigned int)x) : 32; }
                static int numberOfTrailingZeros(int x) { return x ? __builtin_ctz((unsigned int)x) : 32; }
                static int highestOneBit(int x) { return x ? int(1U << (31 - __builtin_clz((unsigned int)x))) : 0; }
                static int lowestOneBit(int x) { return x & -x; }
                static int reverse(int x) {
                    uint32_t u = static_cast<uint32_t>(x);
                    u = ((u & 0x55555555U) << 1) | ((u & 0xAAAAAAAAU) >> 1);
                    u = ((u & 0x33333333U) << 2) | ((u & 0xCCCCCCCCU) >> 2);
                    u = ((u & 0x0F0F0F0FU) << 4) | ((u & 0xF0F0F0F0U) >> 4);
                    u = ((u & 0x00FF00FFU) << 8) | ((u & 0xFF00FF00U) >> 8);
                    u = (u << 16) | (u >> 16);
                    return static_cast<int>(u);
                }
                static int reverseBytes(int x) {
                    return static_cast<int>(__builtin_bswap32(static_cast<uint32_t>(x)));
                }
                static JString toBinaryString(int x) {
                    if (x == 0) return "0";
                    uint32_t u = static_cast<uint32_t>(x);
                    string s;
                    while (u > 0) {
                        s.push_back((u & 1) ? '1' : '0');
                        u >>= 1;
                    }
                    std::reverse(s.begin(), s.end());
                    return s;
                }
                static JString toHexString(int x) {
                    if (x == 0) return "0";
                    uint32_t u = static_cast<uint32_t>(x);
                    stringstream ss;
                    ss << std::hex << u;
                    return ss.str();
                }
                static JString toOctalString(int x) {
                    if (x == 0) return "0";
                    uint32_t u = static_cast<uint32_t>(x);
                    stringstream ss;
                    ss << std::oct << u;
                    return ss.str();
                }
                static JString toString(int x, int radix = 10) {
                    if (radix == 10) return std::to_string(x);
                    if (radix == 2) return toBinaryString(x);
                    if (radix == 16) return toHexString(x);
                    if (radix == 8) return toOctalString(x);
                    return std::to_string(x);
                }
                static int parseInt(const JString& x, int radix = 10) { return stoi(x, nullptr, radix); }
                static int valueOf(const JString& x, int radix = 10) { return stoi(x, nullptr, radix); }
                static int valueOf(int x) { return x; }
            };
            struct JCollections {
                static auto reverseOrder() {
                    return [](const auto& a, const auto& b) { return a < b ? 1 : (a > b ? -1 : 0); };
                }
                template<class Comp> static auto reverseOrder(Comp comp) {
                    return [comp](const auto& a, const auto& b) { return comp(b, a); };
                }
                template<class C> static void sort(C& c) { std::sort(c.begin(), c.end()); }
                template<class C, class Comp> static void sort(C& c, Comp comp) {
                    if constexpr (is_same_v<decay_t<Comp>, nullptr_t>) {
                        std::sort(c.begin(), c.end());
                    } else {
                        std::sort(c.begin(), c.end(), [&](const auto& a, const auto& b) { return comp(a, b) < 0; });
                    }
                }
                template<class C> static void sort(C& c, nullptr_t) {
                    std::sort(c.begin(), c.end());
                }
                template<class C> static void reverse(C& c) { std::reverse(c.begin(), c.end()); }
                template<class C> static void swap(C& c, int i, int j) { std::swap(c[i], c[j]); }
                template<class C> static auto max(const C& c) { return *std::max_element(c.begin(), c.end()); }
                template<class C, class Comp> static auto max(const C& c, Comp comp) {
                    return *std::max_element(c.begin(), c.end(), [&](const auto& a, const auto& b) { return comp(a, b) < 0; });
                }
                template<class C> static auto min(const C& c) { return *std::min_element(c.begin(), c.end()); }
                template<class C, class Comp> static auto min(const C& c, Comp comp) {
                    return *std::min_element(c.begin(), c.end(), [&](const auto& a, const auto& b) { return comp(a, b) < 0; });
                }
                template<class C, class T> static void fill(C& c, const T& val) { std::fill(c.begin(), c.end(), val); }
                template<class C, class T> static int binarySearch(const C& c, const T& key) {
                    auto it = lower_bound(c.begin(), c.end(), key);
                    if (it != c.end() && *it == key) return int(distance(c.begin(), it));
                    return -int(distance(c.begin(), it)) - 1;
                }
                template<class C, class T, class Comp> static int binarySearch(const C& c, const T& key, Comp comp) {
                    auto it = lower_bound(c.begin(), c.end(), key, [&](const auto& x, const auto& y) { return comp(x, y) < 0; });
                    if (it != c.end() && comp(*it, key) == 0) return int(distance(c.begin(), it));
                    return -int(distance(c.begin(), it)) - 1;
                }
            };
            struct Line2D {
                static int relativeCCW(double x1, double y1, double x2, double y2, double px, double py) {
                    double dx2 = x2 - x1;
                    double dy2 = y2 - y1;
                    double dpx = px - x1;
                    double dpy = py - y1;
                    double ccw = dpx * dy2 - dpy * dx2;
                    if (ccw == 0.0) {
                        ccw = dpx * dx2 + dpy * dy2;
                        if (ccw > 0.0) {
                            dpx -= dx2;
                            dpy -= dy2;
                            ccw = dpx * dx2 + dpy * dy2;
                            if (ccw < 0.0) ccw = 0.0;
                        }
                    }
                    return (ccw < 0.0) ? -1 : ((ccw > 0.0) ? 1 : 0);
                }
                static double ptSegDistSq(double x1, double y1, double x2, double y2, double px, double py) {
                    double pd2 = (x2 - x1) * (x2 - x1) + (y2 - y1) * (y2 - y1);
                    if (pd2 == 0) return (px - x1) * (px - x1) + (py - y1) * (py - y1);
                    double u = ((px - x1) * (x2 - x1) + (py - y1) * (y2 - y1)) / pd2;
                    if (u < 0) u = 0;
                    if (u > 1) u = 1;
                    double ix = x1 + u * (x2 - x1);
                    double iy = y1 + u * (y2 - y1);
                    return (px - ix) * (px - ix) + (py - iy) * (py - iy);
                }
                static double ptSegDist(double x1, double y1, double x2, double y2, double px, double py) {
                    return std::sqrt(ptSegDistSq(x1, y1, x2, y2, px, py));
                }
                static double ptLineDistSq(double x1, double y1, double x2, double y2, double px, double py) {
                    double pd2 = (x2 - x1) * (x2 - x1) + (y2 - y1) * (y2 - y1);
                    if (pd2 == 0) return (px - x1) * (px - x1) + (py - y1) * (py - y1);
                    double u = ((px - x1) * (x2 - x1) + (py - y1) * (y2 - y1)) / pd2;
                    double ix = x1 + u * (x2 - x1);
                    double iy = y1 + u * (y2 - y1);
                    return (px - ix) * (px - ix) + (py - iy) * (py - iy);
                }
                static double ptLineDist(double x1, double y1, double x2, double y2, double px, double py) {
                    return std::sqrt(ptLineDistSq(x1, y1, x2, y2, px, py));
                }
                static bool linesIntersect(double x1, double y1, double x2, double y2, double x3, double y3, double x4, double y4) {
                    return ((relativeCCW(x1, y1, x2, y2, x3, y3) * relativeCCW(x1, y1, x2, y2, x4, y4) <= 0)
                         && (relativeCCW(x3, y3, x4, y4, x1, y1) * relativeCCW(x3, y3, x4, y4, x2, y2) <= 0));
                }
            };
            struct JMath {
                static constexpr double PI = 3.14159265358979323846;
                static constexpr double E = 2.71828182845904523536;
            };
            struct JDouble {
                static constexpr double MAX_VALUE = numeric_limits<double>::max();
                static constexpr double MIN_VALUE = numeric_limits<double>::min();
                static int compare(double a, double b) { return (a > b) - (a < b); }
                static double parseDouble(const JString& x) { return stod(x); }
                static double valueOf(const JString& x) { return stod(x); }
                static double valueOf(double x) { return x; }
            };
            struct JComparatorUtils {
                static auto reverseOrder() { return [](const auto& a, const auto& b) { return jcompareTo(b, a); }; }
                static auto naturalOrder() { return [](const auto& a, const auto& b) { return jcompareTo(a, b); }; }
                template<class T> static int compare(const T& a, const T& b) { return jcompareTo(a, b); }
            };
            using Comparator = JComparatorUtils;
            struct JCharacter {
                static int compare(char a, char b) { return a - b; }
                static char toUpperCase(char c) { return static_cast<char>(::toupper(static_cast<unsigned char>(c))); }
                static char toLowerCase(char c) { return static_cast<char>(::tolower(static_cast<unsigned char>(c))); }
                static bool isDigit(char c) { return ::isdigit(static_cast<unsigned char>(c)); }
                static bool isLetter(char c) { return ::isalpha(static_cast<unsigned char>(c)); }
                static bool isAlphabetic(int c) { return ::isalpha(static_cast<unsigned char>(c)); }
                static bool isLetterOrDigit(char c) { return ::isalnum(static_cast<unsigned char>(c)); }
                static bool isWhitespace(char c) { return ::isspace(static_cast<unsigned char>(c)); }
                static bool isUpperCase(char c) { return ::isupper(static_cast<unsigned char>(c)); }
                static bool isLowerCase(char c) { return ::islower(static_cast<unsigned char>(c)); }
            };
            template<class T> struct Comparable {
                virtual ~Comparable() = default;
                virtual int compareTo(T) = 0;
            };
            struct Runnable {
                virtual ~Runnable() = default;
                virtual void run() = 0;
            };
            struct AutoCloseable {
                virtual ~AutoCloseable() = default;
                virtual void close() = 0;
            };
            using Closeable = AutoCloseable;
            struct Serializable {};
            struct Cloneable {};

            struct JThread {
                function<void()> action;
                JThread() : action([]{}) {}
                template<class T> JThread(shared_ptr<T> target)
                    : action([target]() { if (target) target->run(); }) {}
                template<class T> JThread(nullptr_t, shared_ptr<T> target, const JString&, long long)
                    : action([target]() { if (target) target->run(); }) {}
                void start() { if (action) action(); }
                void run() { if (action) action(); }
            };
            struct JRandom {
                mt19937_64 engine;
                JRandom() : engine(chrono::steady_clock::now().time_since_epoch().count()) {}
                explicit JRandom(long long seed) : engine(seed) {}
                int nextInt() { return static_cast<int>(engine()); }
                int nextInt(int bound) { return uniform_int_distribution<int>(0, bound - 1)(engine); }
                int nextInt(int origin, int bound) { return uniform_int_distribution<int>(origin, bound - 1)(engine); }
                long long nextLong() { return static_cast<long long>(engine()); }
                long long nextLong(long long bound) { return uniform_int_distribution<long long>(0, bound - 1)(engine); }
                long long nextLong(long long origin, long long bound) { return uniform_int_distribution<long long>(origin, bound - 1)(engine); }
                double nextDouble() { return uniform_real_distribution<double>(0.0, 1.0)(engine); }
                double nextDouble(double bound) { return uniform_real_distribution<double>(0.0, bound)(engine); }
                double nextDouble(double origin, double bound) { return uniform_real_distribution<double>(origin, bound)(engine); }
                float nextFloat() { return uniform_real_distribution<float>(0.0f, 1.0f)(engine); }
                bool nextBoolean() { return (engine() & 1) != 0; }
                double nextGaussian() { return normal_distribution<double>(0.0, 1.0)(engine); }
            };
            template<class T> void jsort(vector<T>& a) { sort(a.begin(), a.end()); }
            template<class T, class C> void jsort(vector<T>& a, C compare) {
                sort(a.begin(), a.end(), [&](const T& x, const T& y) { return compare(x, y) < 0; });
            }
            template<class T, class U> void jfill(vector<T>& a, const U& x) { fill(a.begin(), a.end(), static_cast<T>(x)); }
            template<class T> vector<T> jcopyOf(const vector<T>& a, int n) {
                vector<T> result(n);
                copy_n(a.begin(), min<int>(a.size(), n), result.begin());
                return result;
            }
            template<class T> vector<T> jcopyOfRange(const vector<T>& a, int from, int to) {
                vector<T> result(max(0, to - from));
                int count = max(0, min<int>(a.size(), to) - from);
                if (count > 0) copy_n(a.begin() + from, count, result.begin());
                return result;
            }
            template<class T> void jarraycopy(const vector<T>& src, int srcPos, vector<T>& dst, int dstPos, int length) {
                copy_n(src.begin() + srcPos, length, dst.begin() + dstPos);
            }
            template<class T, class F> void jsetAll(vector<T>& a, F f) {
                for (int i = 0; i < (int)a.size(); ++i) a[i] = f(i);
            }
            template<class T, class F> void jparallelPrefix(vector<T>& a, F op) {
                for (size_t i = 1; i < a.size(); ++i) a[i] = op(a[i - 1], a[i]);
            }
            template<class T, class F> void jparallelPrefix(vector<T>& a, int from, int to, F op) {
                for (int i = from + 1; i < to; ++i) a[i] = op(a[i - 1], a[i]);
            }
            template<class C, class T> int jbsearch(const C& a, const T& x) {
                auto it = lower_bound(a.begin(), a.end(), x);
                return it != a.end() && *it == x ? int(it - a.begin()) : -int(it - a.begin()) - 1;
            }
            template<class C, class T, class Comp> int jbsearch(const C& a, const T& x, Comp comp) {
                auto it = lower_bound(a.begin(), a.end(), x, comp);
                return it != a.end() && !comp(x, *it) && !comp(*it, x) ? int(it - a.begin()) : -int(it - a.begin()) - 1;
            }
            template<class C1, class C2> int jmismatch(const C1& a, const C2& b) {
                auto [it1, it2] = std::mismatch(a.begin(), a.end(), b.begin(), b.end());
                if (it1 == a.end() && it2 == b.end()) return -1;
                return int(distance(a.begin(), it1));
            }

            struct JBitSet {
                vector<uint64_t> words;

                JBitSet() = default;
                explicit JBitSet(int nbits) {
                    if (nbits > 0) words.resize((nbits + 63) / 64, 0ULL);
                }

                void set(int bitIndex) {
                    if (bitIndex < 0) return;
                    size_t w = bitIndex / 64;
                    if (w >= words.size()) words.resize(w + 1, 0ULL);
                    words[w] |= (1ULL << (bitIndex % 64));
                }
                void set(int bitIndex, bool value) {
                    if (value) set(bitIndex);
                    else clear(bitIndex);
                }
                void set(int fromIndex, int toIndex, bool value = true) {
                    for (int i = fromIndex; i < toIndex; ++i) set(i, value);
                }
                void clear(int bitIndex) {
                    if (bitIndex < 0) return;
                    size_t w = bitIndex / 64;
                    if (w < words.size()) words[w] &= ~(1ULL << (bitIndex % 64));
                }
                void clear(int fromIndex, int toIndex) {
                    for (int i = fromIndex; i < toIndex; ++i) clear(i);
                }
                void clear() {
                    std::fill(words.begin(), words.end(), 0ULL);
                }
                bool get(int bitIndex) const {
                    if (bitIndex < 0) return false;
                    size_t w = bitIndex / 64;
                    if (w >= words.size()) return false;
                    return (words[w] & (1ULL << (bitIndex % 64))) != 0;
                }
                void flip(int bitIndex) {
                    if (bitIndex < 0) return;
                    size_t w = bitIndex / 64;
                    if (w >= words.size()) words.resize(w + 1, 0ULL);
                    words[w] ^= (1ULL << (bitIndex % 64));
                }
                void flip(int fromIndex, int toIndex) {
                    for (int i = fromIndex; i < toIndex; ++i) flip(i);
                }
                void and_(const JBitSet& set) {
                    size_t common = min(words.size(), set.words.size());
                    for (size_t i = 0; i < common; ++i) words[i] &= set.words[i];
                    for (size_t i = common; i < words.size(); ++i) words[i] = 0ULL;
                }
                void or_(const JBitSet& set) {
                    if (set.words.size() > words.size()) words.resize(set.words.size(), 0ULL);
                    for (size_t i = 0; i < set.words.size(); ++i) words[i] |= set.words[i];
                }
                void xor_(const JBitSet& set) {
                    if (set.words.size() > words.size()) words.resize(set.words.size(), 0ULL);
                    for (size_t i = 0; i < set.words.size(); ++i) words[i] ^= set.words[i];
                }
                void andNot(const JBitSet& set) {
                    size_t common = min(words.size(), set.words.size());
                    for (size_t i = 0; i < common; ++i) words[i] &= ~set.words[i];
                }
                int cardinality() const {
                    int count = 0;
                    for (uint64_t w : words) count += __builtin_popcountll(w);
                    return count;
                }
                bool intersects(const JBitSet& set) const {
                    size_t common = min(words.size(), set.words.size());
                    for (size_t i = 0; i < common; ++i) {
                        if ((words[i] & set.words[i]) != 0ULL) return true;
                    }
                    return false;
                }
                int nextSetBit(int fromIndex) const {
                    if (fromIndex < 0) return -1;
                    size_t w = fromIndex / 64;
                    if (w >= words.size()) return -1;
                    uint64_t word = words[w] & (~0ULL << (fromIndex % 64));
                    if (word != 0) return static_cast<int>(w * 64 + __builtin_ctzll(word));
                    for (size_t i = w + 1; i < words.size(); ++i) {
                        if (words[i] != 0) return static_cast<int>(i * 64 + __builtin_ctzll(words[i]));
                    }
                    return -1;
                }
                int length() const {
                    for (int i = static_cast<int>(words.size()) - 1; i >= 0; --i) {
                        if (words[i] != 0) {
                            return i * 64 + 64 - __builtin_clzll(words[i]);
                        }
                    }
                    return 0;
                }
                int size() const { return static_cast<int>(words.size() * 64); }
                bool isEmpty() const { return cardinality() == 0; }
                bool empty() const { return isEmpty(); }
                vector<int8_t> toByteArray() const {
                    int len = (length() + 7) / 8;
                    vector<int8_t> bytes(len, 0);
                    for (int i = 0; i < len; ++i) {
                        size_t w = i / 8;
                        int shift = (i % 8) * 8;
                        if (w < words.size()) {
                            bytes[i] = static_cast<int8_t>((words[w] >> shift) & 0xFF);
                        }
                    }
                    return bytes;
                }
                vector<long long> toLongArray() const {
                    int len = (length() + 63) / 64;
                    vector<long long> res(len, 0);
                    for (int i = 0; i < len; ++i) {
                        if ((size_t)i < words.size()) res[i] = static_cast<long long>(words[i]);
                    }
                    return res;
                }
                static JBitSet valueOf(const vector<long long>& longArray) {
                    JBitSet bs;
                    bs.words.resize(longArray.size());
                    for (size_t i = 0; i < longArray.size(); ++i) {
                        bs.words[i] = static_cast<uint64_t>(longArray[i]);
                    }
                    return bs;
                }
                JString toString() const {
                    JString s = "{";
                    bool first = true;
                    for (int i = nextSetBit(0); i >= 0; i = nextSetBit(i + 1)) {
                        if (!first) s += ", ";
                        s += to_string(i);
                        first = false;
                    }
                    s += "}";
                    return s;
                }
                bool operator==(const JBitSet& o) const {
                    size_t max_sz = max(words.size(), o.words.size());
                    for (size_t i = 0; i < max_sz; ++i) {
                        uint64_t w1 = i < words.size() ? words[i] : 0ULL;
                        uint64_t w2 = i < o.words.size() ? o.words[i] : 0ULL;
                        if (w1 != w2) return false;
                    }
                    return true;
                }
                bool operator!=(const JBitSet& o) const { return !(*this == o); }
            };

            """;
}
