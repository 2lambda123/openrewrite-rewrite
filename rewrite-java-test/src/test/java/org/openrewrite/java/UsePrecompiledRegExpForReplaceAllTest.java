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
package org.openrewrite.java;

import org.junit.jupiter.api.Test;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.java.Assertions.java;

class UsePrecompiledRegExpForReplaceAllTest implements RewriteTest {

    @Test
    void replaceSimpleVar() {
        rewriteRun(recipeSpec -> recipeSpec.recipe(new UsePrecompiledRegExpForReplaceAll()), java("""
          class A {
              public void replace(){
                  String init = "Bob is a Bird... Bob is a Plane... Bob is Superman!";
                  String changed = init.replaceAll("/[@]/g,", "_");
              }
          }
          """, """
          class A {
              private static final java.util.regex.Pattern openRewriteReplaceAllPatternVar = Pattern.compile("/[@]/g,");
              public void replace(){
                  String init = "Bob is a Bird... Bob is a Plane... Bob is Superman!";
                  String changed = openRewriteReplaceAllPatternVar.matcher(init).replaceAll("_");
              }
          }
          """));
    }

    @Test
    void replaceMultipleReplaceAllOccurrences() {
        rewriteRun(recipeSpec -> recipeSpec.recipe(new UsePrecompiledRegExpForReplaceAll()), java("""
          class A {
              public void replace(){
                  String firstReplace = "Bob is a Bird... Bob is a Plane... Bob is Superman!";
                  String firstChanged = firstReplace.replaceAll("/[@]/g,", "_");
                  
                  String secondReplace = "Some other subject";
                  String secondChanged = secondReplace.replaceAll("\\s", "_");
              }
          }
          """, """
          class A {
              private static final java.util.regex.Pattern openRewriteReplaceAllPatternVar1 = Pattern.compile("\\s");
              private static final java.util.regex.Pattern openRewriteReplaceAllPatternVar = Pattern.compile("/[@]/g,");
              public void replace(){
                  String firstReplace = "Bob is a Bird... Bob is a Plane... Bob is Superman!";
                  String firstChanged = openRewriteReplaceAllPatternVar.matcher(firstReplace).replaceAll("_");

                  String secondReplace = "Some other subject";
                  String secondChanged = openRewriteReplaceAllPatternVar1.matcher(secondReplace).replaceAll("_");
              }
          }
          """));
    }

    @Test
    void replaceMultipleReplaceAllOccurrencesAtDifferentLocations() {
        rewriteRun(recipeSpec -> recipeSpec.recipe(new UsePrecompiledRegExpForReplaceAll()), java("""
          class A {
              public void replace(){
                  String firstReplace = "Bob is a Bird... Bob is a Plane... Bob is Superman!";
                  String firstChanged = firstReplace.replaceAll("/[@]/g,", "_");
              }

              public void otherMethod(){
                  String secondReplace = "Some other subject";
                  String secondChanged = secondReplace.replaceAll("\\s", "_");
              }
          }
          """, """
          class A {
              private static final java.util.regex.Pattern openRewriteReplaceAllPatternVar1 = Pattern.compile("\\s");
              private static final java.util.regex.Pattern openRewriteReplaceAllPatternVar = Pattern.compile("/[@]/g,");
              public void replace(){
                  String firstReplace = "Bob is a Bird... Bob is a Plane... Bob is Superman!";
                  String firstChanged = openRewriteReplaceAllPatternVar.matcher(firstReplace).replaceAll("_");
              }

              public void otherMethod(){
                  String secondReplace = "Some other subject";
                  String secondChanged = openRewriteReplaceAllPatternVar1.matcher(secondReplace).replaceAll("_");
              }
          }
          """));
    }
}
