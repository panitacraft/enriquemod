package com.panita.enriquecraft.core.framework.config;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.function.Predicate;

/**
 * The built-in {@link ValueType}s.
 */
final class ValueTypes {

    private ValueTypes() {
    }

    static ValueType<Boolean> bool() {
        return new BoolType();
    }

    static ValueType<Integer> intRange(int min, int max) {
        if (min > max) {
            throw new IllegalArgumentException("min " + min + " is greater than max " + max);
        }
        return new IntRangeType(min, max);
    }

    /**
     * @param validator accepts the valid strings
     * @param rule      states the rule in the form "must ...", or null when any string is accepted
     */
    static ValueType<String> string(Predicate<String> validator, String rule) {
        return new StringType(validator, rule);
    }

    private record BoolType() implements ValueType<Boolean> {

        @Override
        public Boolean parse(JsonElement element) {
            if (!element.isJsonPrimitive() || !element.getAsJsonPrimitive().isBoolean()) {
                throw new InvalidValueException("must be true or false");
            }
            return element.getAsBoolean();
        }

        @Override
        public JsonElement toJson(Boolean value) {
            return new JsonPrimitive(value);
        }

        @Override
        public Optional<String> constraint() {
            return Optional.of("true or false");
        }
    }

    private record IntRangeType(int min, int max) implements ValueType<Integer> {

        @Override
        public Integer parse(JsonElement element) {
            if (!element.isJsonPrimitive() || !element.getAsJsonPrimitive().isNumber()) {
                throw new InvalidValueException("must be a whole number");
            }
            BigDecimal number = element.getAsBigDecimal();
            boolean whole = number.signum() == 0 || number.stripTrailingZeros().scale() <= 0;
            if (!whole) {
                throw new InvalidValueException("must be a whole number");
            }
            if (number.compareTo(BigDecimal.valueOf(min)) < 0 || number.compareTo(BigDecimal.valueOf(max)) > 0) {
                throw new InvalidValueException("must be between " + min + " and " + max);
            }
            return number.intValueExact();
        }

        @Override
        public JsonElement toJson(Integer value) {
            return new JsonPrimitive(value);
        }

        @Override
        public Optional<String> constraint() {
            return Optional.of("a whole number from " + min + " to " + max);
        }
    }

    private record StringType(Predicate<String> validator, String rule) implements ValueType<String> {

        @Override
        public String parse(JsonElement element) {
            if (!element.isJsonPrimitive() || !element.getAsJsonPrimitive().isString()) {
                throw new InvalidValueException("must be text");
            }
            String value = element.getAsString();
            if (!validator.test(value)) {
                throw new InvalidValueException(rule);
            }
            return value;
        }

        @Override
        public JsonElement toJson(String value) {
            return new JsonPrimitive(value);
        }

        @Override
        public Optional<String> constraint() {
            return Optional.ofNullable(rule);
        }
    }
}
