/*
 * Copyright 2023 the original author or authors.
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
package org.openrewrite.java.internal.template;

import org.openrewrite.Cursor;
import org.openrewrite.ExecutionContext;
import org.openrewrite.TreeVisitor;
import org.openrewrite.java.JavaTemplate;
import org.openrewrite.java.JavaVisitor;
import org.openrewrite.java.ShortenFullyQualifiedTypeReferences;
import org.openrewrite.java.cleanup.SimplifyBooleanExpressionVisitor;
import org.openrewrite.java.cleanup.UnnecessaryParenthesesVisitor;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaCoordinates;

import java.util.EnumSet;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

@SuppressWarnings("unused")
public abstract class AbstractRefasterJavaVisitor extends JavaVisitor<ExecutionContext> {

    protected final <T> Supplier<T> memoize(Supplier<T> delegate) {
        AtomicReference<T> value = new AtomicReference<>();
        return () -> {
            T val = value.get();
            if (val == null) {
                val = value.updateAndGet(cur -> cur == null ? Objects.requireNonNull(delegate.get()) : cur);
            }
            return val;
        };
    }

    protected final JavaTemplate.Matcher matcher(Supplier<JavaTemplate> template, Cursor cursor) {
        return template.get().matcher(cursor);
    }

    protected final J apply(Supplier<JavaTemplate> template, Cursor cursor, JavaCoordinates coordinates, Object... parameters) {
        return template.get().apply(cursor, coordinates, parameters);
    }

    protected J embed(J j, Cursor cursor, ExecutionContext ctx) {
        return embed(j, cursor, ctx, EmbeddingOptions.ALL_OPTIONS);
    }

    @SuppressWarnings({"DataFlowIssue", "SameParameterValue"})
    protected J embed(J j, Cursor cursor, ExecutionContext ctx, EnumSet<EmbeddingOptions> options) {
        TreeVisitor<?, ExecutionContext> visitor;
        if (options.contains(EmbeddingOptions.REMOVE_PARENS) && !getAfterVisit().contains(visitor = new UnnecessaryParenthesesVisitor())) {
            doAfterVisit(visitor);
        }
        if (options.contains(EmbeddingOptions.SHORTEN_NAMES) && !getAfterVisit().contains(visitor = new ShortenFullyQualifiedTypeReferences().getVisitor())) {
            doAfterVisit(visitor);
        }
        if (options.contains(EmbeddingOptions.SIMPLIFY_BOOLEANS)) {
            j = new SimplifyBooleanExpressionVisitor().visitNonNull(j, ctx, cursor.getParent());
        }
        return j;
    }

    protected enum EmbeddingOptions {
        SHORTEN_NAMES, SIMPLIFY_BOOLEANS, REMOVE_PARENS;

        public static final EnumSet<EmbeddingOptions> ALL_OPTIONS = EnumSet.allOf(EmbeddingOptions.class);
    }
}
