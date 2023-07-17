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
package org.openrewrite.maven;

import org.junit.jupiter.api.Test;
import org.openrewrite.DocumentExample;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.maven.Assertions.pomXml;

class RemoveExclusionTest implements RewriteTest {
    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(new RemoveExclusion(
          "com.google.guava",
          "guava",
          "commons-lang",
          "commons-lang",
          null
        ));
    }

    @DocumentExample
    @Test
    void removeUnusedExclusions() {
        rewriteRun(
          pomXml(
            """
              <project>
                <groupId>com.mycompany.app</groupId>
                <artifactId>my-app</artifactId>
                <version>1</version>
                <dependencies>
                  <dependency>
                    <groupId>com.google.guava</groupId>
                    <artifactId>guava</artifactId>
                    <version>29.0-jre</version>
                    <exclusions>
                      <exclusion>
                        <groupId>commons-lang</groupId>
                        <artifactId>commons-lang</artifactId>
                      </exclusion>
                    </exclusions>
                  </dependency>
                </dependencies>
              </project>
              """,
            """
              <project>
                <groupId>com.mycompany.app</groupId>
                <artifactId>my-app</artifactId>
                <version>1</version>
                <dependencies>
                  <dependency>
                    <groupId>com.google.guava</groupId>
                    <artifactId>guava</artifactId>
                    <version>29.0-jre</version>
                  </dependency>
                </dependencies>
              </project>
              """
          )
        );
    }

    @Test
    void removeUnusedExclusionsOnlyIneffective() {
        rewriteRun(
          spec -> spec.recipe(new RemoveExclusion(
            "com.google.guava",
            "guava",
            "commons-lang",
            "commons-lang",
            true
          )),
          pomXml(
            """
              <project>
                <groupId>com.mycompany.app</groupId>
                <artifactId>my-app</artifactId>
                <version>1</version>
                <dependencies>
                  <dependency>
                    <groupId>com.google.guava</groupId>
                    <artifactId>guava</artifactId>
                    <version>29.0-jre</version>
                    <exclusions>
                      <exclusion>
                        <groupId>commons-lang</groupId>
                        <artifactId>commons-lang</artifactId>
                      </exclusion>
                    </exclusions>
                  </dependency>
                </dependencies>
              </project>
              """,
            """
              <project>
                <groupId>com.mycompany.app</groupId>
                <artifactId>my-app</artifactId>
                <version>1</version>
                <dependencies>
                  <dependency>
                    <groupId>com.google.guava</groupId>
                    <artifactId>guava</artifactId>
                    <version>29.0-jre</version>
                  </dependency>
                </dependencies>
              </project>
              """
          )
        );
    }

    @Test
    void removeAnyExclusionsOnlyIneffective() {
        rewriteRun(
          spec -> spec.recipe(new RemoveExclusion(
            "*",
            "*",
            "*",
            "*",
            true
          )),
          pomXml(
            """
              <project>
                <groupId>com.mycompany.app</groupId>
                <artifactId>my-app</artifactId>
                <version>1</version>
                <dependencies>
                  <dependency>
                    <groupId>com.google.guava</groupId>
                    <artifactId>guava</artifactId>
                    <version>29.0-jre</version>
                    <exclusions>
                      <exclusion>
                        <groupId>commons-lang</groupId>
                        <artifactId>commons-lang</artifactId>
                      </exclusion>
                      <exclusion>
                        <groupId>com.google.code.findbugs</groupId>
                        <artifactId>jsr305</artifactId>
                      </exclusion>
                    </exclusions>
                  </dependency>
                </dependencies>
              </project>
              """,
            """
              <project>
                <groupId>com.mycompany.app</groupId>
                <artifactId>my-app</artifactId>
                <version>1</version>
                <dependencies>
                  <dependency>
                    <groupId>com.google.guava</groupId>
                    <artifactId>guava</artifactId>
                    <version>29.0-jre</version>
                    <exclusions>
                      <exclusion>
                        <groupId>com.google.code.findbugs</groupId>
                        <artifactId>jsr305</artifactId>
                      </exclusion>
                    </exclusions>
                  </dependency>
                </dependencies>
              </project>
              """
          )
        );
    }

    @Test
    void removeUsedExclusions() {
        rewriteRun(
          spec -> spec.recipe(new RemoveExclusion(
            "com.google.guava",
            "guava",
            "com.google.code.findbugs",
            "jsr305",
            null
          )),
          pomXml(
            """
              <project>
                <groupId>com.mycompany.app</groupId>
                <artifactId>my-app</artifactId>
                <version>1</version>
                <dependencies>
                  <dependency>
                    <groupId>com.google.guava</groupId>
                    <artifactId>guava</artifactId>
                    <version>29.0-jre</version>
                    <exclusions>
                      <exclusion>
                        <groupId>com.google.code.findbugs</groupId>
                        <artifactId>jsr305</artifactId>
                      </exclusion>
                    </exclusions>
                  </dependency>
                </dependencies>
              </project>
              """,
            """
              <project>
                <groupId>com.mycompany.app</groupId>
                <artifactId>my-app</artifactId>
                <version>1</version>
                <dependencies>
                  <dependency>
                    <groupId>com.google.guava</groupId>
                    <artifactId>guava</artifactId>
                    <version>29.0-jre</version>
                  </dependency>
                </dependencies>
              </project>
              """
          )
        );
    }

    @Test
    void removeUsedExclusionsOnlyIneffective() {
        rewriteRun(
          spec -> spec.recipe(new RemoveExclusion(
            "com.google.guava",
            "guava",
            "com.google.code.findbugs",
            "jsr305",
            true
          )),
          pomXml(
            """
              <project>
                <groupId>com.mycompany.app</groupId>
                <artifactId>my-app</artifactId>
                <version>1</version>
                <dependencies>
                  <dependency>
                    <groupId>com.google.guava</groupId>
                    <artifactId>guava</artifactId>
                    <version>29.0-jre</version>
                    <exclusions>
                      <exclusion>
                        <groupId>com.google.code.findbugs</groupId>
                        <artifactId>jsr305</artifactId>
                      </exclusion>
                    </exclusions>
                  </dependency>
                </dependencies>
              </project>
              """
          )
        );
    }

    @Test
    void preserveOtherExclusionsAndComments() {
        rewriteRun(
          pomXml(
            """
              <project>
                <groupId>com.mycompany.app</groupId>
                <artifactId>my-app</artifactId>
                <version>1</version>
                <dependencies>
                  <dependency>
                    <groupId>com.google.guava</groupId>
                    <artifactId>guava</artifactId>
                    <version>29.0-jre</version>
                    <exclusions>
                      <exclusion>
                        <groupId>commons-lang</groupId>
                        <artifactId>commons-lang</artifactId>
                      </exclusion>
                      <!-- comment -->
                      <exclusion>
                        <groupId>foo</groupId>
                        <artifactId>bar</artifactId>
                      </exclusion>
                    </exclusions>
                  </dependency>
                </dependencies>
              </project>
              """,
            """
              <project>
                <groupId>com.mycompany.app</groupId>
                <artifactId>my-app</artifactId>
                <version>1</version>
                <dependencies>
                  <dependency>
                    <groupId>com.google.guava</groupId>
                    <artifactId>guava</artifactId>
                    <version>29.0-jre</version>
                    <exclusions>
                      <!-- comment -->
                      <exclusion>
                        <groupId>foo</groupId>
                        <artifactId>bar</artifactId>
                      </exclusion>
                    </exclusions>
                  </dependency>
                </dependencies>
              </project>
              """
          )
        );
    }

    @Test
    void removeExclusionsTagWhenOnlyCommentsAreLeft() {
        rewriteRun(
          pomXml(
            """
              <project>
                <groupId>com.mycompany.app</groupId>
                <artifactId>my-app</artifactId>
                <version>1</version>
                <dependencies>
                  <dependency>
                    <groupId>com.google.guava</groupId>
                    <artifactId>guava</artifactId>
                    <version>29.0-jre</version>
                    <exclusions>
                      <!-- comment -->
                      <exclusion>
                        <groupId>commons-lang</groupId>
                        <artifactId>commons-lang</artifactId>
                      </exclusion>
                    </exclusions>
                  </dependency>
                </dependencies>
              </project>
              """,
            """
              <project>
                <groupId>com.mycompany.app</groupId>
                <artifactId>my-app</artifactId>
                <version>1</version>
                <dependencies>
                  <dependency>
                    <groupId>com.google.guava</groupId>
                    <artifactId>guava</artifactId>
                    <version>29.0-jre</version>
                  </dependency>
                </dependencies>
              </project>
              """
          )
        );
    }

    @Test
    void removeUnusedExclusionsFromDependencyManagement() {
        rewriteRun(
          pomXml("""
              <project>
                <groupId>com.mycompany.app</groupId>
                <artifactId>my-app</artifactId>
                <version>1</version>
                <dependencyManagement>
                  <dependencies>
                    <dependency>
                      <groupId>com.google.guava</groupId>
                      <artifactId>guava</artifactId>
                      <version>29.0-jre</version>
                      <exclusions>
                        <exclusion>
                          <groupId>commons-lang</groupId>
                          <artifactId>commons-lang</artifactId>
                        </exclusion>
                      </exclusions>
                    </dependency>
                  </dependencies>
                </dependencyManagement>
              </project>
              """,
            """
              <project>
                <groupId>com.mycompany.app</groupId>
                <artifactId>my-app</artifactId>
                <version>1</version>
                <dependencyManagement>
                  <dependencies>
                    <dependency>
                      <groupId>com.google.guava</groupId>
                      <artifactId>guava</artifactId>
                      <version>29.0-jre</version>
                    </dependency>
                  </dependencies>
                </dependencyManagement>
              </project>
              """
          )
        );
    }

    @Test
    void removeAnyExclusionsFromDependencyManagementOnlyIneffectiveUsedDependency() {
        rewriteRun(
          spec -> spec.recipe(new RemoveExclusion(
            "*",
            "*",
            "*",
            "*",
            true
          )),
          pomXml("""
              <project>
                <groupId>com.mycompany.app</groupId>
                <artifactId>my-app</artifactId>
                <version>1</version>
                <dependencyManagement>
                  <dependencies>
                    <dependency>
                      <groupId>com.google.guava</groupId>
                      <artifactId>guava</artifactId>
                      <version>29.0-jre</version>
                      <exclusions>
                        <exclusion>
                          <groupId>commons-lang</groupId>
                          <artifactId>commons-lang</artifactId>
                        </exclusion>
                        <exclusion>
                          <groupId>com.google.code.findbugs</groupId>
                          <artifactId>jsr305</artifactId>
                        </exclusion>
                      </exclusions>
                    </dependency>
                  </dependencies>
                </dependencyManagement>
                <dependencies>
                  <dependency>
                    <groupId>com.google.guava</groupId>
                    <artifactId>guava</artifactId>
                  </dependency>
                </dependencies>
              </project>
              """,
            """
              <project>
                <groupId>com.mycompany.app</groupId>
                <artifactId>my-app</artifactId>
                <version>1</version>
                <dependencyManagement>
                  <dependencies>
                    <dependency>
                      <groupId>com.google.guava</groupId>
                      <artifactId>guava</artifactId>
                      <version>29.0-jre</version>
                      <exclusions>
                        <exclusion>
                          <groupId>com.google.code.findbugs</groupId>
                          <artifactId>jsr305</artifactId>
                        </exclusion>
                      </exclusions>
                    </dependency>
                  </dependencies>
                </dependencyManagement>
                <dependencies>
                  <dependency>
                    <groupId>com.google.guava</groupId>
                    <artifactId>guava</artifactId>
                  </dependency>
                </dependencies>
              </project>
              """
          )
        );
    }

    @Test
    void removeAnyExclusionsFromDependencyManagementOnlyIneffectiveDoesNothingIfUnsure() {
        rewriteRun(
          spec -> spec.recipe(new RemoveExclusion(
            "*",
            "*",
            "*",
            "*",
            true
          )),
          pomXml("""
              <project>
                <groupId>com.mycompany.app</groupId>
                <artifactId>my-app</artifactId>
                <version>1</version>
                <dependencyManagement>
                  <dependencies>
                    <dependency>
                      <groupId>com.google.guava</groupId>
                      <artifactId>guava</artifactId>
                      <version>29.0-jre</version>
                      <exclusions>
                        <exclusion>
                          <groupId>commons-lang</groupId>
                          <artifactId>commons-lang</artifactId>
                        </exclusion>
                        <exclusion>
                          <groupId>com.google.code.findbugs</groupId>
                          <artifactId>jsr305</artifactId>
                        </exclusion>
                      </exclusions>
                    </dependency>
                  </dependencies>
                </dependencyManagement>
              </project>
              """
          )
        );
    }

    @Test
    // https://issues.apache.org/jira/browse/MNG-5600
    void dependencyManagementImportExclusionsAreAlwaysIneffective() {
        rewriteRun(
          spec -> spec.recipe(new RemoveExclusion(
            "*",
            "*",
            "*",
            "*",
            true
          )),
          pomXml("""
              <project>
                <groupId>com.mycompany.app</groupId>
                <artifactId>my-app</artifactId>
                <version>1</version>
                <dependencyManagement>
                  <dependencies>
                    <dependency>
                      <groupId>org.junit</groupId>
                      <artifactId>junit-bom</artifactId>
                      <version>5.9.0</version>
                      <type>pom</type>
                      <scope>import</scope>
                      <exclusions>
                        <exclusion>
                          <groupId>commons-lang</groupId>
                          <artifactId>commons-lang</artifactId>
                        </exclusion>
                        <exclusion>
                          <groupId>org.junit.jupiter</groupId>
                          <artifactId>junit-jupiter</artifactId>
                        </exclusion>
                      </exclusions>
                    </dependency>
                  </dependencies>
                </dependencyManagement>
              </project>
              """,
            """
              <project>
                <groupId>com.mycompany.app</groupId>
                <artifactId>my-app</artifactId>
                <version>1</version>
                <dependencyManagement>
                  <dependencies>
                    <dependency>
                      <groupId>org.junit</groupId>
                      <artifactId>junit-bom</artifactId>
                      <version>5.9.0</version>
                      <type>pom</type>
                      <scope>import</scope>
                    </dependency>
                  </dependencies>
                </dependencyManagement>
              </project>
              """
          )
        );
    }
}
