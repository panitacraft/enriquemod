package com.panita.enriquecraft.profile.config;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.panita.enriquecraft.core.framework.config.ConfigManager;
import com.panita.enriquecraft.core.framework.config.InvalidValueException;
import com.panita.enriquecraft.profile.data.EmblemLook;
import com.panita.enriquecraft.profile.data.PrankLevel;
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
        assertEquals(3, config.loreMax.get());
        assertEquals(604_800, config.prankCooldownSeconds.get());
        assertEquals(7, config.pranks.get().size());
        assertEquals("enrique6_prank_0", config.pranks.get().getFirst().id());
        assertEquals(new EmblemLook("☠⁺", "red"), config.pranks.get().getLast().look());
        assertEquals("https://res.cloudinary.com/panita/image/upload/v1794522697/panita-extra/pranks/prank_level_three_plus.png",
                config.pranks.get().getLast().image());
        assertEquals("https://res.cloudinary.com/panita/image/upload/v1794522806/panita-extra/emblems/no_emblem.png#size=3", config.emptyEmblemImage.get());
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

    private String prankRejection(String text) {
        return assertThrows(InvalidValueException.class, () -> new PranksType().parse(json(text))).getMessage();
    }

    private static String level(String id, String extra) {
        return "{\"id\": \"" + id + "\", \"title\": \"T\", \"icon\": \"☠\", \"color\": \"white\"" + extra + "}";
    }

    @Test
    void theDefaultPranksSurviveAWriteAndARead() {
        ProfileConfig config = new ConfigManager(directory).bind("profile", ProfileConfig.class);
        PranksType pranks = new PranksType();

        assertEquals(config.pranks.get(), pranks.parse(pranks.toJson(config.pranks.get())));
    }

    @Test
    void aPrankListNeedsAtLeastOneLevelWithDistinctIds() {
        assertEquals("must be a list with at least one prank level", prankRejection("[]"));
        assertEquals("the id 'a' is used by more than one level", prankRejection("[" + level("a", "") + "," + level("a", "") + "]"));
    }

    @Test
    void aPrankLevelIsCheckedPartByPart() {
        assertEquals("level 1 has an unknown setting 'size'", prankRejection("[" + level("a", ", \"size\": 2") + "]"));
        assertEquals("the image of level 1 ('a') must be an https address on a trusted image host",
                prankRejection("[" + level("a", ", \"image\": \"https://example.com/a.png\"") + "]"));
        assertEquals("the icon of level 1 ('a') must be one or two visible characters",
                prankRejection("[{\"id\": \"a\", \"title\": \"T\", \"icon\": \"abc\", \"color\": \"white\"}]"));
        assertEquals("the title of level 1 ('a') must be text", prankRejection("[{\"id\": \"a\", \"icon\": \"☠\", \"color\": \"white\"}]"));
    }

    @Test
    void aPrankDescriptionAndImageMayBeLeftOut() {
        List<PrankLevel> parsed = new PranksType().parse(json("[" + level("a", "") + "]"));

        assertEquals(new PrankLevel("a", "T", "", new EmblemLook("☠", "white"), ""), parsed.getFirst());
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
