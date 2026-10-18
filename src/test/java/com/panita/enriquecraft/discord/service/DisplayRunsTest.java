package com.panita.enriquecraft.discord.service;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.panita.enriquecraft.MinecraftTestSupport;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FontDescription;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;
import net.minecraft.resources.Identifier;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DisplayRunsTest {

    @BeforeAll
    static void bootstrap() {
        MinecraftTestSupport.bootstrap();
    }

    @Test
    void eachStyledPartIsOneRunWithOnlyWhatIsOn() {
        Component line = Component.empty()
                .append(Component.literal("[PANITA] ").withStyle(Style.EMPTY.withColor(TextColor.fromRgb(0xAA00FF)).withBold(true)))
                .append(Component.literal("Zychss"));

        JsonArray runs = DisplayRuns.of(line);

        assertEquals(2, runs.size());
        JsonObject rank = runs.get(0).getAsJsonObject();
        assertEquals("[PANITA] ", rank.get("t").getAsString());
        assertEquals("#aa00ff", rank.get("c").getAsString());
        assertTrue(rank.get("b").getAsBoolean());
        assertFalse(rank.has("i"));
        assertFalse(rank.has("f"));
        JsonObject name = runs.get(1).getAsJsonObject();
        assertEquals(1, name.size());
        assertEquals("Zychss", name.get("t").getAsString());
    }

    @Test
    void aFontOtherThanTheDefaultIsNamedAndTheDefaultIsLeftOut() {
        Component line = Component.empty()
                .append(Component.literal("").withStyle(Style.EMPTY.withFont(new FontDescription.Resource(Identifier.fromNamespaceAndPath("panita", "ranks")))))
                .append(Component.literal("x").withStyle(Style.EMPTY.withFont(new FontDescription.Resource(Identifier.withDefaultNamespace("default")))));

        JsonArray runs = DisplayRuns.of(line);

        assertEquals("panita:ranks", runs.get(0).getAsJsonObject().get("f").getAsString());
        assertFalse(runs.get(1).getAsJsonObject().has("f"));
    }

    @Test
    void aLongLineIsCutToWhatAFrameCanHold() {
        JsonArray runs = DisplayRuns.of(Component.literal("x".repeat(500)));
        assertEquals(1, runs.size());
        assertEquals(DisplayRuns.MAX_CHARACTERS, runs.get(0).getAsJsonObject().get("t").getAsString().length());

        Component many = Component.empty();
        for (int i = 0; i < 40; i++) {
            many = many.copy().append(Component.literal("a").withStyle(Style.EMPTY.withBold(i % 2 == 0)));
        }
        assertTrue(DisplayRuns.of(many).size() <= DisplayRuns.MAX_RUNS);
    }

    @Test
    void anEmptyLineHasNoRuns() {
        assertEquals(0, DisplayRuns.of(Component.empty()).size());
    }
}
