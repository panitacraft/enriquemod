package com.panita.enriquecraft.core.message;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StyledWrapTest {

    private static int colorOf(ChatFormatting formatting) {
        return net.minecraft.network.chat.Style.EMPTY.withColor(formatting).getColor().getValue();
    }

    @Test
    void textIsBrokenAtSpacesToTheWidth() {
        assertEquals("aaa bbb\nccc", StyledWrap.wrap(Component.literal("aaa bbb ccc"), 7).getString());
    }

    @Test
    void textThatFitsIsLeftAlone() {
        assertEquals("aaa bbb", StyledWrap.wrap(Component.literal("aaa bbb"), 20).getString());
    }

    @Test
    void aLineBreakInTheTextIsKept() {
        assertEquals("aaa\nbbb", StyledWrap.wrap(Component.literal("aaa\nbbb"), 20).getString());
    }

    @Test
    void aWordLongerThanTheWidthStaysWhole() {
        assertEquals("abcdefghij", StyledWrap.wrap(Component.literal("abcdefghij"), 4).getString());
        assertEquals("ab\nabcdefghij", StyledWrap.wrap(Component.literal("ab abcdefghij"), 4).getString());
    }

    @Test
    void everyPartKeepsItsStyleWhereverTheLineBreaks() {
        Component text = Component.empty().append(Component.literal("aaa bbb ").withStyle(ChatFormatting.RED))
                .append(Component.literal("ccc ddd").withStyle(ChatFormatting.BLUE));

        Component wrapped = StyledWrap.wrap(text, 7);

        assertEquals("aaa bbb\nccc ddd", wrapped.getString());
        var parts = wrapped.toFlatList();
        assertTrue(parts.stream().filter(part -> part.getString().equals("bbb")).allMatch(part -> part.getStyle().getColor().getValue() == colorOf(ChatFormatting.RED)));
        assertTrue(parts.stream().filter(part -> part.getString().equals("ccc")).allMatch(part -> part.getStyle().getColor().getValue() == colorOf(ChatFormatting.BLUE)));
    }
}
