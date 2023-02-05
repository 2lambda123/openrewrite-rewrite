/*
 * Copyright 2021 the original author or authors.
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

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.java.Assertions.java;
import static org.openrewrite.java.Assertions.version;

@SuppressWarnings({"rawtypes", "unchecked", "RedundantCast", "DataFlowIssue", "ConstantValue", "CastCanBeRemovedNarrowingVariableType", "ClassInitializerMayBeStatic"})
class InstanceOfPatternMatchTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(new InstanceOfPatternMatch());
    }

    @Nested
    class If {

        @SuppressWarnings({"PatternVariableCanBeUsed", "UnnecessaryLocalVariable"})
        @Nested
        class Positive {

            @Test
            void ifConditionWithoutPattern() {
                rewriteRun(
                  version(
                    java(
                      """
                        public class A {
                            void test(Object o) {
                                Object s = 1;
                                if (o instanceof String && ((String) (o)).length() > 0) {
                                    if (((String) o).length() > 1) {
                                        System.out.println(o);
                                    }
                                }
                            }
                        }
                        """,
                      """
                        public class A {
                            void test(Object o) {
                                Object s = 1;
                                if (o instanceof String string && ((String) (o)).length() > 0) {
                                    if (string.length() > 1) {
                                        System.out.println(o);
                                    }
                                }
                            }
                        }
                        """
                    ), 17
                  )
                );
            }

            @Test
            void multipleCasts() {
                rewriteRun(
                  version(
                    java(
                      """
                        public class A {
                            void test(Object o, Object o2) {
                                Object string = 1;
                                if (o instanceof String && o2 instanceof Integer) {
                                    System.out.println((String) o);
                                    System.out.println((Integer) o2);
                                }
                            }
                        }
                        """,
                      """
                        public class A {
                            void test(Object o, Object o2) {
                                Object string = 1;
                                if (o instanceof String string1 && o2 instanceof Integer integer) {
                                    System.out.println(string1);
                                    System.out.println(integer);
                                }
                            }
                        }
                        """
                    ), 17
                  )
                );
            }

            @Test
            void longNames() {
                rewriteRun(
                  version(
                    java(
                      """
                        import java.util.ArrayList;
                        public class A {
                            void test(Object o) {
                                Object list = 1;
                                if (o instanceof ArrayList<?>) {
                                    System.out.println((ArrayList<?>) o);
                                }
                            }
                        }
                        """,
                      """
                        import java.util.ArrayList;
                        public class A {
                            void test(Object o) {
                                Object list = 1;
                                if (o instanceof ArrayList<?> arrayList) {
                                    System.out.println(arrayList);
                                }
                            }
                        }
                        """
                    ), 17
                  )
                );
            }

            @Test
            void matchingVariableInBody() {
                rewriteRun(
                  version(
                    java(
                      """
                        public class A {
                            void test(Object o) {
                                if (o instanceof String) {
                                    String str = (String) o;
                                    String str2 = (String) o;
                                    System.out.println(str + str2);
                                }
                            }
                        }
                        """,
                      """
                        public class A {
                            void test(Object o) {
                                if (o instanceof String str) {
                                    String str2 = str;
                                    System.out.println(str + str2);
                                }
                            }
                        }
                        """
                    ), 17
                  )
                );
            }

            @Test
            void conflictingVariableInBody() {
                rewriteRun(
                  version(
                    java(
                      """
                        public class A {
                            void test(Object o) {
                                if (o instanceof String) {
                                    String string = 'x';
                                    System.out.println((String) o);
                        //            String string1 = "y";
                                }
                            }
                        }
                        """,
                      """
                        public class A {
                            void test(Object o) {
                                if (o instanceof String string1) {
                                    String string = 'x';
                                    System.out.println(string1);
                        //            String string1 = "y";
                                }
                            }
                        }
                        """
                    ), 17
                  )
                );
            }
        }

        @Nested
        class Negative {

            @Test
            void expressionWithSideEffects() {
                rewriteRun(
                  version(
                    java(
                      """
                        public class A {
                            void test(Object o) {
                                Object s = 1;
                                if (convert(o) instanceof String && ((String) convert(o)).length() > 0) {
                                    if (((String) convert(o)).length() > 1) {
                                        System.out.println(o);
                                    }
                                }
                            }
                            Object convert(Object o) {
                                return o;
                            }
                        }
                        """
                    ), 17
                  )
                );
            }

            @Test
            void noTypeCast() {
                rewriteRun(
                  version(
                    java(
                      """
                        public class A {
                            void test(Object o) {
                                if (o instanceof String) {
                                    System.out.println(o);
                                }
                            }
                        }
                         """
                    ), 17)
                );
            }

            @Test
            void typeCastInElse() {
                rewriteRun(
                  version(
                    java(
                      """
                        public class A {
                            void test(Object o) {
                                if (o instanceof String) {
                                    System.out.println(o);
                                } else {
                                    System.out.println((String) s);
                                }
                            }
                        }
                         """
                    ), 17)
                );
            }

            @Test
            void ifConditionWithPattern() {
                rewriteRun(
                  version(
                    java(
                      """
                        public class A {
                            void test(Object o) {
                                if (o instanceof String s && s.length() > 0) {
                                    System.out.println(s);
                                }
                            }
                        }
                         """
                    ), 17)
                );
            }

            @Test
            void orOperationInIfCondition() {
                rewriteRun(
                  version(
                    java(
                      """
                        public class A {
                            void test(Object o) {
                                if (o instanceof String || ((String) o).length() > 0) {
                                    if (((String) o).length() > 1) {
                                        System.out.println(o);
                                    }
                                }
                            }
                        }
                        """
                    ), 17
                  )
                );
            }

            @Test
            void negatedInstanceOfMatchedInElse() {
                rewriteRun(
                  version(
                    java(
                      """
                        public class A {
                            void test(Object o) {
                                if (!(o instanceof String)) {
                                    System.out.println(((String) o).length());
                                } else {
                                    System.out.println(((String) o).length());
                                }
                            }
                        }
                        """
                    ), 17
                  )
                );
            }
        }
    }

    @Nested
    class Ternary {

        @Nested
        class Positive {

            @Test
            void typeCastInTrue() {
                rewriteRun(
                  version(
                    java(
                      """
                        public class A {
                            String test(Object o) {
                                return o instanceof String ? ((String) o).substring(1) : o.toString();
                            }
                        }
                        """,
                      """
                        public class A {
                            String test(Object o) {
                                return o instanceof String s ? s.substring(1) : o.toString();
                            }
                        }
                        """
                    ), 17
                  )
                );
            }

            @Test
            void initBlocks() {
                rewriteRun(
                  version(
                    java(
                      """
                        public class A {
                            static {
                                Object o = null;
                                String s = o instanceof String ? ((String) o).substring(1) : String.valueOf(o);
                            }
                            {
                                Object o = null;
                                String s = o instanceof String ? ((String) o).substring(1) : String.valueOf(o);
                            }
                        }
                        """,
                      """
                        public class A {
                            static {
                                Object o = null;
                                String s = o instanceof String s1 ? s1.substring(1) : String.valueOf(o);
                            }
                            {
                                Object o = null;
                                String s = o instanceof String s1 ? s1.substring(1) : String.valueOf(o);
                            }
                        }
                        """
                    ), 17
                  )
                );
            }
        }

        @Nested
        class Negative {

            @Test
            void typeCastInFalse() {
                rewriteRun(
                  version(
                    java(
                      """
                        public class A {
                            String test(Object o) {
                                return o instanceof String ? o.toString() : ((String) o).substring(1);
                            }
                        }
                        """
                    ), 17
                  )
                );
            }
        }
    }

    @Nested
    class Binary {

        @Nested
        class Positive {

            @Test
            void onlyReplacementsBeforeOrOperator() {
                rewriteRun(
                  version(
                    java(
                      """
                        public class A {
                            boolean test(Object o) {
                                return o instanceof String && ((String) o).length() > 1 || ((String) o).length() > 2;
                            }
                        }
                        """,
                      """
                        public class A {
                            boolean test(Object o) {
                                return o instanceof String s && s.length() > 1 || ((String) o).length() > 2;
                            }
                        }
                        """
                    ), 17
                  )
                );
            }
        }

        @Nested
        class Negative {

            @Test
            void methodCallBreaksFlowScope() {
                rewriteRun(
                  version(
                    java(
                      """
                        public class A {
                            boolean m(Object o) {
                                return test(o instanceof String) && ((String) o).length() > 1;
                            }
                            boolean test(boolean b) {
                                return b;
                            }
                        }
                        """
                    ), 17
                  )
                );
            }
        }
    }

    @Nested
    class Arrays {

        @Nested
        class Positive {

            @Test
            void string() {
                rewriteRun(
                  version(
                    java(
                      """
                        public class A {
                            boolean test(Object o) {
                                return o instanceof String[] && ((java.lang.String[]) o).length > 1 || ((String[]) o).length > 2;
                            }
                        }
                        """,
                      """
                        public class A {
                            boolean test(Object o) {
                                return o instanceof String[] ss && ss.length > 1 || ((String[]) o).length > 2;
                            }
                        }
                        """
                    ), 17
                  )
                );
            }

            @Test
            void primitive() {
                rewriteRun(
                  version(
                    java(
                      """
                        public class A {
                            boolean test(Object o) {
                                return o instanceof int[] && ((int[]) o).length > 1 || ((int[]) o).length > 2;
                            }
                        }
                        """,
                      """
                        public class A {
                            boolean test(Object o) {
                                return o instanceof int[] is && is.length > 1 || ((int[]) o).length > 2;
                            }
                        }
                        """
                    ), 17
                  )
                );
            }

            @Test
            void multiDimensional() {
                rewriteRun(
                  version(
                    java(
                      """
                        public class A {
                            boolean test(Object o) {
                                return o instanceof int[][] && ((int[][]) o).length > 1 || ((int[][]) o).length > 2;
                            }
                        }
                        """,
                      """
                        public class A {
                            boolean test(Object o) {
                                return o instanceof int[][] is && is.length > 1 || ((int[][]) o).length > 2;
                            }
                        }
                        """
                    ), 17
                  )
                );
            }
        }

        @Nested
        class Negative {

            @Test
            void dimensionalMismatch() {
                rewriteRun(
                  version(
                    java(
                      """
                        public class A {
                            boolean test(Object o) {
                                return o instanceof int[][] && ((int[]) o).length > 1;
                            }
                        }
                        """
                    ), 17
                  )
                );
            }
        }
    }

    @Nested
    class Generics {
        @Nested
        class Positive {
            @Test
            void rawInstanceOfAndWildcardParameterizedCast() {
                rewriteRun(
                  version(
                    java(
                      """
                      import java.util.List;
                      public class A {
                          Object test(Object o) {
                              return o instanceof List ? ((List<?>) o).get(0) : o.toString();
                          }
                      }
                      """,
                      """
                      import java.util.List;
                      public class A {
                          Object test(Object o) {
                              return o instanceof List l ? l.get(0) : o.toString();
                          }
                      }
                      """
                    ), 17)
                );
            }
            @Test
            void rawInstanceOfAndObjectParameterizedCast() {
                rewriteRun(
                  version(
                    java(
                      """
                        import java.util.List;
                        public class A {
                            Object test(Object o) {
                                return o instanceof List ? ((List<Object>) o).get(0) : o.toString();
                            }
                        }
                        """,
                      """
                        import java.util.List;
                        public class A {
                            Object test(Object o) {
                                return o instanceof List l ? l.get(0) : o.toString();
                            }
                        }
                        """
                    ), 17)
                );
            }
        }
        @Nested
        class Negative {
            @Test
            void rawInstanceOfAndParameterizedCast() {
                rewriteRun(
                  version(
                    java(
                      """
                        import java.util.List;
                        public class A {
                            String test(Object o) {
                                return o instanceof List ? ((List<String>) o).get(0) : o.toString();
                            }
                        }
                        """
                    ), 17
                  )
                );
            }
            @Test
            void unboundGenericTypeVariable() {
                rewriteRun(
                  version(
                    java(
                      """
                        import java.util.List;
                        public class A<T> {
                            void test(Object t) {
                                if (t instanceof List) {
                                    List<T> l = (List<T>) t;
                                    System.out.println(l.size());
                                }
                            }
                        }
                        """
                    ), 17)
                );
            }
        }
    }

    @Nested
    class Various {
        @Nested
        class Positive {
            @Test
            void unaryWithoutSideEffects() {
                rewriteRun(
                  version(
                    java(
                      """
                        public class A {
                            String test(Object o) {
                                return ((Object) ("1" + ~1)) instanceof String ? ((String) ((Object) ("1" + ~1))).substring(1) : o.toString();
                            }
                        }
                        """,
                      """
                        public class A {
                            String test(Object o) {
                                return ((Object) ("1" + ~1)) instanceof String s ? s.substring(1) : o.toString();
                            }
                        }
                        """
                    ), 17
                  )
                );
            }
        }
    }
}