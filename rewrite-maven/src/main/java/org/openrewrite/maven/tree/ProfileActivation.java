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
package org.openrewrite.maven.tree;

import lombok.AccessLevel;
import lombok.Data;
import lombok.experimental.FieldDefaults;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.maven.MavenDownloadingException;
import org.openrewrite.maven.internal.VersionRequirement;

import static java.util.Collections.singletonList;

@FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
@Data
public class ProfileActivation {
    @Nullable
    Boolean activeByDefault;

    @Nullable
    String jdk;

    @Nullable
    Property property;

    // TODO rename these as well?
    public boolean isActive() {
        return isActiveByJdk() || isActiveByProperty();
    }

    private boolean isActiveByJdk() {
        if (jdk == null) {
            return false;
        }

        String version = System.getProperty("java.version");
        if (version.startsWith(jdk)) {
            return true;
        }

        try {
            return version.equals(VersionRequirement.fromVersion(jdk, 0).resolve(() -> singletonList(version)));
        } catch (MavenDownloadingException e) {
            // unreachable
            return false;
        }
    }

    private boolean isActiveByProperty() {
        if (property == null) {
            return false;
        }

        String name = property.getName().trim();

        if (name.startsWith("!")) {
            return !System.getenv().containsKey(name.replace("!", ""));
        }

        if (property.getValue() == null) {
            return System.getenv().containsKey(name);
        }

        return property.getValue().equals(System.getenv(name));
    }

    @FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
    @Data
    public static class Property {
        String name;

        @Nullable
        String value;
    }
}
