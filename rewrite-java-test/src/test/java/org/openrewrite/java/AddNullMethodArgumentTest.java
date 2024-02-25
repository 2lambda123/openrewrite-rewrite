/*
 * Copyright 2024 the original author or authors.
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

import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.Test;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.java.Assertions.java;

class AddNullMethodArgumentTest implements RewriteTest {
    @Override
    public void defaults(RecipeSpec spec) {
        spec.parser(JavaParser.fromJavaVersion().dependsOn("""
          class B {
             public static void foo() {}
             public static void foo(Integer n) {}
             public static void foo(Integer n1, Integer n2) {}
             public static void foo(Integer n1, Integer n2, Integer n3) {}
             public B() {}
             public B(Integer n) {}
          }
          """));
    }

    @Test
    void addToMiddleArgument() {
        rewriteRun(
          spec -> spec.recipe(new AddNullMethodArgument("B foo(Integer, Integer)", 1)),
          java(
            "public class A {{ B.foo(0, 1); }}",
            "public class A {{ B.foo(0, null, 1); }}"
          )
        );
    }

    @Test
    void addArgumentsConsecutively() {
        rewriteRun(
          spec -> spec.recipes(
            new AddNullMethodArgument("B foo(Integer)", 1),
            new AddNullMethodArgument("B foo(Integer, null)", 1)
          ),
          java(b),
          java(
            "class A {{ B.foo(0); }}",
            "class A {{ B.foo(0, null, null); }}"
          )
        );
    }

    @Test
    void addToConstructorArgument() {
        rewriteRun(
          spec -> spec.recipe(new AddNullMethodArgument("B <constructor>()", 0)),
          java(b),
          java(
            "public class A { B b = new B(); }",
            "public class A { B b = new B(null); }"
          )
        );
    }
}
