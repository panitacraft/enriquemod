package com.panita.enriquecraft.core.gui;

import com.panita.enriquecraft.MinecraftTestSupport;
import com.panita.enriquecraft.core.message.Message;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ItemLore;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ItemBuilderTest {

    private static final Style UPRIGHT = Style.EMPTY.withItalic(false);

    @TempDir
    Path directory;

    private MenuFactory factory;

    @BeforeEach
    void createFactory() {
        factory = MinecraftTestSupport.menuFactory(directory);
    }

    @Test
    void nameIsParsedAndNeverItalic() {
        ItemStack stack = factory.item(Items.DIAMOND).name("<red>Diamante</red>").build();

        Component name = stack.get(DataComponents.CUSTOM_NAME);

        assertEquals("Diamante", name.getString());
        assertEquals(UPRIGHT, name.getStyle());
    }

    @Test
    void loreLinesAreParsedAndNeverItalic() {
        ItemStack stack = factory.item(Items.DIAMOND).lore("Primera", "&aSegunda").build();

        List<Component> lines = stack.get(DataComponents.LORE).lines();

        assertEquals(List.of("Primera", "Segunda"), lines.stream().map(Component::getString).toList());
        assertTrue(lines.stream().allMatch(line -> UPRIGHT.equals(line.getStyle())));
    }

    @Test
    void loreFromMessagesResolvesArguments() {
        ItemStack stack = factory.item(Items.DIAMOND)
                .lore(Message.plain("Jugador {name}").with("name", "<red>Ana</red>"))
                .build();

        assertEquals("Jugador <red>Ana</red>", stack.get(DataComponents.LORE).lines().get(0).getString());
    }

    @Test
    void loreIsAddedAfterLoreAlreadyOnTheItem() {
        ItemStack original = new ItemStack(Items.DIAMOND);
        original.set(DataComponents.LORE, new ItemLore(List.of(Component.literal("Existente"))));

        ItemStack stack = factory.item(original).lore("Nueva").build();

        assertEquals(List.of("Existente", "Nueva"),
                stack.get(DataComponents.LORE).lines().stream().map(Component::getString).toList());
    }

    @Test
    void copyingAnItemLeavesTheOriginalUntouched() {
        ItemStack original = new ItemStack(Items.DIAMOND, 3);

        ItemStack built = factory.item(original).name("Otro").amount(5).build();

        assertEquals(3, original.getCount());
        assertEquals(5, built.getCount());
        assertTrue(original.get(DataComponents.CUSTOM_NAME) == null);
    }

    @Test
    void glintAndHiddenTooltipAreApplied() {
        ItemStack stack = factory.item(Items.DIAMOND).glint().hideTooltip().build();

        assertEquals(Boolean.TRUE, stack.get(DataComponents.ENCHANTMENT_GLINT_OVERRIDE));
        assertTrue(stack.get(DataComponents.TOOLTIP_DISPLAY).hideTooltip());
    }

    @Test
    void fillerIsABlackPaneWithNoTooltip() {
        ItemStack filler = factory.filler();

        assertEquals(Items.STAINED_GLASS_PANE.black(), filler.getItem());
        assertTrue(filler.get(DataComponents.TOOLTIP_DISPLAY).hideTooltip());
    }
}
