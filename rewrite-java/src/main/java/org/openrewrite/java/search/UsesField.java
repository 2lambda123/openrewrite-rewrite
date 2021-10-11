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
package org.openrewrite.java.search;

import lombok.RequiredArgsConstructor;
import org.openrewrite.Incubating;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaSourceFile;
import org.openrewrite.java.tree.JavaType;
import org.openrewrite.java.tree.TypeUtils;

import java.util.Set;

@RequiredArgsConstructor
@Incubating(since = "7.7.0")
public class UsesField<P> extends JavaIsoVisitor<P> {
    private final String owner;
    private final String field;

    @Override
    public JavaSourceFile visitJavaSourceFile(JavaSourceFile cu, P p) {
        Set<JavaType> types = cu.getTypesInUse();
        for (JavaType type : types) {
            if (type instanceof JavaType.Variable) {
                JavaType.Variable variable = (JavaType.Variable) type;
                if (variable.getName().equals(field) && TypeUtils.isOfClassType(variable.getOwner(), owner)) {
                    return cu.withMarkers(cu.getMarkers().searchResult());
                }
            }
        }
        return cu;
    }
}
