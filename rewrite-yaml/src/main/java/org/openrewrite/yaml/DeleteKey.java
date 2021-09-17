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
package org.openrewrite.yaml;

import lombok.EqualsAndHashCode;
import lombok.Value;
import org.openrewrite.*;
import org.openrewrite.internal.ListUtils;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.yaml.tree.Yaml;

import java.util.concurrent.atomic.AtomicReference;

@Value
@EqualsAndHashCode(callSuper = true)
public class DeleteKey extends Recipe {
    @Option(displayName = "Key path",
            description = "A JsonPath expression to locate a YAML entry.",
            example = "$.source.kind")
    String keyPath;

    @Incubating(since = "7.8.0")
    @Option(displayName = "Optional file matcher",
            description = "Matching files will be modified. This is a glob expression.",
            required = false,
            example = "**/application-*.yml")
    @Nullable
    String fileMatcher;

    @Override
    public String getDisplayName() {
        return "Delete key";
    }

    @Override
    public String getDescription() {
        return "Delete a YAML mapping entry key.";
    }

    @Override
    protected TreeVisitor<?, ExecutionContext> getSingleSourceApplicableTest() {
        if (fileMatcher != null) {
            return new HasSourcePath<>(fileMatcher);
        }
        return null;
    }

    @Override
    protected TreeVisitor<?, ExecutionContext> getVisitor() {
        JsonPathMatcher matcher = new JsonPathMatcher(keyPath);
        return new YamlIsoVisitor<ExecutionContext>() {
            @Override
            public Yaml.Mapping visitMapping(Yaml.Mapping mapping, ExecutionContext ctx) {
                Yaml.Mapping m = super.visitMapping(mapping, ctx);
                AtomicReference<String> copyFirstPrefix = new AtomicReference<>();
                m = m.withEntries(ListUtils.map(m.getEntries(), (i, e) -> {
                    if (matcher.matches(new Cursor(getCursor(), e))) {
                        if (i == 0 && getCursor().getParentOrThrow().getValue() instanceof Yaml.Sequence.Entry) {
                            copyFirstPrefix.set(e.getPrefix());
                        }
                        removeUnused();
                        return null;
                    }
                    return e;
                }));

                if (!m.getEntries().isEmpty() && copyFirstPrefix.get() != null) {
                    m = m.withEntries(ListUtils.mapFirst(m.getEntries(), e -> e.withPrefix(copyFirstPrefix.get())));
                }

                return m;
            }
        };
    }
}
