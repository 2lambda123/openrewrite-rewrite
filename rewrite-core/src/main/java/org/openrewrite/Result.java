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
package org.openrewrite;

import lombok.Getter;
import org.eclipse.jgit.diff.DiffEntry;
import org.eclipse.jgit.diff.DiffFormatter;
import org.eclipse.jgit.internal.storage.dfs.DfsRepositoryDescription;
import org.eclipse.jgit.internal.storage.dfs.InMemoryRepository;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.FileMode;
import org.eclipse.jgit.lib.ObjectInserter;
import org.openrewrite.config.RecipeDescriptor;
import org.openrewrite.internal.lang.Nullable;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.lang.ref.WeakReference;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

public class Result {
    /**
     * Possible {@code null} if a new file is being created.
     */
    @Getter
    @Nullable
    private final SourceFile before;

    /**
     * Possibly {@code null} if the change results in the file being deleted.
     */
    @Getter
    @Nullable
    private final SourceFile after;

    @Getter
    private final Collection<Stack<Recipe>> recipes;

    @Getter
    @Nullable
    private final Duration timeSavings;

    @Nullable
    private transient WeakReference<String> diff;

    @Nullable
    private Path relativeTo;

    @Deprecated
    public Set<Recipe> getRecipesThatMadeChanges() {
        return recipes.stream().map(Stack::peek).collect(Collectors.toSet());
    }

    /**
     * Return a list of recipes that have made changes as a hierarchy of descriptors.
     * The method transforms the flat, stack-based representation into descriptors where children are grouped under their common parents.
     */
    @Incubating(since = "7.22.0")
    public List<RecipeDescriptor> getRecipeDescriptorsThatMadeChanges() {
        List<RecipeDescriptor> recipesToDisplay = new ArrayList<>();

        for (Stack<Recipe> currentStack : recipes) {
            // The first recipe is an Environment.CompositeRecipe and should not be included in the list of RecipeDescriptors
            Recipe root = currentStack.get(1);
            RecipeDescriptor rootDescriptor = root.getDescriptor().withRecipeList(new ArrayList<>());

            RecipeDescriptor index;
            if (recipesToDisplay.contains(rootDescriptor)) {
                index = recipesToDisplay.get(recipesToDisplay.indexOf(rootDescriptor));
            } else {
                recipesToDisplay.add(rootDescriptor);
                index = rootDescriptor;
            }

            for (int i = 2; i < currentStack.size(); i++) {
                RecipeDescriptor nextDescriptor = currentStack.get(i).getDescriptor().withRecipeList(new ArrayList<>());
                if (index.getRecipeList().contains(nextDescriptor)) {
                    index = index.getRecipeList().get(index.getRecipeList().indexOf(nextDescriptor));
                } else {
                    index.getRecipeList().add(nextDescriptor);
                    index = nextDescriptor;
                }
            }
        }
        return recipesToDisplay;
    }

    public Result(@Nullable SourceFile before, @Nullable SourceFile after, Collection<Stack<Recipe>> recipes) {
        this.before = before;
        this.after = after;
        this.recipes = recipes;

        Duration timeSavings = null;
        for (Stack<Recipe> recipesStack : recipes) {
            Duration perOccurrence = recipesStack.peek().getEstimatedEffortPerOccurrence();
            if(perOccurrence != null) {
                timeSavings = timeSavings == null ? perOccurrence : timeSavings.plus(perOccurrence);
            }
        }

        this.timeSavings = timeSavings;
    }

    /**
     * @return Git-style patch diff representing the changes to this compilation unit.
     */
    public String diff() {
        return diff(null);
    }

    /**
     * @param relativeTo  Optional relative path that is used to relativize file paths of reported differences.
     * @return Git-style patch diff representing the changes to this compilation unit.
     */
    public String diff(@Nullable Path relativeTo) {
        String d;
        if (this.diff == null) {
            d = computeDiff(relativeTo);
            this.diff = new WeakReference<>(d);
        } else {
            d = this.diff.get();
            if (d == null || !Objects.equals(this.relativeTo, relativeTo)) {
                d = computeDiff(relativeTo);
                this.diff = new WeakReference<>(d);
            }
        }
        return d;
    }

    private String computeDiff(@Nullable Path relativeTo) {
        Path sourcePath;
        if (after != null) {
            sourcePath = after.getSourcePath();
        } else if (before != null) {
            sourcePath = before.getSourcePath();
        } else {
            sourcePath = (relativeTo == null ? Paths.get(".") : relativeTo).resolve("partial-" + System.nanoTime());
        }

        Path originalSourcePath = sourcePath;
        if (before != null && after != null && !before.getSourcePath().equals(after.getSourcePath())) {
            originalSourcePath = before.getSourcePath();
        }

        try (InMemoryDiffEntry diffEntry = new InMemoryDiffEntry(
                originalSourcePath,
                sourcePath,
                relativeTo,
                before == null ? "" : before.printAll(),
                after == null ? "" : after.printAll(),
                recipes.stream().map(Stack::peek).collect(Collectors.toSet())
        )) {
            this.relativeTo = relativeTo;
            return diffEntry.getDiff();
        }
    }

    @Override
    public String toString() {
        return diff();
    }

    static class InMemoryDiffEntry extends DiffEntry implements AutoCloseable {
        private final InMemoryRepository repo;
        private final Set<Recipe> recipesThatMadeChanges;

        InMemoryDiffEntry(Path originalFilePath, Path filePath, @Nullable Path relativeTo, String oldSource,
                          String newSource, Set<Recipe> recipesThatMadeChanges) {
            this.changeType = originalFilePath.equals(filePath) ? ChangeType.MODIFY : ChangeType.RENAME;
            this.recipesThatMadeChanges = recipesThatMadeChanges;

            this.oldPath = (relativeTo == null ? originalFilePath : relativeTo.relativize(originalFilePath)).toString().replace("\\", "/");
            this.newPath = (relativeTo == null ? filePath : relativeTo.relativize(filePath)).toString().replace("\\", "/");

            try {
                this.repo = new InMemoryRepository.Builder()
                        .setRepositoryDescription(new DfsRepositoryDescription())
                        .build();

                ObjectInserter inserter = repo.getObjectDatabase().newInserter();
                oldId = inserter.insert(Constants.OBJ_BLOB, oldSource.getBytes(StandardCharsets.UTF_8)).abbreviate(40);
                newId = inserter.insert(Constants.OBJ_BLOB, newSource.getBytes(StandardCharsets.UTF_8)).abbreviate(40);
                inserter.flush();

                oldMode = FileMode.REGULAR_FILE;
                newMode = FileMode.REGULAR_FILE;
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }

        String getDiff() {
            if (oldId.equals(newId) && oldPath.equals(newPath)) {
                return "";
            }

            ByteArrayOutputStream patch = new ByteArrayOutputStream();
            try (DiffFormatter formatter = new DiffFormatter(patch)) {
                formatter.setRepository(repo);
                formatter.format(this);
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }

            String diff = patch.toString();

            AtomicBoolean addedComment = new AtomicBoolean(false);
            // NOTE: String.lines() would remove empty lines which we don't want
            return Arrays.stream(diff.split("\n"))
                    .map(l -> {
                        if (!addedComment.get() && l.startsWith("@@") && l.endsWith("@@")) {
                            addedComment.set(true);

                            Set<String> sortedRecipeNames = new LinkedHashSet<>();
                            for (Recipe recipesThatMadeChange : recipesThatMadeChanges) {
                                sortedRecipeNames.add(recipesThatMadeChange.getName());
                            }
                            StringJoiner joinedRecipeNames = new StringJoiner(", ", " ", "");
                            for (String name : sortedRecipeNames) {
                                joinedRecipeNames.add(name);
                            }

                            return l + joinedRecipeNames.toString();
                        }
                        return l;
                    })
                    .collect(Collectors.joining("\n")) + "\n";
        }

        @Override
        public void close() {
            this.repo.close();
        }
    }
}
