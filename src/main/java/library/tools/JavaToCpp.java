package library.tools;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
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
            "namespace", "concept", "requires", "and", "or", "xor", "compl", "not", "new", "union",
            "bitand", "bitor", "and_eq", "or_eq", "xor_eq", "not_eq");

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
        /** C++出力ランタイムへ置換するソース内型名。 */
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
        /** コンストラクタ初期化子リストでパラメータ出力中ならそのパラメータ列。 */
        private List<CtParameter<?>> currentInitializerListParameters;
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
            out.append(CppRuntimePrelude.prelude());
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
            boolean hasDeclaredSuperclass = type instanceof CtClass<?> cls && cls.getSuperclass() != null
                    && !cls.getSuperclass().getQualifiedName().equals("java.lang.Object")
                    && !cls.getSuperclass().getQualifiedName().equals("java.lang.Record")
                    && isDeclaredType(cls.getSuperclass());
            if (!type.isLocalType() && !type.isInterface() && !hasDeclaredSuperclass) {
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
                    ) {
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
            if (type instanceof CtClass<?> cls) {
                for (CtConstructor<?> constructor : cls.getConstructors()) emitConstructor(constructor, type);
            }
            for (CtMethod<?> method : type.getMethods()) {
                if (isPrimitiveIteratorMethod(method)) emitCppIterator(type);
                else emitMethod(method);
            }
            for (CtField<?> field : type.getFields()) emitField(field);
            CtMethod<?> iterMethod = type.getMethods().stream().filter(m -> m.getSimpleName().equals("iterator") && m.getParameters().isEmpty()).findFirst().orElse(null);
            boolean hasPrimitiveIter = type.getMethods().stream().anyMatch(this::isPrimitiveIteratorMethod);
            if (iterMethod != null && !hasPrimitiveIter) {
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

        private boolean containsTypeParameterNotInOwner(CtTypeReference<?> t, CtType<?> owner) {
            if (t == null) return false;
            if (t instanceof CtTypeParameterReference tpr) {
                if (owner != null && owner.getFormalCtTypeParameters().stream().anyMatch(p -> p.getSimpleName().equals(tpr.getSimpleName()))) {
                    return false;
                }
                return true;
            }
            for (CtTypeReference<?> arg : t.getActualTypeArguments()) {
                if (containsTypeParameterNotInOwner(arg, owner)) return true;
            }
            return false;
        }

        private CtTypeReference<?> substituteTypeArguments(CtTypeReference<?> type, java.util.Map<String, CtTypeReference<?>> map) {
            if (type == null || map.isEmpty()) return type;
            if (type instanceof CtTypeParameterReference tpr) {
                if (map.containsKey(tpr.getSimpleName())) {
                    return map.get(tpr.getSimpleName());
                }
                return type;
            }
            if (!type.getActualTypeArguments().isEmpty()) {
                CtTypeReference<?> copy = type.clone();
                List<CtTypeReference<?>> newArgs = new java.util.ArrayList<>();
                for (CtTypeReference<?> arg : type.getActualTypeArguments()) {
                    newArgs.add(substituteTypeArguments(arg, map));
                }
                copy.setActualTypeArguments(newArgs);
                return copy;
            }
            return type;
        }

        private CtTypeReference<?> getBaseMethodReturnType(CtMethod<?> method, CtType<?> owner) {
            if (owner == null) return null;
            CtTypeReference<?> rootType = null;
            for (CtTypeReference<?> iface : owner.getSuperInterfaces()) {
                CtType<?> decl = iface.getTypeDeclaration();
                if (decl != null) {
                    java.util.Map<String, CtTypeReference<?>> map = new java.util.HashMap<>();
                    for (int i = 0; i < Math.min(decl.getFormalCtTypeParameters().size(), iface.getActualTypeArguments().size()); i++) {
                        map.put(decl.getFormalCtTypeParameters().get(i).getSimpleName(), iface.getActualTypeArguments().get(i));
                    }
                    for (CtMethod<?> m : decl.getMethods()) {
                        if (m.getSimpleName().equals(method.getSimpleName()) && m.getParameters().size() == method.getParameters().size()) {
                            rootType = substituteTypeArguments(m.getType(), map);
                        }
                    }
                    CtTypeReference<?> parentType = getBaseMethodReturnType(method, decl);
                    if (parentType != null) rootType = substituteTypeArguments(parentType, map);
                }
            }
            if (owner instanceof CtClass<?> cls && cls.getSuperclass() != null) {
                CtTypeReference<?> superClassRef = cls.getSuperclass();
                CtType<?> decl = superClassRef.getTypeDeclaration();
                if (decl != null) {
                    java.util.Map<String, CtTypeReference<?>> map = new java.util.HashMap<>();
                    for (int i = 0; i < Math.min(decl.getFormalCtTypeParameters().size(), superClassRef.getActualTypeArguments().size()); i++) {
                        map.put(decl.getFormalCtTypeParameters().get(i).getSimpleName(), superClassRef.getActualTypeArguments().get(i));
                    }
                    for (CtMethod<?> m : decl.getMethods()) {
                        if (m.getSimpleName().equals(method.getSimpleName()) && m.getParameters().size() == method.getParameters().size()) {
                            rootType = substituteTypeArguments(m.getType(), map);
                        }
                    }
                    CtTypeReference<?> parentType = getBaseMethodReturnType(method, decl);
                    if (parentType != null) rootType = substituteTypeArguments(parentType, map);
                }
            }
            if (rootType != null && containsTypeParameterNotInOwner(rootType, method.getDeclaringType())) {
                return null;
            }
            return rootType;
        }

        private boolean isVariableAlreadyDeclaredInBlock(CtLocalVariable<?> v) {
            CtBlock<?> parentBlock = v.getParent(CtBlock.class);
            while (parentBlock != null) {
                for (CtStatement stmt : parentBlock.getStatements()) {
                    if (stmt == v) break;
                    if (stmt instanceof CtLocalVariable<?> other && id(other.getSimpleName()).equals(id(v.getSimpleName()))) {
                        return true;
                    }
                }
                parentBlock = parentBlock.getParent(CtBlock.class);
            }
            return false;
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
                currentInitializerListParameters = constructor.getParameters();
                initializers.add(baseTypeStr + arguments(invocation.getArguments()));
                currentInitializerListParameters = null;
            }
            if (!initializers.isEmpty()) s.append(" : ").append(String.join(", ", initializers));
            s.append(' ');
            writeIndent();
            out.append(s);
            emitBlock(constructor.getBody(), false, constructor.getParameters());
            out.append('\n');
            if (constructor.getParameters().stream().anyMatch(p -> type(p.getType()).startsWith("function<"))) {
                StringBuilder ts = new StringBuilder();
                ts.append("template<");
                for (int i = 0; i < constructor.getParameters().size(); i++) {
                    if (i > 0) ts.append(", ");
                    ts.append("class F").append(i);
                }
                ts.append(", class = enable_if_t<!is_same_v<decay_t<F0>, ").append(id(owner.getSimpleName())).append(">");
                ts.append(" && !(");
                boolean firstFunc = true;
                for (int i = 0; i < constructor.getParameters().size(); i++) {
                    String pType = type(constructor.getParameters().get(i).getType());
                    if (pType.startsWith("function<")) {
                        if (!firstFunc) ts.append(" && ");
                        ts.append("is_same_v<decay_t<F").append(i).append(">, ").append(pType).append(">");
                        firstFunc = false;
                    }
                }
                ts.append(")");
                ts.append(">> ");
                ts.append(id(owner.getSimpleName())).append("(");
                if (isNonStaticNested(owner.getReference())) {
                    ts.append(id(owner.getDeclaringType().getSimpleName())).append("* outer_, ");
                }
                for (int i = 0; i < constructor.getParameters().size(); i++) {
                    if (i > 0) ts.append(", ");
                    ts.append("F").append(i).append("&& p").append(i);
                }
                ts.append(") : ").append(id(owner.getSimpleName())).append("(");
                if (isNonStaticNested(owner.getReference())) {
                    ts.append("outer_, ");
                }
                for (int i = 0; i < constructor.getParameters().size(); i++) {
                    if (i > 0) ts.append(", ");
                    CtParameter<?> p = constructor.getParameters().get(i);
                    String pType = type(p.getType());
                    if (pType.startsWith("function<")) {
                        ts.append(pType).append("(std::forward<F").append(i).append(">(p").append(i).append("))");
                    } else {
                        ts.append("std::forward<F").append(i).append(">(p").append(i).append(")");
                    }
                }
                ts.append(") {}\n");
                line(ts.toString());
            }
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
            else if (method.getFormalCtTypeParameters().isEmpty()) s.append("virtual ");
            String methodName = method.getDeclaringType().getSimpleName().equals("Main")
                    && method.getSimpleName().equals("main") ? "javaMain" : id(method.getSimpleName());
            CtTypeReference<?> returnTypeRef = method.getType();
            CtTypeReference<?> baseReturnTypeRef = getBaseMethodReturnType(method, method.getDeclaringType());
            if (baseReturnTypeRef != null && isDeclaredType(baseReturnTypeRef) && !containsTypeParameterNotInOwner(baseReturnTypeRef, method.getDeclaringType())) {
                if (!baseReturnTypeRef.getActualTypeArguments().isEmpty() && !method.getType().getActualTypeArguments().isEmpty()) {
                    baseReturnTypeRef = baseReturnTypeRef.clone();
                    baseReturnTypeRef.setActualTypeArguments(method.getType().getActualTypeArguments());
                }
                returnTypeRef = baseReturnTypeRef;
            }
            s.append(type(returnTypeRef)).append(' ').append(methodName).append('(');
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
        private Set<CtTypeReference<?>> collectAllSuperInterfaces(CtType<?> type) {
            Set<CtTypeReference<?>> result = new HashSet<>(type.getSuperInterfaces());
            for (CtTypeReference<?> ref : type.getSuperInterfaces()) {
                CtType<?> decl = ref.getTypeDeclaration();
                if (decl != null) result.addAll(collectAllSuperInterfaces(decl));
            }
            return result;
        }

        private void emitInheritedInterfaceBridges(CtType<?> type) {
            if (!(type instanceof CtClass<?> cls) || cls.getSuperclass() == null) return;
            CtType<?> superclass = cls.getSuperclass().getTypeDeclaration();
            if (superclass == null) return;
            for (CtTypeReference<?> interfaceReference : collectAllSuperInterfaces(type)) {
                CtType<?> interfaceType = interfaceReference.getTypeDeclaration();
                if (interfaceType == null) continue;
                for (CtMethod<?> required : interfaceType.getMethods()) {
                    if (required.getBody() != null || !type.getMethodsByName(required.getSimpleName()).isEmpty()) continue;
                    CtMethod<?> implementation = superclass.getMethodsByName(required.getSimpleName()).stream()
                            .filter(m -> m.getParameters().size() == required.getParameters().size()).findFirst().orElse(null);
                    if (implementation == null) continue;
                    StringBuilder s = new StringBuilder("virtual ").append(type(implementation.getType())).append(' ')
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
            currentType = method.getDeclaringType();
            CtTypeReference<?> returnTypeRef = method.getType();
            CtTypeReference<?> baseReturnTypeRef = getBaseMethodReturnType(method, method.getDeclaringType());
            if (baseReturnTypeRef != null && isDeclaredType(baseReturnTypeRef) && !containsTypeParameterNotInOwner(baseReturnTypeRef, method.getDeclaringType())) {
                if (!baseReturnTypeRef.getActualTypeArguments().isEmpty() && !method.getType().getActualTypeArguments().isEmpty()) {
                    baseReturnTypeRef = baseReturnTypeRef.clone();
                    baseReturnTypeRef.setActualTypeArguments(method.getType().getActualTypeArguments());
                }
                returnTypeRef = baseReturnTypeRef;
            }
            currentType = null;
            String returnType = type(returnTypeRef);
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
            if (block != null) {
                List<CtLocalVariable<?>> patternVars = findPatternVariables(block);
                for (CtLocalVariable<?> v : patternVars) {
                    if (!isVariableAlreadyDeclaredInBlock(v)) {
                        writeIndent();
                        out.append(type(v.getType())).append(' ').append(id(v.getSimpleName())).append("{};\n");
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
            if (t.getFinalizer() != null && !t.getFinalizer().getStatements().isEmpty()) {
                writeIndent();
                out.append("{\n");
                indent++;
                line("auto __finally = [&]() {");
                indent++;
                for (CtStatement s : t.getFinalizer().getStatements()) emitStatement(s);
                indent--;
                line("};");
                line("struct __RAII { decltype(__finally)& f; ~__RAII() { f(); } } __raii{__finally};");
                if (!t.getCatchers().isEmpty()) {
                    writeIndent();
                    out.append("try ");
                    emitBlock(t.getBody(), false);
                    for (CtCatch catcher : t.getCatchers()) {
                        out.append(" catch (const exception& ").append(id(catcher.getParameter().getSimpleName())).append(") ");
                        emitBlock(catcher.getBody(), false);
                    }
                    out.append('\n');
                } else {
                    emitBlock(t.getBody(), true);
                    out.append('\n');
                }
                indent--;
                writeIndent();
                out.append("}\n");
                return;
            }
            writeIndent();
            out.append("try ");
            emitBlock(t.getBody(), false);
            for (CtCatch catcher : t.getCatchers()) {
                out.append(" catch (const exception& ").append(id(catcher.getParameter().getSimpleName())).append(") ");
                emitBlock(catcher.getBody(), false);
            }
            out.append('\n');
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
                if (cast instanceof CtArrayTypeReference<?> arrayCast) {
                    if (copy instanceof CtNewArray<?> newArray && !newArray.getDimensionExpressions().isEmpty()) {
                        return vectorAllocation(cast, newArray.getDimensionExpressions(), 0);
                    }
                    if (copy instanceof CtInvocation<?> inv && isArrayNewInstance(inv) && inv.getArguments().size() >= 2) {
                        return vectorAllocation(arrayCast, inv.getArguments().subList(1, inv.getArguments().size()), 0);
                    }
                }
                if (e.getType() != null && e.getType().getQualifiedName().equals("java.lang.Object")) {
                    return "any_cast<" + type(cast) + ">(" + expr(copy) + ")";
                }
                String castType = type(cast);
                if (cast.getQualifiedName().equals("java.lang.Comparable") || cast.getSimpleName().equals("Comparable")) {
                    return expr(copy);
                }
                if (castType.startsWith("shared_ptr<") && castType.endsWith(">")) {
                    String copyType = copy.getType() != null ? type(copy.getType()) : "";
                    if (copyType.startsWith("shared_ptr<")) {
                        String innerType = castType.substring("shared_ptr<".length(), castType.length() - 1);
                        return "dynamic_pointer_cast<" + innerType + ">(" + expr(copy) + ")";
                    }
                }
                return "static_cast<" + castType + ">(" + expr(copy) + ")";
            }
            if (e instanceof CtLiteral<?> literal) return literal(literal);
            if (e instanceof CtFieldAccess<?> field) return field(field);
            if (e instanceof CtThisAccess<?> thisAccess) {
                CtTypeReference<?> targetType = thisAccess.getType() != null ? thisAccess.getType() : (currentType != null ? currentType.getReference() : null);
                if (targetType != null) {
                    String tStr = type(targetType);
                    if (tStr.startsWith("shared_ptr<") && tStr.endsWith(">")) {
                        tStr = tStr.substring("shared_ptr<".length(), tStr.length() - 1);
                    }
                    return "static_pointer_cast<" + tStr + ">(this->shared_from_this())";
                }
                return "this->shared_from_this()";
            }
            if (e instanceof CtSuperAccess<?>) return "this";
            if (e instanceof CtVariableAccess<?> variable) {
                if (variable.getVariable() instanceof CtFieldReference<?> field) return fieldName(field);
                String name = id(variable.getVariable().getSimpleName());
                if (currentInitializerListParameters != null) {
                    for (CtParameter<?> p : currentInitializerListParameters) {
                        if (p.getSimpleName().equals(variable.getVariable().getSimpleName())) {
                            if (isPassByReferenceType(type(p.getType()))) {
                                return name + "_const";
                            }
                        }
                    }
                }
                return name;
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
                        if (v != null && pattern.getParent(CtIf.class) != null) {
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
            if (f.getTarget() instanceof CtTypeAccess<?> access) {
                String targetName = access.getAccessedType().getQualifiedName();
                if (targetName.equals("java.lang.System")) {
                    if (name.equals("in")) return "JInputStream()";
                    if (name.equals("out") || name.equals("err")) return "JPrintWriter()";
                }
                if (name.equals("class")) {
                    return "JClass(typeid(" + type(access.getAccessedType()) + "))";
                }
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

        private boolean isArrayNewInstance(CtInvocation<?> inv) {
            if (inv == null || inv.getExecutable() == null) return false;
            String name = inv.getExecutable().getSimpleName();
            if (!name.equals("newInstance")) return false;
            if (inv.getTarget() != null) {
                String t = simpleTargetName(inv.getTarget());
                String q = inv.getExecutable().getDeclaringType() != null ? inv.getExecutable().getDeclaringType().getQualifiedName() : "";
                return q.equals("java.lang.reflect.Array") || t.equals("Array") || t.equals("java.lang.reflect.Array");
            }
            return false;
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
                String declType = rawCustomType(i.getExecutable().getDeclaringType());
                if (!declType.contains("<") && currentType != null && !currentType.getFormalCtTypeParameters().isEmpty()) {
                    declType += templateArgs(currentType.getFormalCtTypeParameters());
                }
                return declType + "::" + id(name) + arguments(i.getArguments());
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
            if (name.equals("getClass") && i.getTarget() != null) {
                return "JClass(typeid(" + expr(i.getTarget()) + "))";
            }
            if (name.equals("printStackTrace")) {
                return "(void)0";
            }
            if ((qualified.equals("java.lang.reflect.Array") || simpleTargetName(i.getTarget()).equals("Array"))
                    && name.equals("newInstance")) {
                if (i.getArguments().size() >= 2) {
                    return "vector<JObject>(" + expr(i.getArguments().get(1)) + ")";
                }
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
            if (name.equals("replaceFirst") && i.getTarget() != null && i.getArguments().size() == 2) {
                return "jreplaceFirst(" + expr(i.getTarget()) + ", " + expr(i.getArguments().get(0)) + ", " + expr(i.getArguments().get(1)) + ")";
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
            if (name.equals("contains") && i.getTarget() != null && i.getArguments().size() == 1
                    && isJavaString(i.getTarget().getType())) {
                return "jcontains(" + expr(i.getTarget()) + ", " + expr(i.getArguments().get(0)) + ")";
            }
            if (name.equals("matches") && i.getTarget() != null && i.getArguments().size() == 1
                    && isJavaString(i.getTarget().getType())) {
                return "jmatches(" + expr(i.getTarget()) + ", " + expr(i.getArguments().get(0)) + ")";
            }
            if ((name.equals("trim") || name.equals("strip")) && i.getTarget() != null && i.getArguments().isEmpty()
                    && isJavaString(i.getTarget().getType())) {
                return "jtrim(" + expr(i.getTarget()) + ")";
            }
            if (name.equals("stripLeading") && i.getTarget() != null && i.getArguments().isEmpty()
                    && isJavaString(i.getTarget().getType())) {
                return "jstripLeading(" + expr(i.getTarget()) + ")";
            }
            if (name.equals("stripTrailing") && i.getTarget() != null && i.getArguments().isEmpty()
                    && isJavaString(i.getTarget().getType())) {
                return "jstripTrailing(" + expr(i.getTarget()) + ")";
            }
            if (name.equals("isBlank") && i.getTarget() != null && i.getArguments().isEmpty()
                    && isJavaString(i.getTarget().getType())) {
                return "jisBlank(" + expr(i.getTarget()) + ")";
            }
            if ((qualified.equals("java.lang.String") || simpleTargetName(i.getTarget()).equals("String")) && name.equals("join")) {
                return "jjoin" + arguments(i.getArguments());
            }
            if (name.startsWith("thenComparing") && i.getTarget() != null && !i.getArguments().isEmpty()) {
                return "jcomparing(" + expr(i.getTarget()) + ")." + name + "(" + expr(i.getArguments().get(0)) + ")";
            }
            if (name.equals("compare") && i.getTarget() != null && i.getArguments().size() == 2) {
                if (i.getTarget() instanceof CtTypeAccess<?>) {
                    return expr(i.getTarget()) + "::" + id(name) + arguments(i.getArguments());
                }
                if (qualified.equals("java.util.Comparator") || simpleTargetName(i.getTarget()).equals("Comparator")) {
                    return expr(i.getTarget()) + arguments(i.getArguments());
                }
            }
            if ((qualified.equals("java.lang.String") || i.getTarget() != null && isJavaString(i.getTarget().getType())) && name.equals("concat") && i.getTarget() != null && i.getArguments().size() == 1) {
                return "jconcat(" + expr(i.getTarget()) + ", " + expr(i.getArguments().get(0)) + ")";
            }
            if ((qualified.equals("java.lang.String") || i.getTarget() != null && isJavaString(i.getTarget().getType())) && name.equals("toString") && i.getTarget() != null && i.getArguments().isEmpty()) {
                return expr(i.getTarget());
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
            if (name.equals("hashCode") && i.getArguments().isEmpty() && i.getTarget() != null) {
                return "static_cast<int>(jhash(" + expr(i.getTarget()) + "))";
            }
            if (qualified.equals("java.util.Objects") && name.equals("equals")) {
                return "(" + expr(i.getArguments().get(0)) + " == " + expr(i.getArguments().get(1)) + ")";
            }
            if (name.equals("equals") && i.getArguments().size() == 1 && i.getTarget() != null) {
                CtTypeReference<?> targetType = i.getTarget().getType();
                if (targetType != null && supportsCppValueEquality(targetType)) {
                    return "(" + expr(i.getTarget()) + " == " + expr(i.getArguments().get(0)) + ")";
                }
                if (targetType == null || targetType.isPrimitive()
                        || targetType instanceof CtTypeParameterReference
                        || targetType.getQualifiedName().equals("java.lang.Object")) {
                    return "jequals(" + expr(i.getTarget()) + ", " + expr(i.getArguments().get(0)) + ")";
                }
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
                if (i.getTarget() != null) {
                    if (isObjectExpression(i.getTarget())) return expr(i.getTarget()) + ".type()";
                    return "typeid(" + expr(i.getTarget()) + ")";
                }
                return "typeid(shared_ptr<" + id(currentType != null ? currentType.getSimpleName() : "Object") + ">)";
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
                boolean validArgs = typeArgs.stream().noneMatch(ta -> ta instanceof CtTypeParameterReference tpr
                        && tpr.getDeclaration() != null && tpr.getDeclaration().getParent(CtMethod.class) != null);
                if (validArgs) {
                    StringBuilder sb = new StringBuilder("<");
                    for (int idx = 0; idx < typeArgs.size(); idx++) {
                        if (idx > 0) sb.append(", ");
                        sb.append(type(typeArgs.get(idx)));
                    }
                    sb.append(">");
                    typeArgsStr = sb.toString();
                }
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
                        "addExact", "subtractExact", "multiplyExact", "toIntExact", "negateExact",
                        "incrementExact", "decrementExact", "multiplyFull", "multiplyHigh",
                        "toRadians", "toDegrees" -> "j" + name;
                case "abs", "sqrt", "cbrt", "pow", "floor", "ceil", "round", "log", "log10", "log2", "exp",
                        "sin", "cos", "tan", "asin", "acos", "atan", "atan2", "hypot", "rint",
                        "log1p", "expm1", "sinh", "cosh", "tanh", "nextAfter", "copySign", "scalb" -> "std::" + name;
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
                case "parallelSetAll" -> "jparallelSetAll" + arguments(args);
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
            if (q.equals("java.util.Scanner")) return "JScanner()";
            if (q.equals("java.io.PrintWriter")) return "JPrintWriter()";
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
                CtTypeReference<?> expected = getExpectedType(c);
                if (expected != null && !expected.getActualTypeArguments().isEmpty()) {
                    rawType = rawCustomType(expected);
                } else if (!c.getArguments().isEmpty()) {
                    rawType = "decltype(" + rawType + args + ")";
                } else {
                    StringBuilder s = new StringBuilder(rawType).append('<');
                    int count = c.getType().getTypeDeclaration().getFormalCtTypeParameters().size();
                    for (int i = 0; i < count; i++) {
                        if (i > 0) s.append(", ");
                        s.append("JObject");
                    }
                    rawType = s.append('>').toString();
                }
            }
            if (isDeclaredType(c.getType()) && !rawType.startsWith("J") && !rawType.startsWith("std::")
                    && !rawType.startsWith("struct ") && !rawType.contains("::")) {
                rawType = "struct " + rawType;
            }
            if (rawType.contains("::") && rawType.contains("<")) {
                rawType = rawType.replaceAll("::([A-Za-z0-9_]+)<", "::template $1<");
                rawType = "typename " + rawType;
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
            if (currentStaticContext) {
                String typeName = t != null ? id(t.getSimpleName()) : (currentType != null ? id(currentType.getSimpleName()) : "Main");
                return "make_shared<" + typeName + ">()";
            }
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
            if (t != null && isDeclaredType(t) && e != null && e.getType() instanceof CtArrayTypeReference<?>) {
                String targetTypeStr = type(t);
                if (targetTypeStr.startsWith("vector<JList<")) {
                    return targetTypeStr + "(" + expr(e) + ")";
                }
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
                    .filter(m -> !m.isImplicit() && !(m.getSimpleName().equals("equals") && m.getParameters().size() == 1 && m.getParameters().get(0).getType().getQualifiedName().equals("java.lang.Object")))
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
                emitBlock(method.getBody(), false, method.getParameters());
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
            for (CtField<?> f : anonymous.getFields()) {
                s.append("        ").append(type(f.getType())).append(" ").append(fieldName(f.getReference()));
                if (f.getDefaultExpression() != null) {
                    s.append(" = ").append(initializer(f.getType(), f.getDefaultExpression()));
                }
                s.append(";\n");
            }
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
                    String sep = isDeclaredType(access.getAccessedType()) && !isRuntimeType(access.getAccessedType().getSimpleName()) ? "->" : ".";
                    return "[](const auto& x, auto&&... args) { return x" + sep + id(name) + "(std::forward<decltype(args)>(args)...); }";
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
                case "test", "apply", "applyAsInt", "applyAsLong", "applyAsDouble", "accept", "getAsInt", "getAsLong", "getAsBoolean", "get" -> true;
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
                emitBlock(l.getBody(), false, l.getParameters());
                String block = out.substring(length);
                out.setLength(length);
                if (saved.length() != length) throw new AssertionError();
                return s.append(block).toString();
            }
            boolean hasRefParam = l.getParameters().stream()
                    .anyMatch(p -> isPassByReferenceType(type(p.getType())));
            if (hasRefParam) {
                StringBuilder b = new StringBuilder("{\n");
                for (CtParameter<?> p : l.getParameters()) {
                    String parameterType = type(p.getType());
                    if (isPassByReferenceType(parameterType)) {
                        b.append("    auto& ").append(id(p.getSimpleName())).append(" = const_cast<").append(parameterType)
                         .append("&>(").append(id(p.getSimpleName())).append("_const);\n");
                    }
                }
                b.append("    return ").append(expr(l.getExpression())).append(";\n}");
                return s.append(b.toString()).toString();
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
            if (q.equals("java.util.Scanner")) return "JScanner";
            if (q.equals("java.io.PrintWriter")) return "JPrintWriter";
            String primitive = switch (q) {
                case "void" -> "void"; case "boolean", "java.lang.Boolean" -> "bool";
                case "byte" -> "int8_t"; case "short" -> "int16_t"; case "int", "java.lang.Integer" -> "int";
                case "long", "java.lang.Long" -> "long long"; case "char", "java.lang.Character" -> "char";
                case "float", "java.lang.Float" -> "float"; case "double", "java.lang.Double" -> "double";
                case "java.lang.String" -> "JString"; case "java.lang.Object" -> "JObject";
                case "java.lang.Number", "Number" -> "JNumber";
                case "java.lang.StringBuilder" -> "JStringBuilder";
                case "java.util.StringTokenizer", "StringTokenizer" -> "JStringTokenizer";
                case "java.util.Scanner", "Scanner" -> "JScanner"; case "java.io.PrintWriter", "PrintWriter" -> "JPrintWriter";
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
                    yield "function<int(" + type(arg) + ", " + type(arg) + ")>";
                }
                case "java.lang.Comparable" -> "shared_ptr<Comparable<" + (t.getActualTypeArguments().isEmpty() ? "JObject" : type(t.getActualTypeArguments().get(0))) + ">>";
                case "java.lang.Runnable" -> "Runnable";
                case "java.lang.AutoCloseable", "java.io.Closeable" -> "AutoCloseable";
                case "java.io.Serializable" -> "Serializable";
                case "java.lang.Cloneable" -> "Cloneable";
                case "java.lang.Runtime", "Runtime" -> "JRuntime";
                case "java.lang.Thread" -> "JThread"; case "java.util.Random", "java.util.random.RandomGenerator" -> "JRandom";
                case "java.util.Date", "Date" -> "JDate";
                case "java.lang.Class", "Class" -> "JClass";
                case "java.lang.Iterable" -> t.getActualTypeArguments().isEmpty() ? "JIterable<JObject>" : "JIterable<" + type(t.getActualTypeArguments().get(0)) + ">";
                case "java.util.BitSet", "BitSet" -> "JBitSet";
                case "java.math.BigInteger", "BigInteger" -> "JBigInteger";
                case "java.math.BigDecimal", "BigDecimal" -> "JBigDecimal";
                case "java.util.stream.IntStream" -> "JIntStream"; case "java.util.stream.LongStream" -> "JLongStream";
                case "java.util.Optional", "Optional" -> t.getActualTypeArguments().isEmpty() ? "JOptional<JObject>" : "JOptional<" + type(t.getActualTypeArguments().get(0)) + ">";
                case "java.util.OptionalInt", "OptionalInt" -> "JOptional<int>";
                case "java.util.OptionalLong", "OptionalLong" -> "JOptional<long long>";
                case "java.util.OptionalDouble", "OptionalDouble" -> "JOptional<double>";
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
            if (t instanceof CtTypeParameterReference parameter) {
                if (currentType != null && !currentType.getFormalCtTypeParameters().isEmpty()) {
                    boolean inScope = currentType.getFormalCtTypeParameters().stream()
                            .anyMatch(tp -> tp.getSimpleName().equals(parameter.getSimpleName()));
                    if (!inScope && (currentAnonymousMethod == null || currentAnonymousMethod.getFormalCtTypeParameters().stream().noneMatch(tp -> tp.getSimpleName().equals(parameter.getSimpleName())))) {
                        return id(currentType.getFormalCtTypeParameters().get(0).getSimpleName());
                    }
                }
                return typeParameterName(parameter);
            }
            if (isDeclaredType(t)) {
                String raw = rawCustomType(t);
                boolean hasFormalParams = (t.getTypeDeclaration() != null && !t.getTypeDeclaration().getFormalCtTypeParameters().isEmpty())
                        || model.getAllTypes().stream().anyMatch(decl -> decl.getSimpleName().equals(simpleName(t.getSimpleName())) && !decl.getFormalCtTypeParameters().isEmpty());
                if (t.getActualTypeArguments().isEmpty() && hasFormalParams && !raw.contains("<")) {
                    StringBuilder sb = new StringBuilder(raw).append('<');
                    int paramCount = t.getTypeDeclaration() != null ? t.getTypeDeclaration().getFormalCtTypeParameters().size() : 1;
                    for (int i = 0; i < paramCount; i++) {
                        if (i > 0) sb.append(", ");
                        if (currentType != null && !currentType.getFormalCtTypeParameters().isEmpty()) {
                            sb.append(id(currentType.getFormalCtTypeParameters().get(Math.min(i, currentType.getFormalCtTypeParameters().size() - 1)).getSimpleName()));
                        } else {
                            sb.append("JObject");
                        }
                    }
                    raw = sb.append('>').toString();
                }
                if (!raw.startsWith("J") && !raw.startsWith("std::") && !raw.startsWith("struct ") && !raw.contains("::")) {
                    raw = "struct " + raw;
                }
                if (raw.contains("::") && raw.contains("<")) {
                    raw = raw.replaceAll("::([A-Za-z0-9_]+)<", "::template $1<");
                    raw = "typename " + raw;
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
                case "java.io.PrintWriter", "PrintWriter" -> "JPrintWriter";
                case "java.util.Scanner", "Scanner" -> "JScanner";
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
            return false;
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
                case "java.util.function.IntPredicate" -> "function<bool(int)>";
                case "java.util.function.LongPredicate" -> "function<bool(long long)>";
                case "java.util.function.DoublePredicate" -> "function<bool(double)>";
                case "java.util.function.IntConsumer" -> "function<void(int)>";
                case "java.util.function.LongConsumer" -> "function<void(long long)>";
                case "java.util.function.DoubleConsumer" -> "function<void(double)>";
                case "java.util.function.IntSupplier" -> "function<int()>";
                case "java.util.function.LongSupplier" -> "function<long long()>";
                case "java.util.function.DoubleSupplier" -> "function<double()>";
                case "java.util.function.BooleanSupplier" -> "function<bool()>";
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
}
