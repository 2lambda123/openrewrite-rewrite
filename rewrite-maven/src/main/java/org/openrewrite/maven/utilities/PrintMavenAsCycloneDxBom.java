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

import org.openrewrite.ExecutionContext;
import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.maven.MavenParser;
import org.openrewrite.maven.cache.MapdbMavenPomCache;
import org.openrewrite.maven.cache.MavenPomCache;
import org.openrewrite.maven.tree.Maven;
import org.openrewrite.maven.tree.Pom;
import org.openrewrite.maven.tree.Scope;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.*;

/**
 * Print the dependency graph in the CycloneDX (https://cyclonedx.org/) bill of materials (BOM) format.
 */
public final class PrintMavenAsCycloneDxBom {

    private PrintMavenAsCycloneDxBom() {
    }

    public static String print(Maven maven) {
        Pom pom = maven.getModel();
        StringBuilder bom = new StringBuilder("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        bom.append("<bom xmlns=\"http://cyclonedx.org/schema/bom/1.2\" serialNumber=\"urn:uuid:")
                .append(maven.getId().toString()).append("\" version=\"1\">\n");
        writeMetadata(maven.getModel(), bom);

        //Collect all transitive dependencies, The returned map is a bomReference -> dependency. This method will add
        //a dependency the first time it is encountered. A linked hash map is used to maintain the order in which
        //the dependencies are inserted into the map.
        Map <String, Pom.Dependency> dependencies = traverseDependencies(pom.getDependencies(), new LinkedHashMap<>());
        writeComponents(dependencies, bom);
        writeDependencies(dependencies, bom);

        bom.append("</bom>\n");

        return bom.toString();
    }

    private static void writeMetadata(Pom pom, StringBuilder bom) {
        bom.append("    <metadata>\n");
        bom.append("        <timestamp>").append(Instant.now().toString()).append("</timestamp>\n");
        bom.append("        <tools>\n");
        bom.append("            <tool>\n");
        bom.append("                <vendor>OpenRewrite</vendor>\n");
        bom.append("                <name>OpenRewrite CycloneDX</name>\n");
        //Probably should pull the version from build properties.
        bom.append("                <version>7.0.0</version>\n");
        bom.append("            </tool>\n");
        bom.append("        </tools>\n");
        String bomReference = getBomReference(pom.getGroupId(), pom.getArtifactId(), pom.getVersion());

        writeComponent(pom, bomReference, pom.getVersion(), Scope.Compile, "        ",  bom);
        bom.append("    </metadata>\n");
    }
    private static void writeComponents(Map <String, Pom.Dependency> dependencyMap, StringBuilder bom) {
        if (dependencyMap.isEmpty()) {
            return;
        }
        bom.append("    <components>\n");
        for (Map.Entry<String, Pom.Dependency> entry : dependencyMap.entrySet()) {
            writeComponent(entry.getValue().getModel(), entry.getKey(), entry.getValue().getVersion(), entry.getValue().getScope(), "        ", bom);
        }
        bom.append("    </components>\n");
    }

    private static void writeDependencies(Map <String, Pom.Dependency> dependencyMap, StringBuilder bom) {
        if (dependencyMap.isEmpty()) {
            return;
        }
        bom.append("    <dependencies>\n");
        for (Map.Entry<String, Pom.Dependency> entry : dependencyMap.entrySet()) {
            writeDependency(entry.getKey(), entry.getValue(), bom);
        }
        bom.append("    </dependencies>\n");
    }

    private static void writeDependency(String bomReference, Pom.Dependency dependency, StringBuilder bom) {
        bom.append("        <dependency ref=\"").append(bomReference).append("\">\n");
        if (dependency.getModel().getDependencies() != null) {
            for (Pom.Dependency nested : dependency.getModel().getDependencies()) {
                bom.append("            <dependency ref=\"")
                        .append(getBomReference(
                                nested.getGroupId(),
                                nested.getArtifactId(),
                                nested.getVersion())
                        ).append("\"/>\n");
            }
        }
        bom.append("        </dependency>\n");
    }

    private static void writeComponent(Pom pom, String bomReference, String version, Scope scope, String indent, StringBuilder bom) {
        String type = "library";
        if (pom.getPackaging().equals("war") || pom.getPackaging().equals("ear")) {
            type = "application";
        }
        bom.append(indent).append("<component bom-ref=\"").append(bomReference).append("\" type=\"library\">\n");
        bom.append(indent).append("    <group>").append(pom.getGroupId()).append("</group>\n");
        bom.append(indent).append("    <name>").append(pom.getArtifactId()).append("</name>\n");
        bom.append(indent).append("    <version>").append(version).append("</version>\n");
        if (pom.getDescription() != null) {
            bom.append(indent).append("    <description>\n");
            bom.append(indent).append("        <![CDATA[").append(pom.getDescription()).append("]]>\n");
            bom.append(indent).append("    </description>\n");
        }
        if (scope != null) {
            //Cyclone schema allows three scopes:
            String cycloneScope;
            switch (scope) {
                case Compile:
                case System:
                    cycloneScope = "required";
                    break;
                case None:
                case Invalid:
                case Test:
                    cycloneScope = "excluded";
                    break;
                default:
                    cycloneScope = "optional";
            }
            bom.append(indent).append("    <scope>").append(cycloneScope).append("</scope>\n");
        }
        writeLicenses(pom, bom, indent);
        bom.append(indent).append("    <purl>").append(bomReference).append("</purl>\n");
        bom.append(indent).append("</component>\n");
    }

    private static void writeLicenses(Pom pom, StringBuilder bom, String indent) {
        if (!pom.getLicenses().isEmpty()) {
            bom.append(indent).append("    <licenses>\n");

            for (Pom.License license : pom.getLicenses()) {
                bom.append(indent).append("        <license>\n");
                String spdxId = null;

                //This logic maps the rewrite license type to the spdx equivalent.

                //The only license type that we can establish unambiguously is the Apache 2.0 license.

                //BSD has several SPDX Mappings (no way to resolve this)
                //CDDL has a v1.0 and v1.1 (we do not distinguish them)
                //CreativeCommons has several SPDX Mappings (no way to resolve this)
                //Eclipse has a v1.0 and v2.0 (we do not distinguish them)
                //GPL has several SPDX Mappings (no way to resolve this)
                //LGPL has several SPDX Mappings (no way to resolve this)
                //MIT has several SPDX Mappings (no way to resolve this)
                //Mozilla has several SPDX Mappings (no way to resolve this)
                //PublicDomain unclear which ID to use.

                if (license.getType() == Pom.LicenseType.Apache2) {
                    spdxId = "Apache-2.0";
                }
                if (spdxId != null) {
                    bom.append(indent).append("            <id>").append(spdxId).append("</id>\n");
                }
                bom.append(indent).append("            <name>").append(license.getName()).append("</name>\n");
                bom.append(indent).append("        </license>\n");
            }
            bom.append(indent).append("      </licenses>\n");
        }
    }

    private static String getBomReference(String group, String artifactId, String version) {
        return "pkg:maven/" + group + "/" + artifactId + "@" + version + "?type=jar";
    }

    private static Map<String, Pom.Dependency> traverseDependencies(Collection<Pom.Dependency> dependencies, final Map<String, Pom.Dependency> dependencyMap) {
        if (dependencies == null) {
            return dependencyMap;
        }
        dependencies.stream()
                .filter(PrintMavenAsCycloneDxBom::isDependencyInScope)
                .forEach(d -> {
                    String bomReference = getBomReference(d.getGroupId(), d.getArtifactId(), d.getVersion());
                    if (!dependencyMap.containsKey(bomReference)) {
                        dependencyMap.put(bomReference, d);
                        traverseDependencies(d.getModel().getDependencies() , dependencyMap);
                    }
                });
        return dependencyMap;
    }

    private static boolean isDependencyInScope(Pom.Dependency dependency) {
        boolean check = !dependency.isOptional() && dependency.getScope() != Scope.Test;
        if (check) {
            System.out.println(dependency.getScope() + " - " + dependency.getGroupId() + "/" + dependency.getArtifactId());
        }
        return check;
    }
}
