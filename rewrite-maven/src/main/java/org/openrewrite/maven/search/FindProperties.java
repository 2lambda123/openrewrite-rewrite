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
package org.openrewrite.maven.search;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.internal.ListUtils;
import org.openrewrite.marker.RecipeSearchResult;
import org.openrewrite.maven.MavenVisitor;
import org.openrewrite.xml.tree.Xml;

import java.util.Optional;
import java.util.regex.Pattern;

@EqualsAndHashCode(callSuper = true)
@Data
public class FindProperties extends Recipe {
    private final String propertyPattern;

    @Override
    protected TreeVisitor<?, ExecutionContext> getVisitor() {
        Pattern propertyMatcher = Pattern.compile(propertyPattern.replace(".", "\\.")
                .replace("*", ".*"));
        return new MavenVisitor() {
            {
                setCursoringOn();
            }

            @Override
            public Xml visitTag(Xml.Tag tag, ExecutionContext context) {
                Xml.Tag t = (Xml.Tag) super.visitTag(tag, context);
                if (isPropertyTag() && propertyMatcher.matcher(tag.getName()).matches()) {
                    t = t.withMarker(new RecipeSearchResult(FindProperties.this));
                }

                Optional<String> value = tag.getValue();
                if (t.getContent() != null && value.isPresent() && value.get().contains("${")) {
                    t = t.withContent(ListUtils.mapFirst(t.getContent(), v -> v.withMarker(new RecipeSearchResult(FindProperties.this,
                            model.getValue(value.get())))));
                }
                return t;
            }
        };
    }
}
