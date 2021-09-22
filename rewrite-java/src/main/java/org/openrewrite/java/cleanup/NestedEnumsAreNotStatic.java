package org.openrewrite.java.cleanup;

import org.openrewrite.ExecutionContext;
import org.openrewrite.Recipe;
import org.openrewrite.internal.ListUtils;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.tree.J;

import java.util.Collections;
import java.util.Set;

public class NestedEnumsAreNotStatic extends Recipe {
    @Override
    public String getDisplayName() {
        return "Nested enums are not static";
    }

    @Override
    public String getDescription() {
        return "Remove static modifier from nested enum types since they are implicitly static.";
    }

    @Override
    public Set<String> getTags() {
        return Collections.singleton("RSPEC-2786");
    }

    @Override
    protected JavaIsoVisitor<ExecutionContext> getSingleSourceApplicableTest() {
        return new JavaIsoVisitor<ExecutionContext>() {
            @Override
            public J.ClassDeclaration visitClassDeclaration(J.ClassDeclaration classDecl, ExecutionContext executionContext) {
                J.ClassDeclaration cd = super.visitClassDeclaration(classDecl, executionContext);
                if (cd.getKind() == J.ClassDeclaration.Kind.Type.Enum && cd.getType() != null && cd.getType().getOwningClass() != null) {
                    cd = cd.withMarkers(cd.getMarkers().searchResult());
                }
                return cd;
            }
        };
    }

    @Override
    protected JavaIsoVisitor<ExecutionContext> getVisitor() {
        return new JavaIsoVisitor<ExecutionContext>() {
            @Override
            public J.ClassDeclaration visitClassDeclaration(J.ClassDeclaration classDecl, ExecutionContext executionContext) {
                J.ClassDeclaration cd = super.visitClassDeclaration(classDecl, executionContext);
                if (cd.getKind() == J.ClassDeclaration.Kind.Type.Enum && cd.getType() != null && cd.getType().getOwningClass() != null) {
                    if (J.Modifier.hasModifier(cd.getModifiers(), J.Modifier.Type.Static)) {
                        cd = maybeAutoFormat(cd,cd.withModifiers(ListUtils.map(cd.getModifiers(), mod -> mod.getType() == J.Modifier.Type.Static ? null : mod)), executionContext);
                    }
                }
                return cd;
            }
        };
    }
}
