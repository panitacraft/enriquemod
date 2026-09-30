package com.panita.enriquecraft.core.framework.config;

import com.google.gson.JsonElement;

import java.util.Optional;

/**
 * How a config value is read from and written to the file, and which rule it must satisfy.
 */
interface ValueType<T> {

    /**
     * Converts a raw file value into a usable one.
     *
     * @throws InvalidValueException if the value has the wrong JSON type or breaks the type's rule
     */
    T parse(JsonElement element);

    JsonElement toJson(T value);

    /** A short description of what is accepted, written into the file as a comment. */
    Optional<String> constraint();
}
