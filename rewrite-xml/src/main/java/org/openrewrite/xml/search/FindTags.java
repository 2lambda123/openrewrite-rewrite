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
package org.openrewrite.xml.search;

import lombok.EqualsAndHashCode;
import lombok.Value;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Option;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.xml.XPathMatcher;
import org.openrewrite.xml.XmlVisitor;
import org.openrewrite.xml.marker.XmlSearchResult;
import org.openrewrite.xml.tree.Xml;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import static org.openrewrite.Tree.randomId;

@EqualsAndHashCode(callSuper = true)
@Value
public class FindTags extends Recipe {

    @Option(displayName = "XPath",
            description = "XPath expression used to find matching tags.",
            example = "/dependencies/dependency")
    String xPath;

    @Override
    public String getDisplayName() {
        return "Find XML tags";
    }

    @Override
    public String getDescription() {
        return "Find XML tags by XPath expression.";
    }

    UUID id = randomId();

    @Override
    protected TreeVisitor<?, ExecutionContext> getVisitor() {
        XPathMatcher xPathMatcher = new XPathMatcher(xPath);
        return new XmlVisitor<ExecutionContext>() {

            @Override
            public Xml visitTag(Xml.Tag tag, ExecutionContext ctx) {
                Xml.Tag t = (Xml.Tag) super.visitTag(tag, ctx);
                if (xPathMatcher.matches(getCursor())) {
                    t = t.withMarkers(t.getMarkers().addIfAbsent(new XmlSearchResult(id,FindTags.this)));
                }
                return t;
            }
        };
    }

    public static Set<Xml.Tag> find(Xml x, String xPath) {
        XPathMatcher xPathMatcher = new XPathMatcher(xPath);
        XmlVisitor<Set<Xml.Tag>> findVisitor = new XmlVisitor<Set<Xml.Tag>>() {

            @Override
            public Xml visitTag(Xml.Tag tag, Set<Xml.Tag> ts) {
                if (xPathMatcher.matches(getCursor())) {
                    ts.add(tag);
                }
                return super.visitTag(tag, ts);
            }
        };

        Set<Xml.Tag> ts = new HashSet<>();
        findVisitor.visit(x, ts);
        return ts;
    }
}
