package com.panita.enriquecraft.core.network;

import com.panita.enriquecraft.MinecraftTestSupport;
import io.netty.handler.codec.DecoderException;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** The text area, the toggle, the large grid and the header roles on the wire (protocol 13). */
final class UiElementCodecFormTest {

    private static UiElement roundTrip(UiElement element) {
        RegistryFriendlyByteBuf buffer = MinecraftTestSupport.buffer();
        UiElementCodec.STREAM_CODEC.encode(buffer, element);
        return UiElementCodec.STREAM_CODEC.decode(buffer);
    }

    @Test
    void theProtocolVersionIsThirteen() {
        assertEquals(13, NetworkProtocol.VERSION);
    }

    @Test
    void aTextAreaKeepsEverythingIncludingItsNewlines() {
        UiElement.TextArea area = new UiElement.TextArea(6, Component.literal("Biografía"), "uno\ndos\n\ntres", 500, 5, 220);

        assertEquals(area, assertInstanceOf(UiElement.TextArea.class, roundTrip(area)));
    }

    @Test
    void aTextAreaHoldsUpToTheTextAreaLimitButATextInputStaysAtItsOwn() {
        assertEquals(1024, UiElement.TextArea.MAX_LENGTH);
        assertEquals(256, UiElement.TextInput.MAX_LENGTH);

        UiElement.TextArea full = new UiElement.TextArea(0, Component.empty(), "x".repeat(1024), 1024, 4, 200);
        assertEquals(1024, assertInstanceOf(UiElement.TextArea.class, roundTrip(full)).value().length());
        assertThrows(IllegalArgumentException.class,
                () -> new UiElement.TextInput(0, Component.empty(), "", UiElement.TextArea.MAX_LENGTH));
    }

    @Test
    void aTextAreaRejectsValuesAndShapesOutOfRange() {
        assertThrows(IllegalArgumentException.class, () -> new UiElement.TextArea(0, Component.empty(), "", 1025, 4, 200));
        assertThrows(IllegalArgumentException.class, () -> new UiElement.TextArea(0, Component.empty(), "", 0, 4, 200));
        assertThrows(IllegalArgumentException.class, () -> new UiElement.TextArea(0, Component.empty(), "abc", 2, 4, 200));
        assertThrows(IllegalArgumentException.class, () -> new UiElement.TextArea(0, Component.empty(), "", 10, 0, 200));
        assertThrows(IllegalArgumentException.class, () -> new UiElement.TextArea(0, Component.empty(), "", 10, 17, 200));
        assertThrows(IllegalArgumentException.class, () -> new UiElement.TextArea(0, Component.empty(), "", 10, 4, 59));
        assertThrows(IllegalArgumentException.class, () -> new UiElement.TextArea(0, Component.empty(), "", 10, 4, 401));
    }

    @Test
    void aTextAreaWithAnImpossibleShapeFailsToDecode() {
        RegistryFriendlyByteBuf buffer = MinecraftTestSupport.buffer();
        buffer.writeByte(14);
        buffer.writeVarInt(1);
        net.minecraft.network.chat.ComponentSerialization.STREAM_CODEC.encode(buffer, Component.empty());
        buffer.writeUtf("", UiElement.TextArea.MAX_LENGTH);
        buffer.writeVarInt(5000);
        buffer.writeVarInt(4);
        buffer.writeVarInt(200);

        assertThrows(DecoderException.class, () -> UiElementCodec.STREAM_CODEC.decode(buffer));
    }

    @Test
    void aToggleKeepsItsLabelAndState() {
        UiElement.Toggle on = new UiElement.Toggle(4, Component.literal("Mostrar cumpleaños"), true);
        UiElement.Toggle off = new UiElement.Toggle(5, Component.literal("Mostrar"), false);

        assertEquals(on, roundTrip(on));
        assertFalse(assertInstanceOf(UiElement.Toggle.class, roundTrip(off)).checked());
    }

    @Test
    void aGridIsNotLargeUnlessMadeSo() {
        UiElement.Grid plain = new UiElement.Grid(2, 1, List.of(new UiElement.Spacer()));

        assertFalse(assertInstanceOf(UiElement.Grid.class, roundTrip(plain)).large());
        UiElement.Grid large = assertInstanceOf(UiElement.Grid.class, roundTrip(plain.withLargeTiles()));
        assertTrue(large.large());
        assertEquals(2, large.columns());
        assertEquals(1, large.children().size());
    }

    @Test
    void theNewRolesComeAfterEveryOldOneAndSurviveARoundTrip() {
        // Roles travel by position, so the old values must keep theirs.
        assertEquals(8, ButtonRole.SUCCESS.ordinal());
        assertEquals(9, ButtonRole.REFRESH.ordinal());
        assertEquals(10, ButtonRole.EDIT.ordinal());
        for (ButtonRole role : List.of(ButtonRole.REFRESH, ButtonRole.EDIT)) {
            UiElement button = new UiElement.Button(1, role, ItemStack.EMPTY, Component.literal("x"), List.of());

            assertEquals(role, assertInstanceOf(UiElement.Button.class, roundTrip(button)).role());
        }
    }

    @Test
    void aValueTheSizeOfATextAreaFitsTheSubmitPayload() {
        UiSubmitC2S submit = new UiSubmitC2S(1, 2, "a\n".repeat(512));
        RegistryFriendlyByteBuf buffer = MinecraftTestSupport.buffer();

        UiSubmitC2S.STREAM_CODEC.encode(buffer, submit);

        assertEquals(submit, UiSubmitC2S.STREAM_CODEC.decode(buffer));
    }
}
