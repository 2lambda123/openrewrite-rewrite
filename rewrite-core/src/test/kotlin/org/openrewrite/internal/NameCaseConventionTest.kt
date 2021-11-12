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
package org.openrewrite.internal

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource

@Suppress("SpellCheckingInspection")
class NameCaseConventionTest {

    @ParameterizedTest
    @CsvSource(
        value = [
            "foo.config-client.enabled:foo.config-client.enabled",
            "com.fooBar.FooBar:com.foo-bar.foo-bar",
            "foo_bar.bar:foo-bar.bar",
            "FooBar:foo-bar",
            "com.bar.FooBar:com.bar.foo-bar",
            "Foo:foo",
            "FooBBar:foo-bbar",
            "fooBBar:foo-bbar",
            "fooBar:foo-bar",
            "foo bar:foo-bar",
        ], delimiter = ':'
    )
    fun lowerHyphen(input: String, expected: String) {
        assertThat(NameCaseConvention.LOWER_HYPHEN.format(input)).isEqualTo(expected)
    }

    @ParameterizedTest
    @CsvSource(
        value = [
            "Foo:foo",
            "Foo-Bar:foo_bar",
            "FOO.FOO-BAR:foo.foo_bar",
            "foo bar:foo_bar",
        ], delimiter = ':'
    )
    fun lowerUnderscore(input: String, expected: String) {
        assertThat(NameCaseConvention.LOWER_UNDERSCORE.format(input)).isEqualTo(expected)
    }

    @ParameterizedTest
    @CsvSource(
        value = [
            "rename_one:renameOne",
            "RenameTwo:renameTwo",
            "__rename__three__:renameThree",
            "_Rename__Four:renameFour",
            "foo.config-client.enabled:foo.configClient.enabled",
            "foo-bar:fooBar",
        ], delimiter = ':'
    )
    fun lowerCamel(input: String, expected: String) {
        assertThat(NameCaseConvention.LOWER_CAMEL.format(input)).isEqualTo(expected)
    }

    @ParameterizedTest
    @CsvSource(
        value = [
            "rename_one:RenameOne",
            "RenameTwo:RenameTwo",
            "__rename__three__:RenameThree",
            "_Rename__Four:RenameFour",
            "foo-bar:FooBar",
        ], delimiter = ':'
    )
    fun upperCamel(input: String, expected: String) {
        assertThat(NameCaseConvention.UPPER_CAMEL.format(input)).isEqualTo(expected)
    }

    @ParameterizedTest
    @CsvSource(
        value = [
            "foo:FOO",
            "foo-bar:FOO_BAR",
            "foo_bar:FOO_BAR",
            "FooBar:FOO_BAR",
            "Foo.fooBar:FOO.FOO_BAR",
            "foo bar:FOO_BAR"
        ], delimiter = ':'
    )
    fun upperUnderscore(input: String, expected: String) {
        assertThat(NameCaseConvention.UPPER_UNDERSCORE.format(input)).isEqualTo(expected)
    }

    @ParameterizedTest
    @CsvSource(
        value = [
            "foo.fooBar:foo.foo-bar",
            "foo.foo-bar:foo.fooBar",
        ], delimiter = ':'
    )
    fun equalsRelaxedBinding(input: String, expected: String) {
        assertThat(NameCaseConvention.equalsRelaxedBinding(input, expected)).isTrue
    }

}
