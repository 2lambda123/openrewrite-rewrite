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
package org.openrewrite.java.tree;

import org.junit.jupiter.api.Test;
import org.openrewrite.Issue;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.java.Assertions.java;

public class SwitchExpressionTest implements RewriteTest {

    @Issue("https://github.com/openrewrite/rewrite/issues/2164")
    @Test
    void basicSyntax() {
        rewriteRun(
          java(
            """
              class Test {
                int test(int i) {
                    return switch (i) {
                        case 1 -> 1;
                        case 2 -> 2;
                        default -> 3;
                    };
                }
              }
              """
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/2164")
    @Test
    void multipleExpressions() {
        rewriteRun(
          java(
            """
              class Test {
                int test(int i) {
                    return switch (i) {
                        case 1, 2 -> 1;
                        default -> 3;
                    };
                }
              }
              """
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/2164")
    @Test
    void yieldFromStatement() {
        rewriteRun(
          java(
            """
              class Test {
                int test(int i) {
                    return switch (i) {
                        case 1, 2:
                            yield 1;
                        default:
                            yield 3;
                    };
                }
              }
              """
          )
        );
    }

    @SuppressWarnings("RedundantLabeledSwitchRuleCodeBlock")
    @Issue("https://github.com/openrewrite/rewrite/issues/2164")
    @Test
    void yieldFromRule() {
        rewriteRun(
          java(
            """
              class Test {
                int test(int i) {
                    return switch (i) {
                        case 1, 2 -> { yield 1; }
                        default -> { yield 3; }
                    };
                }
              }
              """
          )
        );
    }
}
