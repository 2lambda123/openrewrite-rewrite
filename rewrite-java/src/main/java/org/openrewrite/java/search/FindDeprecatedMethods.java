/*
 * Copyright 2021 the original author or authors.
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
package org.openrewrite.java.search;

import lombok.EqualsAndHashCode;
import lombok.Value;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Option;
import org.openrewrite.Recipe;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.JavaVisitor;
import org.openrewrite.java.MethodMatcher;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaType;
import org.openrewrite.java.tree.TypeUtils;

import java.util.Iterator;
import java.util.List;

@Value
@EqualsAndHashCode(callSuper = true)
public class FindDeprecatedMethods extends Recipe {
    @Option(displayName = "Method pattern",
            description = "A method pattern that is used to find matching method invocations.",
            example = "java.util.List add(..)",
            required = false)
    @Nullable
    String methodPattern;

    @Option(displayName = "Ignore deprecated scopes",
            description = "When a deprecated method is used in a deprecated method or class, ignore it.",
            required = false)
    @Nullable
    Boolean ignoreDeprecatedScopes;

    @Override
    public String getDisplayName() {
        return "Find uses of deprecated methods";
    }

    @Override
    public String getDescription() {
        return "Find uses of deprecated methods in any API.";
    }

    @Override
    protected JavaVisitor<ExecutionContext> getSingleSourceApplicableTest() {
        MethodMatcher methodMatcher = methodPattern == null ? null : new MethodMatcher(methodPattern);

        return new JavaIsoVisitor<ExecutionContext>() {
            @Override
            public J.CompilationUnit visitCompilationUnit(J.CompilationUnit cu, ExecutionContext ctx) {
                for (JavaType javaType : cu.getTypesInUse()) {
                    JavaType.Method method = TypeUtils.asMethod(javaType);
                    if (method != null && (methodMatcher == null || methodMatcher.matches(method))) {
                        for (JavaType.FullyQualified annotation : method.getAnnotations()) {
                            if (TypeUtils.isOfClassType(annotation, "java.lang.Deprecated")) {
                                return cu.withMarkers(cu.getMarkers().searchResult());
                            }
                        }
                    }
                }
                return cu;
            }
        };
    }

    @Override
    public JavaVisitor<ExecutionContext> getVisitor() {
        return new JavaIsoVisitor<ExecutionContext>() {
            @Override
            public J.MethodInvocation visitMethodInvocation(J.MethodInvocation method, ExecutionContext ctx) {
                J.MethodInvocation m = super.visitMethodInvocation(method, ctx);
                if (method.getType() != null) {
                    for (JavaType.FullyQualified annotation : method.getType().getAnnotations()) {
                        if (TypeUtils.isOfClassType(annotation, "java.lang.Deprecated")) {
                            if (Boolean.TRUE.equals(ignoreDeprecatedScopes)) {
                                Iterator<Object> cursorPath = getCursor().getPath();
                                while (cursorPath.hasNext()) {
                                    Object ancestor = cursorPath.next();
                                    if (ancestor instanceof J.MethodDeclaration &&
                                            isDeprecated(((J.MethodDeclaration) ancestor).getAllAnnotations())) {
                                        return m;
                                    }
                                    if (ancestor instanceof J.ClassDeclaration &&
                                            isDeprecated(((J.ClassDeclaration) ancestor).getAllAnnotations())) {
                                        return m;
                                    }
                                }
                            }

                            m = m.withMarkers(m.getMarkers().searchResult());
                        }
                    }
                }
                return m;
            }

            private boolean isDeprecated(List<J.Annotation> annotations) {
                for (J.Annotation annotation : annotations) {
                    if (TypeUtils.isOfClassType(annotation.getType(), "java.lang.Deprecated")) {
                        return true;
                    }
                }
                return false;
            }
        };
    }
}
