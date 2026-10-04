package com.panita.enriquecraft.core.network;

import com.panita.enriquecraft.MinecraftTestSupport;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;

/** The badge, the pressable detail, the scroll area and the dropdown on the wire. */
final class UiElementCodecWidgetsTest {

    private static UiElement roundTrip(UiElement element) {
        RegistryFriendlyByteBuf buffer = MinecraftTestSupport.buffer();
        UiElementCodec.STREAM_CODEC.encode(buffer, element);
        return UiElementCodec.STREAM_CODEC.decode(buffer);
    }

    @Test
    void aBadgeSurvivesARoundTrip() {
        UiElement button = new UiElement.Button(2, ButtonRole.NONE, new ItemStack(Items.SKELETON_SKULL),
                Component.literal("x"), List.of(), "✔");

        assertEquals("✔", assertInstanceOf(UiElement.Button.class, roundTrip(button)).badge());
    }

    @Test
    void aBadgeIsLimitedInLength() {
        assertThrows(IllegalArgumentException.class,
                () -> new UiElement.Button(0, ButtonRole.NONE, ItemStack.EMPTY, Component.empty(), List.of(), "12345"));
    }

    @Test
    void aPressableDetailKeepsItsIdAndTooltip() {
        UiElement detail = new UiElement.Detail(new ItemStack(Items.DIAMOND), Component.literal("Base"), List.of(), 7,
                List.of(Component.literal("cambiar")));

        UiElement.Detail decoded = assertInstanceOf(UiElement.Detail.class, roundTrip(detail));

        assertEquals(7, decoded.iconId());
        assertEquals("cambiar", decoded.iconTooltip().getFirst().getString());
    }

    @Test
    void aDetailIsNotPressableUnlessMadeSo() {
        UiElement.Detail detail = new UiElement.Detail(ItemStack.EMPTY, Component.empty(), List.of());

        assertEquals(UiElement.Detail.NOT_PRESSABLE,
                assertInstanceOf(UiElement.Detail.class, roundTrip(detail)).iconId());
    }

    @Test
    void aScrollAreaKeepsItsContentAndHeight() {
        UiElement scroll = new UiElement.Scroll(new UiElement.Grid(2, 1, List.of(new UiElement.Spacer())), 90);

        UiElement.Scroll decoded = assertInstanceOf(UiElement.Scroll.class, roundTrip(scroll));

        assertEquals(90, decoded.maxHeight());
        assertInstanceOf(UiElement.Grid.class, decoded.content());
    }

    @Test
    void aScrollAreaNeedsAPositiveHeight() {
        assertThrows(IllegalArgumentException.class, () -> new UiElement.Scroll(new UiElement.Spacer(), 0));
    }

    @Test
    void aDropdownKeepsItsOptionsAndChoice() {
        UiElement dropdown = new UiElement.Dropdown(3, Component.literal("Filtrar"),
                List.of(Component.literal("a"), Component.literal("b")), 1);

        UiElement.Dropdown decoded = assertInstanceOf(UiElement.Dropdown.class, roundTrip(dropdown));

        assertEquals(1, decoded.selected());
        assertEquals(List.of("a", "b"), decoded.options().stream().map(Component::getString).toList());
    }

    @Test
    void aDropdownNeedsTheChosenOptionToExist() {
        List<Component> two = List.of(Component.literal("a"), Component.literal("b"));

        assertThrows(IllegalArgumentException.class, () -> new UiElement.Dropdown(0, Component.empty(), two, 2));
        assertThrows(IllegalArgumentException.class, () -> new UiElement.Dropdown(0, Component.empty(), List.of(), 0));
        assertThrows(IllegalArgumentException.class, () -> new UiElement.Dropdown(0, Component.empty(),
                Collections.nCopies(UiElement.Dropdown.MAX_OPTIONS + 1, Component.empty()), 0));
    }
}
