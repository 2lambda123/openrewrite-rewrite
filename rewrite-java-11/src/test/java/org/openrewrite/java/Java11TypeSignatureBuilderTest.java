package org.openrewrite.java;

import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.util.TreeScanner;
import com.sun.tools.javac.code.Type;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.util.List;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.Parser;
import org.openrewrite.internal.StringUtils;

import java.io.ByteArrayInputStream;
import java.nio.file.Paths;

import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;

class Java11TypeSignatureBuilderTest implements JavaTypeSignatureBuilderTest {
    private final Java11Parser javaParser = Java11Parser.builder()
            .logCompilationWarningsAndErrors(true)
            .build();

    private final Java11TypeSignatureBuilder signatureBuilder = new Java11TypeSignatureBuilder();

    @BeforeEach
    void before() {
        javaParser.reset();
    }

    @Override
    @Test
    public void arraySignature() {
        methodFirstParameterSignatureEquals("" +
                        "interface Test {" +
                        "  void test(Integer[] n);" +
                        "}",
                "java.lang.Integer[]"
        );
    }

    @Override
    @Test
    public void classSignature() {
        methodFirstParameterSignatureEquals("" +
                        "import java.io.File;" +
                        "interface Test {" +
                        "    void test(File f);" +
                        "}",
                "java.io.File"
        );
    }

    @Override
    @Test
    public void primitiveSignature() {
        methodFirstParameterSignatureEquals("" +
                        "import java.io.File;" +
                        "interface Test {" +
                        "    void test(int n);" +
                        "}",
                "int"
        );
    }

    @Override
    @Test
    public void parameterizedSignature() {
        methodFirstParameterSignatureEquals("" +
                        "import java.util.List;" +
                        "interface Test {" +
                        "    void test(List<String> l);" +
                        "}",
                "java.util.List<java.lang.String>"
        );
    }

    @Override
    @Test
    public void genericTypeVariable() {
        methodFirstParameterSignatureEquals("" +
                        "import java.util.List;" +
                        "interface Test {" +
                        "    void test(List<? extends String> l);" +
                        "}",
                "java.util.List<? extends java.lang.String>"
        );
    }

    @Override
    @Test
    public void genericVariableContravariant() {
        methodFirstParameterSignatureEquals("" +
                        "import java.util.List;" +
                        "interface Test {" +
                        "    void test(List<? super Test> l);" +
                        "}",
                "java.util.List<? super Test>"
        );
    }

    @Override
    @Test
    public void traceySpecial() {
        classTypeParameterSignatureEquals("" +
                        "import java.util.*;" +
                        "interface Test<T extends A<? extends T>> {}" +
                        "interface A<U> {}",
                "T extends A<? extends T>");
    }

    @Override
    @Test
    public void genericVariableMultipleBounds() {
        classTypeParameterSignatureEquals("" +
                        "import java.util.*;" +
                        "interface Test<T extends A & B> {}" +
                        "interface A {}" +
                        "interface B {}",
                "T extends A & B");
    }

    @Override
    @Test
    public void genericTypeVariableUnbounded() {
        methodFirstParameterSignatureEquals("" +
                        "import java.util.List;" +
                        "interface Test {" +
                        "    <T> void test(List<T> l);" +
                        "}",
                "java.util.List<T>"
        );
    }

    private void classTypeParameterSignatureEquals(@Language("java") String source, String signature) {
        assertThat(signatureBuilder.signature(classTypeParameterSignature(source))).isEqualTo(signature);
    }

    private Type classTypeParameterSignature(@Language("java") String source) {
        JCTree.JCCompilationUnit cu = compilationUnit(source);
        return new TreeScanner<Type, Integer>() {
            @Override
            public Type visitCompilationUnit(CompilationUnitTree node, Integer integer) {
                JCTree.JCClassDecl classDecl = (JCTree.JCClassDecl) ((JCTree.JCCompilationUnit) node).getTypeDecls().get(0);
                return classDecl.getTypeParameters().get(0).type;
            }
        }.scan(cu, 0);
    }

    private void methodFirstParameterSignatureEquals(@Language("java") String source, String signature) {
        assertThat(signatureBuilder.signature(methodFirstParameterType(source))).isEqualTo(signature);
    }

    private Type methodFirstParameterType(@Language("java") String source) {
        JCTree.JCCompilationUnit cu = compilationUnit(source);
        return new TreeScanner<Type, Integer>() {
            @Override
            public Type visitMethod(MethodTree node, Integer integer) {
                List<JCTree.JCVariableDecl> params = ((JCTree.JCMethodDecl) node).getParameters();
                return params.iterator().next().type;
            }
        }.scan(cu, 0);
    }

    private JCTree.JCCompilationUnit compilationUnit(@Language("java") String source) {
        return javaParser.parseInputsToCompilerAst(
                singletonList(new Parser.Input(Paths.get("Test.java"), () -> new ByteArrayInputStream(StringUtils.trimIndent(source).getBytes()))),
                new InMemoryExecutionContext(Throwable::printStackTrace)).entrySet().iterator().next().getValue();
    }
}
