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
package org.openrewrite.java.recipes;

import org.junit.jupiter.api.Test;
import org.openrewrite.java.JavaParser;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;
import org.openrewrite.test.TypeValidation;

import static org.openrewrite.java.Assertions.java;

public class MigrateJavaTemplateToRewrite8Test implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(new MigrateJavaTemplateToRewrite8())
          .parser(JavaParser.fromJavaVersion()
            .classpath(JavaParser.runtimeClasspath())
          )
          .typeValidationOptions(TypeValidation.none());
    }

    @SuppressWarnings("all")
    @Test
    void regular() {
        rewriteRun(
          java(
            """
              package org.openrewrite.java;

              import lombok.EqualsAndHashCode;
              import lombok.Value;
              import org.openrewrite.ExecutionContext;
              import org.openrewrite.Recipe;
              import org.openrewrite.java.tree.Expression;
              import org.openrewrite.java.tree.J;
              import java.util.List;

              @Value
              @EqualsAndHashCode(callSuper = true)
              public class AddOrUpdateAnnotationAttribute extends Recipe {
                  @Override
                  public String getDisplayName() {
                      return "Add or update annotation attribute";
                  }

                  @Override
                  public String getDescription() {
                      return "Add or update annotation attribute.";
                  }

                  @Override
                  public TreeVisitor<?, ExecutionContext> getVisitor() {
                      return new JavaIsoVisitor<ExecutionContext>() {
                          @Override
                          public J.Annotation visitAnnotation(J.Annotation a, ExecutionContext context) {
                              String param1 = "test parameter 1";
                              String param2 = "test parameter 2";
                              List<Expression> currentArgs = a.getArguments();
                              if (currentArgs == null || currentArgs.isEmpty()) {
                                  return a.withTemplate(
                                      JavaTemplate.builder(this::getCursor, "#{}")
                                          .build(),
                                      a.getCoordinates().replaceArguments(),
                                      param1,
                                      param2);
                              }
                              return a;
                          }
                      };
                  }
              }
              """,
            """
              package org.openrewrite.java;

              import lombok.EqualsAndHashCode;
              import lombok.Value;
              import org.openrewrite.ExecutionContext;
              import org.openrewrite.Recipe;
              import org.openrewrite.java.tree.Expression;
              import org.openrewrite.java.tree.J;
              import java.util.List;

              @Value
              @EqualsAndHashCode(callSuper = true)
              public class AddOrUpdateAnnotationAttribute extends Recipe {
                  @Override
                  public String getDisplayName() {
                      return "Add or update annotation attribute";
                  }

                  @Override
                  public String getDescription() {
                      return "Add or update annotation attribute.";
                  }

                  @Override
                  public TreeVisitor<?, ExecutionContext> getVisitor() {
                      return new JavaIsoVisitor<ExecutionContext>() {
                          @Override
                          public J.Annotation visitAnnotation(J.Annotation a, ExecutionContext context) {
                              String param1 = "test parameter 1";
                              String param2 = "test parameter 2";
                              List<Expression> currentArgs = a.getArguments();
                              if (currentArgs == null || currentArgs.isEmpty()) {
                                  return JavaTemplate.builder("#{}")/*[Rewrite8 migration]`contextSensitive()` could be unnecessary and can be removed, please double-check manually*/.contextSensitive()
                                          .build().apply(/*[Rewrite8 migration] please double-check correctness of this parameter manually, it could be updateCursor() if the value is updated somewhere*/getCursor(),
                                          a.getCoordinates().replaceArguments(),
                                          param1,
                                          param2);
                              }
                              return a;
                          }
                      };
                  }
              }
              """
          )
        );
    }

    @SuppressWarnings("all")
    @Test
    void templaveVariable() {
        rewriteRun(
          java(
            """
              package org.openrewrite.java;

              import lombok.EqualsAndHashCode;
              import lombok.Value;
              import org.openrewrite.ExecutionContext;
              import org.openrewrite.Recipe;
              import org.openrewrite.java.tree.Expression;
              import org.openrewrite.java.tree.J;
              import java.util.List;

              @Value
              @EqualsAndHashCode(callSuper = true)
              public class AddOrUpdateAnnotationAttribute extends Recipe {
                  @Override
                  public String getDisplayName() {
                      return "Add or update annotation attribute";
                  }

                  @Override
                  public String getDescription() {
                      return "Add or update annotation attribute.";
                  }

                  @Override
                  public TreeVisitor<?, ExecutionContext> getVisitor() {
                      return new JavaIsoVisitor<ExecutionContext>() {
                          @Override
                          public J.Annotation visitAnnotation(J.Annotation a, ExecutionContext context) {
                              String param1 = "test parameter 1";
                              String param2 = "test parameter 2";
                              List<Expression> currentArgs = a.getArguments();
                              if (currentArgs == null || currentArgs.isEmpty()) {
                                  JavaTemplate t = JavaTemplate.builder(this::getCursor, "#{}")
                                          .build();
                                  return a.withTemplate(t,
                                      a.getCoordinates().replaceArguments(),
                                      param1,
                                      param2);
                              }
                              return a;
                          }
                      };
                  }
              }
              """,
            """
              package org.openrewrite.java;

              import lombok.EqualsAndHashCode;
              import lombok.Value;
              import org.openrewrite.ExecutionContext;
              import org.openrewrite.Recipe;
              import org.openrewrite.java.tree.Expression;
              import org.openrewrite.java.tree.J;
              import java.util.List;

              @Value
              @EqualsAndHashCode(callSuper = true)
              public class AddOrUpdateAnnotationAttribute extends Recipe {
                  @Override
                  public String getDisplayName() {
                      return "Add or update annotation attribute";
                  }

                  @Override
                  public String getDescription() {
                      return "Add or update annotation attribute.";
                  }

                  @Override
                  public TreeVisitor<?, ExecutionContext> getVisitor() {
                      return new JavaIsoVisitor<ExecutionContext>() {
                          @Override
                          public J.Annotation visitAnnotation(J.Annotation a, ExecutionContext context) {
                              String param1 = "test parameter 1";
                              String param2 = "test parameter 2";
                              List<Expression> currentArgs = a.getArguments();
                              if (currentArgs == null || currentArgs.isEmpty()) {
                                  JavaTemplate t = JavaTemplate.builder( "#{}")/*[Rewrite8 migration]`contextSensitive()` could be unnecessary and can be removed, please double-check manually*/.contextSensitive()
                                          .build();
                                  return t.apply(/*[Rewrite8 migration] please double-check correctness of this parameter manually, it could be updateCursor() if the value is updated somewhere*/getCursor(),
                                          a.getCoordinates().replaceArguments(),
                                          param1,
                                          param2);
                              }
                              return a;
                          }
                      };
                  }
              }
              """
          )
        );
    }
}
