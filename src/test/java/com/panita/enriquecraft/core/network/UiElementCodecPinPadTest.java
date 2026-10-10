package com.panita.enriquecraft.core.network;

import com.panita.enriquecraft.MinecraftTestSupport;
import io.netty.handler.codec.DecoderException;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/** The PIN keypad on the wire. */
final class UiElementCodecPinPadTest {

    private static UiElement roundTrip(UiElement element) {
        RegistryFriendlyByteBuf buffer = MinecraftTestSupport.buffer();
        UiElementCodec.STREAM_CODEC.encode(buffer, element);
        return UiElementCodec.STREAM_CODEC.decode(buffer);
    }

    @Test
    void aKeypadSurvivesARoundTrip() {
        UiElement.PinPad pad = new UiElement.PinPad(Component.literal("Ingresa tu PIN"), Component.literal("Intentos"),
                6, 2, 2, 3, 1, false, 40);

        assertEquals(pad, roundTrip(pad));
    }

    @Test
    void aKeypadInsideAColumnSurvives() {
        UiElement.PinPad pad = new UiElement.PinPad(Component.empty(), Component.empty(), 4, 4, 0, 0, 0, true, 0);

        UiElement.Column column = new UiElement.Column(java.util.List.of(pad));

        assertEquals(column, roundTrip(column));
    }

    @Test
    void impossibleKeypadsAreRejectedOnCreation() {
        Component empty = Component.empty();

        assertThrows(IllegalArgumentException.class, () -> new UiElement.PinPad(empty, empty, 0, 0, 0, 0, 0, false, 0));
        assertThrows(IllegalArgumentException.class, () -> new UiElement.PinPad(empty, empty, 4, 5, 0, 0, 0, false, 0));
        assertThrows(IllegalArgumentException.class, () -> new UiElement.PinPad(empty, empty, 4, 0, 4, 3, 0, false, 0));
        assertThrows(IllegalArgumentException.class, () -> new UiElement.PinPad(empty, empty, 4, 0, 0, 11, 0, false, 0));
        assertThrows(IllegalArgumentException.class, () -> new UiElement.PinPad(empty, empty, 4, 0, 0, 0, -1, false, 0));
    }

    @Test
    void impossibleKeypadsAreRejectedOnTheWire() {
        RegistryFriendlyByteBuf buffer = MinecraftTestSupport.buffer();
        buffer.writeByte(12);
        net.minecraft.network.chat.ComponentSerialization.STREAM_CODEC.encode(buffer, Component.empty());
        net.minecraft.network.chat.ComponentSerialization.STREAM_CODEC.encode(buffer, Component.empty());
        buffer.writeVarInt(99);
        buffer.writeVarInt(0);
        buffer.writeVarInt(0);
        buffer.writeVarInt(0);
        buffer.writeVarInt(0);
        buffer.writeBoolean(false);
        buffer.writeVarInt(0);

        assertThrows(DecoderException.class, () -> UiElementCodec.STREAM_CODEC.decode(buffer));
    }
}
