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
package org.openrewrite.maven.internal;

import org.openrewrite.maven.tree.Scope;
import org.openrewrite.xml.tree.Content;
import org.openrewrite.xml.tree.Xml;

import java.util.*;

public class InsertDependencyComparator implements Comparator<Content> {
    private final Map<Content, Float> positions = new HashMap<>();

    public InsertDependencyComparator(List<? extends Content> existingDependencies, Xml.Tag dependencyTag) {
        for (int i = 0, existingDependenciesSize = existingDependencies.size(); i < existingDependenciesSize; i++) {
            positions.put(existingDependencies.get(i), (float) i);
        }

        // if everything were ideally sorted, which dependency would the addable dependency
        // come after?
        List<Content> ideallySortedDependencies = new ArrayList<>(existingDependencies);
        ideallySortedDependencies.add(dependencyTag);
        ideallySortedDependencies.sort(dependencyComparator);

        Content afterDependency = null;
        for (int i = 0; i < ideallySortedDependencies.size(); i++) {
            Content d = ideallySortedDependencies.get(i);
            if (dependencyTag == d) {
                if (i > 0) {
                    afterDependency = ideallySortedDependencies.get(i - 1);
                }
                break;
            }
        }

        positions.put(dependencyTag, afterDependency == null ? -0.5f :
                positions.get(afterDependency) + 0.5f);
    }

    @Override
    public int compare(Content o1, Content o2) {
        return positions.get(o1).compareTo(positions.get(o2));
    }

    private static final Comparator<Content> dependencyComparator = (d1, d2) -> {
        if (!(d1 instanceof Xml.Tag) || !(d2 instanceof Xml.Tag)) {
            return 1;
        }

        Xml.Tag t1 = (Xml.Tag) d1;
        Xml.Tag t2 = (Xml.Tag) d2;
        Scope scope1 = Scope.fromName(t1.getChildValue("scope").orElse(null));
        Scope scope2 = Scope.fromName(t2.getChildValue("scope").orElse(null));
        if (!scope1.equals(scope2)) {
            return scope1.compareTo(scope2);
        }

        String groupId1 = t1.getChildValue("groupId").orElse("");
        String groupId2 = t2.getChildValue("groupId").orElse("");
        if (!groupId1.equals(groupId2)) {
            return comparePartByPart(groupId1, groupId2);
        }

        String artifactId1 = t1.getChildValue("artifactId").orElse("");
        String artifactId2 = t2.getChildValue("artifactId").orElse("");
        if (!artifactId1.equals(artifactId2)) {
            return comparePartByPart(artifactId1, artifactId2);
        }

        String classifier1 = t1.getChildValue("classifier").orElse(null);
        String classifier2 = t2.getChildValue("classifier").orElse(null);

        if (classifier1 == null && classifier2 != null) {
            return -1;
        } else if (classifier1 != null) {
            if (classifier2 == null) {
                return 1;
            }
            if (!classifier1.equals(classifier2)) {
                return classifier1.compareTo(classifier2);
            }
        }

        // in every case imagined so far, group and artifact comparison are enough,
        // so this is just for completeness
        return t1.getChildValue("version").orElse("")
                .compareTo(t2.getChildValue("version").orElse(""));
    };

    private static int comparePartByPart(String d1, String d2) {
        String[] d1Parts = d1.split("[.-]");
        String[] d2Parts = d2.split("[.-]");

        for (int i = 0; i < Math.min(d1Parts.length, d2Parts.length); i++) {
            if (!d1Parts[i].equals(d2Parts[i])) {
                return d1Parts[i].compareTo(d2Parts[i]);
            }
        }

        return d1Parts.length - d2Parts.length;
    }
}
