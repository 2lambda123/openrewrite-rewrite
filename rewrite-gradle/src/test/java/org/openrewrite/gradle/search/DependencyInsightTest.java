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
package org.openrewrite.gradle.search;

import org.junit.jupiter.api.Test;
import org.openrewrite.maven.table.DependenciesInUse;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.openrewrite.gradle.Assertions.buildGradle;
import static org.openrewrite.gradle.Assertions.withToolingApi;

class DependencyInsightTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.beforeRecipe(withToolingApi())
          .recipe(new DependencyInsight("com.google.guava", "failureaccess", null));
    }

    @Test
    void findTransitiveDependency() {
        rewriteRun(
          buildGradle("""
              plugins {
                  id 'java-library'
              }
              
              repositories {
                  mavenCentral()
              }
              
              dependencies {
                  implementation 'com.google.guava:guava:31.1-jre'
              }
              """,
            """
              plugins {
                  id 'java-library'
              }
              
              repositories {
                  mavenCentral()
              }
              
              dependencies {
                  /*~~(com.google.guava:failureaccess:1.0.1)~~>*/implementation 'com.google.guava:guava:31.1-jre'
              }
              """
          )
        );
    }

    @Test
    void pattern() {
        rewriteRun(
          spec ->  spec.recipe(new DependencyInsight("*", "jackson-core", null))
            .dataTable(DependenciesInUse.Row.class, rows -> {
                assertThat(rows).isNotEmpty();
                DependenciesInUse.Row row = rows.get(0);
                assertThat(row.getGroupId()).isEqualTo("com.fasterxml.jackson.core");
                assertThat(row.getArtifactId()).isEqualTo("jackson-core");
                assertThat(row.getVersion()).isEqualTo("2.13.4");
            }),
          buildGradle("""
              plugins {
                  id 'java-library'
              }
              
              repositories {
                  mavenCentral()
              }
              
              dependencies {
                  implementation 'org.openrewrite:rewrite-core:7.39.1'
              }
              """,
            """
              plugins {
                  id 'java-library'
              }
              
              repositories {
                  mavenCentral()
              }
              
              dependencies {
                  /*~~(com.fasterxml.jackson.core:jackson-core:2.13.4)~~>*/implementation 'org.openrewrite:rewrite-core:7.39.1'
              }
              """
          )
        );
    }

}
