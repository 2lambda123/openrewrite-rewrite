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
package org.openrewrite.maven.utilities;

import lombok.Value;
import org.openrewrite.ExecutionContext;
import org.openrewrite.maven.cache.InMemoryMavenPomCache;
import org.openrewrite.maven.cache.LocalMavenArtifactCache;
import org.openrewrite.maven.cache.ReadOnlyLocalMavenArtifactCache;
import org.openrewrite.maven.internal.MavenPomDownloader;
import org.openrewrite.maven.internal.RawMaven;
import org.openrewrite.maven.internal.RawMavenResolver;
import org.openrewrite.maven.tree.*;
import org.openrewrite.xml.tree.Xml;

import java.net.URI;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.function.Predicate;

public class MavenArtifactHelper {

    private static final MavenRepository SUPER_POM_REPOSITORY = new MavenRepository("central",
            URI.create("https://repo.maven.apache.org/maven2"), true, false, null, null);

    public static List<Path> downloadArtifactAndDependencies(String groupId, String artifactId, String version, ExecutionContext ctx) {
        return downloadArtifactAndDependencies(groupId, artifactId, version, ctx, SUPER_POM_REPOSITORY, true);
    }

    public static List<Path> downloadArtifactAndDependencies(String groupId, String artifactId, String version, ExecutionContext ctx, MavenRepository repository) {
        return downloadArtifactAndDependencies(groupId, artifactId, version, ctx, repository, true);
    }

    public static List<Path> downloadArtifactAndDependencies(String groupId, String artifactId, String version, ExecutionContext ctx, MavenRepository repository, boolean normalizeRepositories) {
        MavenPomDownloader mavenPomDownloader = new MavenPomDownloader(new InMemoryMavenPomCache(),
                Collections.emptyMap(), ctx, normalizeRepositories);
        List<MavenRepository> repositories = new ArrayList<>();
        repositories.add(repository);
        RawMaven rawMaven = mavenPomDownloader.download(groupId, artifactId, version, null, null,
                repositories, ctx);
        if (rawMaven == null) {
            return Collections.emptyList();
        }
        Xml.Document xml = new RawMavenResolver(mavenPomDownloader, Collections.emptyList(), true, ctx, null).resolve(rawMaven, new HashMap<>());
        if (xml == null) {
            return Collections.emptyList();
        }
        Maven maven = new Maven(xml);
        MavenArtifactDownloader mavenArtifactDownloader = new MavenArtifactDownloader(ReadOnlyLocalMavenArtifactCache.MAVEN_LOCAL.orElse(
                new LocalMavenArtifactCache(Paths.get(System.getProperty("user.home"), ".rewrite-cache", "artifacts"))
        ), null, ctx.getOnError());
        List<Path> artifactPaths = new ArrayList<>();
        artifactPaths.add(mavenArtifactDownloader.downloadArtifact(new Pom.Dependency(repository, Scope.Compile, null, null, false, new Pom(
                groupId,
                artifactId,
                version,
                null,
                null,
                null,
                null,
                null,
                null,
                Collections.emptyList(),
                new Pom.DependencyManagement(Collections.emptyList()),
                Collections.emptyList(),
                repositories,
                Collections.emptyMap(),
                Collections.emptyMap()
        ), null, null, Collections.emptySet())));
        for (Pom.Dependency dependency : collectTransitiveDependencies(maven.getModel().getDependencies(),
                d -> !d.isOptional() && d.getScope() != Scope.Test)) {
            artifactPaths.add(mavenArtifactDownloader.downloadArtifact(dependency));
        }
        return artifactPaths;
    }

    public static List<Pom.Dependency> collectTransitiveDependencies(Collection<Pom.Dependency> dependencies, Predicate<Pom.Dependency> dependencyFilter) {
        return new ArrayList<>(traverseDependencies(dependencies, new LinkedHashMap<>(), dependencyFilter).values());
    }

    private static Map<DependencyKey, Pom.Dependency> traverseDependencies(
            Collection<Pom.Dependency> dependencies,
            final Map<DependencyKey, Pom.Dependency> dependencyMap,
            Predicate<Pom.Dependency> dependencyFilter) {
        if (dependencies == null) {
            return dependencyMap;
        }
        dependencies.stream()
                .filter(dependencyFilter)
                .forEach(d -> {
                    DependencyKey key = getDependencyKey(d);
                    if (!dependencyMap.containsKey(key)) {
                        dependencyMap.put(key, d);
                        traverseDependencies(d.getModel().getDependencies(), dependencyMap, dependencyFilter);
                    }
                });
        return dependencyMap;
    }

    private static DependencyKey getDependencyKey(Pom.Dependency dependency) {
        return new DependencyKey(dependency.getGroupId(), dependency.getArtifactId(), dependency.getVersion(),
                dependency.getExclusions());
    }

    @Value
    static class DependencyKey {
        String groupId;
        String artifactId;
        String version;
        Set<GroupArtifact> exclusions;
    }
}
