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
package org.openrewrite.groovy.tree

import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.openrewrite.Issue

class BinaryTest : GroovyTreeTest {

    @Test
    fun equals() = assertParsePrintAndProcess(
        """
            int n = 0;
            boolean b = n == 0;
        """
    )

    @Disabled
    @Test
    fun regexMatches() = assertParsePrintAndProcess(
        """
            def REGEX = /\d+/
            def text = "123"
            def result = text =~ REGEX
        """
    )

    @Issue("https://github.com/openrewrite/rewrite/issues/1520")
    @Test
    fun minusEquals() = assertParsePrintAndProcess(
        """
            def a = 5
            a -= 5
        """
    )

    @Issue("https://github.com/openrewrite/rewrite/issues/1520")
    @Test
    fun divisionEquals() = assertParsePrintAndProcess(
        """
            def a = 5
            a /= 5
        """
    )

    @Issue("https://github.com/openrewrite/rewrite/issues/1520")
    @Test
    fun bitwiseAnd() = assertParsePrintAndProcess(
        """
            def a = 4
            a &= 1
        """
    )

    @Issue("https://github.com/openrewrite/rewrite/issues/1520")
    @Test
    fun bitwiseOr() = assertParsePrintAndProcess(
        """
            def a = 4
            a |= 1
        """
    )

    @Issue("https://github.com/openrewrite/rewrite/issues/1520")
    @Test
    fun bitwiseXOr() = assertParsePrintAndProcess(
        """
            def a = 4
            a ^= 1
        """
    )
}
