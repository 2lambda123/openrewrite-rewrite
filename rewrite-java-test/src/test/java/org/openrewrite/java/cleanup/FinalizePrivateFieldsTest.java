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
package org.openrewrite.java.cleanup;

import org.junit.jupiter.api.Test;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.java.Assertions.java;

public class FinalizePrivateFieldsTest implements RewriteTest {
    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(new FinalizePrivateFields());
    }

    @Test
    void initializerMadeFinal() {
        rewriteRun(
          java(
            """
              class A {
                  private String name = "ABC";

                  String getName() {
                      return name;
                  }
              }
              """,
            """
                class A {
                    private final String name = "ABC";

                    String getName() {
                        return name;
                    }
                }
              """));
    }

    @Test
    void initByMethodMadeFinal() {
        rewriteRun(
          java(
            """
                  class A {
                      private String name = initName();

                      String initName() {
                          return "A name";
                      }
                  }
              """,
            """
                  class A {
                      private final String name = initName();

                      String initName() {
                          return "A name";
                      }
                  }
              """
          )
        );
    }

    @Test
    void InitByConstructorMadeFinal() {
        rewriteRun(
          java(
            """
                  class A {
                      private String name;

                      A() {
                          name = "XYZ";
                      }
                  }
              """,
            """
                  class A {
                      private final String name;

                      A() {
                          name = "XYZ";
                      }
                  }
              """
          )
        );
    }

    @Test
    void multiVariablesMadeFinal() {
        rewriteRun(
          java(
            """
                  class A {
                      private int a, b;

                      A() {
                          a = 0;
                          b = 1;
                      }

                      int func() {
                          return a + b;
                      }
                  }
              """,
            """
                  class A {
                      private final int a, b;

                      A() {
                          a = 0;
                          b = 1;
                      }

                      int func() {
                          return a + b;
                      }
                  }
              """
          )
        );
    }

    @Test
    void multiVariablesReassigned() {
        rewriteRun(
          java(
            """
                  class A {
                      private int a, b;

                      A() {
                          a = 0;
                          b = 1;
                      }

                      int func(int c) {
                          b += c;
                          return a + b;
                      }
                  }
              """
          )
        );
    }

    @Test
    void fieldReassignedByAMethod() {
        rewriteRun(
          java(
            """
                class A {
                    private String name = "ABC";

                    void func() {
                        name = "XYZ";
                    }

                    String getName() {
                        return name;
                    }
                }
              """
          )
        );
    }

    @Test
    void fieldReassignedByConstructor() {
        rewriteRun(
          java(
            """
                  class A {
                      private String name = "ABC";

                      A() {
                          name = "XYZ";
                      }
                  }
              """
          )
        );
    }

    @Test
    void constructorInitTwiceOrMore() {
        rewriteRun(
          java(
            """
                  class A {
                      private String name;

                      A() {
                          name = "ABC";
                          name = "XYZ";
                      }
                  }
              """
          )
        );
    }

    @Test
    void fieldsReassignedInLoops() {
        rewriteRun(
          java(
            """
                  class A {
                      private int a;
                      private int b;
                      private int c;

                      A() {
                          for (int i = 0; i< 10; i++) {
                              a = i;
                          }

                          int k = 0;
                          while (k < 10) {
                              b = k;
                              k++;
                          }

                          do {
                              k--;
                              c = k;
                          } while(k > 5);
                      }
                  }
              """
          )
        );
    }

    @Test
    void nonPrivateFieldsIgnored() {
        rewriteRun(
          java(
            """
                  class A {
                      int a = 0;
                      public int b = 1;
                      protected int c = 2;
                  }
              """
          )
        );
    }

    @Test
    void finalFieldsIgnored() {
        rewriteRun(
          java(
            """
                  class A {
                      private final int a = 0;
                  }
              """
          )
        );
    }

    @Test
    void staticFieldsMadeFinal() {
        rewriteRun(
          java(
            """
                  class A {
                      private static int num = 10;
                      int func() {
                          return num;
                      }
                  }
              """,
            """
                  class A {
                      private static final int num = 10;
                      int func() {
                          return num;
                      }
                  }
              """
          )
        );
    }

    @Test
    void initializedByInitializerBlock() {
        rewriteRun(
          java(
            """
                  public class Person {
                      {
                          name = "N1";
                          age = 10;
                          address = "CA";
                      }

                      private String name = "N2";
                      private int age = 15;
                      private String address;

                      public Person() {
                          name = "N3";
                          age = 20;
                      }
                  }
              """,
            """
                  public class Person {
                      {
                          name = "N1";
                          age = 10;
                          address = "CA";
                      }

                      private String name = "N2";
                      private int age = 15;
                      private final String address;

                      public Person() {
                          name = "N3";
                          age = 20;
                      }
                  }
              """
          )
        );
    }

    @Test
    void staticInitializerBlock() {
        rewriteRun(
          java(
            """
                  class A {
                      static {
                          num = 10;
                      }
                      private static int num;

                      int func() {
                          return num;
                      }
                  }
              """,
            """
                  class A {
                      static {
                          num = 10;
                      }
                      private static final int num;

                      int func() {
                          return num;
                      }
                  }
              """
          )
        );
    }

    @Test
    void innerClassFieldMadeFinal() {
        rewriteRun(
          java(
            """
                  class OuterClass {
                      int a;

                      class InnerClass {
                          private int b = 1;
                      }
                  }
              """,
            """
                  class OuterClass {
                      int a;

                      class InnerClass {
                          private final int b = 1;
                      }
                  }
              """
          )
        );
    }

    @Test
    void staticNestedClassFieldMadeFinal() {
        rewriteRun(
          java(
            """
                  class OuterClass {
                      int a;

                      static class InnerClass {
                          private int b = 1;

                          int func() {
                              return b;
                          }
                      }
                  }
              """,
            """
                  class OuterClass {
                      int a;

                      static class InnerClass {
                          private final int b = 1;

                          int func() {
                              return b;
                          }
                      }
                  }
              """
          )
        );
    }

    @Test
    void notInitializedByClassIgnored() {
        rewriteRun(
          java(
            """
              class A {
                  private int a;
                  void func() {
                      a = 1;
                  }
              }
              """
          )
        );
    }
}
