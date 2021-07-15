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
package org.openrewrite.java.style;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.puppycrawl.tools.checkstyle.ConfigurationLoader;
import com.puppycrawl.tools.checkstyle.api.CheckstyleException;
import com.puppycrawl.tools.checkstyle.api.Configuration;
import org.intellij.lang.annotations.Language;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.java.cleanup.*;
import org.openrewrite.style.NamedStyles;
import org.openrewrite.style.Style;
import org.xml.sax.InputSource;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Stream;

import static java.lang.Boolean.parseBoolean;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Arrays.stream;
import static java.util.Collections.emptySet;
import static java.util.function.Function.identity;
import static java.util.stream.Collectors.*;
import static org.openrewrite.Tree.randomId;

public class Checkstyle extends NamedStyles {
    private static final Checkstyle INSTANCE = new Checkstyle();
    private static final String NAME = "org.openrewrite.java.Checkstyle";
    private static final String DISPLAY_NAME = "Checkstyle";
    private static final String DESCRIPTION = "Checkstyle defaults for styles";

    private Checkstyle() {
        super(randomId(),
                NAME,
                DISPLAY_NAME,
                DESCRIPTION,
                emptySet(),
                Arrays.asList(
                        defaultComesLast(),
                        emptyBlock(),
                        equalsAvoidsNull(),
                        explicitInitialization(),
                        fallThrough(),
                        hiddenFieldStyle(),
                        hideUtilityClassConstructorStyle(),
                        unnecessaryParentheses()
                ));
    }

    private Checkstyle(Collection<Style> styles) {
        super(randomId(), NAME, DISPLAY_NAME, DESCRIPTION, emptySet(), styles);
    }

    @JsonCreator
    public static Checkstyle defaults() {
        return INSTANCE;
    }

    public static Checkstyle parseCheckstyleConfig(Path checkstyleConf, Map<String, Object> properties) throws CheckstyleException {
        try(InputStream is = Files.newInputStream(checkstyleConf)) {
            return parseCheckstyleConfig(is, properties);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static Checkstyle parseCheckstyleConfig(@Language("XML") String checkstyleConf, Map<String, Object> properties) throws CheckstyleException {
        try(InputStream is = new ByteArrayInputStream(checkstyleConf.getBytes(UTF_8))) {
            return parseCheckstyleConfig(is, properties);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static Checkstyle parseCheckstyleConfig(InputStream checkstyleConf, Map<String, Object> properties) throws CheckstyleException {
        Map<String, Module> conf = loadConfiguration(checkstyleConf, properties);

        return new Checkstyle(Stream.of(
                defaultComesLast(conf),
                emptyBlock(conf),
                equalsAvoidsNull(conf),
                explicitInitialization(conf),
                fallThrough(conf),
                hiddenFieldStyle(conf),
                hideUtilityClassConstructorStyle(conf),
                unnecessaryParentheses(conf)
        )
                .filter(Objects::nonNull)
                .collect(toSet()));
    }

    public static DefaultComesLastStyle defaultComesLast() {
        return new DefaultComesLastStyle(false);
    }

    @Nullable
    private static DefaultComesLastStyle defaultComesLast(Map<String, Module> conf) {
        Module defaultComesLastRaw = conf.get("DefaultComesLast");
        if(defaultComesLastRaw == null) {
            return null;
        }
        return new DefaultComesLastStyle(parseBoolean(defaultComesLastRaw.properties.get("skipIfLastAndSharedWithCase")));
    }

    private static final EmptyBlockStyle.BlockPolicy defaultBlockPolicy = EmptyBlockStyle.BlockPolicy.STATEMENT;
    public static EmptyBlockStyle emptyBlock() {
        return new EmptyBlockStyle(defaultBlockPolicy, true, true, true, true,
                true, true, true, true,true, true, true, true);
    }

    @Nullable
    private static EmptyBlockStyle emptyBlock(Map<String, Module> conf) {
        Module emptyBlockRaw = conf.get("EmptyBlock");
        if(emptyBlockRaw == null) {
            return null;
        }
        String rawOption = emptyBlockRaw.properties.get("option");
        EmptyBlockStyle.BlockPolicy blockPolicy = defaultBlockPolicy;
        if(rawOption != null) {
            blockPolicy = Enum.valueOf(EmptyBlockStyle.BlockPolicy.class, rawOption.toUpperCase());
        }
        String rawTokens = emptyBlockRaw.properties.get("tokens");
        boolean instanceInit = true;
        boolean literalCatch = true;
        boolean literalDo = true;
        boolean literalElse = true;
        boolean literalFinally = true;
        boolean literalFor = true;
        boolean literalIf = true;
        boolean literalSwitch = true;
        boolean literalSynchronized = true;
        boolean literalTry = true;
        boolean literalWhile = true;
        boolean staticInit = true;
        if(rawTokens != null) {
            Set<String> tokens = Arrays.stream(rawTokens.split("\\s*,\\s*"))
                    .collect(toSet());
            instanceInit = tokens.contains("INSTANCE_INIT");
            literalCatch = tokens.contains("LITERAL_CATCH");
            literalDo = tokens.contains("LITERAL_DO");
            literalElse = tokens.contains("LITERAL_ELSE");
            literalFinally = tokens.contains("LITERAL_FINALLY");
            literalFor = tokens.contains("LITERAL_FOR");
            literalIf = tokens.contains("LITERAL_IF");
            literalSwitch = tokens.contains("LITERAL_SWITCH");
            literalSynchronized = tokens.contains("LITERAL_SYNCHRONIZED");
            literalTry = tokens.contains("LITERAL_TRY");
            literalWhile = tokens.contains("LITERAL_WHILE");
            staticInit = tokens.contains("STATIC_INIT");
        }

        return new EmptyBlockStyle(blockPolicy, instanceInit, literalCatch, literalDo,
                literalElse, literalFinally, literalFor, literalIf, literalSwitch, literalSynchronized, literalTry,
                literalWhile, staticInit);
    }


    public static EqualsAvoidsNullStyle equalsAvoidsNull() {
        return new EqualsAvoidsNullStyle(false);
    }

    @Nullable
    private static DefaultComesLastStyle equalsAvoidsNull(Map<String, Module> conf) {
        Module module = conf.get("EqualsAvoidNull");
        if(module == null) {
            return null;
        }
        return new DefaultComesLastStyle(module.prop("ignoreEqualsIgnoreCase", false));
    }


    public static ExplicitInitializationStyle explicitInitialization() {
        return new ExplicitInitializationStyle(false);
    }

    @Nullable
    private static ExplicitInitializationStyle explicitInitialization(Map<String, Module> conf) {
        Module module = conf.get("ExplicitInitialization");
        if(module == null) {
            return null;
        }
        return new ExplicitInitializationStyle(module.prop("onlyObjectReferences", false));
    }

    public static FallThroughStyle fallThrough() {
        return new FallThroughStyle(false);
    }

    @Nullable
    private static FallThroughStyle fallThrough(Map<String, Module> conf) {
        Module module = conf.get("FallThrough");
        if(module == null) {
            return null;
        }
        return new FallThroughStyle(module.prop("checkLastCaseGroup", false));
    }

    public static HiddenFieldStyle hiddenFieldStyle() {
        return new HiddenFieldStyle(false, false, false, false);
    }

    @Nullable
    private static HiddenFieldStyle hiddenFieldStyle(Map<String, Module> conf) {
        Module module = conf.get("HiddenField");
        if(module == null) {
            return null;
        }
        return new HiddenFieldStyle(
                module.prop("ignoreConstructorParameter", false),
                module.prop("ignoreSetter", false),
                module.prop("setterCanReturnItsClass", false),
                module.prop("ignoreAbstractMethods", false)
            );
    }

    public static HideUtilityClassConstructorStyle hideUtilityClassConstructorStyle() {
        return new HideUtilityClassConstructorStyle(Arrays.asList(
                "@lombok.experimental.UtilityClass",
                "@lombok.Data"
        ));
    }

    @Nullable
    private static HideUtilityClassConstructorStyle hideUtilityClassConstructorStyle(Map<String, Module> conf) {
        Module module = conf.get("HiddenField");
        if(module == null) {
            return null;
        }
        return hideUtilityClassConstructorStyle();
    }

    public static UnnecessaryParenthesesStyle unnecessaryParentheses() {
        return new UnnecessaryParenthesesStyle(true, true, true, true, true,
                true, true, true, true, true, true, true,
                true, true, true, true, true, true,
                true, true, true, true, true);
    }

    @SuppressWarnings("DuplicatedCode")
    @Nullable
    private static UnnecessaryParenthesesStyle unnecessaryParentheses(Map<String, Module> conf) {
        Module module = conf.get("UnnecessaryParentheses");
        if(module == null) {
            return null;
        }
        String rawTokens = module.properties.get("tokens");
        boolean expr = true;
        boolean ident = true;
        boolean numDouble = true;
        boolean numFloat = true;
        boolean numInt = true;
        boolean numLong = true;
        boolean stringLiteral = true;
        boolean literalNull = true;
        boolean literalFalse = true;
        boolean literalTrue = true;
        boolean assign = true;
        boolean bitAndAssign = true;
        boolean bitOrAssign = true;
        boolean bitShiftRightAssign = true;
        boolean bitXorAssign = true;
        boolean divAssign = true;
        boolean minusAssign = true;
        boolean modAssign = true;
        boolean plusAssign = true;
        boolean shiftLeftAssign = true;
        boolean shiftRightAssign = true;
        boolean starAssign = true;
        boolean lambda = true;
        if(rawTokens != null) {
            Set<String> tokens = Arrays.stream(rawTokens.split("\\s*,\\s*"))
                    .collect(toSet());
            expr = tokens.contains("EXPR");
            ident = tokens.contains("IDENT");
            numDouble = tokens.contains("NUM_DOUBLE");
            numFloat = tokens.contains("NUM_FLOAT");
            numInt = tokens.contains("NUM_INT");
            numLong = tokens.contains("NUM_LONG");
            stringLiteral = tokens.contains("STRING_LITERAL");
            literalNull = tokens.contains("LITERAL_NULL");
            literalFalse = tokens.contains("LITERAL_FALSE");
            literalTrue = tokens.contains("LITERAL_TRUE");
            assign = tokens.contains("ASSIGN");
            bitAndAssign = tokens.contains("BIT_AND_ASSIGN");
            bitOrAssign = tokens.contains("BIT_OR_ASSIGN");
            bitShiftRightAssign = tokens.contains("BIT_SHIFT_RIGHT_ASSIGN");
            bitXorAssign = tokens.contains("BIT_XOR_ASSIGN");
            divAssign = tokens.contains("DIV_ASSIGN");
            minusAssign = tokens.contains("MINUS_ASSIGN");
            modAssign = tokens.contains("MOD_ASSIGN");
            plusAssign = tokens.contains("PLUS_ASSIGN");
            shiftLeftAssign = tokens.contains("SHIFT_LEFT_ASSIGN");
            shiftRightAssign = tokens.contains("SHIFT_RIGHT_ASSIGN");
            starAssign = tokens.contains("STAR_ASSIGN");
            lambda = tokens.contains("LAMBDA");
        }
        return new UnnecessaryParenthesesStyle(expr, ident, numDouble, numFloat, numInt, numLong, stringLiteral,
                literalNull, literalFalse, literalTrue, assign, bitAndAssign, bitOrAssign, bitShiftRightAssign,
                bitXorAssign, divAssign, minusAssign, modAssign, plusAssign, shiftLeftAssign, shiftRightAssign,
                starAssign, lambda
        );
    }

    protected static class Module {
        private final String name;
        private final Map<String, String> properties;

        public Module(String name, Map<String, String> properties) {
            this.name = name;
            this.properties = properties;
        }

        public String getName() {
            return name;
        }

        public boolean prop(String key, boolean defaultValue) {
            return properties.containsKey(key) ? parseBoolean(properties.get(key))
                    : defaultValue;
        }
    }

    private static Map<String, Module> loadConfiguration(InputStream inputStream, Map<String, Object> properties) throws CheckstyleException {
        Configuration checkstyleConfig = ConfigurationLoader.loadConfiguration(new InputSource(inputStream),
                name -> {
                    Object prop = properties.get(name);
                    return prop == null ?
                            name.equals("config_loc") ? "config/checkstyle" : null :
                            prop.toString();
                },
                ConfigurationLoader.IgnoredModulesOptions.OMIT);

        List<Module> modules = new ArrayList<>();
        for (Configuration firstLevelChild : checkstyleConfig.getChildren()) {
            if ("TreeWalker".equals(firstLevelChild.getName())) {
                modules.addAll(stream(firstLevelChild.getChildren())
                        .map(child -> {
                            try {
                                Map<String, String> props = new HashMap<>();
                                for (String propertyName : child.getAttributeNames()) {
                                    props.put(propertyName, child.getAttribute(propertyName));
                                }
                                return new Module(child.getName(), props);
                            } catch (CheckstyleException e) {
                                return null;
                            }
                        })
                        .filter(Objects::nonNull)
                        .collect(toList())
                );
            } else if("SuppressionFilter".equals(firstLevelChild.getName())) {
                continue;
            }else {
                Map<String, String> props = new HashMap<>();
                for (String propertyName : firstLevelChild.getAttributeNames()) {
                    props.put(propertyName, firstLevelChild.getAttribute(propertyName));
                }
                modules.add(new Module(firstLevelChild.getName(), props));
            }
        }

        return modules.stream()
                .collect(toMap(Module::getName, identity()));
    }
}
