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
package org.openrewrite.gradle;

import org.junit.jupiter.api.Test;
import org.openrewrite.java.JavaParser;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;
import org.openrewrite.test.TypeValidation;

import static org.openrewrite.gradle.Assertions.buildGradle;
import static org.openrewrite.gradle.Assertions.withToolingApi;

@SuppressWarnings("FieldCanBeLocal")
class ExcludeTransitiveDependencyTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.beforeRecipe(withToolingApi())
          .parser(JavaParser.fromJavaVersion().classpath("commons-beanutils", "guava"));
    }

    @Test
    void noExclusionNecessary() {
        ExcludeTransitiveDependency excludeDep = new ExcludeTransitiveDependency("commons-collections", "commons-collections", "testImplementation");
        rewriteRun(
          spec -> spec.recipe(excludeDep)
            .typeValidationOptions(TypeValidation.none()),
          buildGradle(
            """
              plugins {
                  id "java-library"
              }
              
              repositories {
                  mavenCentral()
              }
              
              dependencies {
                  implementation("com.google.guava:guava:latest.release")
              }
              """
          )
        );
    }

    @Test
    void exclusionAlreadyExists() {
        ExcludeTransitiveDependency excludeDep = new ExcludeTransitiveDependency("commons-collections", "commons-collections", null);
        rewriteRun(
          spec -> spec.recipe(excludeDep)
            .typeValidationOptions(TypeValidation.none()),
          buildGradle(
            """
              plugins {
                  id "java-library"
              }
              
              repositories {
                  mavenCentral()
              }
              
              dependencies {
                  implementation("commons-beanutils:commons-beanutils:1.9.4") {
                      exclude group: "commons-collections", module: "commons-collections"
                  }
              }
              """
          )
        );
    }

    @Test
    void regularExclusion() {
        ExcludeTransitiveDependency excludeDep = new ExcludeTransitiveDependency("commons-collections", "commons-collections", null);
        rewriteRun(
          spec -> spec.recipe(excludeDep)
            .typeValidationOptions(TypeValidation.none()),
          buildGradle(
            """
              plugins {
                  id "java-library"
              }
              
              repositories {
                  mavenCentral()
              }
              
              dependencies {
                  implementation("commons-beanutils:commons-beanutils:1.9.4")
              }
              """,
            """
              plugins {
                  id "java-library"
              }
              
              repositories {
                  mavenCentral()
              }
              
              dependencies {
                  implementation("commons-beanutils:commons-beanutils:1.9.4") {
                      exclude group: "commons-collections", module: "commons-collections"
                  }
              }
              """
          )
        );
    }

    @Test
    void appliesToConfiguration() {
        ExcludeTransitiveDependency excludeDep = new ExcludeTransitiveDependency("commons-collections", "commons-collections", "testImplementation");
        rewriteRun(
          spec -> spec.recipe(excludeDep)
            .typeValidationOptions(TypeValidation.none()),
          buildGradle(
            """
              plugins {
                  id "java-library"
              }
              
              repositories {
                  mavenCentral()
              }
              
              dependencies {
                  implementation("commons-beanutils:commons-beanutils:1.9.4")
                  
                  testImplementation("commons-beanutils:commons-beanutils:1.9.4")
              }
              """,
            """
              plugins {
                  id "java-library"
              }
              
              repositories {
                  mavenCentral()
              }
              
              dependencies {
                  implementation("commons-beanutils:commons-beanutils:1.9.4")
                  
                  testImplementation("commons-beanutils:commons-beanutils:1.9.4") {
                      exclude group: "commons-collections", module: "commons-collections"
                  }
              }
              """
          )
        );
    }

    @Test
    void excludeAllTransitiveDependencies() {
        ExcludeTransitiveDependency excludeDep = new ExcludeTransitiveDependency("commons-collections", "commons-collections", null);
        rewriteRun(
          spec -> spec.recipe(excludeDep)
            .typeValidationOptions(TypeValidation.none()),
          buildGradle(
            """
              plugins {
                  id "java-library"
              }
              
              repositories {
                  mavenCentral()
              }
              
              dependencies {
                  implementation("commons-beanutils:commons-beanutils:1.9.4")
                  implementation("com.opencsv:opencsv:4.6")
                  
                  testImplementation("commons-beanutils:commons-beanutils:1.9.4")
                  testImplementation("com.opencsv:opencsv:4.6")
              }
              """,
            """
              plugins {
                  id "java-library"
              }
              
              repositories {
                  mavenCentral()
              }
              
              dependencies {
                  implementation("commons-beanutils:commons-beanutils:1.9.4") {
                      exclude group: "commons-collections", module: "commons-collections"
                  }
                  implementation("com.opencsv:opencsv:4.6") {
                      exclude group: "commons-collections", module: "commons-collections"
                  }
              
                  testImplementation("commons-beanutils:commons-beanutils:1.9.4") {
                      exclude group: "commons-collections", module: "commons-collections"
                  }
                  testImplementation("com.opencsv:opencsv:4.6") {
                      exclude group: "commons-collections", module: "commons-collections"
                  }
              }
              """
          )
        );
    }
}
