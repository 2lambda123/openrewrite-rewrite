/*
 * Copyright 2021 the original author or authors.
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
package org.openrewrite.gradle

import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource

class ChangeDependencyGroupIdAndArtifactIdTest : GradleRecipeTest {
    @ParameterizedTest
    @ValueSource(strings = ["org.openrewrite:rewrite-core", "*:*"])
    fun findDependency(ga: String) = assertChanged(
        recipe = ChangeDependencyGroupIdAndArtifactId(
            ga.substringBefore(':'),
            ga.substringAfter(':'),
            "org.dewrite",
            "dewrite-core",
            null
        ),
        before = """
            dependencies {
                api 'org.openrewrite:rewrite-core:latest.release'
                api "org.openrewrite:rewrite-core:latest.release"
            }
        """,
        after = """
            dependencies {
                api 'org.dewrite:dewrite-core:latest.release'
                api "org.dewrite:dewrite-core:latest.release"
            }
        """
    )

    @ParameterizedTest
    @ValueSource(strings = ["org.openrewrite:rewrite-core", "*:*"])
    fun findMapStyleDependency(ga: String) = assertChanged(
        recipe = ChangeDependencyGroupIdAndArtifactId(
            ga.substringBefore(':'),
            ga.substringAfter(':'),
            "org.dewrite",
            "dewrite-core",
            null
        ),
        before = """
            dependencies {
                api group: 'org.openrewrite', name: 'rewrite-core', version: 'latest.release'
                api group: "org.openrewrite", name: "rewrite-core", version: "latest.release"
            }
        """,
        after = """
            dependencies {
                api group: 'org.dewrite', name: 'dewrite-core', version: 'latest.release'
                api group: "org.dewrite", name: "dewrite-core", version: "latest.release"
            }
        """
    )
}