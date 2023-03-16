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
package org.openrewrite.groovy.tree;

import org.junit.jupiter.api.Test;
import org.openrewrite.java.tree.J;
import org.openrewrite.test.RewriteTest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.openrewrite.groovy.Assertions.groovy;

@SuppressWarnings({"GrUnnecessarySemicolon", "GroovyVariableNotAssigned"})
class SwitchTest implements RewriteTest {

    @Test
    void basicSwitch() {
        rewriteRun(
          groovy(
            """
              switch ("foo") {
                  case "foo"  :
                      return true
              }
              """
          )
        );
    }

    @Test
    void singleCase() {
        rewriteRun(
          groovy(
            """
              int n;
              switch(n) {
                 case 0: break;
              }
              """
          )
        );
    }

    @Test
    void defaultSwitch() {
        rewriteRun(
          groovy(
            """
              switch(0) {
                  default: System.out.println("default!");
              }
              """,
            spec -> spec.afterRecipe(cu -> {
                assertThat(cu.getStatements()).hasSize(1);
                assertThat(cu.getStatements().get(0)).isInstanceOf(J.Switch.class);
                assertThat(((J.Switch) cu.getStatements().get(0)).getCases().getStatements()).hasSize(1);
            })
          )
        );
    }

    @Test
    void multipleCases() {
        rewriteRun(
          groovy(
            """
              switch("foo") {
                  case "foo": {
                     break
                  }
                  case "bar": {
                     break
                  }
              }
              """
          )
        );
    }

    @Test
    void multipleCasesWithDefault() {
        rewriteRun(
          groovy(
            """
              switch("foo") {
                  case "foo":
                      break
                  case "bar":
                      break
                  default:
                      break
              }
              """,
            spec -> spec.afterRecipe(cu -> {
                assertThat(cu.getStatements()).hasSize(1);
                assertThat(cu.getStatements().get(0)).isInstanceOf(J.Switch.class);
                assertThat(((J.Switch) cu.getStatements().get(0)).getCases().getStatements()).hasSize(3);
            })
          )
        );
    }
}
