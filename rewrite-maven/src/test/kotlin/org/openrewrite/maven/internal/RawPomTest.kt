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
package org.openrewrite.maven.internal

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.openrewrite.maven.tree.ProfileActivation
import kotlin.streams.toList

class RawPomTest {
    @Test
    fun profileActivationByJdk() {
        assertThat(ProfileActivation(false, "11", null).isActive).isTrue()
        assertThat(ProfileActivation(false, "[,12)", null).isActive).isTrue()
        assertThat(ProfileActivation(false, "[,11]", null).isActive).isFalse()
    }

    @Test
    fun profileActivationByAbsenceOfProperty() {
        assertThat(ProfileActivation(false, null,
            ProfileActivation.Property("!inactive", null)).isActive).isTrue()
    }

    @Test
    fun repositoriesSerializationAndDeserialization() {
        val pom = RawPom.parse(
            """
                <project>
                  `<modelVersion>4.0.0</modelVersion>
                 
                  <groupId>com.mycompany.app</groupId>
                  <artifactId>my-app</artifactId>
                  <version>1</version>
                  
                  <repositories>
                    <repository>
                        <id>spring-milestones</id>
                        <name>Spring Milestones</name>
                        <url>http://repo.spring.io/milestone</url>
                    </repository>
                  </repositories>
                </project>
            """.trimIndent().byteInputStream(), null)

        assertThat(pom.repositories).hasSize(1)
    }

    @Suppress("UNCHECKED_CAST")
    @Test
    fun deserializePom() {
        val pomString = """
            <project>
                <modelVersion>4.0.0</modelVersion>
            
                <parent>
                    <groupId>org.springframework.boot</groupId>
                    <artifactId>spring-boot-starter-parent</artifactId>
                    <version>2.4.0</version>
                </parent>
            
                <groupId>com.mycompany.app</groupId>
                <artifactId>my-app</artifactId>
                <version>1</version>
                <packaging>jar</packaging>
                
                <dependencyManagement>
                    <dependencies>
                        <dependency>
                            <groupId>org.springframework.cloud</groupId>
                            <artifactId>spring-cloud-dependencies</artifactId>
                            <version>Greenwich.SR6</version>
                            <type>pom</type>
                            <scope>import</scope>
                        </dependency>
                    </dependencies>
                </dependencyManagement>
                
                <dependencies>
                  <dependency>
                    <groupId>org.junit.jupiter</groupId>
                    <artifactId>junit-jupiter-api</artifactId>
                    <version>5.7.0</version>
                    <scope>test</scope>
                    <exclusions>
                        <exclusion>
                          <groupId>com.google.guava</groupId>
                          <artifactId>guava</artifactId>
                        </exclusion>
                    </exclusions>
                  </dependency>
                </dependencies>

                <build>
                    <plugins>
                        <plugin>
                            <groupId>org.apache.maven.plugins</groupId>
                            <artifactId>maven-surefire-plugin</artifactId>
                            <version>2.22.1</version>
                            <configuration>
                                <includes>
                                        <include>**/*Tests.java</include>
                                        <include>**/*Test.java</include>
                                </includes>
                                <excludes>
                                        <exclude>**/Abstract*.java</exclude>
                                </excludes>
                                <!-- see https://stackoverflow.com/questions/18107375/getting-skipping-jacoco-execution-due-to-missing-execution-data-file-upon-exec -->
                                <argLine>hello</argLine>
                            </configuration>
                        </plugin>
                        <plugin>
                            <groupId>org.jacoco</groupId>
                            <artifactId>jacoco-maven-plugin</artifactId>
                            <executions>
                                <execution>
                                    <id>agent</id>
                                    <goals>
                                        <goal>prepare-agent</goal>
                                    </goals>
                                </execution>
                                <execution>
                                        <id>report</id>
                                        <phase>test</phase>
                                        <goals>
                                            <goal>report</goal>
                                        </goals>
                                </execution>
                            </executions>
                        </plugin>
                    </plugins>
                    <pluginManagement>
                        <plugins>
                            <plugin>
                                <groupId>org.openrewrite.maven</groupId>
                                <artifactId>rewrite-maven-plugin</artifactId>
                                <version>4.22.2</version>
                                <configuration>
                                    <activeRecipes>
                                        <recipe>org.openrewrite.java.format.AutoFormat</recipe>
                                        <recipe>com.yourorg.VetToVeterinary</recipe>
                                        <recipe>org.openrewrite.java.spring.boot2.SpringBoot2JUnit4to5Migration</recipe>
                                    </activeRecipes>
                                </configuration>
                                <dependencies>
                                    <dependency>
                                        <groupId>org.openrewrite.recipe</groupId>
                                        <artifactId>rewrite-spring</artifactId>
                                        <version>4.19.3</version>
                                    </dependency>
                                </dependencies>
                            </plugin>
                        </plugins>
                    </pluginManagement>
                </build>
                <licenses>
                  <license>
                    <name>Apache License, Version 2.0</name>
                    <url>https://www.apache.org/licenses/LICENSE-2.0.txt</url>
                    <distribution>repo</distribution>
                    <comments>A business-friendly OSS license</comments>
                  </license>
                </licenses>
                
                <repositories>
                  <repository>
                    <releases>
                      <enabled>false</enabled>
                      <updatePolicy>always</updatePolicy>
                      <checksumPolicy>warn</checksumPolicy>
                    </releases>
                    <snapshots>
                      <enabled>true</enabled>
                      <updatePolicy>never</updatePolicy>
                      <checksumPolicy>fail</checksumPolicy>
                    </snapshots>
                    <name>Nexus Snapshots</name>
                    <id>snapshots-repo</id>
                    <url>https://oss.sonatype.org/content/repositories/snapshots</url>
                    <layout>default</layout>
                  </repository>
                </repositories>
                
                <profiles>
                    <profile>
                        <id>java9+</id>
                        <activation>
                            <jdk>[9,)</jdk>
                        </activation>
                        <dependencies>
                            <dependency>
                                <groupId>javax.xml.bind</groupId>
                                <artifactId>jaxb-api</artifactId>
                            </dependency>
                        </dependencies>
                    </profile>
                    <profile>
                        <id>java11+</id>
                        <dependencies>
                        </dependencies>
                    </profile>
                    <profile>
                        <id>plugin-stuff</id>
                        <build>
                            <plugins>
                                <plugin>
                                    <groupId>org.apache.maven.plugins</groupId>
                                    <artifactId>maven-surefire-plugin</artifactId>
                                    <version>2.22.1</version>
                                    <configuration>
                                        <includes>
                                                <include>**/*Tests.java</include>
                                                <include>**/*Test.java</include>
                                        </includes>
                                        <excludes>
                                                <exclude>**/Abstract*.java</exclude>
                                        </excludes>
                                        <!-- see https://stackoverflow.com/questions/18107375/getting-skipping-jacoco-execution-due-to-missing-execution-data-file-upon-exec -->
                                        <argLine>hello</argLine>
                                    </configuration>
                                </plugin>
                                <plugin>
                                    <groupId>org.jacoco</groupId>
                                    <artifactId>jacoco-maven-plugin</artifactId>
                                    <executions>
                                        <execution>
                                            <id>agent</id>
                                            <goals>
                                                <goal>prepare-agent</goal>
                                            </goals>
                                        </execution>
                                        <execution>
                                                <id>report</id>
                                                <phase>test</phase>
                                                <goals>
                                                    <goal>report</goal>
                                                </goals>
                                        </execution>
                                    </executions>
                                </plugin>
                            </plugins>
                            <pluginManagement>
                                <plugins>
                                    <plugin>
                                        <groupId>org.openrewrite.maven</groupId>
                                        <artifactId>rewrite-maven-plugin</artifactId>
                                        <version>4.22.2</version>
                                        <configuration>
                                            <activeRecipes>
                                                <recipe>org.openrewrite.java.format.AutoFormat</recipe>
                                                <recipe>com.yourorg.VetToVeterinary</recipe>
                                                <recipe>org.openrewrite.java.spring.boot2.SpringBoot2JUnit4to5Migration</recipe>
                                            </activeRecipes>
                                        </configuration>
                                        <dependencies>
                                            <dependency>
                                                <groupId>org.openrewrite.recipe</groupId>
                                                <artifactId>rewrite-spring</artifactId>
                                                <version>4.19.3</version>
                                            </dependency>
                                        </dependencies>
                                    </plugin>
                                </plugins>
                            </pluginManagement>
                        </build>

                    </profile>
                </profiles>
            </project>
        """.trimIndent()

        val model = MavenXmlMapper.readMapper().readValue(pomString, RawPom::class.java).toPom(null, null)
        assertThat(model.parent!!.groupId).isEqualTo("org.springframework.boot")

        assertThat(model.packaging).isEqualTo("jar")

        assertThat(model.dependencies.get(0)?.groupId)
            .isEqualTo("org.junit.jupiter")

        assertThat(model.dependencies.get(0)?.exclusions!!.first().groupId)
            .isEqualTo("com.google.guava")

        assertThat(model.dependencyManagement.first()?.groupId)
            .isEqualTo("org.springframework.cloud")

        assertThat(model.plugins).hasSize(2)
        var surefirePlugin = model.plugins.stream()
            .filter { p -> p.artifactId.equals("maven-surefire-plugin") }!!.toList()[0]
        var jacocoPlugin = model.plugins.stream()
            .filter { p -> p.artifactId.equals("jacoco-maven-plugin") }!!.toList()[0]

        assertThat(jacocoPlugin.executions).hasSize(2)

        var rewritePlugin = model.pluginManagement.stream()
            .filter { p -> p.artifactId.equals("rewrite-maven-plugin") }!!.toList()[0]

        assertThat(rewritePlugin.dependencies).hasSize(1)
        assertThat(rewritePlugin.dependencies[0].groupId).isEqualTo("org.openrewrite.recipe")
        assertThat(rewritePlugin.dependencies[0].artifactId).isEqualTo("rewrite-spring")
        assertThat(rewritePlugin.dependencies[0].artifactId).isEqualTo("rewrite-spring")
        assertThat(rewritePlugin.dependencies[0].version).isEqualTo("4.19.3")

        var activeRecipes = rewritePlugin.configuration["activeRecipes"] as List<String>
        assertThat(activeRecipes).contains(
            "org.openrewrite.java.format.AutoFormat",
            "com.yourorg.VetToVeterinary",
            "org.openrewrite.java.spring.boot2.SpringBoot2JUnit4to5Migration"
        )

        assertThat(model.licenses.first()?.name)
            .isEqualTo("Apache License, Version 2.0")

        assertThat(model.repositories.first()?.uri)
            .isEqualTo("https://oss.sonatype.org/content/repositories/snapshots")
        val java9Profile = model.profiles.stream().filter { p -> p.id!!.equals("java9+") }!!.toList()[0]
        val java11Profile = model.profiles.stream().filter { p -> p.id!!.equals("java11+") }!!.toList()[0]
        assertThat(java9Profile.dependencies[0].groupId).isEqualTo("javax.xml.bind")
        assertThat(java11Profile.dependencies).isEmpty()

        val rewriteProfile = model.profiles.stream().filter { p -> p.id!!.equals("plugin-stuff") }!!.toList()[0]

        assertThat(rewriteProfile.plugins).hasSize(2)
        surefirePlugin = rewriteProfile.plugins.stream()
            .filter { p -> p.artifactId.equals("maven-surefire-plugin") }!!.toList()[0]
        jacocoPlugin = rewriteProfile.plugins.stream()
            .filter { p -> p.artifactId.equals("jacoco-maven-plugin") }!!.toList()[0]

        assertThat(jacocoPlugin.executions).hasSize(2)

        rewritePlugin = rewriteProfile.pluginManagement.stream()
            .filter { p -> p.artifactId.equals("rewrite-maven-plugin") }!!.toList()[0]

        assertThat(rewritePlugin.dependencies).hasSize(1)
        assertThat(rewritePlugin.dependencies[0].groupId).isEqualTo("org.openrewrite.recipe")
        assertThat(rewritePlugin.dependencies[0].artifactId).isEqualTo("rewrite-spring")
        assertThat(rewritePlugin.dependencies[0].artifactId).isEqualTo("rewrite-spring")
        assertThat(rewritePlugin.dependencies[0].version).isEqualTo("4.19.3")

        activeRecipes = rewritePlugin.configuration.get("activeRecipes") as List<String>
        assertThat(activeRecipes).contains(
            "org.openrewrite.java.format.AutoFormat",
            "com.yourorg.VetToVeterinary",
            "org.openrewrite.java.spring.boot2.SpringBoot2JUnit4to5Migration"
        )


    }
}
