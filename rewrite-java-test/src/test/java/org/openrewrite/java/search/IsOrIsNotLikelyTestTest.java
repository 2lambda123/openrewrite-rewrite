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

import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.openrewrite.java.JavaParser;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;
import org.openrewrite.test.SourceSpecs;

import static org.openrewrite.java.Assertions.*;
import static org.openrewrite.test.SourceSpecs.dir;

public class IsOrIsNotLikelyTestTest {

    private interface Base extends RewriteTest {
        @Language("java")
        String MAIN_INITIAL = """
          class Main { }
          """;

        @Language("java")
        String MAIN_SEARCH_RESULT = """
          /*~~>*/class Main { }
          """;

        default SourceSpecs assertMainIsFound() {
            return java(MAIN_INITIAL, MAIN_SEARCH_RESULT);
        }

        default SourceSpecs assertMainNoChanges() {
            return java(MAIN_INITIAL);
        }

        @Language("java")
        String TEST_INITIAL = """
          class ATest { }
          """;

        @Language("java")
        String TEST_SEARCH_RESULT = """
          /*~~>*/class ATest { }
          """;

        @Language("java")
        String JUNIT_TEST_INITIAL = """
          import org.junit.jupiter.api.Test;
                    
          class TestTest {
            @Test
            void test() {}
          }
          """;

        @Language("java")
        String JUNIT_TEST_SEARCH_RESULT = """
          /*~~>*/import org.junit.jupiter.api.Test;
                    
          class TestTest {
            @Test
            void test() {}
          }
          """;

        default SourceSpecs assertTestIsFound() {
            return dir("com/example",
              java(TEST_INITIAL, TEST_SEARCH_RESULT),
              java(JUNIT_TEST_INITIAL, JUNIT_TEST_SEARCH_RESULT)
            );
        }

        default SourceSpecs assertTestNoChanges() {
            return dir("com/example",
              java(TEST_INITIAL),
              java(JUNIT_TEST_INITIAL)
            );
        }

        @SuppressWarnings("SpellCheckingInspection")
        default SourceSpecs srcIntegTestJava(SourceSpecs... javaSources) {
            return dir("src/integTest/java", spec -> sourceSet(spec, "integTest"), javaSources);
        }

        default SourceSpecs srcCompatibilityTestJava(SourceSpecs... javaSources) {
            return dir("src/compatibility-test/java", spec -> sourceSet(spec, "compatibilityTest"), javaSources);
        }
    }

    @Nested
    class IsLikelyTestTest implements Base {

        @Override
        public void defaults(RecipeSpec spec) {
            spec
              .parser(JavaParser.fromJavaVersion().classpath("junit-jupiter-api"))
              .recipe(new IsLikelyTest());
        }

        @Test
        void testStandardMainAndTestSourceSet() {
            rewriteRun(
              srcMainJava(assertMainNoChanges()),
              srcTestJava(assertTestIsFound())
            );
        }

        @Test
        @SuppressWarnings("SpellCheckingInspection")
        void testStandardMainAndIntegTestSourceSet() {
            rewriteRun(
              srcMainJava(assertMainNoChanges()),
              srcIntegTestJava(assertTestIsFound())
            );
        }

        @Test
        void testStandardMainAndCompatibilityTestSourceSet() {
            rewriteRun(
              srcMainJava(assertMainNoChanges()),
              srcCompatibilityTestJava(assertTestIsFound())
            );
        }

        @Test
        void junitTestNotInTestSourceSet() {
            rewriteRun(java(JUNIT_TEST_INITIAL, JUNIT_TEST_SEARCH_RESULT));
        }
    }

    @Nested
    class IsLikelyNotTestTest implements Base {

        @Override
        public void defaults(RecipeSpec spec) {
            spec
              .parser(JavaParser.fromJavaVersion().classpath("junit-jupiter-api"))
              .recipe(new IsLikelyNotTest());
        }

        @Test
        void testStandardMainAndTestSourceSet() {
            rewriteRun(
              srcMainJava(assertMainIsFound()),
              srcTestJava(assertTestNoChanges())
            );
        }

        @Test
        @SuppressWarnings("SpellCheckingInspection")
        void testStandardMainAndIntegTestSourceSet() {
            rewriteRun(
              srcMainJava(assertMainIsFound()),
              srcIntegTestJava(assertTestNoChanges())
            );
        }

        @Test
        void testStandardMainAndCompatibilityTestSourceSet() {
            rewriteRun(
              srcMainJava(assertMainIsFound()),
              srcCompatibilityTestJava(assertTestNoChanges())
            );
        }

        @Test
        void junitTestNotInTestSourceSet() {
            rewriteRun(java(JUNIT_TEST_INITIAL));
        }
    }

}
