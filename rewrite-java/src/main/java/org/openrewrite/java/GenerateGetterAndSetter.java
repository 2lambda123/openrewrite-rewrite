/*
 * Copyright 2020 the original author or authors.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * https://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.openrewrite.java;

import lombok.EqualsAndHashCode;
import lombok.Value;
import org.openrewrite.*;
import org.openrewrite.internal.ListUtils;
import org.openrewrite.internal.StringUtils;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaType;
import org.openrewrite.java.tree.Statement;
import org.openrewrite.java.tree.TypeUtils;

import java.util.Collections;

@Value
@EqualsAndHashCode(callSuper = true)
public class GenerateGetterAndSetter extends Recipe {

    @Option(displayName = "Field name",
            description = "Name of field to generate getter and setter for.",
            example = "foo")
    String fieldName;

    @Override
    public String getDisplayName() {
        return "Generate getter and setter methods for field";
    }

    @Override
    public String getDescription() {
        return "Generate `get` and `set` methods.";
    }

    @Override
    protected TreeVisitor<?, ExecutionContext> getVisitor() {
        return new GenerateGetterVisitor<>();
    }

    private class GenerateGetterVisitor<P> extends JavaIsoVisitor<P> {
        private final String capitalizedFieldName = StringUtils.capitalize(fieldName);
        private final JavaTemplate getter = JavaTemplate
                .builder(this::getCursor, "" + "public #{} #{}() {return #{};}").build();
        private final JavaTemplate setter = JavaTemplate
                .builder(this::getCursor, "" + "public void set#{}(#{} #{}) {this.#{} = #{};}").build();

        @Override
        public J.VariableDeclarations.NamedVariable visitVariable(J.VariableDeclarations.NamedVariable variable, P p) {
            if (variable.isField(getCursor()) && variable.getSimpleName().equals(fieldName)) {
                getCursor().putMessageOnFirstEnclosing(J.ClassDeclaration.class, "varCursor", getCursor());
            }
            return super.visitVariable(variable, p);
        }

        @Override
        public J.MethodDeclaration visitMethodDeclaration(J.MethodDeclaration method, P p) {
            J.MethodDeclaration mi = super.visitMethodDeclaration(method, p);

            if (mi.getSimpleName().equals("get" + capitalizedFieldName) || mi.getSimpleName().equals("is" + capitalizedFieldName)) {
                getCursor().dropParentUntil(J.ClassDeclaration.class::isInstance).putMessage("getter-exists", true);
            }
            if (mi.getSimpleName().equals("set" + capitalizedFieldName)) {
                getCursor().dropParentUntil(J.ClassDeclaration.class::isInstance).putMessage("setter-exists", true);
            }
            return mi;
        }

        @Override
        public J.ClassDeclaration visitClassDeclaration(J.ClassDeclaration classDecl, P p) {
            J.ClassDeclaration c = super.visitClassDeclaration(classDecl, p);
            Cursor varCursor = getCursor().pollMessage("varCursor");
            if (varCursor != null) {
                J.VariableDeclarations.NamedVariable var = varCursor.getValue();
                JavaType.FullyQualified fullyQualified = TypeUtils.asFullyQualified(var.getType());
                JavaType.Primitive primitive = TypeUtils.asPrimitive(var.getType());

                String getterPrefix = "get";
                String fieldType = null;

                if (primitive != null) {
                    getterPrefix = primitive == JavaType.Primitive.Boolean ? "is" : "get";
                    fieldType = primitive.getKeyword();
                } else if (fullyQualified != null) {
                    fieldType = fullyQualified.getClassName();
                }
                if (getCursor().pollMessage("getter-exists") == null) {
                    Statement getterStatement = maybeAutoFormat(c, c.withBody(c.getBody().withStatements(Collections.emptyList()))
                            .withTemplate(getter, c.getBody().getCoordinates().lastStatement(), fieldType,
                                    getterPrefix + capitalizedFieldName, var.getSimpleName()), p).getBody().getStatements().get(0);
                    c = c.withBody(c.getBody().withStatements(ListUtils.concat(c.getBody().getStatements(), getterStatement)));
                }
                if (getCursor().pollMessage("setter-exists") == null) {
                    Statement setterStatement = maybeAutoFormat(c, c.withBody(c.getBody().withStatements(Collections.emptyList()))
                            .withTemplate(setter, c.getBody().getCoordinates().lastStatement(), capitalizedFieldName, fieldType,
                                    var.getSimpleName(), var.getSimpleName(), var.getSimpleName()), p).getBody().getStatements().get(0);
                    c = c.withBody(c.getBody().withStatements(ListUtils.concat(c.getBody().getStatements(), setterStatement)));
                }
            }
            return c;
        }
    }
}
