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
package org.openrewrite.java.cleanup

import org.junit.jupiter.api.Test
import org.openrewrite.Recipe
import org.openrewrite.java.JavaParser
import org.openrewrite.java.JavaRecipeTest
import org.openrewrite.test.JavaTestingSupport

interface AddSerialVersionUidToSerializableTest : JavaTestingSupport {
    override val recipe: Recipe?
        get() = AddSerialVersionUidToSerializable()

    override val javaParser: JavaParser
        get() = JavaParser.fromJavaVersion()
            .logCompilationWarningsAndErrors(true)
            .build()

    @Test
    fun doNothingNotSerializable() = assertUnchanged(
        before = javaParser.parse(source="""
            public class Example {
                private String fred;
                private int numberOfFreds;
            }
        """)
    )

    @Test
    fun addSerialVersionUID() = assertChanged(
        before = javaParser.parse(source="""
            import java.io.Serializable;
                        
            public class Example implements Serializable {
                private String fred;
                private int numberOfFreds;
            }
        """),
        after = """
            import java.io.Serializable;
                        
            public class Example implements Serializable {
                private static final long serialVersionUID = 1;
                private String fred;
                private int numberOfFreds;
            }
        """.trimIndent()
    )

    @Test
    fun fixSerialVersionUIDModifiers() = assertChanged(
        before = javaParser.parse(source="""
            import java.io.Serializable;
                        
            public class Example implements Serializable {
                private final long serialVersionUID = 1;
                private String fred;
                private int numberOfFreds;
            }
        """),
        after = """
            import java.io.Serializable;
                        
            public class Example implements Serializable {
                private static final long serialVersionUID = 1;
                private String fred;
                private int numberOfFreds;
            }
        """.trimIndent()
    )

    @Test
    fun fixSerialVersionUIDNoModifiers() = assertChanged(
        before = javaParser.parse(source="""
            import java.io.Serializable;
                        
            public class Example implements Serializable {
                long serialVersionUID = 1;
                private String fred;
                private int numberOfFreds;
            }
        """),
        after = """
            import java.io.Serializable;
                        
            public class Example implements Serializable {
                private static final long serialVersionUID = 1;
                private String fred;
                private int numberOfFreds;
            }
        """.trimIndent()
    )

    @Test
    fun fixSerialVersionUIDNoModifiersWrongType() = assertChanged(
        before = javaParser.parse(source="""
            import java.io.Serializable;

            public class Example implements Serializable {
                Long serialVersionUID = 1L;
                private String fred;
                private int numberOfFreds;
            }
        """),
        after = """
            import java.io.Serializable;

            public class Example implements Serializable {
                private static final long serialVersionUID = 1L;
                private String fred;
                private int numberOfFreds;
            }
        """
    )

    @Test
    fun uidAlreadyPresent() = assertUnchanged(
        before = javaParser.parse(source="""
            import java.io.Serializable;
                        
            public class Example implements Serializable {
                private static final long serialVersionUID = 1;
                private String fred;
                private int numberOfFreds;
            }
        """)
    )

    @Test
    fun methodDeclarationsAreNotVisited() = assertChanged(
        before = javaParser.parse(source="""
            import java.io.Serializable;
                        
            public class Example implements Serializable {
                private String fred;
                private int numberOfFreds;
                void doSomething() {
                    int serialVersionUID = 1;
                }
            }
        """),
        after = """
            import java.io.Serializable;
                        
            public class Example implements Serializable {
                private static final long serialVersionUID = 1;
                private String fred;
                private int numberOfFreds;
                void doSomething() {
                    int serialVersionUID = 1;
                }
            }
        """
    )

    @Test
    fun doNotAlterAnInterface() = assertUnchanged(
        before = javaParser.parse(source="""
            import java.io.Serializable;
                        
            public interface Example extends Serializable {
            }
        """)
    )

    @Test
    fun doNotAlterAnException() = assertUnchanged(
        before = javaParser.parse(source="""
            import java.io.Serializable;
                        
            public class MyException extends Exception implements Serializable {
            }
        """)
    )

    @Test
    fun doNotAlterARuntimeException() = assertUnchanged(
        before = javaParser.parse(source="""
            import java.io.Serializable;
                        
            public class MyException extends RuntimeException implements Serializable {
            }
        """)
    )
}
