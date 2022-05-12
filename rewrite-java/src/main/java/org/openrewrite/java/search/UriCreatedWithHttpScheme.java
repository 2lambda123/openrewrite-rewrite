/*
 * Copyright 2022 the original author or authors.
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

import org.openrewrite.Cursor;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.JavaVisitor;
import org.openrewrite.java.MethodMatcher;
import org.openrewrite.java.dataflow.LocalFlowSpec;
import org.openrewrite.java.tree.Expression;
import org.openrewrite.java.tree.J;

public class UriCreatedWithHttpScheme extends Recipe {
    private static final MethodMatcher URI_CREATE = new MethodMatcher("java.net.URI create(..)");
    private static final MethodMatcher STRING_REPLACE = new MethodMatcher("java.lang.String replace(..)");

    private static final LocalFlowSpec<J.Literal, J.MethodInvocation> INSECURE_URI = new LocalFlowSpec<J.Literal, J.MethodInvocation>() {
        @Override
        public boolean isSource(J.Literal source, Cursor cursor) {
            return source.getValue() != null && source.getValue().toString().startsWith("http://");
        }

        @Override
        public boolean isSink(J.MethodInvocation expr, Cursor cursor) {
            return URI_CREATE.matches(expr);
        }

        @Override
        public boolean isBarrierGuard(Expression expr) {
            return STRING_REPLACE.matches(expr);
        }
    };

    @Override
    public String getDisplayName() {
        return "URIs created with an HTTP scheme";
    }

    @Override
    public String getDescription() {
        return "This is a sample recipe demonstrating a simple application of local data flow analysis.";
    }

    @Override
    protected TreeVisitor<?, ExecutionContext> getSingleSourceApplicableTest() {
        return new UsesMethod<>(URI_CREATE);
    }

    @Override
    protected JavaVisitor<ExecutionContext> getVisitor() {
        return new JavaIsoVisitor<ExecutionContext>() {
            @Override
            public J.Literal visitLiteral(J.Literal literal, ExecutionContext ctx) {
                J.Literal l = super.visitLiteral(literal, ctx);
                if (dataflow().findSinks(INSECURE_URI).isNotEmpty()) {
                    //noinspection ConstantConditions
                    return l.withValue(l.getValue().toString().replace("http://", "https://"))
                            .withValueSource(l.getValueSource().replace("http://", "https://"));
                }
                return l;
            }
        };
    }
}
