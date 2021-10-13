package org.openrewrite.java.cleanup;

import org.openrewrite.ExecutionContext;
import org.openrewrite.Recipe;
import org.openrewrite.Tree;
import org.openrewrite.TreeVisitor;
import org.openrewrite.internal.ListUtils;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.JavaTemplate;
import org.openrewrite.java.tree.*;
import org.openrewrite.marker.Markers;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

public class AddSerialVersionUidToSerializable extends Recipe {

    private static final JavaType.Class SERIALIZABLE_FQ =  JavaType.Class.build("java.io.Serializable");
    private static final JavaType.Class COLLECTION_FQ =  JavaType.Class.build("java.util.Collection");
    private static final JavaType.Class MAP_FQ =  JavaType.Class.build("java.util.Map");
    private static final JavaType.Class THROWABLE_FQ = JavaType.Class.build("java.lang.Throwable");

    @Override
    public String getDisplayName() {
        return "Add `SerialVersionUID` to a Serializable class when it's missing.";
    }

    @Override
    public String getDescription() {
        return "A `serialVersionUID` field is strongly recommended in all `Serializable` classes. If this is not " +
                "defined on a `Serializable` class, the compiler will generate this value. If a change is later made " +
                "to the class, the generated value will change and attempts to deserialize the class will fail.";
    }

    @Override
    public Set<String> getTags() {
        return Collections.singleton("RSPEC-2057");
    }

    @Override
    protected TreeVisitor<?, ExecutionContext> getVisitor() {
        return new JavaIsoVisitor<ExecutionContext>() {
            JavaTemplate template = JavaTemplate.builder(this::getCursor, "private static final long serialVersionUID = 1").build();

            @Override
            public J.ClassDeclaration visitClassDeclaration(J.ClassDeclaration classDecl, ExecutionContext ctx) {
                J.ClassDeclaration c = super.visitClassDeclaration(classDecl, ctx);
                if (implementsSerializable(c.getType())) {
                    AtomicBoolean serializedFound = new AtomicBoolean(false);
                    c = c.withBody(c.getBody().withStatements(
                            ListUtils.map(c.getBody().getStatements(), (i, s) -> {
                                if (s instanceof J.VariableDeclarations) {
                                    J.VariableDeclarations variableDeclarations = (J.VariableDeclarations) s;
                                    for (J.VariableDeclarations.NamedVariable v : variableDeclarations.getVariables()) {
                                        if (v.getSimpleName().equals("serialVersionUID")) {
                                            serializedFound.set(true);
                                            List<J.Modifier> modifiers = variableDeclarations.getModifiers();

                                            if (!J.Modifier.hasModifier(modifiers, J.Modifier.Type.Private)
                                              || !J.Modifier.hasModifier(modifiers, J.Modifier.Type.Static)
                                              || !J.Modifier.hasModifier(modifiers, J.Modifier.Type.Final)) {
                                                variableDeclarations = variableDeclarations.withModifiers(Arrays.asList(
                                                        new J.Modifier(Tree.randomId(), Space.EMPTY, Markers.EMPTY, J.Modifier.Type.Private, Collections.emptyList()),
                                                        new J.Modifier(Tree.randomId(), Space.format(" "), Markers.EMPTY, J.Modifier.Type.Static, Collections.emptyList()),
                                                        new J.Modifier(Tree.randomId(), Space.format(" "), Markers.EMPTY, J.Modifier.Type.Final, Collections.emptyList())
                                                ));
                                            }
                                            JavaType.Primitive variableType = TypeUtils.asPrimitive(variableDeclarations.getType());
                                            if (variableType != JavaType.Primitive.Long) {
                                                variableDeclarations = variableDeclarations.withTypeExpression(new J.Primitive(Tree.randomId(), Space.EMPTY, Markers.EMPTY, JavaType.Primitive.Long));
                                            }
                                            if (s != variableDeclarations) {
                                                return autoFormat(variableDeclarations, ctx).withPrefix(s.getPrefix());
                                            }
                                        }
                                    }
                                }
                                return s;
                            })
                    ));
                    if (!serializedFound.get()) {
                        c = c.withTemplate(template, c.getBody().getCoordinates().firstStatement());
                    }
                }
                return c;
            }
        };
    }

    public static boolean implementsSerializable(@Nullable JavaType type) {
        if (type == null) {
            return false;
        } else if (type instanceof JavaType.Primitive) {
            return true;
        } else if (type instanceof JavaType.Array) {
            return implementsSerializable(((JavaType.Array) type).getElemType());
        } else if (type instanceof JavaType.Parameterized) {
            JavaType.Parameterized parameterized = (JavaType.Parameterized) type;
            if (COLLECTION_FQ.isAssignableFrom(parameterized) || MAP_FQ.isAssignableFrom(parameterized)) {
                //If the type is either a collection or a map, make sure the type parameters are serializable. We
                //force all type parameters to be checked to correctly scoop up all non-serializable candidates.
                boolean typeParametersSerializable = true;
                for (JavaType typeParameter : parameterized.getTypeParameters()) {
                    typeParametersSerializable = typeParametersSerializable && implementsSerializable(typeParameter);
                }
                return typeParametersSerializable;
            }
            //All other parameterized types fall through
        } else if (type instanceof JavaType.FullyQualified) {
            JavaType.FullyQualified fq = (JavaType.FullyQualified) type;
            if (fq.getKind() != JavaType.Class.Kind.Interface &&
                    !THROWABLE_FQ.isAssignableFrom(fq)) {
                return SERIALIZABLE_FQ.isAssignableFrom(fq);
            }
        }
        return false;
    }

}
