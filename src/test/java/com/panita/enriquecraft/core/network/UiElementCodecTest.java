package com.panita.enriquecraft.core.network;

import com.panita.enriquecraft.MinecraftTestSupport;
import io.netty.handler.codec.DecoderException;
import io.netty.handler.codec.EncoderException;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class UiElementCodecTest {

    private static byte[] encode(UiElement element) {
        RegistryFriendlyByteBuf buffer = MinecraftTestSupport.buffer();
        UiElementCodec.STREAM_CODEC.encode(buffer, element);
        byte[] bytes = new byte[buffer.readableBytes()];
        buffer.getBytes(buffer.readerIndex(), bytes);
        return bytes;
    }

    private static UiElement decode(RegistryFriendlyByteBuf buffer) {
        return UiElementCodec.STREAM_CODEC.decode(buffer);
    }

    private static UiElement nested(int levels) {
        UiElement element = new UiElement.Spacer();
        for (int level = 0; level < levels; level++) {
            element = new UiElement.Column(List.of(element));
        }
        return element;
    }

    @Test
    void aTreeSurvivesARoundTrip() {
        UiElement.Button button = new UiElement.Button(3, ButtonRole.NONE, new ItemStack(Items.COMPASS), Component.literal("Base"),
                List.of(Component.literal("uno"), Component.literal("dos")));
        UiElement tree = new UiElement.Column(List.of(
                new UiElement.Grid(7, 4, List.of(button, new UiElement.Spacer(), new UiElement.Label(Component.literal("x")))),
                new UiElement.Row(List.of(new UiElement.Spacer(), button))));
        RegistryFriendlyByteBuf buffer = MinecraftTestSupport.buffer();

        UiElementCodec.STREAM_CODEC.encode(buffer, tree);
        UiElement decoded = decode(buffer);

        // Items have no equality of their own, so compare what they encode to.
        assertArrayEquals(encode(tree), encode(decoded));
        UiElement.Column column = assertInstanceOf(UiElement.Column.class, decoded);
        UiElement.Grid grid = assertInstanceOf(UiElement.Grid.class, column.children().getFirst());
        assertEquals(7, grid.columns());
        UiElement.Button decodedButton = assertInstanceOf(UiElement.Button.class, grid.children().getFirst());
        assertEquals(3, decodedButton.id());
        assertEquals("Base", decodedButton.label().getString());
        assertEquals(List.of("uno", "dos"), decodedButton.tooltip().stream().map(Component::getString).toList());
    }

    @Test
    void aButtonWithoutAnIconKeepsItEmpty() {
        UiElement button = new UiElement.Button(0, ButtonRole.NONE, ItemStack.EMPTY, Component.literal("Cerrar"), List.of());
        RegistryFriendlyByteBuf buffer = MinecraftTestSupport.buffer();

        UiElementCodec.STREAM_CODEC.encode(buffer, button);

        assertTrue(assertInstanceOf(UiElement.Button.class, decode(buffer)).icon().isEmpty());
    }

    @Test
    void anUnknownElementTypeIsRejected() {
        RegistryFriendlyByteBuf buffer = MinecraftTestSupport.buffer();
        buffer.writeByte(99);

        assertThrows(DecoderException.class, () -> decode(buffer));
    }

    @Test
    void aContainerClaimingTooManyChildrenIsRejected() {
        RegistryFriendlyByteBuf buffer = MinecraftTestSupport.buffer();
        buffer.writeByte(0);
        buffer.writeVarInt(1_000_000);

        assertThrows(DecoderException.class, () -> decode(buffer));
    }

    @Test
    void aGridThatCannotHoldItsChildrenIsRejected() {
        RegistryFriendlyByteBuf buffer = MinecraftTestSupport.buffer();
        buffer.writeByte(2);
        buffer.writeVarInt(1);
        buffer.writeVarInt(1);
        buffer.writeVarInt(2);
        buffer.writeByte(5);
        buffer.writeByte(5);

        assertThrows(DecoderException.class, () -> decode(buffer));
    }

    @Test
    void deepNestingIsRejectedWhenDecoding() {
        RegistryFriendlyByteBuf buffer = MinecraftTestSupport.buffer();
        for (int level = 0; level < 20; level++) {
            buffer.writeByte(0);
            buffer.writeVarInt(1);
        }
        buffer.writeByte(5);

        assertThrows(DecoderException.class, () -> decode(buffer));
    }

    @Test
    void deepNestingIsRejectedWhenEncoding() {
        assertThrows(EncoderException.class, () -> encode(nested(20)));
    }

    @Test
    void tooManyChildrenAreRejectedWhenEncoding() {
        UiElement tooMany = new UiElement.Row(Collections.nCopies(300, new UiElement.Spacer()));

        assertThrows(EncoderException.class, () -> encode(tooMany));
    }

    @Test
    void aGridMustFitItsChildren() {
        assertThrows(IllegalArgumentException.class,
                () -> new UiElement.Grid(1, 1, List.of(new UiElement.Spacer(), new UiElement.Spacer())));
    }

    @Test
    void aTextInputSurvivesARoundTrip() {
        UiElement input = new UiElement.TextInput(4, Component.literal("Buscar"), "base", 32);
        RegistryFriendlyByteBuf buffer = MinecraftTestSupport.buffer();

        UiElementCodec.STREAM_CODEC.encode(buffer, input);

        UiElement.TextInput decoded = assertInstanceOf(UiElement.TextInput.class, decode(buffer));
        assertEquals(4, decoded.id());
        assertEquals("Buscar", decoded.hint().getString());
        assertEquals("base", decoded.value());
        assertEquals(32, decoded.maxLength());
    }

    @Test
    void aTextInputThatCannotHoldItsValueIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> new UiElement.TextInput(0, Component.empty(), "abc", 2));
        assertThrows(IllegalArgumentException.class, () -> new UiElement.TextInput(0, Component.empty(), "", 0));
        assertThrows(IllegalArgumentException.class,
                () -> new UiElement.TextInput(0, Component.empty(), "", UiElement.TextInput.MAX_LENGTH + 1));
    }

    @Test
    void aTextInputWithAnImpossibleLimitIsRejectedWhenDecoding() {
        RegistryFriendlyByteBuf buffer = MinecraftTestSupport.buffer();
        buffer.writeByte(6);
        buffer.writeVarInt(0);
        ComponentSerialization.STREAM_CODEC.encode(buffer, Component.empty());
        buffer.writeUtf("abc");
        buffer.writeVarInt(1);

        assertThrows(DecoderException.class, () -> decode(buffer));
    }

    @Test
    void aButtonKeepsItsRoleAcrossTheWire() {
        for (ButtonRole role : ButtonRole.values()) {
            UiElement button = new UiElement.Button(1, role, ItemStack.EMPTY, Component.literal("x"), List.of());
            RegistryFriendlyByteBuf buffer = MinecraftTestSupport.buffer();

            UiElementCodec.STREAM_CODEC.encode(buffer, button);

            assertEquals(role, assertInstanceOf(UiElement.Button.class, decode(buffer)).role());
        }
    }

    @Test
    void aPageSurvivesARoundTrip() {
        RegistryFriendlyByteBuf buffer = MinecraftTestSupport.buffer();

        UiElementCodec.STREAM_CODEC.encode(buffer, new UiElement.Page(2, 5));

        assertEquals(new UiElement.Page(2, 5), decode(buffer));
    }

    @Test
    void aPageThatDoesNotExistIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> new UiElement.Page(0, 1));
        assertThrows(IllegalArgumentException.class, () -> new UiElement.Page(3, 2));
    }

    @Test
    void aPageThatDoesNotExistIsRejectedWhenDecoding() {
        RegistryFriendlyByteBuf buffer = MinecraftTestSupport.buffer();
        buffer.writeByte(7);
        buffer.writeVarInt(4);
        buffer.writeVarInt(2);

        assertThrows(DecoderException.class, () -> decode(buffer));
    }

    @Test
    void aDetailSurvivesARoundTrip() {
        UiElement detail = new UiElement.Detail(new ItemStack(Items.DIAMOND), Component.literal("Base"),
                List.of(Component.literal("uno"), Component.literal("dos")));
        RegistryFriendlyByteBuf buffer = MinecraftTestSupport.buffer();

        UiElementCodec.STREAM_CODEC.encode(buffer, detail);

        UiElement.Detail decoded = assertInstanceOf(UiElement.Detail.class, decode(buffer));
        assertEquals(Items.DIAMOND, decoded.icon().getItem());
        assertEquals("Base", decoded.title().getString());
        assertEquals(List.of("uno", "dos"), decoded.lines().stream().map(Component::getString).toList());
    }

    @Test
    void anEditableTitleAndAPressableItemSurviveARoundTrip() {
        UiElement.TextInput field = new UiElement.TextInput(7, Component.literal("Nombre"), "Mi base", 32);
        UiElement detail = new UiElement.Detail(new ItemStack(Items.DIAMOND), Component.literal("Base"), List.of(), 3,
                List.of(Component.literal("cambiar")), java.util.Optional.of(field));
        RegistryFriendlyByteBuf buffer = MinecraftTestSupport.buffer();

        UiElementCodec.STREAM_CODEC.encode(buffer, detail);

        UiElement.Detail decoded = assertInstanceOf(UiElement.Detail.class, decode(buffer));
        assertEquals(3, decoded.iconId());
        assertEquals(field.id(), decoded.editableTitle().orElseThrow().id());
        assertEquals("Mi base", decoded.editableTitle().orElseThrow().value());
        assertEquals(32, decoded.editableTitle().orElseThrow().maxLength());
    }

    @Test
    void aDetailWithoutAnEditableTitleDecodesWithout() {
        RegistryFriendlyByteBuf buffer = MinecraftTestSupport.buffer();

        UiElementCodec.STREAM_CODEC.encode(buffer, new UiElement.Detail(new ItemStack(Items.DIAMOND), Component.literal("Base"), List.of()));

        assertEquals(java.util.Optional.empty(), assertInstanceOf(UiElement.Detail.class, decode(buffer)).editableTitle());
    }

    @Test
    void everyButtonRoleSurvivesARoundTrip() {
        for (com.panita.enriquecraft.core.network.ButtonRole role : com.panita.enriquecraft.core.network.ButtonRole.values()) {
            RegistryFriendlyByteBuf buffer = MinecraftTestSupport.buffer();

            UiElementCodec.STREAM_CODEC.encode(buffer, new UiElement.Button(1, role, new ItemStack(Items.DIAMOND),
                    Component.literal("x"), List.of()));

            assertEquals(role, assertInstanceOf(UiElement.Button.class, decode(buffer)).role());
        }
    }

    @Test
    void aTintedButtonSurvivesARoundTripAndAnUntintedOneStaysUntinted() {
        RegistryFriendlyByteBuf tinted = MinecraftTestSupport.buffer();
        RegistryFriendlyByteBuf plain = MinecraftTestSupport.buffer();

        UiElementCodec.STREAM_CODEC.encode(tinted, new UiElement.Button(1, com.panita.enriquecraft.core.network.ButtonRole.NONE,
                new ItemStack(Items.CHEST), Component.literal("x"), List.of(), "", 0x9C6B3A));
        UiElementCodec.STREAM_CODEC.encode(plain, new UiElement.Button(2, com.panita.enriquecraft.core.network.ButtonRole.NONE,
                new ItemStack(Items.CHEST), Component.literal("x"), List.of()));

        assertEquals(0x9C6B3A, assertInstanceOf(UiElement.Button.class, decode(tinted)).tint());
        assertEquals(UiElement.Button.NO_TINT, assertInstanceOf(UiElement.Button.class, decode(plain)).tint());
    }

    @Test
    void aTintMustBeAnRgbColor() {
        org.junit.jupiter.api.Assertions.assertThrows(IllegalArgumentException.class, () -> new UiElement.Button(1,
                com.panita.enriquecraft.core.network.ButtonRole.NONE, new ItemStack(Items.CHEST), Component.literal("x"),
                List.of(), "", 0x1000000));
    }
}
