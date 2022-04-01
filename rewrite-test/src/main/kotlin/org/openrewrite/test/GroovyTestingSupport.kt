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
package org.openrewrite.test

import org.intellij.lang.annotations.Language
import org.openrewrite.ExecutionContext
import org.openrewrite.Recipe
import org.openrewrite.SourceFile
import org.openrewrite.groovy.GroovyParser
import org.openrewrite.groovy.tree.G
import org.openrewrite.java.JavaParser
import org.openrewrite.java.tree.J
import org.openrewrite.marker.Marker
import org.openrewrite.xml.tree.Xml

interface GroovyTestingSupport : RecipeTestingSupport {

    val groovyParser: GroovyParser
        get() = GroovyParser.builder().build()

    fun GroovyParser.parse(
        @Language("groovy") source: String,
        markers : List<Marker> = emptyList(),
        ctx: ExecutionContext = executionContext
    ): G.CompilationUnit {
        return parse(ctx, source.trimIndent()).map { j -> j.addMarkers(markers) }.single()
    }

    fun GroovyParser.parse(
        @Language("groovy") vararg sources: String,
        markers : List<Marker> = emptyList(),
        ctx: ExecutionContext = executionContext
    ): List<G.CompilationUnit> {
        return parse(ctx, *sources.map { it.trimIndent() }.toTypedArray()).map { j -> j.addMarkers(markers) }
    }

    fun assertChanged(
        before: Xml.Document,
        @Language("groovy") after: String,
        additionalSources: List<SourceFile>,
        recipe: Recipe? = this.recipe,
        ctx: ExecutionContext = this.executionContext,
        cycles: Int = 2,
        expectedCyclesThatMakeChanges: Int = cycles - 1,
        afterConditions: (Xml.Document) -> Unit = { },
    ) {
        assertChangedBase(before, after, additionalSources, recipe, executionContext, cycles, expectedCyclesThatMakeChanges, afterConditions)
    }

}