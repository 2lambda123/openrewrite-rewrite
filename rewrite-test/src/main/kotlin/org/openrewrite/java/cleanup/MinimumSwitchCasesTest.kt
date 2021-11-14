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
@file:Suppress("DuplicateBranchesInSwitch", "IfStatementWithIdenticalBranches")

package org.openrewrite.java.cleanup

import org.junit.jupiter.api.Test
import org.openrewrite.Issue
import org.openrewrite.Recipe
import org.openrewrite.java.JavaParser
import org.openrewrite.java.JavaRecipeTest

@Suppress("SwitchStatementWithTooFewBranches", "ConstantConditions")
interface MinimumSwitchCasesTest : JavaRecipeTest {
    override val recipe: Recipe
        get() = MinimumSwitchCases()

    @Issue("https://github.com/openrewrite/rewrite/issues/800")
    @Test
    fun primitiveAndDefault(jp: JavaParser) = assertChanged(
        jp,
        before = """
            class Test {
                int variable;
                void test() {
                    switch (variable) {
                      case 0:
                          doSomething();
                          break;
                      default:
                          doSomethingElse();
                          break;
                    }
                }
            }
        """,
        after = """
            class Test {
                int variable;
                void test() {
                    if (variable == 0) {
                        doSomething();
                    } else {
                        doSomethingElse();
                    }
                }
            }
        """,
        typeValidation = {
            methodInvocations = false
        }
    )

    @Issue("https://github.com/openrewrite/rewrite/issues/800")
    @Test
    fun twoPrimitives(jp: JavaParser) = assertChanged(
        jp,
        before = """
            class Test {
                int variable;
                void test() {
                    switch (variable) {
                      case 0:
                          doSomething();
                          break;
                      case 1:
                          doSomethingElse();
                          break;
                    }
                }
            }
        """,
        after = """
            class Test {
                int variable;
                void test() {
                    if (variable == 0) {
                        doSomething();
                    } else if (variable == 1) {
                        doSomethingElse();
                    }
                }
            }
        """,
        typeValidation = {
            methodInvocations = false
        }
    )

    @Issue("https://github.com/openrewrite/rewrite/issues/800")
    @Test
    fun stringAndDefault(jp: JavaParser) = assertChanged(
        jp,
        before = """
            class Test {
                String name;
                void test() {
                    switch (name) {
                      case "jonathan":
                          doSomething();
                          break;
                      default:
                          doSomethingElse();
                          break;
                    }
                }
            }
        """,
        after = """
            class Test {
                String name;
                void test() {
                    if ("jonathan".equals(name)) {
                        doSomething();
                    } else {
                        doSomethingElse();
                    }
                }
            }
        """,
        typeValidation = {
            methodInvocations = false
        }
    )

    @Issue("https://github.com/openrewrite/rewrite/issues/800")
    @Test
    fun twoStrings(jp: JavaParser) = assertChanged(
        jp,
        before = """
            class Test {
                String name;
                void test() {
                    switch (name) {
                      case "jonathan":
                          doSomething();
                          break;
                      case "jon":
                          doSomethingElse();
                          break;
                    }
                }
            }
        """,
        after = """
            class Test {
                String name;
                void test() {
                    if ("jonathan".equals(name)) {
                        doSomething();
                    } else if ("jon".equals(name)) {
                        doSomethingElse();
                    }
                }
            }
        """,
        typeValidation = {
            methodInvocations = false
        }
    )

    @Issue("https://github.com/openrewrite/rewrite/issues/800")
    @Test
    fun onePrimitive(jp: JavaParser) = assertChanged(
        jp,
        before = """
            class Test {
                int variable;
                void test() {
                    switch (variable) {
                      case 0:
                          doSomething();
                          break;
                    }
                }
            }
        """,
        after = """
            class Test {
                int variable;
                void test() {
                    if (variable == 0) {
                        doSomething();
                    }
                }
            }
        """,
        typeValidation = {
            methodInvocations = false
        }
    )

    @Issue("https://github.com/openrewrite/rewrite/issues/800")
    @Test
    fun oneString(jp: JavaParser) = assertChanged(
        jp,
        before = """
            class Test {
                String name;
                void test() {
                    switch (name) {
                      case "jonathan":
                          doSomething();
                          break;
                    }
                }
            }
        """,
        after = """
            class Test {
                String name;
                void test() {
                    if ("jonathan".equals(name)) {
                        doSomething();
                    }
                }
            }
        """,
        typeValidation = {
            methodInvocations = false
        }
    )

    @Suppress("StatementWithEmptyBody")
    @Issue("https://github.com/openrewrite/rewrite/issues/800")
    @Test
    fun noCases(jp: JavaParser) = assertUnchanged(
        jp,
        before = """
            class Test {
                int variable;
                void test() {
                    switch (variable) {
                    }
                }
            }
        """
    )

    @Test
    @Issue("https://github.com/openrewrite/rewrite/issues/1212")
    fun importsOnEnum() = assertChanged(
        before = """
            import java.time.DayOfWeek;

            class Test {
                DayOfWeek day;

                void test() {
                    switch(day) {
                        case MONDAY:
                            someMethod();
                            break;
                    }
                    switch(day) {
                        case MONDAY:
                            someMethod();
                        default:
                            someMethod();
                            break;
                    }
                    switch (day) {
                        case MONDAY:
                            someMethod();
                            break;
                        case TUESDAY:
                            someMethod();
                            break;
                    }
                }

                void someMethod() {
                }
            }
        """,
        after = """
            import java.time.DayOfWeek;

            class Test {
                DayOfWeek day;

                void test() {
                    if (day == DayOfWeek.MONDAY) {
                        someMethod();
                    }
                    if (day == DayOfWeek.MONDAY) {
                        someMethod();
                    } else {
                        someMethod();
                    }
                    if (day == DayOfWeek.MONDAY) {
                        someMethod();
                    } else if (day == DayOfWeek.TUESDAY) {
                        someMethod();
                    }
                }

                void someMethod() {
                }
            }
        """
    )
}
