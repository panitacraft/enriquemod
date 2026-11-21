package com.panita.enriquecraft.core.framework.config;

import com.google.gson.JsonElement;

import java.util.Optional;

/**
 * A typed config value. Read it with {@link #get()} at the moment it is needed, so a reload is
 * picked up without any listener. The value is always valid: anything missing or rejected by its
 * rule falls back to the default.
 */
public final class ConfigValue<T> {

    private final String path;
    private final T defaultValue;
    private final String comment;
    private final ValueType<T> type;
    private volatile T value;

    ConfigValue(String path, T defaultValue, String comment, ValueType<T> type) {
        this.path = path;
        this.defaultValue = defaultValue;
        this.comment = comment;
        this.type = type;
        this.value = defaultValue;
    }

    public T get() {
        return value;
    }

    String path() {
        return path;
    }

    String comment() {
        return comment;
    }

    Optional<String> constraint() {
        return type.constraint();
    }

    JsonElement defaultJson() {
        return type.toJson(defaultValue);
    }

    void reset() {
        value = defaultValue;
    }

    /**
     * Applies a raw value read from the file.
     *
     * @return the reason the value was rejected, in which case the default is used; empty if it was accepted
     */
    Optional<String> apply(JsonElement raw) {
        try {
            value = type.parse(raw);
            return Optional.empty();
        } catch (InvalidValueException e) {
            value = defaultValue;
            return Optional.of(e.getMessage());
        }
    }
}
