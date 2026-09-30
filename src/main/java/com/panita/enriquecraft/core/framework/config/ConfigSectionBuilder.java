package com.panita.enriquecraft.core.framework.config;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;
import java.util.regex.Pattern;

/**
 * Declares the values of one module's config section. A path such as {@code messages.prefix}
 * places the value in nested objects inside the section. Defaults must satisfy their own rule.
 */
public final class ConfigSectionBuilder {

    private static final Pattern PATH = Pattern.compile("[A-Za-z0-9_]+(\\.[A-Za-z0-9_]+)*");

    private final List<ConfigValue<?>> values = new ArrayList<>();

    ConfigSectionBuilder() {
    }

    public ConfigValue<Boolean> bool(String path, boolean defaultValue, String comment) {
        return define(path, defaultValue, comment, ValueTypes.bool());
    }

    public ConfigValue<Integer> intRange(String path, int defaultValue, int min, int max, String comment) {
        return define(path, defaultValue, comment, ValueTypes.intRange(min, max));
    }

    /**
     * @param validator accepts the valid strings
     * @param rule      the rule in the form "must ...", shown in the file and in reports
     */
    public ConfigValue<String> string(String path, String defaultValue, String comment,
                                      Predicate<String> validator, String rule) {
        return define(path, defaultValue, comment, ValueTypes.string(validator, rule));
    }

    List<ConfigValue<?>> values() {
        return List.copyOf(values);
    }

    private <T> ConfigValue<T> define(String path, T defaultValue, String comment, ValueType<T> type) {
        if (!PATH.matcher(path).matches()) {
            throw new IllegalArgumentException("Invalid config path '" + path + "'");
        }
        for (ConfigValue<?> existing : values) {
            if (existing.path().equals(path) || isNested(existing.path(), path) || isNested(path, existing.path())) {
                throw new IllegalArgumentException("Config path '" + path + "' conflicts with '" + existing.path() + "'");
            }
        }
        try {
            type.parse(type.toJson(defaultValue));
        } catch (InvalidValueException e) {
            throw new IllegalArgumentException("The default of '" + path + "' is invalid: " + e.getMessage());
        }
        ConfigValue<T> value = new ConfigValue<>(path, defaultValue, comment, type);
        values.add(value);
        return value;
    }

    private static boolean isNested(String parent, String child) {
        return child.startsWith(parent + ".");
    }
}
