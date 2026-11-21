package com.panita.enriquecraft.core.framework.config;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ConfigTableValuesTest {

    private static final Map<String, String> DEFAULTS = Map.of(":heart:", "<red>❤");

    private final ConfigSectionBuilder builder = new ConfigSectionBuilder();

    private ConfigValue<Map<String, String>> emojis() {
        return builder.stringMap("emojis", DEFAULTS, "Emojis.", key -> !key.isBlank() && !key.contains(" "),
                "must not be empty or contain spaces");
    }

    private static JsonElement json(String text) {
        return JsonParser.parseString(text);
    }

    @Test
    void tableKeepsTheOrderOfTheFile() {
        ConfigValue<Map<String, String>> value = emojis();

        assertEquals(Optional.empty(), value.apply(json("{\"b\": \"2\", \"a\": \"1\", \"c\": \"3\"}")));

        assertEquals(List.of("b", "a", "c"), List.copyOf(value.get().keySet()));
    }

    @Test
    void tableMustBeAnObject() {
        ConfigValue<Map<String, String>> value = emojis();

        assertEquals(Optional.of("must be an object of texts"), value.apply(new JsonArray()));
        assertEquals(DEFAULTS, value.get());
    }

    @Test
    void keyBreakingItsRuleIsNamedAndTheDefaultIsUsed() {
        ConfigValue<Map<String, String>> value = emojis();

        Optional<String> rejection = value.apply(json("{\"a b\": \"x\"}"));

        assertEquals(Optional.of("the key 'a b' must not be empty or contain spaces"), rejection);
        assertEquals(DEFAULTS, value.get());
    }

    @Test
    void valueThatIsNotATextOrIsBlankIsNamed() {
        ConfigValue<Map<String, String>> value = emojis();

        assertEquals(Optional.of("the value of 'a' must be a text that is not blank"), value.apply(json("{\"a\": 3}")));
        assertEquals(Optional.of("the value of 'a' must be a text that is not blank"), value.apply(json("{\"a\": \"  \"}")));
    }

    @Test
    void emptyTableIsAcceptedSoAnAdministratorCanTurnEverythingOff() {
        ConfigValue<Map<String, String>> value = emojis();

        assertEquals(Optional.empty(), value.apply(new JsonObject()));

        assertTrue(value.get().isEmpty());
    }

    @Test
    void acceptedTableCannotBeChanged() {
        ConfigValue<Map<String, String>> value = emojis();
        value.apply(json("{\"a\": \"1\"}"));

        assertThrows(UnsupportedOperationException.class, () -> value.get().put("b", "2"));
    }

    @Test
    void defaultOfTheWrongShapeIsAProgrammingError() {
        Map<String, String> bad = new LinkedHashMap<>();
        bad.put("a b", "x");

        assertThrows(IllegalArgumentException.class, () -> builder.stringMap("table", bad, "Table.",
                key -> !key.contains(" "), "must not contain spaces"));
    }

    @Test
    void customTypeIsDeclaredLikeAnyOther() {
        ValueType<Integer> doubling = new ValueType<>() {
            @Override
            public Integer parse(JsonElement element) {
                if (!element.isJsonPrimitive()) {
                    throw new InvalidValueException("must be a number");
                }
                return element.getAsInt() * 2;
            }

            @Override
            public JsonElement toJson(Integer value) {
                return new JsonPrimitive(value / 2);
            }

            @Override
            public Optional<String> constraint() {
                return Optional.empty();
            }
        };
        ConfigValue<Integer> value = builder.custom("twice", 4, "Doubled.", doubling);

        assertEquals(4, value.get());
        assertEquals(Optional.empty(), value.apply(new JsonPrimitive(5)));
        assertEquals(10, value.get());
        assertEquals(Optional.of("must be a number"), value.apply(new JsonObject()));
        assertEquals(4, value.get());
    }

    public static final class TableConfig implements ModConfig {
        public final ConfigValue<Map<String, String>> emojis;
        public final ConfigValue<Map<String, String>> big;

        public TableConfig(ConfigSectionBuilder builder) {
            emojis = builder.stringMap("emojis", Map.of(":heart:", "<red>❤"), "Emojis.", key -> !key.isBlank(),
                    "must not be empty");
            Map<String, String> many = new LinkedHashMap<>();
            for (int i = 0; i < 30; i++) {
                many.put(":emoji" + i + ":", "<red>❤");
            }
            big = builder.stringMap("big", many, "Many.", key -> !key.isBlank(), "must not be empty");
        }
    }

    @TempDir
    Path directory;

    @Test
    void tableIsWrittenAsAnObjectAndLargeDefaultsSkipTheOneLineComment() throws IOException {
        new ConfigManager(directory).bind("chat", TableConfig.class);

        String text = Files.readString(directory.resolve(ConfigManager.FILE_NAME), StandardCharsets.UTF_8);

        assertTrue(text.contains("\":heart:\": \"<red>❤\""), text);
        assertTrue(text.contains("// Default: {\":heart:\":\"<red>❤\"}"), text);
        assertFalse(text.contains("// Default: {\":emoji0:\""), text);
        assertTrue(text.contains("// Allowed: an object of texts whose keys must not be empty"), text);
    }

    @Test
    void tableWrittenByTheAdministratorIsReadBackAndKept() throws IOException {
        Files.writeString(directory.resolve(ConfigManager.FILE_NAME),
                "{\"chat\": {\"emojis\": {\":star:\": \"<yellow>★\"}}}", StandardCharsets.UTF_8);

        TableConfig config = new ConfigManager(directory).bind("chat", TableConfig.class);

        assertEquals(Map.of(":star:", "<yellow>★"), config.emojis.get());
        String text = Files.readString(directory.resolve(ConfigManager.FILE_NAME), StandardCharsets.UTF_8);
        assertTrue(text.contains("\":star:\": \"<yellow>★\""), text);
        assertFalse(text.contains("\":heart:\": \""), text);
    }
}
