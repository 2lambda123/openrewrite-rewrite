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
package org.openrewrite.config;

import org.openrewrite.Recipe;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

import static java.util.Collections.emptyList;

/**
 * A recipe that exists only to wrap other recipes.
 * Anonymous recipe classes aren't serializable/deserializable so use this, or another named type, instead
 */
public class CompositeRecipe extends Recipe {

    @Override
    public String getDisplayName() {
        return getName();
    }

    @Override
    public Duration getEstimatedEffortPerOccurrence() {
        Duration total = Duration.ofMinutes(0);
        for (Recipe recipe : getRecipeList()) {
            if (recipe.getEstimatedEffortPerOccurrence() != null) {
                total = total.plus(recipe.getEstimatedEffortPerOccurrence());
            }
        }

        if (total.getSeconds() == 0) {
            return Duration.ofMinutes(5);
        }

        return total;
    }

    @Override
    public List<DataTableDescriptor> getDataTableDescriptors() {
        List<DataTableDescriptor> dataTableDescriptors = null;
        for (Recipe recipe : getRecipeList()) {
            List<DataTableDescriptor> dtd = recipe.getDataTableDescriptors();
            if (!dtd.isEmpty()) {
                if (dataTableDescriptors == null) {
                    dataTableDescriptors = new ArrayList<>();
                }
                dataTableDescriptors.addAll(dtd);
            }
        }
        return dataTableDescriptors == null ? emptyList() : dataTableDescriptors;
    }
}
