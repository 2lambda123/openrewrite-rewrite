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
package org.openrewrite.gradle;

import lombok.EqualsAndHashCode;
import lombok.Value;
import org.openrewrite.*;
import org.openrewrite.gradle.marker.GradleDependencyConfiguration;
import org.openrewrite.gradle.marker.GradleProject;
import org.openrewrite.gradle.search.FindGradleProject;
import org.openrewrite.gradle.util.ChangeStringLiteral;
import org.openrewrite.gradle.util.Dependency;
import org.openrewrite.gradle.util.DependencyStringNotationConverter;
import org.openrewrite.groovy.GroovyIsoVisitor;
import org.openrewrite.groovy.GroovyVisitor;
import org.openrewrite.groovy.tree.G;
import org.openrewrite.internal.ListUtils;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.java.MethodMatcher;
import org.openrewrite.java.tree.Expression;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaSourceFile;
import org.openrewrite.java.tree.JavaType;
import org.openrewrite.marker.Markup;
import org.openrewrite.maven.MavenDownloadingException;
import org.openrewrite.maven.MavenDownloadingExceptions;
import org.openrewrite.maven.internal.MavenPomDownloader;
import org.openrewrite.maven.table.MavenMetadataFailures;
import org.openrewrite.maven.tree.*;
import org.openrewrite.semver.*;

import java.util.*;

import static java.util.Collections.emptyList;
import static java.util.Collections.emptyMap;
import static java.util.Objects.requireNonNull;

@Value
@EqualsAndHashCode(callSuper = false)
public class UpgradeDependencyVersion extends Recipe {
    private static final String VERSION_VARIABLE_KEY = "VERSION_VARIABLE";
    private static final String NEW_VERSION_KEY = "NEW_VERSION";

    @EqualsAndHashCode.Exclude
    MavenMetadataFailures metadataFailures = new MavenMetadataFailures(this);

    @Option(displayName = "Group",
            description = "The first part of a dependency coordinate `com.google.guava:guava:VERSION`. This can be a glob expression.",
            example = "com.fasterxml.jackson*")
    String groupId;

    @Option(displayName = "Artifact",
            description = "The second part of a dependency coordinate `com.google.guava:guava:VERSION`. This can be a glob expression.",
            example = "jackson-module*")
    String artifactId;

    @Option(displayName = "New version",
            description = "An exact version number or node-style semver selector used to select the version number. " +
                          "You can also use `latest.release` for the latest available version and `latest.patch` if " +
                          "the current version is a valid semantic version. For more details, you can look at the documentation " +
                          "page of [version selectors](https://docs.openrewrite.org/reference/dependency-version-selectors). " +
                          "Defaults to `latest.release`.",
            example = "29.X",
            required = false)
    @Nullable
    String newVersion;

    @Option(displayName = "Version pattern",
            description = "Allows version selection to be extended beyond the original Node Semver semantics. So for example," +
                          "Setting 'newVersion' to \"25-29\" can be paired with a metadata pattern of \"-jre\" to select Guava 29.0-jre",
            example = "-jre",
            required = false)
    @Nullable
    String versionPattern;

    @Override
    public String getDisplayName() {
        return "Upgrade Gradle dependency versions";
    }

    @Override
    public String getDescription() {
        //language=markdown
        return "Upgrade the version of a dependency in a build.gradle file. " +
                "Supports updating dependency declarations of various forms:\n" +
                "* `String` notation: `\"group:artifact:version\"` \n" +
                "* `Map` notation: `group: 'group', name: 'artifact', version: 'version'`\n" +
                "Can update version numbers which are defined earlier in the same file in variable declarations.";
    }

    @Override
    public Validated<Object> validate() {
        Validated<Object> validated = super.validate();
        if (newVersion != null) {
            validated = validated.and(Semver.validate(newVersion, versionPattern));
        }
        return validated;
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        MethodMatcher dependencyDsl = new MethodMatcher("DependencyHandlerSpec *(..)");
        DependencyMatcher dependencyMatcher = new DependencyMatcher(groupId, artifactId, null);
        return Preconditions.check(new FindGradleProject(FindGradleProject.SearchCriteria.Marker), new GroovyVisitor<ExecutionContext>() {

            @Override
            public J postVisit(J tree, ExecutionContext ctx) {
                if (tree instanceof JavaSourceFile) {
                    JavaSourceFile cu = (JavaSourceFile) tree;
                    Map<String, Map<GroupArtifact, Set<String>>>  variableNames = getCursor().getMessage(VERSION_VARIABLE_KEY);
                    if (variableNames != null) {
                        Optional<GradleProject> maybeGp = cu.getMarkers()
                                .findFirst(GradleProject.class);
                        if (!maybeGp.isPresent()) {
                            return cu;
                        }

                        cu = (JavaSourceFile) new UpdateVariable(variableNames, maybeGp.get()).visitNonNull(cu, ctx);
                    }
                    Map<GroupArtifactVersion, Set<String>> versionUpdates = getCursor().getMessage(NEW_VERSION_KEY);
                    if (versionUpdates != null) {
                        Optional<GradleProject> maybeGp = cu.getMarkers()
                                .findFirst(GradleProject.class);
                        if (!maybeGp.isPresent()) {
                            return cu;
                        }
                        GradleProject newGp = maybeGp.get();
                        for (Map.Entry<GroupArtifactVersion, Set<String>> gavToConfigurations : versionUpdates.entrySet()) {
                            newGp = replaceVersion(newGp, ctx, gavToConfigurations.getKey(), gavToConfigurations.getValue());
                        }
                        cu = cu.withMarkers(cu.getMarkers().removeByType(GradleProject.class).add(newGp));
                    }
                    return cu;
                }
                return tree;
            }

            @Override
            public J visitMethodInvocation(J.MethodInvocation method, ExecutionContext ctx) {
                J.MethodInvocation m = (J.MethodInvocation) super.visitMethodInvocation(method, ctx);
                if (dependencyDsl.matches(m)) {
                    List<Expression> depArgs = m.getArguments();
                    if (depArgs.get(0) instanceof J.Literal || depArgs.get(0) instanceof G.GString || depArgs.get(0) instanceof G.MapEntry) {
                        m = updateDependency(m, ctx);
                    } else if (depArgs.get(0) instanceof J.MethodInvocation &&
                            (((J.MethodInvocation) depArgs.get(0)).getSimpleName().equals("platform") ||
                                    ((J.MethodInvocation) depArgs.get(0)).getSimpleName().equals("enforcedPlatform"))) {
                        m = m.withArguments(ListUtils.mapFirst(depArgs, platform -> updateDependency((J.MethodInvocation) platform, ctx)));
                    }
                }
                return m;
            }

            private J.MethodInvocation updateDependency(J.MethodInvocation m, ExecutionContext ctx) {
                List<Expression> depArgs = m.getArguments();
                if (depArgs.get(0) instanceof G.GString) {
                    G.GString gString = (G.GString) depArgs.get(0);
                    List<J> strings = gString.getStrings();
                    if (strings.size() != 2 || !(strings.get(0) instanceof J.Literal) || !(strings.get(1) instanceof G.GString.Value)) {
                        return m;
                    }
                    J.Literal groupArtifact = (J.Literal) strings.get(0);
                    G.GString.Value versionValue = (G.GString.Value) strings.get(1);
                    if (!(versionValue.getTree() instanceof J.Identifier) || !(groupArtifact.getValue() instanceof String)) {
                        return m;
                    }
                    Dependency dep = DependencyStringNotationConverter.parse((String) groupArtifact.getValue());
                    if (dependencyMatcher.matches(dep.getGroupId(), dep.getArtifactId())) {
                        String versionVariableName = ((J.Identifier) versionValue.getTree()).getSimpleName();
                        getCursor().dropParentUntil(p -> p instanceof SourceFile)
                                .computeMessageIfAbsent(VERSION_VARIABLE_KEY, v -> new HashMap<String, Map<GroupArtifact, Set<String>>>())
                                .computeIfAbsent(versionVariableName, it -> new HashMap<>())
                                .computeIfAbsent(new GroupArtifact(dep.getGroupId(), dep.getArtifactId()), it -> new HashSet<>())
                                .add(m.getSimpleName());
                    }
                } else if (depArgs.get(0) instanceof J.Literal) {
                    String gav = (String) ((J.Literal) depArgs.get(0)).getValue();
                    if (gav == null) {
                        return Markup.warn(m, new IllegalStateException("Unable to update version"));
                    }
                    Dependency dep = DependencyStringNotationConverter.parse(gav);
                    if (dependencyMatcher.matches(dep.getGroupId(), dep.getArtifactId())
                            && dep.getVersion() != null
                            && !dep.getVersion().startsWith("$")) {
                        GradleProject gradleProject = getCursor().firstEnclosingOrThrow(JavaSourceFile.class)
                                .getMarkers()
                                .findFirst(GradleProject.class)
                                .orElseThrow(() -> new IllegalArgumentException("Gradle files are expected to have a GradleProject marker."));
                        String version = dep.getVersion();
                        try {
                            String newVersion = "classpath".equals(m.getSimpleName()) ?
                                    findNewerPluginVersion(dep.getGroupId(), dep.getArtifactId(), version, gradleProject, ctx) :
                                    findNewerProjectDependencyVersion(dep.getGroupId(), dep.getArtifactId(), version, gradleProject, ctx);
                            if (newVersion == null || version.equals(newVersion)) {
                                return m;
                            }
                            getCursor().dropParentUntil(p -> p instanceof SourceFile)
                                    .computeMessageIfAbsent(NEW_VERSION_KEY, it -> new HashMap<GroupArtifactVersion, Set<String>>())
                                    .computeIfAbsent(new GroupArtifactVersion(dep.getGroupId(), dep.getArtifactId(), newVersion), it -> new HashSet<>())
                                    .add(m.getSimpleName());

                            return m.withArguments(ListUtils.mapFirst(m.getArguments(), arg -> {
                                J.Literal literal = (J.Literal) arg;
                                String newGav = dep
                                        .withVersion(newVersion)
                                        .toStringNotation();
                                return literal
                                        .withValue(newGav)
                                        .withValueSource(requireNonNull(literal.getValueSource()).replace(gav, newGav));
                            }));
                        } catch (MavenDownloadingException e) {
                            return e.warn(m);
                        }
                    }
                } else if (depArgs.size() >= 3 && depArgs.get(0) instanceof G.MapEntry
                        && depArgs.get(1) instanceof G.MapEntry
                        && depArgs.get(2) instanceof G.MapEntry) {
                    Expression groupValue = ((G.MapEntry) depArgs.get(0)).getValue();
                    Expression artifactValue = ((G.MapEntry) depArgs.get(1)).getValue();
                    if (!(groupValue instanceof J.Literal) || !(artifactValue instanceof J.Literal)) {
                        return m;
                    }
                    J.Literal groupLiteral = (J.Literal) groupValue;
                    J.Literal artifactLiteral = (J.Literal) artifactValue;
                    //noinspection DataFlowIssue
                    if (!dependencyMatcher.matches((String) groupLiteral.getValue(), (String) artifactLiteral.getValue())) {
                        return m;
                    }
                    G.MapEntry versionEntry = (G.MapEntry) depArgs.get(2);
                    Expression versionExp = versionEntry.getValue();
                    if (versionExp instanceof J.Literal && ((J.Literal) versionExp).getValue() instanceof String) {
                        GradleProject gradleProject = getCursor().firstEnclosingOrThrow(JavaSourceFile.class)
                                .getMarkers()
                                .findFirst(GradleProject.class)
                                .orElseThrow(() -> new IllegalArgumentException("Gradle files are expected to have a GradleProject marker."));
                        J.Literal versionLiteral = (J.Literal) versionExp;
                        String version = (String) versionLiteral.getValue();
                        if (version.startsWith("$")) {
                            return m;
                        }
                        String newVersion;
                        try {
                            newVersion = "classpath".equals(m.getSimpleName()) ?
                                    findNewerPluginVersion((String) groupLiteral.getValue(), (String) artifactLiteral.getValue(), version, gradleProject, ctx) :
                                    findNewerProjectDependencyVersion((String) groupLiteral.getValue(), (String) artifactLiteral.getValue(), version, gradleProject, ctx);
                        } catch (MavenDownloadingException e) {
                            return e.warn(m);
                        }
                        if (newVersion == null || version.equals(newVersion)) {
                            return m;
                        }
                        List<Expression> newArgs = new ArrayList<>(3);
                        newArgs.add(depArgs.get(0));
                        newArgs.add(depArgs.get(1));
                        newArgs.add(versionEntry.withValue(
                                versionLiteral
                                        .withValueSource(requireNonNull(versionLiteral.getValueSource()).replace(version, newVersion))
                                        .withValue(newVersion)));
                        newArgs.addAll(depArgs.subList(3, depArgs.size()));

                        return m.withArguments(newArgs);
                    } else if (versionExp instanceof J.Identifier) {
                        String versionVariableName = ((J.Identifier) versionExp).getSimpleName();
                        getCursor().dropParentUntil(p -> p instanceof SourceFile)
                                .computeMessageIfAbsent(VERSION_VARIABLE_KEY, v -> new HashMap<String, Map<GroupArtifact, Set<String>>>())
                                .computeIfAbsent(versionVariableName, it -> new HashMap<>())
                                .computeIfAbsent(new GroupArtifact((String) groupLiteral.getValue(), (String) artifactLiteral.getValue()), it -> new HashSet<>())
                                .add(m.getSimpleName());
                    }
                }

                return m;
            }
        });
    }

    @Value
    @EqualsAndHashCode(callSuper = true)
    private class UpdateVariable extends GroovyIsoVisitor<ExecutionContext> {
        Map<String, Map<GroupArtifact, Set<String>>> versionVariableNames;
        GradleProject gradleProject;

        @Override
        public J.VariableDeclarations.NamedVariable visitVariable(J.VariableDeclarations.NamedVariable variable, ExecutionContext ctx) {
            J.VariableDeclarations.NamedVariable v = super.visitVariable(variable, ctx);
            boolean noneMatch = true;
            Map<GroupArtifact, Set<String>> gaToConfigurations = null;
            for (Map.Entry<String, Map<GroupArtifact, Set<String>>> versionVariableNameEntry : versionVariableNames.entrySet()) {
                if (versionVariableNameEntry.getKey().equals((v.getSimpleName()))) {
                    noneMatch = false;
                    gaToConfigurations = versionVariableNameEntry.getValue();
                    break;
                }
            }
            if (noneMatch) {
                return v;
            }
            if (!(v.getInitializer() instanceof J.Literal)) {
                return v;
            }
            J.Literal initializer = (J.Literal) v.getInitializer();
            if (initializer.getType() != JavaType.Primitive.String) {
                return v;
            }
            String version = (String) initializer.getValue();
            if (version == null) {
                return v;
            }

            try {
                for (Map.Entry<GroupArtifact, Set<String>> gaEntry : gaToConfigurations.entrySet()) {
                    GroupArtifact ga = gaEntry.getKey();
                    String newVersion = findNewerProjectDependencyVersion(ga.getGroupId(), ga.getArtifactId(), version, gradleProject, ctx);
                    if (newVersion == null) {
                        newVersion = findNewerPluginVersion(ga.getGroupId(), ga.getArtifactId(), version, gradleProject, ctx);
                    }
                    if (newVersion == null) {
                        return v;
                    }
                    getCursor().dropParentUntil(p -> p instanceof SourceFile)
                            .computeMessageIfAbsent(NEW_VERSION_KEY, m -> new HashMap<GroupArtifactVersion, Set<String>>())
                            .computeIfAbsent(new GroupArtifactVersion(ga.getGroupId(), ga.getArtifactId(), newVersion), it -> new HashSet<>())
                            .addAll(gaEntry.getValue());

                    J.Literal newVersionLiteral = ChangeStringLiteral.withStringValue(initializer, newVersion);
                    v = v.withInitializer(newVersionLiteral);
                }
            } catch (MavenDownloadingException e) {
                return e.warn(v);
            }
            return v;
        }

        @Override
        public J.Assignment visitAssignment(J.Assignment assignment, ExecutionContext ctx) {
            J.Assignment a = super.visitAssignment(assignment, ctx);
            if (!(a.getVariable() instanceof J.Identifier)) {
                return a;
            }
            J.Identifier identifier = (J.Identifier) a.getVariable();
            Map<GroupArtifact, Set<String>> gaToConfigurations = null;
            boolean noneMatch = true;
            for (Map.Entry<String, Map<GroupArtifact, Set<String>>> versionVariableNameEntry : versionVariableNames.entrySet()) {
                if (versionVariableNameEntry.getKey().equals(identifier.getSimpleName())) {
                    noneMatch = false;
                    gaToConfigurations = versionVariableNameEntry.getValue();
                    break;
                }
            }
            if (noneMatch) {
                return a;
            }
            if (!(a.getAssignment() instanceof J.Literal)) {
                return a;
            }
            J.Literal literal = (J.Literal) a.getAssignment();
            if (literal.getType() != JavaType.Primitive.String) {
                return a;
            }
            String version = (String) literal.getValue();
            if (version == null) {
                return a;
            }

            try {
                for (Map.Entry<GroupArtifact, Set<String>> gaEntry : gaToConfigurations.entrySet()) {
                    GroupArtifact ga = gaEntry.getKey();
                    String newVersion = findNewerProjectDependencyVersion(ga.getGroupId(), ga.getArtifactId(), version, gradleProject, ctx);
                    if (newVersion == null) {
                        newVersion = findNewerPluginVersion(ga.getGroupId(), ga.getArtifactId(), version, gradleProject, ctx);
                    }
                    if (newVersion == null) {
                        return a;
                    }
                    getCursor().dropParentUntil(p -> p instanceof SourceFile)
                            .computeMessageIfAbsent(NEW_VERSION_KEY, m -> new HashMap<GroupArtifactVersion, Set<String>>())
                            .computeIfAbsent(new GroupArtifactVersion(ga.getGroupId(), ga.getArtifactId(), newVersion), it -> new HashSet<>())
                            .addAll(gaEntry.getValue());

                    J.Literal newVersionLiteral = ChangeStringLiteral.withStringValue(literal, newVersion);
                    a = a.withAssignment(newVersionLiteral);
                }
            } catch (MavenDownloadingException e) {
                return e.warn(a);
            }
            return a;
        }
    }

    @Nullable
    private String findNewerPluginVersion(String groupId, String artifactId, String version, GradleProject gradleProject,
                                          ExecutionContext ctx) throws MavenDownloadingException {
        return AddDependencyVisitor.resolveDependencyVersion(groupId, artifactId, version, newVersion, versionPattern, gradleProject.getMavenPluginRepositories(), metadataFailures, ctx)
                .orElse(null);
    }

    @Nullable
    private String findNewerProjectDependencyVersion(String groupId, String artifactId, String version, GradleProject gradleProject,
                                                     ExecutionContext ctx) throws MavenDownloadingException {
        return AddDependencyVisitor.resolveDependencyVersion(groupId, artifactId, version, newVersion, versionPattern, gradleProject.getMavenRepositories(), metadataFailures, ctx)
                .orElse(null);
    }

    static GradleProject replaceVersion(GradleProject gp, ExecutionContext ctx, GroupArtifactVersion gav, Set<String> configurations) {
        try {
            if (gav.getGroupId() == null || gav.getArtifactId() == null) {
                return gp;
            }

            Set<String> remainingConfigurations = new HashSet<>(configurations);
            remainingConfigurations.remove("classpath");

            if (remainingConfigurations.isEmpty()) {
                return gp;
            }

            MavenPomDownloader mpd = new MavenPomDownloader(emptyMap(), ctx, null, null);
            Pom pom = mpd.download(gav, null, null, gp.getMavenRepositories());
            ResolvedPom resolvedPom = pom.resolve(emptyList(), mpd, gp.getMavenRepositories(), ctx);
            ResolvedGroupArtifactVersion resolvedGav = resolvedPom.getGav();
            List<ResolvedDependency> transitiveDependencies = resolvedPom.resolveDependencies(Scope.Runtime, mpd, ctx);
            Map<String, GradleDependencyConfiguration> nameToConfiguration = gp.getNameToConfiguration();
            Map<String, GradleDependencyConfiguration> newNameToConfiguration = new HashMap<>(nameToConfiguration.size());
            boolean anyChanged = false;
            for (GradleDependencyConfiguration gdc : nameToConfiguration.values()) {
                GradleDependencyConfiguration newGdc = gdc;
                newGdc = newGdc.withRequested(ListUtils.map(gdc.getRequested(), requested -> {
                    if (!Objects.equals(requested.getGroupId(), gav.getGroupId()) || !Objects.equals(requested.getArtifactId(), gav.getArtifactId())) {
                        return requested;
                    }
                    return requested.withGav(gav);
                }));
                newGdc = newGdc.withResolved(ListUtils.map(gdc.getResolved(), resolved -> {
                    if (!Objects.equals(resolved.getGroupId(), resolvedGav.getGroupId()) || !Objects.equals(resolved.getArtifactId(), resolvedGav.getArtifactId())) {
                        return resolved;
                    }
                    return resolved.withGav(resolvedGav)
                            .withDependencies(transitiveDependencies);
                }));
                anyChanged |= newGdc != gdc;
                newNameToConfiguration.put(newGdc.getName(), newGdc);
            }
            if (anyChanged) {
                gp = gp.withNameToConfiguration(newNameToConfiguration);
            }
        } catch (MavenDownloadingException | MavenDownloadingExceptions e) {
            return gp;
        }
        return gp;
    }
}
