package com.panita.enriquecraft.chat.message;

import com.panita.enriquecraft.MinecraftTestSupport;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.Style;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RankTooltipTest {

    private static int colorOf(ChatFormatting formatting) {
        return Style.EMPTY.withColor(formatting).getColor().getValue();
    }

    private final RankTooltip tooltip = new RankTooltip(() -> 20);

    @BeforeEach
    void bootstrap() {
        MinecraftTestSupport.bootstrap();
    }

    private static Component hover(Style style) {
        return assertInstanceOf(HoverEvent.ShowText.class, style.getHoverEvent()).value();
    }

    @Test
    void theTooltipIsTheTitleThenTheDescriptionAndNeverTheRankItself() {
        Style style = tooltip.styleOf(Component.literal("Miembro"), Component.literal("Un miembro leal.")).orElseThrow();

        assertEquals("Miembro\nUn miembro leal.", hover(style).getString());
    }

    @Test
    void aRankWithOnlyADescriptionStartsWithIt() {
        Style style = tooltip.styleOf(Component.empty(), Component.literal("Un miembro.")).orElseThrow();

        assertEquals("Un miembro.", hover(style).getString());
    }

    @Test
    void aRankWithOnlyATitleShowsJustThat() {
        Style style = tooltip.styleOf(Component.literal("Miembro"), Component.empty()).orElseThrow();

        assertEquals("Miembro", hover(style).getString());
    }

    @Test
    void aRankWithNeitherShowsNothing() {
        assertEquals(Optional.empty(), tooltip.styleOf(Component.empty(), Component.literal("  ")));
    }

    @Test
    void aLongDescriptionIsBrokenIntoShortLines() {
        Style style = tooltip.styleOf(Component.literal("Miembro"), Component.literal("uno dos tres cuatro cinco seis siete ocho nueve")).orElseThrow();

        String[] lines = hover(style).getString().split("\n");

        assertTrue(lines.length >= 4);
        for (int i = 1; i < lines.length; i++) {
            assertTrue(lines[i].length() <= 20, lines[i]);
        }
    }

    @Test
    void theTitleKeepsItsOwnColorAndTheDescriptionIsGrayWhereItSetsNone() {
        Component title = Component.literal("Miembro").withStyle(ChatFormatting.GREEN);
        Component description = Component.empty().append(Component.literal("solo ")).append(Component.literal("rojo").withStyle(ChatFormatting.RED));

        Component shown = hover(tooltip.styleOf(title, description).orElseThrow());

        var parts = shown.toFlatList();
        assertEquals(colorOf(ChatFormatting.GREEN), parts.get(0).getStyle().getColor().getValue());
        assertEquals(colorOf(ChatFormatting.GRAY), parts.stream().filter(part -> part.getString().equals("solo")).findFirst().orElseThrow()
                .getStyle().getColor().getValue());
        assertEquals(colorOf(ChatFormatting.RED), parts.stream().filter(part -> part.getString().equals("rojo")).findFirst().orElseThrow()
                .getStyle().getColor().getValue());
    }

    @Test
    void clickingDoesNothingButMakesTheCursorAPointer() {
        Style style = tooltip.styleOf(Component.literal("Miembro"), Component.empty()).orElseThrow();

        ClickEvent.Custom click = assertInstanceOf(ClickEvent.Custom.class, style.getClickEvent());
        assertEquals("enriquecraft:none", click.id().toString());
        assertEquals(Optional.empty(), click.payload());
    }
}
