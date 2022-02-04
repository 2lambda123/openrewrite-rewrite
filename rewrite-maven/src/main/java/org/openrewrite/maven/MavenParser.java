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
package org.openrewrite.maven;

import org.intellij.lang.annotations.Language;
import org.openrewrite.ExecutionContext;
import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.Parser;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.maven.internal.MavenPomDownloader;
import org.openrewrite.maven.internal.RawPom;
import org.openrewrite.maven.tree.*;
import org.openrewrite.xml.XmlParser;
import org.openrewrite.xml.tree.Xml;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.util.Collections.*;
import static org.openrewrite.Tree.randomId;

public class MavenParser implements Parser<Xml.Document> {
    private final Collection<String> activeProfiles;

    /**
     * @param activeProfiles  The maven profile names set to be active. Profiles are typically defined in the settings.xml
     */
    private MavenParser(Collection<String> activeProfiles) {
        this.activeProfiles = activeProfiles;
    }

    @Override
    public List<Xml.Document> parse(@Language("xml") String... sources) {
        return parse(new InMemoryExecutionContext(), sources);
    }

    @Override
    public List<Xml.Document> parse(ExecutionContext ctx, @Language("xml") String... sources) {
        return Parser.super.parse(ctx, sources);
    }

    private static class MavenXmlParser extends XmlParser {
        @Override
        public boolean accept(Path path) {
            return super.accept(path) || path.toString().endsWith(".pom");
        }
    }

    @Override
    public List<Xml.Document> parseInputs(Iterable<Input> sources, @Nullable Path relativeTo,
                                   ExecutionContext ctx) {
        Map<Xml.Document, Pom> projectPoms = new LinkedHashMap<>();
        Map<Path, Pom> projectPomsByPath = new HashMap<>();
        for (Input source : sources) {
            Pom pom = RawPom.parse(source.getSource(), null).toPom(source.getPath(), null);
            if (relativeTo != null) {
                if (pom.getProperties() == null || pom.getProperties().isEmpty()) {
                    pom = pom.withProperties(new LinkedHashMap<>());
                }
                pom.getProperties().put("project.basedir", relativeTo.toString());
                pom.getProperties().put("basedir", relativeTo.toString());
            }

            Xml.Document xml = new MavenXmlParser()
                    .parseInputs(singletonList(source), relativeTo, ctx)
                    .iterator().next();

            projectPoms.put(xml, pom);
            projectPomsByPath.put(source.getPath(), pom);
        }

        List<Xml.Document> parsed = new ArrayList<>();

        MavenPomDownloader downloader = new MavenPomDownloader(projectPomsByPath, ctx);

        for (Map.Entry<Xml.Document, Pom> docToPom : projectPoms.entrySet()) {
            ResolvedPom resolvedPom = docToPom.getValue().resolve(activeProfiles, downloader, ctx);
            MavenResolutionResult model = new MavenResolutionResult(randomId(), resolvedPom, emptyList(), null, emptyMap())
                    .resolveDependencies(downloader, ctx);
            parsed.add(docToPom.getKey().withMarkers(docToPom.getKey().getMarkers().compute(model, (old, n) -> n)));
        }

        for (int i = 0; i < parsed.size(); i++) {
            Xml.Document maven = parsed.get(i);
            MavenResolutionResult resolutionResult = maven.getMarkers().findFirst(MavenResolutionResult.class)
                    .orElseThrow(() -> new IllegalStateException("Expected to find a maven resolution marker"));

            List<MavenResolutionResult> modules = new ArrayList<>(0);
            for (Xml.Document possibleModule : parsed) {
                if(possibleModule == maven) {
                    continue;
                }
                MavenResolutionResult moduleResolutionResult = possibleModule.getMarkers().findFirst(MavenResolutionResult.class)
                        .orElseThrow(() -> new IllegalStateException("Expected to find a maven resolution marker"));
                Parent parent = moduleResolutionResult.getPom().getRequested().getParent();
                if (parent != null &&
                        parent.getGroupId().equals(resolutionResult.getPom().getGroupId()) &&
                        parent.getArtifactId().equals(resolutionResult.getPom().getArtifactId()) &&
                        parent.getVersion().equals(resolutionResult.getPom().getVersion())) {
                    moduleResolutionResult.unsafeSetParent(resolutionResult);
                    modules.add(moduleResolutionResult);
                }
            }

            if (!modules.isEmpty()) {
                parsed.set(i, maven.withMarkers(maven.getMarkers().computeByType(resolutionResult.withModules(modules), (old, n) -> n)));
            }
        }

        return parsed;
    }

    @Override
    public boolean accept(Path path) {
        return path.toString().equals("pom.xml") || path.toString().endsWith(".pom");
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private final Collection<String> activeProfiles = new HashSet<>();

        public Builder activeProfiles(@Nullable String... profiles) {
            //noinspection ConstantConditions
            if (profiles != null) {
                Collections.addAll(this.activeProfiles, profiles);
            }
            return this;
        }

        public Builder mavenConfig(@Nullable Path mavenConfig) {
            if (mavenConfig != null && mavenConfig.toFile().exists()) {
                try {
                    String mavenConfigText = new String(Files.readAllBytes(mavenConfig));
                    Matcher matcher = Pattern.compile("(?:$|\\s)-P\\s+([^\\s]+)").matcher(mavenConfigText);
                    if (matcher.find()) {
                        String[] profiles = matcher.group(1).split(",");
                        return activeProfiles(profiles);
                    }
                } catch (IOException e) {
                    throw new UncheckedIOException(e);
                }
            }
            return this;
        }

        public MavenParser build() {
            return new MavenParser(activeProfiles);
        }
    }
}
