package com.panita.enriquecraft.core.network;

import com.panita.enriquecraft.MinecraftTestSupport;
import io.netty.handler.codec.DecoderException;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;

final class UiPayloadsTest {

    private static <T> T roundTrip(StreamCodec<? super RegistryFriendlyByteBuf, T> codec, T payload) {
        RegistryFriendlyByteBuf buffer = MinecraftTestSupport.buffer();
        codec.encode(buffer, payload);
        return codec.decode(buffer);
    }

    @Test
    void openCarriesTheSessionTheTitleAndTheTree() {
        UiElement tree = new UiElement.Row(List.of(
                new UiElement.Button(5, ButtonRole.NONE, new ItemStack(Items.COMPASS), Component.literal("Base"), List.of())));

        OpenUiS2C decoded = roundTrip(OpenUiS2C.STREAM_CODEC, new OpenUiS2C(12, Component.literal("Título"), new ItemStack(Items.COMPASS), tree));

        assertEquals(12, decoded.sessionId());
        assertEquals("Título", decoded.title().getString());
        assertEquals(Items.COMPASS, decoded.icon().getItem());
        UiElement.Row row = assertInstanceOf(UiElement.Row.class, decoded.root());
        assertEquals(5, assertInstanceOf(UiElement.Button.class, row.children().getFirst()).id());
    }

    @Test
    void updateCarriesTheSessionAndTheTree() {
        UpdateUiS2C decoded = roundTrip(UpdateUiS2C.STREAM_CODEC, new UpdateUiS2C(3, new UiElement.Spacer()));

        assertEquals(3, decoded.sessionId());
        assertInstanceOf(UiElement.Spacer.class, decoded.root());
    }

    @Test
    void closeCarriesTheSession() {
        assertEquals(new CloseUiS2C(9), roundTrip(CloseUiS2C.STREAM_CODEC, new CloseUiS2C(9)));
    }

    @Test
    void aClickCarriesEverythingTheServerChecks() {
        UiClickC2S click = new UiClickC2S(4, 17, 1, true);

        assertEquals(click, roundTrip(UiClickC2S.STREAM_CODEC, click));
    }

    @Test
    void closedCarriesTheSession() {
        assertEquals(new UiClosedC2S(6), roundTrip(UiClosedC2S.STREAM_CODEC, new UiClosedC2S(6)));
    }

    @Test
    void aSubmitCarriesTheTypedText() {
        UiSubmitC2S submit = new UiSubmitC2S(2, 8, "base ñ");

        assertEquals(submit, roundTrip(UiSubmitC2S.STREAM_CODEC, submit));
    }

    @Test
    void aSubmitLongerThanAnyFieldIsRejectedWhenDecoding() {
        RegistryFriendlyByteBuf buffer = MinecraftTestSupport.buffer();
        buffer.writeVarInt(1);
        buffer.writeVarInt(1);
        buffer.writeUtf("x".repeat(1000), 1000);

        assertThrows(DecoderException.class,
                () -> UiSubmitC2S.STREAM_CODEC.decode(buffer));
    }
}
