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

import org.openrewrite.ExecutionContext;
import org.openrewrite.Recipe;
import org.openrewrite.TreeProcessor;
import org.openrewrite.Validated;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaType;

import static org.openrewrite.Validated.required;

public class UseStaticImport extends Recipe {
    private MethodMatcher methodMatcher;

    @Override
    protected TreeProcessor<?, ExecutionContext> getProcessor() {
        return new UseStaticImportProcessor(methodMatcher);
    }

    public void setMethod(String method) {
        this.methodMatcher = new MethodMatcher(method);
    }

    @Override
    public Validated validate() {
        return required("method", methodMatcher);
    }

    private static class UseStaticImportProcessor extends JavaIsoProcessor<ExecutionContext> {
        private final MethodMatcher methodMatcher;

        private UseStaticImportProcessor(MethodMatcher methodMatcher) {
            this.methodMatcher = methodMatcher;
        }

        @Override
        public J.MethodInvocation visitMethodInvocation(J.MethodInvocation method, ExecutionContext ctx) {
            J.MethodInvocation m = super.visitMethodInvocation(method, ctx);
            if (methodMatcher.matches(m) && m.getSelect() != null) {
                if (m.getType() != null) {
                    JavaType.FullyQualified receiverType = m.getType().getDeclaringType();
                    maybeRemoveImport(receiverType);

                    AddImport<ExecutionContext> addStatic = new AddImport<>(
                            receiverType.getFullyQualifiedName(),
                            m.getSimpleName(),
                            false);
                    if (!getAfterVisit().contains(addStatic)) {
                        doAfterVisit(addStatic);
                    }
                }

                m = m.withSelect(null).withName(m.getName().withPrefix(m.getSelect().getElem().getPrefix()));
            }
            return m;
        }
    }
}
