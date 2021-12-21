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
package org.openrewrite.java.internal;

import io.github.classgraph.*;
import org.openrewrite.ExecutionContext;
import org.openrewrite.internal.ListUtils;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.java.JavaTypeMapping;
import org.openrewrite.java.tree.Flag;
import org.openrewrite.java.tree.JavaType;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.openrewrite.java.tree.JavaType.GenericTypeVariable.Variance.*;

public class ClassgraphTypeMapping implements JavaTypeMapping<ClassInfo> {
    private final ClassgraphJavaTypeSignatureBuilder signatureBuilder = new ClassgraphJavaTypeSignatureBuilder();
    private final Map<ClassInfo, JavaType.FullyQualified> stack = new IdentityHashMap<>();

    private final Map<String, Object> typeBySignature;
    private final JavaReflectionTypeMapping reflectionTypeMapping;
    private final Map<String, JavaType.FullyQualified> jvmTypes;
    private final ExecutionContext ctx;

    public ClassgraphTypeMapping(Map<String, Object> typeBySignature, Map<String, JavaType.FullyQualified> jvmTypes, ExecutionContext ctx) {
        this.typeBySignature = typeBySignature;
        this.reflectionTypeMapping = new JavaReflectionTypeMapping(typeBySignature);
        this.jvmTypes = jvmTypes;
        this.ctx = ctx;
    }

    public JavaType.FullyQualified type(@Nullable ClassInfo aClass) {
        if (aClass == null) {
            return JavaType.Class.Unknown.getInstance();
        }

        JavaType.FullyQualified existingClass = stack.get(aClass);
        if (existingClass != null) {
            return existingClass;
        }

        AtomicBoolean newlyCreated = new AtomicBoolean(false);

        JavaType.Class clazz = (JavaType.Class) typeBySignature.get(aClass.getName());
        if (clazz == null) {
            JavaType.Class.Kind kind;
            if (aClass.isInterface()) {
                kind = JavaType.Class.Kind.Interface;
            } else if (aClass.isEnum()) {
                kind = JavaType.Class.Kind.Enum;
            } else if (aClass.isAnnotation()) {
                kind = JavaType.Class.Kind.Annotation;
            } else {
                kind = JavaType.Class.Kind.Class;
            }

            if (aClass.getName().startsWith("com.sun.") ||
                    aClass.getName().startsWith("sun.") ||
                    aClass.getName().startsWith("java.awt.") ||
                    aClass.getName().startsWith("jdk.") ||
                    aClass.getName().startsWith("org.graalvm")) {
                return new JavaType.Class(
                        null, aClass.getModifiers(), aClass.getName(), kind,
                        null, null, null, null, null, null);
            }

            newlyCreated.set(true);

            clazz = new JavaType.Class(
                    null,
                    aClass.getModifiers(),
                    aClass.getName(),
                    kind,
                    null, null, null, null, null, null
            );
        }

        if (newlyCreated.get()) {
            stack.put(aClass, clazz);

            ClassInfo superclassInfo = aClass.getSuperclass();
            JavaType.FullyQualified supertype;
            if(superclassInfo == null) {
                // Classgraph reports null for the supertype of interfaces, for consistency with other TypeMappings we report Object
                supertype = (JavaType.FullyQualified) reflectionTypeMapping.type(Object.class);
            } else {
                supertype = type(superclassInfo);
            }
            JavaType.FullyQualified owner = aClass.getOuterClasses().isEmpty() ? null :
                    type(aClass.getOuterClasses().get(0));

            List<JavaType.FullyQualified> annotations = null;
            if (!aClass.getAnnotationInfo().isEmpty()) {
                annotations = new ArrayList<>(aClass.getAnnotationInfo().size());
                for (AnnotationInfo annotationInfo : aClass.getAnnotationInfo()) {
                    annotations.add(type(annotationInfo.getClassInfo()));
                }
            }

            List<JavaType.FullyQualified> interfaces = null;
            if (!aClass.getInterfaces().isEmpty()) {
                interfaces = new ArrayList<>(aClass.getInterfaces().size());
                for (ClassInfo anInterface : aClass.getInterfaces()) {
                    interfaces.add(type(anInterface));
                }
            }

            List<JavaType.Variable> variables = null;
            if (!aClass.getFieldInfo().isEmpty()) {
                variables = new ArrayList<>(aClass.getFieldInfo().size());
                for (FieldInfo fieldInfo : aClass.getFieldInfo()) {
                    JavaType.Variable variable = variableType(fieldInfo);
                    variables.add(variable);
                }
            }

            List<JavaType.Method> methods = null;
            if (!aClass.getMethodInfo().isEmpty()) {
                methods = new ArrayList<>(aClass.getMethodInfo().size());
                for (MethodInfo methodInfo : aClass.getMethodInfo()) {
                    JavaType.Method method = methodType(methodInfo);
                    if (method != null) {
                        methods.add(method);
                    }
                }
            }

            clazz.unsafeSet(supertype, owner, annotations, interfaces, variables, methods);

            stack.remove(aClass);
        }

        ClassTypeSignature typeSignature = aClass.getTypeSignature();
        if (typeSignature == null || typeSignature.getTypeParameters() == null || typeSignature.getTypeParameters().isEmpty()) {
            return clazz;
        }

        newlyCreated.set(false);

        JavaType.Parameterized parameterized = (JavaType.Parameterized) typeBySignature.computeIfAbsent(signatureBuilder.signature(aClass.getTypeSignature()), ignored -> {
            newlyCreated.set(true);
            //noinspection ConstantConditions
            return new JavaType.Parameterized(null, null, null);
        });

        if (newlyCreated.get()) {
            List<JavaType> typeParameters = new ArrayList<>(typeSignature.getTypeParameters().size());
            for (TypeParameter tParam : typeSignature.getTypeParameters()) {
                JavaType javaType = type(tParam);
                typeParameters.add(javaType);
            }

            parameterized.unsafeSet(clazz, typeParameters);
        }

        return parameterized;
    }

    private JavaType.Variable variableType(FieldInfo fieldInfo) {
        JavaType.Variable existing = (JavaType.Variable) typeBySignature.get(signatureBuilder.variableSignature(fieldInfo));
        if (existing != null) {
            return existing;
        }

        JavaType.FullyQualified owner = type(fieldInfo.getClassInfo());

        List<JavaType.FullyQualified> annotations = emptyList();
        if (!fieldInfo.getAnnotationInfo().isEmpty()) {
            annotations = new ArrayList<>(fieldInfo.getAnnotationInfo().size());
            for (AnnotationInfo annotationInfo : fieldInfo.getAnnotationInfo()) {
                annotations.add(type(annotationInfo.getClassInfo()));
            }
        }

        assert owner != null;
        return new JavaType.Variable(fieldInfo.getModifiers(), fieldInfo.getName(), owner,
                type(fieldInfo.getTypeDescriptor()), annotations);
    }

    @Nullable
    private JavaType.Method methodType(MethodInfo methodInfo) {
        JavaType.Method existing = (JavaType.Method) typeBySignature.get(signatureBuilder.methodSignature(methodInfo));
        if (existing != null) {
            return existing;
        }

        try {
            long flags = methodInfo.getModifiers();

            // The field access modifier "volatile" corresponds to the "bridge" modifier on methods.
            // We don't represent "bridge" because it is a compiler internal that cannot appear in source code.
            // See https://github.com/openrewrite/rewrite/issues/995
            if ((flags & Flag.Volatile.getBitMask()) != 0) {
                return null;
            }

            List<JavaType> genericParameterTypes = new ArrayList<>(methodInfo.getParameterInfo().length);
            for (MethodParameterInfo methodParameterInfo : methodInfo.getParameterInfo()) {
                genericParameterTypes.add(methodParameterInfo.getTypeSignature() == null ?
                        type(methodParameterInfo.getTypeDescriptor()) :
                        type(methodParameterInfo.getTypeSignature()));
            }
            JavaType.Method.Signature genericSignature = new JavaType.Method.Signature(
                    methodInfo.getTypeSignature() == null ?
                            type(methodInfo.getTypeDescriptor().getResultType()) :
                            type(methodInfo.getTypeSignature().getResultType()),
                    genericParameterTypes
            );

            List<JavaType> resolvedParameterTypes = new ArrayList<>(methodInfo.getParameterInfo().length);
            for (MethodParameterInfo methodParameterInfo : methodInfo.getParameterInfo()) {
                resolvedParameterTypes.add(type(methodParameterInfo.getTypeDescriptor()));
            }
            JavaType.Method.Signature resolvedSignature = new JavaType.Method.Signature(type(methodInfo.getTypeDescriptor().getResultType()), resolvedParameterTypes);

            List<String> paramNames = null;
            if (methodInfo.getParameterInfo().length > 0) {
                paramNames = new ArrayList<>(methodInfo.getParameterInfo().length);
                for (MethodParameterInfo methodParameterInfo : methodInfo.getParameterInfo()) {
                    paramNames.add(methodParameterInfo.getName());
                }
            }

            List<JavaType.FullyQualified> thrownExceptions = null;
            if (!methodInfo.getTypeDescriptor().getThrowsSignatures().isEmpty()) {
                thrownExceptions = new ArrayList<>(methodInfo.getTypeDescriptor().getThrowsSignatures().size());
                for (ClassRefOrTypeVariableSignature throwsSignature : methodInfo.getTypeDescriptor().getThrowsSignatures()) {
                    if (throwsSignature instanceof ClassRefTypeSignature) {
                        thrownExceptions.add(type(((ClassRefTypeSignature) throwsSignature).getClassInfo()));
                    }
                }
            }

            List<JavaType.FullyQualified> annotations = null;
            if (!methodInfo.getAnnotationInfo().isEmpty()) {
                annotations = new ArrayList<>(methodInfo.getAnnotationInfo().size());
                for (AnnotationInfo annotationInfo : methodInfo.getAnnotationInfo()) {
                    annotations.add(type(annotationInfo.getClassInfo()));
                }
            }

            //noinspection ConstantConditions
            return new JavaType.Method(
                    methodInfo.getModifiers(),
                    type(methodInfo.getClassInfo()),
                    methodInfo.getName(),
                    paramNames,
                    genericSignature,
                    resolvedSignature,
                    thrownExceptions,
                    annotations
            );
        } catch (Exception e) {
            ctx.getOnError().accept(e);
            return null;
        }
    }

    private JavaType.GenericTypeVariable type(TypeParameter typeParameter) {
        JavaType.GenericTypeVariable existing = (JavaType.GenericTypeVariable) typeBySignature.get(signatureBuilder.signature(typeParameter));
        if (existing != null) {
            return existing;
        }

        List<JavaType> bounds = null;
        if (typeParameter.getClassBound() != null) {
            if (typeParameter.getClassBound() instanceof ClassRefTypeSignature) {
                ReferenceTypeSignature bound = typeParameter.getClassBound();
                ClassRefTypeSignature classBound = (ClassRefTypeSignature) bound;
                if (classBound.getClassInfo() != null && !"java.lang.Object".equals(classBound.getFullyQualifiedClassName())) {
                    bounds = new ArrayList<>();
                    bounds.add(type(typeParameter.getClassBound()));
                }
            } else {
                bounds = new ArrayList<>();
                bounds.add(type(typeParameter.getClassBound()));
            }
        }
        if (typeParameter.getInterfaceBounds() != null && !typeParameter.getInterfaceBounds().isEmpty()) {
            if (bounds == null) {
                bounds = new ArrayList<>();
            }
            for (ReferenceTypeSignature interfaceBound : typeParameter.getInterfaceBounds()) {
                bounds.add(type(interfaceBound));
            }
        }

        return new JavaType.GenericTypeVariable(null, typeParameter.getName(),
                bounds == null ? INVARIANT : COVARIANT, bounds);
    }

    private JavaType type(HierarchicalTypeSignature typeSignature) {
        JavaType existing = (JavaType) typeBySignature.get(signatureBuilder.signature(typeSignature));
        if (existing != null) {
            return existing;
        }

        if (typeSignature instanceof ClassRefTypeSignature) {
            ClassRefTypeSignature classRefSig = (ClassRefTypeSignature) typeSignature;
            ClassInfo classInfo = classRefSig.getClassInfo();
            if (classInfo == null) {
                JavaType.FullyQualified jvmType = jvmTypes.get(classRefSig.getBaseClassName());
                if (jvmType != null) {
                    return jvmType;
                } else if (classRefSig.getBaseClassName().equals("java.lang.Object")) {
                    //noinspection ConstantConditions
                    return reflectionTypeMapping.type(Object.class);
                } else {
                    return JavaType.Unknown.getInstance();
                }
            }

            JavaType.FullyQualified type = type(classInfo);
            if (type instanceof JavaType.Parameterized) {
                JavaType.Parameterized pt = (JavaType.Parameterized) type;
                type = pt.withTypeParameters(ListUtils.map(pt.getTypeParameters(), (i, tp) -> {
                    if (classRefSig.getTypeArguments().size() > i) {
                        return type(classRefSig.getTypeArguments().get(i));
                    }
                    return null;
                }));
            }

            //noinspection ConstantConditions
            return type;
        } else if (typeSignature instanceof ClassTypeSignature) {
            ClassTypeSignature classSig = (ClassTypeSignature) typeSignature;

            try {
                Method getClassInfo = classSig.getClass().getDeclaredMethod("getClassInfo");
                getClassInfo.setAccessible(true);

                JavaType.Class clazz = (JavaType.Class) type((ClassInfo) getClassInfo.invoke(classSig));

                List<JavaType> typeParameters = new ArrayList<>(classSig.getTypeParameters().size());
                for (TypeParameter typeParameter : classSig.getTypeParameters()) {
                    typeParameters.add(type(typeParameter));
                }

                return new JavaType.Parameterized(null, clazz, typeParameters);
            } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
                throw new RuntimeException(e);
            }
        } else if (typeSignature instanceof ArrayTypeSignature) {
            ArrayClassInfo arrClassInfo = ((ArrayTypeSignature) typeSignature).getArrayClassInfo();
            JavaType type = type(arrClassInfo.getElementClassInfo());
            assert type != null;
            for (int i = 0; i < arrClassInfo.getNumDimensions(); i++) {
                type = new JavaType.Array(type);
            }
            return type;
        } else if (typeSignature instanceof TypeVariableSignature) {
            TypeVariableSignature typeVariableSignature = (TypeVariableSignature) typeSignature;
            try {
                return type(typeVariableSignature.resolve());
            } catch (IllegalArgumentException ignored) {
                return new JavaType.GenericTypeVariable(null, typeVariableSignature.getName(),
                        INVARIANT, null);
            }
        } else if (typeSignature instanceof BaseTypeSignature) {
            return JavaType.Primitive.fromKeyword(((BaseTypeSignature) typeSignature).getTypeStr());
        } else if (typeSignature instanceof TypeArgument) {
            TypeArgument typeArgument = (TypeArgument) typeSignature;

            JavaType.GenericTypeVariable.Variance variance;
            List<JavaType> bounds = null;

            switch (typeArgument.getWildcard()) {
                case NONE:
                    return type(typeArgument.getTypeSignature());
                case EXTENDS:
                    variance = COVARIANT;
                    bounds = singletonList(type(typeArgument.getTypeSignature()));
                    break;
                case SUPER:
                    variance = CONTRAVARIANT;
                    bounds = singletonList(type(typeArgument.getTypeSignature()));
                    break;
                case ANY:
                default:
                    variance = INVARIANT;
                    break;
            }

            if (bounds != null && bounds.get(0) instanceof JavaType.FullyQualified &&
                    ((JavaType.FullyQualified) bounds.get(0)).getFullyQualifiedName().equals("java.lang.Object")) {
                bounds = null;
                variance = INVARIANT;
            }

            return new JavaType.GenericTypeVariable(null, "?", variance, bounds);
        }

        throw new UnsupportedOperationException("Unexpected signature type " + typeSignature.getClass().getName());
    }
}
