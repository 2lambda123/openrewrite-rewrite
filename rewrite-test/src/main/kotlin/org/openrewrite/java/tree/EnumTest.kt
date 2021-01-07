/*
 * Copyright 2020 the original author or authors.
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
package org.openrewrite.java.tree

import org.junit.jupiter.api.Test
import org.openrewrite.java.JavaParser
import org.openrewrite.java.JavaTreeTest
import org.openrewrite.java.JavaTreeTest.NestingLevel.CompilationUnit

interface EnumTest : JavaTreeTest {

    @Test
    fun anonymousClassInitializer(jp: JavaParser) = assertParsePrintAndProcess(
        jp, CompilationUnit, """
            public enum A {
                A1(1) {
                    @Override
                    void foo() {}
                },

                A2 {
                    @Override
                    void foo() {}
                };
                
                A() {}
                A(int n) {}
                
                abstract void foo();
            }
        """
    )

    @Test
    fun enumConstructor(jp: JavaParser) = assertParsePrintAndProcess(
        jp, CompilationUnit, """
            public class Outer {
                public enum A {
                    A1(1);
    
                    A(int n) {}
                }
                
                private static final class ContextFailedToStart {
                    private static Object[] combineArguments(String context, Throwable ex, Object[] arguments) {
                        return new Object[arguments.length + 2];
                    }
                }
            }
        """
    )

    @Test
    fun noArguments(jp: JavaParser) = assertParsePrintAndProcess(
        jp, CompilationUnit, """
            public enum A {
                A1, A2();
            }
        """
    )

    @Test
    fun enumWithParameters(jp: JavaParser) = assertParsePrintAndProcess(
        jp, CompilationUnit, """
            public enum A {
                ONE(1),
                TWO(2);
            
                A(int n) {}
            }
        """
    )

    @Test
    fun enumWithoutParameters(jp: JavaParser) = assertParsePrintAndProcess(
        jp, CompilationUnit, "public enum A { ONE, TWO }"
    )

    @Test
    fun enumUnnecessarilyTerminatedWithSemicolon(jp: JavaParser) = assertParsePrintAndProcess(
        jp, CompilationUnit, "public enum A { ONE ; }"
    )

    @Test
    fun enumWithEmptyParameters(jp: JavaParser) = assertParsePrintAndProcess(
        jp, CompilationUnit, "public enum A { ONE ( ), TWO ( ) }"
    )
}
