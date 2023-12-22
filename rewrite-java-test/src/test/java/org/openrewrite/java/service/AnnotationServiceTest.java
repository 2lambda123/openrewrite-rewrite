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
package org.openrewrite.java.service;

import org.junit.jupiter.api.Test;
import org.openrewrite.test.RewriteTest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.openrewrite.java.Assertions.java;

public class AnnotationServiceTest implements RewriteTest {

    @Test
    void classAnnotations() {
        rewriteRun(
          java(
            """
              import javax.annotation.processing.Generated;
              
              @SuppressWarnings("all")
              public @Generated("foo") class T {}
              """,
            spec -> spec.afterRecipe(cu -> {
                AnnotationService service = cu.service(AnnotationService.class);
                assertThat(service.getAllAnnotations(cu)).isEmpty();
                assertThat(service.getAllAnnotations(cu.getClasses().get(0))).satisfiesExactly(
                  ann0 -> assertThat(ann0.getSimpleName()).isEqualTo("SuppressWarnings"),
                  ann1 -> assertThat(ann1.getSimpleName()).isEqualTo("Generated")
                );
            })
          )
        );
    }
}
