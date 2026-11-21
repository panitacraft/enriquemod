package com.panita.enriquecraft.core.message;

import com.panita.enriquecraft.MinecraftTestSupport;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.TextColor;
import net.minecraft.network.chat.contents.PlainTextContents;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TabTextTest {

    @BeforeEach
    void bootstrap() {
        MinecraftTestSupport.bootstrap();
    }

    private static List<Component> parts(Component root) {
        List<Component> all = new ArrayList<>();
        all.add(root);
        root.getSiblings().forEach(sibling -> all.addAll(parts(sibling)));
        return all;
    }

    @Test
    void anImageTranslationBecomesItsFallbackTextInItsColor() {
        Component glyph = Component.literal("☠").withStyle(ChatFormatting.RED);
        Component art = Component.translatableWithFallback("enriquecraft.art.v1", "%1$s", glyph, Component.literal("image"));

        Component plain = TabText.plain(art);

        assertEquals("☠", plain.getString());
        assertTrue(parts(plain).stream().allMatch(part -> part.getContents() instanceof PlainTextContents));
        // The root also reads "☠" through its siblings, so only the part that holds the text itself is the glyph
        var skull = parts(plain).stream().filter(part -> part.getContents() instanceof PlainTextContents text && text.text().equals("☠"))
                .findFirst().orElseThrow();
        assertEquals(TextColor.fromLegacyFormat(ChatFormatting.RED), skull.getStyle().getColor());
    }

    @Test
    void hoverAndClickAreDropped() {
        Component text = Component.literal("Admin").withStyle(style -> style
                .withHoverEvent(new HoverEvent.ShowText(Component.literal("tip")))
                .withClickEvent(new ClickEvent.SuggestCommand("/x")));

        Component plain = TabText.plain(text);

        assertTrue(parts(plain).stream().allMatch(part -> part.getStyle().getHoverEvent() == null && part.getStyle().getClickEvent() == null));
    }

    @Test
    void everyPartHasAnExplicitColorAndKeepsTheStyles() {
        Component text = Component.empty()
                .append(Component.literal("a").withStyle(style -> style.withColor(0x112233).withBold(true)))
                .append(Component.literal("b"));

        Component plain = TabText.plain(text);

        assertEquals("ab", plain.getString());
        assertTrue(parts(plain).stream().allMatch(part -> part.getStyle().getColor() != null));
        var a = parts(plain).stream().filter(part -> part.getString().equals("a")).findFirst().orElseThrow();
        assertEquals(0x112233, a.getStyle().getColor().getValue());
        assertTrue(a.getStyle().isBold());
        var b = parts(plain).stream().filter(part -> part.getString().equals("b")).findFirst().orElseThrow();
        assertEquals(TextColor.fromLegacyFormat(ChatFormatting.WHITE), b.getStyle().getColor());
        assertNull(b.getStyle().getHoverEvent());
        assertNotNull(b.getStyle().getColor());
    }

    @Test
    void anEmptyTextStaysEmpty() {
        assertEquals("", TabText.plain(Component.empty()).getString());
    }
}
