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
package org.openrewrite.maven

import org.intellij.lang.annotations.Language
import org.openrewrite.Recipe
import org.openrewrite.RecipeTest
import org.openrewrite.maven.cache.InMemoryMavenPomCache
import org.openrewrite.maven.tree.Maven
import java.io.File

@Suppress("unused")
interface MavenRecipeTest : RecipeTest<Maven> {
    companion object {
        private val mavenCache = InMemoryMavenPomCache()
    }

    override val parser: MavenParser
        get() = MavenParser.builder()
            .cache(mavenCache)
            .build()

    fun assertChanged(
        parser: MavenParser = this.parser,
        recipe: Recipe = this.recipe!!,
        @Language("xml") before: String,
        @Language("xml") dependsOn: Array<String> = emptyArray(),
        @Language("xml") after: String,
        cycles: Int = 2,
        expectedCyclesThatMakeChanges: Int = cycles - 1,
        afterConditions: (Maven) -> Unit = { }
    ) {
        super.assertChangedBase(parser, recipe, before, dependsOn, after, cycles, expectedCyclesThatMakeChanges, afterConditions)
    }

    fun assertChanged(
        parser: MavenParser = this.parser,
        recipe: Recipe = this.recipe!!,
        @Language("xml") before: File,
        @Language("xml") dependsOn: Array<File> = emptyArray(),
        @Language("xml") after: String,
        cycles: Int = 2,
        expectedCyclesThatMakeChanges: Int = cycles - 1,
        afterConditions: (Maven) -> Unit = { }
    ) {
        super.assertChangedBase(parser, recipe, before, dependsOn, after, cycles, expectedCyclesThatMakeChanges, afterConditions)
    }

    fun assertUnchanged(
        parser: MavenParser = this.parser,
        recipe: Recipe = this.recipe!!,
        @Language("xml") before: String,
        @Language("xml") dependsOn: Array<String> = emptyArray()
    ) {
        super.assertUnchangedBase(parser, recipe, before, dependsOn)
    }

    fun assertUnchanged(
        parser: MavenParser = this.parser,
        recipe: Recipe = this.recipe!!,
        @Language("xml") before: File,
        @Language("xml") dependsOn: Array<File> = emptyArray()
    ) {
        super.assertUnchangedBase(parser, recipe, before, dependsOn)
    }
}
