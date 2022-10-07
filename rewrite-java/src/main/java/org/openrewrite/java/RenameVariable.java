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
package org.openrewrite.java;

import lombok.RequiredArgsConstructor;
import org.openrewrite.Cursor;
import org.openrewrite.Incubating;
import org.openrewrite.Tree;
import org.openrewrite.internal.StringUtils;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.java.tree.*;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.Stack;

/**
 * Renames a NamedVariable to the target name.
 * Prevents variables from being renamed to reserved java keywords.
 * Notes:
 *  - The current version will rename variables even if a variable with `toName` is already declared in the same scope.
 */
@Incubating(since = "7.5.0")
public class RenameVariable<P> extends JavaIsoVisitor<P> {
    private final J.VariableDeclarations.NamedVariable variable;
    private final String toName;

    public RenameVariable(J.VariableDeclarations.NamedVariable variable, String toName) {
        this.variable = variable;
        this.toName = toName;
    }

    @Override
    public J.VariableDeclarations.NamedVariable visitVariable(J.VariableDeclarations.NamedVariable variable, P p) {
        if (!JavaKeywords.isReserved(toName) && !StringUtils.isBlank(toName) && variable.equals(this.variable)) {
            doAfterVisit(new RenameVariableVisitor(variable, toName));
            return variable;
        }
        return super.visitVariable(variable, p);
    }

    @RequiredArgsConstructor
    private class RenameVariableVisitor extends JavaIsoVisitor<P> {
        private final Stack<Tree> currentNameScope = new Stack<>();
        private final J.VariableDeclarations.NamedVariable renameVariable;
        private final String newName;

        @Override
        public J.Block visitBlock(J.Block block, P p) {
            // A variable name scope is owned by the ClassDeclaration regardless of what order it is declared.
            // Variables declared in a child block are owned by the corresponding block until the block is exited.
            if (getCursor().getParent() != null && getCursor().getParent().getValue() instanceof J.ClassDeclaration) {
                boolean isClassScope = false;
                for (Statement statement : block.getStatements()) {
                    if (statement instanceof J.VariableDeclarations) {
                        if (((J.VariableDeclarations) statement).getVariables().contains(renameVariable)) {
                            isClassScope = true;
                            break;
                        }
                    }
                }

                if (isClassScope) {
                    currentNameScope.add(block);
                }
            }
            return super.visitBlock(block, p);
        }

        @Override
        public J.Identifier visitIdentifier(J.Identifier ident, P p) {
            J parent  = getCursor().dropParentUntil(J.class::isInstance).getValue();
            if (ident.getSimpleName().equals(renameVariable.getSimpleName())) {
                if (parent instanceof J.FieldAccess) {
                    if (fieldAccessTargetsVariable((J.FieldAccess)parent)) {
                        return ident.withSimpleName(newName);
                    }
                } else if (currentNameScope.size() == 1) {
                    // The size of the stack will be 1 if the identifier is in the right scope.
                    return ident.withSimpleName(newName);
                }
            }
            return super.visitIdentifier(ident, p);
        }

        /**
         * FieldAccess targets the variable if its target is an Identifier and either
         *  its target FieldType equals variable.Name.FieldType
         *  or its target Type equals variable.Name.FieldType.Owner
         */
        private boolean fieldAccessTargetsVariable(J.FieldAccess fieldAccess) {
            if (renameVariable.getName().getFieldType() != null && fieldAccess.getTarget() instanceof J.Identifier) {
                J.Identifier fieldAccessTarget = (J.Identifier) fieldAccess.getTarget();
                JavaType.Variable variableNameFieldType = renameVariable.getName().getFieldType();
                // TODO:  Handle case where FieldAccessTarget is ParameterizedType and variable ower is JavaType.Class
                return variableNameFieldType.equals(fieldAccessTarget.getFieldType()) ||
                        (fieldAccessTarget.getType() != null && fieldAccessTarget.getType().equals(variableNameFieldType.getOwner()));

            }
            return false;
        }

        @Override
        public J.VariableDeclarations.NamedVariable visitVariable(J.VariableDeclarations.NamedVariable namedVariable, P p) {
            if (namedVariable.getSimpleName().equals(renameVariable.getSimpleName())) {
                Cursor parentScope = getCursorToParentScope(getCursor());
                // The target variable was found and was not declared in a class declaration block.
                if (currentNameScope.isEmpty()) {
                    if (namedVariable.equals(renameVariable)) {
                        currentNameScope.add(parentScope.getValue());
                    }
                } else {
                    // A variable has been declared and created a new name scope.
                    if (!parentScope.getValue().equals(currentNameScope.peek()) && getCursor().isScopeInPath(currentNameScope.peek())) {
                        currentNameScope.add(parentScope.getValue());
                    }
                }
            }
            return super.visitVariable(namedVariable, p);
        }

        @Override
        public J.Try.Catch visitCatch(J.Try.Catch _catch, P p) {
            // Check if the try added a new scope to the stack,
            // postVisit doesn't happen until after both the try and catch are processed.
            maybeChangeNameScope(getCursorToParentScope(getCursor()).getValue());
            return super.visitCatch(_catch, p);
        }

        @Nullable
        @Override
        public J postVisit(J tree, P p) {
            maybeChangeNameScope(tree);
            return super.postVisit(tree, p);
        }

        /**
         * Used to check if the name scope has changed.
         * Pops the stack if the tree element is at the top of the stack.
         */
        private void maybeChangeNameScope(Tree tree) {
            if (!currentNameScope.isEmpty() && currentNameScope.peek().equals(tree)) {
                currentNameScope.pop();
            }
        }

        /**
         * Returns either the current block or a J.Type that may create a reference to a variable.
         * I.E. for(int target = 0; target < N; target++) creates a new name scope for `target`.
         * The name scope in the next J.Block `{}` cannot create new variables with the name `target`.
         * <p>
         * J.* types that may only reference an existing name and do not create a new name scope are excluded.
         */
        private Cursor getCursorToParentScope(Cursor cursor) {
            return cursor.dropParentUntil(is ->
                    is instanceof JavaSourceFile ||
                            is instanceof J.ClassDeclaration ||
                            is instanceof J.MethodDeclaration ||
                            is instanceof J.Block ||
                            is instanceof J.ForLoop ||
                            is instanceof J.ForEachLoop ||
                            is instanceof J.Case ||
                            is instanceof J.Try ||
                            is instanceof J.Try.Catch ||
                            is instanceof J.Lambda);
        }
    }

    private static final class JavaKeywords {
        JavaKeywords() {}

        private static final String[] RESERVED_WORDS = new String[] {
                "abstract",
                "assert",
                "boolean",
                "break",
                "byte",
                "case",
                "catch",
                "char",
                "class",
                "const",
                "continue",
                "default",
                "do",
                "double",
                "else",
                "enum",
                "extends",
                "final",
                "finally",
                "float",
                "for",
                "goto",
                "if",
                "implements",
                "import",
                "instanceof",
                "int",
                "interface",
                "long",
                "native",
                "new",
                "package",
                "private",
                "protected",
                "public",
                "return",
                "short",
                "static",
                "strictfp",
                "super",
                "switch",
                "synchronized",
                "this",
                "throw",
                "throws",
                "transient",
                "try",
                "void",
                "volatile",
                "while",
        };

        private static final Set<String> RESERVED_WORDS_SET = new HashSet<>(Arrays.asList(RESERVED_WORDS));

        public static boolean isReserved(String word) {
            return RESERVED_WORDS_SET.contains(word);
        }
    }
}
