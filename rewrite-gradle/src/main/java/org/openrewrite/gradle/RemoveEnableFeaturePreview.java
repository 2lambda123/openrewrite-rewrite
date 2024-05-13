package org.openrewrite.gradle;

import java.util.List;
import lombok.EqualsAndHashCode;
import lombok.Value;
import org.jetbrains.annotations.NotNull;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Option;
import org.openrewrite.Preconditions;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.groovy.GroovyIsoVisitor;
import org.openrewrite.java.MethodMatcher;
import org.openrewrite.java.tree.Expression;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.J.Literal;
import org.openrewrite.java.tree.J.MethodInvocation;

@Value
@EqualsAndHashCode(callSuper = false)
public class RemoveEnableFeaturePreview extends Recipe {

  @Option(displayName = "The feature preview name",
      description = "The name of the feature preview to remove.",
      example = "ONE_LOCKFILE_PER_PROJECT"
  )
  public String previewFeatureName;

  @Override
  public @NotNull String getDisplayName() {
    return "Remove an enabled Gradle preview feature";
  }

  @Override
  public @NotNull String getDescription() {
    return "Remove an enabled Gradle preview feature from `settings.gradle`.";
  }

  @Override
  public TreeVisitor<?, ExecutionContext> getVisitor() {
    return Preconditions.check(
        new IsSettingsGradle<>(),
        new RemoveEnableFeaturePreviewVisitor());
  }

  public class RemoveEnableFeaturePreviewVisitor extends GroovyIsoVisitor<ExecutionContext> {

    @Override
    public MethodInvocation visitMethodInvocation(MethodInvocation method,
        @NotNull ExecutionContext executionContext) {
      final MethodMatcher methodMatcher =
          new MethodMatcher("org.gradle.api.initialization.Settings enableFeaturePreview(String)");

      if (methodMatcher.matches(method)) {
        List<Expression> arguments = method.getArguments();
        for (Expression argument : arguments) {
          if (argument instanceof J.Literal) {
            String candidatePreviewFeatureName = (String) ((Literal) argument).getValue();
            if (previewFeatureName.equals(candidatePreviewFeatureName)) {
              return null;
            }
          }
        }
      }
      return method;
    }
  }

}
