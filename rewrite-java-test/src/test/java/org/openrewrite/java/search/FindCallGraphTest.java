/*
 * Copyright 2023 the original author or authors.
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
package org.openrewrite.java.search;

import org.junit.jupiter.api.Test;
import org.openrewrite.java.table.MethodCallGraph;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.openrewrite.java.Assertions.java;

public class FindCallGraphTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(new FindCallGraph());
    }

    @Test
    void findUniqueCallsPerDeclaration() {
        rewriteRun(
          spec -> spec.dataTable(MethodCallGraph.Row.class, row ->
            assertThat(row).containsExactly(
              new MethodCallGraph.Row(
                "Test test()",
                "java.io.PrintStream println(java.lang.String)"
              ),
              new MethodCallGraph.Row(
                "Test test2()",
                "java.io.PrintStream println(java.lang.String)"
              )
            )
          ),
          java(
            """
              class Test {
                  void test() {
                      System.out.println("Hello");
                      System.out.println("Hello");
                  }
                            
                  void test2() {
                      System.out.println("Hello");
                      System.out.println("Hello");
                  }
              }
              """
          )
        );
    }
}
