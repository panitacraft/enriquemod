package com.panita.enriquecraft.core.framework.config;

import com.google.gson.JsonArray;
import com.google.gson.JsonPrimitive;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ConfigValuesTest {

    private final ConfigSectionBuilder builder = new ConfigSectionBuilder();

    @Test
    void valueStartsAtItsDefault() {
        ConfigValue<Boolean> value = builder.bool("enabled", true, "Enables the feature.");

        assertTrue(value.get());
    }

    @Test
    void acceptedValueReplacesTheDefault() {
        ConfigValue<Integer> value = builder.intRange("limit", 10, 1, 100, "Limit.");

        Optional<String> rejection = value.apply(new JsonPrimitive(42));

        assertTrue(rejection.isEmpty());
        assertEquals(42, value.get());
    }

    @Test
    void wrongJsonTypeFallsBackToTheDefaultWithAReason() {
        ConfigValue<Boolean> value = builder.bool("enabled", true, "Enables the feature.");

        Optional<String> rejection = value.apply(new JsonPrimitive("yes"));

        assertEquals(Optional.of("must be true or false"), rejection);
        assertTrue(value.get());
    }

    @Test
    void valueOutsideTheRangeFallsBackToTheDefault() {
        ConfigValue<Integer> value = builder.intRange("limit", 10, 1, 100, "Limit.");
        value.apply(new JsonPrimitive(50));

        Optional<String> rejection = value.apply(new JsonPrimitive(101));

        assertEquals(Optional.of("must be between 1 and 100"), rejection);
        assertEquals(10, value.get());
    }

    @Test
    void fractionalNumberIsNotAWholeNumber() {
        ConfigValue<Integer> value = builder.intRange("limit", 10, 1, 100, "Limit.");

        assertEquals(Optional.of("must be a whole number"), value.apply(new JsonPrimitive(5.5)));
        assertEquals(Optional.empty(), value.apply(new JsonPrimitive(5.0)));
        assertEquals(5, value.get());
    }

    @Test
    void numberInTextFormIsRejected() {
        ConfigValue<Integer> value = builder.intRange("limit", 10, 1, 100, "Limit.");

        assertEquals(Optional.of("must be a whole number"), value.apply(new JsonPrimitive("5")));
    }

    @Test
    void stringFailingItsRuleFallsBackToTheDefault() {
        ConfigValue<String> value = builder.string("name", "default", "Name.", text -> !text.isBlank(), "must not be blank");

        Optional<String> rejection = value.apply(new JsonPrimitive("   "));

        assertEquals(Optional.of("must not be blank"), rejection);
        assertEquals("default", value.get());
    }

    @Test
    void stringMustBeText() {
        ConfigValue<String> value = builder.string("name", "default", "Name.", text -> true, null);

        assertEquals(Optional.of("must be text"), value.apply(new JsonArray()));
        assertEquals(Optional.of("must be text"), value.apply(new JsonPrimitive(3)));
    }

    @Test
    void resetRestoresTheDefault() {
        ConfigValue<Integer> value = builder.intRange("limit", 10, 1, 100, "Limit.");
        value.apply(new JsonPrimitive(42));

        value.reset();

        assertEquals(10, value.get());
    }

    @Test
    void constraintDescribesWhatIsAccepted() {
        ConfigValue<Integer> number = builder.intRange("limit", 10, 1, 100, "Limit.");
        ConfigValue<String> plain = builder.string("text", "a", "Text.", text -> true, null);
        ConfigValue<String> ruled = builder.string("ruled", "a", "Ruled.", text -> true, "must be short");

        assertEquals(Optional.of("a whole number from 1 to 100"), number.constraint());
        assertFalse(plain.constraint().isPresent());
        assertEquals(Optional.of("must be short"), ruled.constraint());
    }

    @Test
    void defaultThatBreaksItsOwnRuleIsAProgrammingError() {
        assertThrows(IllegalArgumentException.class, () -> builder.intRange("limit", 500, 1, 100, "Limit."));
        assertThrows(IllegalArgumentException.class,
                () -> builder.string("name", " ", "Name.", text -> !text.isBlank(), "must not be blank"));
    }

    @Test
    void rangeWithMinimumAboveMaximumIsAProgrammingError() {
        assertThrows(IllegalArgumentException.class, () -> builder.intRange("limit", 5, 10, 1, "Limit."));
    }

    @Test
    void duplicatePathIsRejected() {
        builder.bool("enabled", true, "Enables the feature.");

        assertThrows(IllegalArgumentException.class, () -> builder.bool("enabled", false, "Again."));
    }

    @Test
    void valueCannotSitInsideAnotherValue() {
        builder.bool("messages", true, "A flag.");

        assertThrows(IllegalArgumentException.class, () -> builder.bool("messages.prefix", true, "Nested."));
    }

    @Test
    void valueCannotContainAnotherValue() {
        builder.bool("messages.prefix", true, "Nested.");

        assertThrows(IllegalArgumentException.class, () -> builder.bool("messages", true, "A flag."));
    }

    @Test
    void pathMustBeMadeOfPlainSegments() {
        assertThrows(IllegalArgumentException.class, () -> builder.bool("", true, "Empty."));
        assertThrows(IllegalArgumentException.class, () -> builder.bool("a..b", true, "Empty segment."));
        assertThrows(IllegalArgumentException.class, () -> builder.bool("a b", true, "Space."));
    }
}
