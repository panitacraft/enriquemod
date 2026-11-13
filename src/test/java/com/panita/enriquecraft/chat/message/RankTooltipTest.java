package com.panita.enriquecraft.chat.message;

import com.panita.enriquecraft.MinecraftTestSupport;
import com.panita.enriquecraft.core.art.InlineArts;
import com.panita.enriquecraft.core.framework.message.MessageCatalog;
import com.panita.enriquecraft.core.message.MessageFormatter;
import com.panita.enriquecraft.core.network.ClientCapabilities;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.Style;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RankTooltipTest {

    private static final Component PREFIX = Component.literal("[MIEMBRO]");

    @TempDir
    Path directory;

    private RankTooltip tooltip;

    @BeforeEach
    void create() {
        MinecraftTestSupport.bootstrap();
        MessageCatalog catalog = new MessageCatalog(directory);
        catalog.register("chat", ChatMessages.class);
        tooltip = new RankTooltip(new MessageFormatter(catalog), new InlineArts(new ClientCapabilities()), () -> 20);
    }

    private static String shown(Style style) {
        return assertInstanceOf(HoverEvent.ShowText.class, style.getHoverEvent()).value().getString();
    }

    @Test
    void theTooltipIsTheRankThenItsTitleThenItsDescription() {
        Style style = tooltip.styleOf(PREFIX, Optional.empty(), "Miembro", "Un miembro leal.").orElseThrow();

        assertEquals("[MIEMBRO] Miembro\nUn miembro leal.", shown(style));
    }

    @Test
    void aRankWithOnlyADescriptionShowsItUnderTheRank() {
        Style style = tooltip.styleOf(PREFIX, Optional.empty(), "", "Un miembro.").orElseThrow();

        assertEquals("[MIEMBRO]\nUn miembro.", shown(style));
    }

    @Test
    void aRankWithOnlyATitleShowsJustThat() {
        Style style = tooltip.styleOf(PREFIX, Optional.empty(), "Miembro", "").orElseThrow();

        assertEquals("[MIEMBRO] Miembro", shown(style));
    }

    @Test
    void aRankWithNeitherShowsNothingAndCannotBeClicked() {
        assertEquals(Optional.empty(), tooltip.styleOf(PREFIX, Optional.empty(), "", "  "));
    }

    @Test
    void aLongDescriptionIsBrokenIntoShortLines() {
        Style style = tooltip.styleOf(PREFIX, Optional.empty(), "Miembro", "uno dos tres cuatro cinco seis siete ocho nueve").orElseThrow();

        String[] lines = shown(style).split("\n");

        assertTrue(lines.length >= 3);
        for (int i = 1; i < lines.length; i++) {
            assertTrue(lines[i].length() <= 20, lines[i]);
        }
    }

    @Test
    void theTitleAcceptsTextTagsButNeverShowsThem() {
        Style style = tooltip.styleOf(PREFIX, Optional.empty(), "<bold><gold>Miembro", "").orElseThrow();

        assertEquals("[MIEMBRO] Miembro", shown(style));
    }

    @Test
    void clickingCopiesTheTitleOrTheRankWhenThereIsNoTitle() {
        assertEquals("Miembro", assertInstanceOf(ClickEvent.CopyToClipboard.class,
                tooltip.styleOf(PREFIX, Optional.empty(), "Miembro", "algo").orElseThrow().getClickEvent()).value());
        assertEquals("[MIEMBRO]", assertInstanceOf(ClickEvent.CopyToClipboard.class,
                tooltip.styleOf(PREFIX, Optional.empty(), "", "algo").orElseThrow().getClickEvent()).value());
    }

    @Test
    void theImageOfARankIsAddedForAClientThatDrawsArt() {
        Style style = tooltip.styleOf(PREFIX, Optional.of("https://res.cloudinary.com/panita/a.png"), "Miembro", "").orElseThrow();

        // A client without the companion shows the prefix, which is the first argument of the translation
        assertEquals("[MIEMBRO] Miembro", shown(style));
    }
}
