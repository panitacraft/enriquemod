package com.panita.enriquecraft.core.ui;

import com.panita.enriquecraft.MinecraftTestSupport;
import com.panita.enriquecraft.core.network.ButtonRole;
import com.panita.enriquecraft.core.network.UiElement;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ConfirmMenuTest {

    @TempDir
    Path directory;

    private final List<String> confirmed = new ArrayList<>();
    private ConfirmMenu menu;

    @BeforeEach
    void create() {
        UiService ui = MinecraftTestSupport.uiService(directory);
        menu = new ConfirmMenu(ui, null, Component.literal("Eliminar"), new ItemStack(Items.TNT),
                Component.literal("¿Eliminar base?"), List.of(Component.literal("No se puede deshacer")),
                player -> confirmed.add("yes"));
        UiTesting.drawAsChest(menu);
    }

    @Test
    void theQuestionIsShownAsItsIconWithTheDetails() {
        ItemStack shown = UiTesting.itemAt(menu, 4).stack();

        assertEquals(Items.TNT, shown.getItem());
        assertEquals("¿Eliminar base?", shown.get(DataComponents.CUSTOM_NAME).getString());
        assertEquals(List.of("No se puede deshacer"),
                shown.get(DataComponents.LORE).lines().stream().map(Component::getString).toList());
    }

    @Test
    void cancelAndConfirmAreOnTheSecondRow() {
        assertEquals("Cancelar", UiTesting.itemAt(menu, 9).stack().get(DataComponents.CUSTOM_NAME).getString());
        assertEquals("Confirmar", UiTesting.itemAt(menu, 14).stack().get(DataComponents.CUSTOM_NAME).getString());
    }

    @Test
    void confirmingRunsTheAction() {
        UiTesting.click(menu, 14);

        assertEquals(List.of("yes"), confirmed);
    }

    @Test
    void nothingRunsUntilTheQuestionIsConfirmed() {
        assertTrue(confirmed.isEmpty());
    }

    @Test
    void theClientCompanionSeesTheRolesOfCancelAndConfirm() {
        UiElement.Column root = assertInstanceOf(UiElement.Column.class, UiTesting.root(menu));
        UiElement.Row controls = assertInstanceOf(UiElement.Row.class, root.children().getLast());

        assertEquals(ButtonRole.BACK, assertInstanceOf(UiElement.Button.class, controls.children().get(0)).role());
        assertEquals(ButtonRole.CONFIRM, assertInstanceOf(UiElement.Button.class, controls.children().get(5)).role());
    }
}
