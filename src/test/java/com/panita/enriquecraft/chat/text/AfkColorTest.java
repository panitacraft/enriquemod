package com.panita.enriquecraft.chat.text;

import com.panita.enriquecraft.MinecraftTestSupport;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextColor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AfkColorTest {

    @BeforeEach
    void bootstrap() {
        MinecraftTestSupport.bootstrap();
    }

    @Test
    void readsALegacyCode() {
        assertEquals(TextColor.fromLegacyFormat(ChatFormatting.GRAY), AfkColor.parse("&7").orElseThrow());
        assertEquals(TextColor.fromLegacyFormat(ChatFormatting.AQUA), AfkColor.parse(" &B ").orElseThrow());
    }

    @Test
    void readsAHexColorWithOrWithoutTheAmpersand() {
        assertEquals(TextColor.fromRgb(0xAAAAAA), AfkColor.parse("#aaaaaa").orElseThrow());
        assertEquals(TextColor.fromRgb(0x12AB34), AfkColor.parse("&#12ab34").orElseThrow());
    }

    @Test
    void refusesWhatIsNotAColor() {
        for (String text : new String[]{"", "&", "&k", "&r", "&77", "gray", "#abc", "#gggggg", "&#12345"}) {
            assertFalse(AfkColor.isValid(text), text);
        }
    }

    @Test
    void paintReplacesEveryColorAndKeepsTheOtherStyles() {
        Component text = Component.empty()
                .append(Component.literal("[").withStyle(ChatFormatting.RED))
                .append(Component.literal("Admin").withStyle(style -> style.withColor(0x00FF00).withBold(true)
                        .withClickEvent(new ClickEvent.SuggestCommand("/x"))));
        TextColor gray = AfkColor.parse("&7").orElseThrow();

        Component painted = AfkColor.paint(text, gray);

        assertEquals("[Admin", painted.getString());
        var parts = painted.toFlatList();
        assertTrue(parts.stream().allMatch(part -> gray.equals(part.getStyle().getColor())));
        var admin = parts.stream().filter(part -> part.getString().equals("Admin")).findFirst().orElseThrow();
        assertTrue(admin.getStyle().isBold());
        assertEquals(Optional.of("/x"), Optional.of(((ClickEvent.SuggestCommand) admin.getStyle().getClickEvent()).command()));
    }
}
