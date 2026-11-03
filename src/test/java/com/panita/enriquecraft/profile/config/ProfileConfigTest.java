package com.panita.enriquecraft.profile.config;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.panita.enriquecraft.core.framework.config.ConfigManager;
import com.panita.enriquecraft.core.framework.config.InvalidValueException;
import com.panita.enriquecraft.profile.data.EmblemLook;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ProfileConfigTest {

    @TempDir
    Path directory;

    private final LooksType type = new LooksType(Predicate.not(String::isBlank), "emblem id");

    private static JsonElement json(String text) {
        return JsonParser.parseString(text);
    }

    private String rejection(String text) {
        return assertThrows(InvalidValueException.class, () -> type.parse(json(text))).getMessage();
    }

    @Test
    void defaultsAreValidAndTheFileReloadsClean() {
        ConfigManager manager = new ConfigManager(directory);

        ProfileConfig config = manager.bind("profile", ProfileConfig.class);

        assertTrue(manager.reload().isClean());
        assertEquals("enriquecraft6", config.editionId.get());
        assertEquals(2, config.systemMax.get());
        assertEquals(3, config.loreMax.get());
        assertEquals(new EmblemLook("★", "#ff5555"), config.looks.get().get("enriquecraft6_staff"));
        assertEquals(new EmblemLook("●", "gold"), config.categoryLooks.get().get("system"));
        assertEquals(new EmblemLook("○", "gray"), config.categoryLooks.get().get("emblem"));
    }

    @Test
    void idListsAreSplitOnCommasWithoutBlanks() {
        assertEquals(List.of("staff", "owner"), ProfileConfig.idsOf(" staff , owner ,, "));
        assertEquals(List.of(), ProfileConfig.idsOf(""));
    }

    @Test
    void looksSurviveAWriteAndARead() {
        Map<String, EmblemLook> looks = Map.of("a", new EmblemLook("★", "gold"), "b", new EmblemLook("○", "#00ff00"));

        assertEquals(looks, type.parse(type.toJson(looks)));
    }

    @Test
    void aLookNeedsAnIconAndAColor() {
        assertEquals("the icon of 'a' must be text", rejection("{\"a\": {\"color\": \"gold\"}}"));
        assertEquals("the color of 'a' must be text", rejection("{\"a\": {\"icon\": \"★\"}}"));
    }

    @Test
    void anIconIsOneOrTwoVisibleCharacters() {
        assertEquals("the icon of 'a' must be one or two visible characters", rejection("{\"a\": {\"icon\": \"abc\", \"color\": \"gold\"}}"));
        assertEquals("the icon of 'a' must be one or two visible characters", rejection("{\"a\": {\"icon\": \" \", \"color\": \"gold\"}}"));
        assertTrue(EmblemLook.isIcon("★"));
        assertTrue(EmblemLook.isIcon("🔥"));
        assertFalse(EmblemLook.isIcon(""));
    }

    @Test
    void aColorIsANameOrACode() {
        assertEquals("the color of 'a' must be a color name or a code such as #ffaa00",
                rejection("{\"a\": {\"icon\": \"★\", \"color\": \"orange\"}}"));
        assertEquals(0xFFAA00, new EmblemLook("★", "#ffaa00").textColor().getValue());
    }

    @Test
    void unknownSettingsAreRefused() {
        assertEquals("the look of 'a' has an unknown setting 'size'", rejection("{\"a\": {\"icon\": \"★\", \"color\": \"gold\", \"size\": 2}}"));
    }

    @Test
    void categoriesAreLimitedToTheKnownOnes() {
        ConfigManager manager = new ConfigManager(directory);
        ProfileConfig config = manager.bind("profile", ProfileConfig.class);

        assertEquals(List.of("system", "emblem", "local_emblem"), List.copyOf(config.categoryLooks.get().keySet()));
        assertThrows(InvalidValueException.class, () -> new LooksType(List.of("system")::contains, "category")
                .parse(json("{\"title\": {\"icon\": \"★\", \"color\": \"gold\"}}")));
    }
}
