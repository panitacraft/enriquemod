package com.panita.enriquecraft.core.art;

import com.panita.enriquecraft.core.network.ClientCapabilities;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FontDescription;
import net.minecraft.network.chat.contents.TranslatableContents;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class InlineArtsTest {

    private static final String URL = "https://res.cloudinary.com/panita/image/upload/v1/ranks/member.png";

    private final InlineArts arts = new InlineArts(new ClientCapabilities());

    @Test
    void onlyHttpsAddressesOnTrustedHostsAreTrusted() {
        assertTrue(ArtLinks.isTrusted(URL));
        assertFalse(ArtLinks.isTrusted("http://res.cloudinary.com/a.png"));
        assertFalse(ArtLinks.isTrusted("https://evil.example.com/a.png"));
        assertFalse(ArtLinks.isTrusted("https://res.cloudinary.com.evil.com/a.png"));
        assertFalse(ArtLinks.isTrusted("https://user@res.cloudinary.com/a.png"));
        assertFalse(ArtLinks.isTrusted("https://localhost/a.png"));
        assertFalse(ArtLinks.isTrusted("not an address"));
        assertFalse(ArtLinks.isTrusted(null));
        assertFalse(ArtLinks.isTrusted("https://res.cloudinary.com/" + "a".repeat(ArtLinks.MAX_URL)));
    }

    @Test
    void anAddressHasAShortStableId() {
        String id = ArtLinks.idOf(URL);

        assertEquals(12, id.length());
        assertTrue(ArtLinks.isId(id));
        assertEquals(id, ArtLinks.idOf(URL));
        assertFalse(ArtLinks.isId("ABCDEF123456"));
        assertFalse(ArtLinks.isId("../../x"));
    }

    @Test
    void anUntrustedAddressLeavesTheTextAlone() {
        Component text = Component.literal("[MIEMBRO]");

        assertSame(text, arts.wrap(text, "https://evil.example.com/a.png"));
    }

    @Test
    void theWrappedTextIsATranslationWhoseFallbackShowsTheTextAndWhoseSecondArgumentIsTheGlyph() {
        Component text = Component.literal("[MIEMBRO]");

        Component wrapped = arts.wrap(text, URL);

        TranslatableContents translation = assertInstanceOf(TranslatableContents.class, wrapped.getContents());
        assertEquals(InlineArts.KEY, translation.getKey());
        assertEquals("%1$s", translation.getFallback());
        assertEquals(2, translation.getArgs().length);
        assertEquals(text, translation.getArgs()[0]);
        Component glyph = assertInstanceOf(Component.class, translation.getArgs()[1]);
        assertEquals(InlineArts.GLYPH, glyph.getString());
        FontDescription.Resource font = assertInstanceOf(FontDescription.Resource.class, glyph.getStyle().getFont());
        assertEquals("enriquecraft", font.id().getNamespace());
        assertEquals(InlineArts.FONT_PATH + ArtLinks.idOf(URL), font.id().getPath());
    }

    @Test
    void theIdsOfTheImagesOfALineAreFoundWhereverTheyAre() {
        Component line = Component.literal("a ").append(arts.wrap(Component.literal("x"), URL))
                .append(Component.literal(" b").append(arts.wrap(Component.literal("y"), URL + "?2")));
        Set<String> ids = new HashSet<>();

        InlineArts.collect(line, ids);

        assertEquals(Set.of(ArtLinks.idOf(URL), ArtLinks.idOf(URL + "?2")), ids);
    }

    @Test
    void aLineWithoutArtHasNoIds() {
        Set<String> ids = new HashSet<>();

        InlineArts.collect(Component.literal("hola").append(Component.translatable("chat.type.text", "x")), ids);

        assertTrue(ids.isEmpty());
    }
}
