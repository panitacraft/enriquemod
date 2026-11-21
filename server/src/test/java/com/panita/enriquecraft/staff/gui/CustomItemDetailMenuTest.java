package com.panita.enriquecraft.staff.gui;

import com.panita.enriquecraft.MinecraftTestSupport;
import com.panita.enriquecraft.core.framework.data.WorldData;
import com.panita.enriquecraft.core.message.Timestamps;
import com.panita.enriquecraft.core.network.ButtonRole;
import com.panita.enriquecraft.core.network.UiElement;
import com.panita.enriquecraft.core.ui.CopyTextTesting;
import com.panita.enriquecraft.core.ui.UiService;
import com.panita.enriquecraft.core.ui.UiTesting;
import com.panita.enriquecraft.staff.message.CustomItemView;
import com.panita.enriquecraft.staff.service.CustomItemService;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ItemLore;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CustomItemDetailMenuTest {

    private static final Instant NOW = Instant.parse("2026-09-30T04:12:00Z");

    @TempDir
    Path directory;

    private UiService ui;
    private CustomItemService service;
    private CustomItemView view;

    @BeforeEach
    void create() {
        ui = MinecraftTestSupport.uiService(directory.resolve("config"));
        WorldData worldData = new WorldData();
        worldData.attach(directory.resolve("world"), MinecraftTestSupport.ops());
        service = new CustomItemService(worldData);
        view = new CustomItemView(MinecraftTestSupport.messenger(directory.resolve("config")));
    }

    private void save(String name, ItemStack held) {
        service.save(held, name, UUID.randomUUID(), "<red>Ana", NOW);
    }

    private CustomItemDetailMenu detail(String name) {
        CustomItemDetailMenu menu = new CustomItemDetailMenu(ui, service, view, name, new CustomItemsMenu(ui, service, view));
        UiTesting.drawAsChest(menu);
        return menu;
    }

    private static ItemStack stack(CustomItemDetailMenu menu, int slot) {
        return UiTesting.itemAt(menu, slot).stack();
    }

    private static List<String> lore(ItemStack stack) {
        return stack.get(DataComponents.LORE).lines().stream().map(Component::getString).toList();
    }

    @Test
    void theItemIsShownLargeUnderItsOwnName() {
        ItemStack held = new ItemStack(Items.DIAMOND_SWORD);
        held.set(DataComponents.CUSTOM_NAME, Component.literal("Filo de fuego"));
        save("espada", held);

        ItemStack shown = stack(detail("espada"), 4);

        assertEquals(Items.DIAMOND_SWORD, shown.getItem());
        assertEquals("Filo de fuego", shown.get(DataComponents.CUSTOM_NAME).getString());
    }

    @Test
    void ourOwnDataComesFirstThenAGapThenTheItemsData() {
        ItemStack held = new ItemStack(Items.DIAMOND_SWORD);
        held.set(DataComponents.LORE, new ItemLore(List.of(Component.literal("Lore propio"))));
        save("espada", held);

        List<String> lore = lore(stack(detail("espada"), 4));

        assertEquals("ID: enriquecraft:espada", lore.get(0));
        assertTrue(lore.get(1).startsWith("Guardado por: ") && lore.get(1).endsWith("<red>Ana"), lore.get(1));
        assertEquals("Fecha: " + Timestamps.dateTime(NOW), lore.get(2));
        assertEquals("", lore.get(3));
    }

    @Test
    void theClientCompanionSeesTheDataADividerAnArrowAScrollingAreaAndTheControls() {
        save("espada", new ItemStack(Items.DIAMOND_SWORD));
        CustomItemDetailMenu menu = detail("espada");

        UiElement.Column root = assertInstanceOf(UiElement.Column.class, UiTesting.root(menu));

        assertInstanceOf(UiElement.Detail.class, root.children().get(0));
        assertInstanceOf(UiElement.Divider.class, root.children().get(1));
        UiElement.Button arrow = assertInstanceOf(UiElement.Button.class, root.children().get(2));
        assertEquals("▴ Ocultar detalles", arrow.label().getString());
        assertInstanceOf(UiElement.Scroll.class, root.children().get(3));
        UiElement.Row controls = assertInstanceOf(UiElement.Row.class, root.children().getLast());
        assertEquals(ButtonRole.BACK, assertInstanceOf(UiElement.Button.class, controls.children().getFirst()).role());
    }

    @Test
    void theArrowHidesTheExtraDataAndShowsItAgain() {
        save("espada", new ItemStack(Items.DIAMOND_SWORD));
        CustomItemDetailMenu menu = detail("espada");
        UiElement.Column before = assertInstanceOf(UiElement.Column.class, UiTesting.root(menu));
        UiTesting.press(menu, assertInstanceOf(UiElement.Button.class, before.children().get(2)).id());

        UiElement.Column hidden = assertInstanceOf(UiElement.Column.class, UiTesting.root(menu));

        assertEquals(4, hidden.children().size(), "the item, the divider, the arrow and the controls");
        UiElement.Button arrow = assertInstanceOf(UiElement.Button.class, hidden.children().get(2));
        assertEquals("▾ Mostrar detalles", arrow.label().getString());
        UiTesting.press(menu, arrow.id());
        assertEquals(5, assertInstanceOf(UiElement.Column.class, UiTesting.root(menu)).children().size());
    }

    @Test
    void relatedDataIsGroupedAndSplitIntoTwoColumnsThatStartAtTheSameEdge() {
        ItemStack sword = new ItemStack(Items.DIAMOND_SWORD);
        sword.setDamageValue(5);
        save("espada", sword);
        UiElement.Column root = assertInstanceOf(UiElement.Column.class, UiTesting.root(detail("espada")));
        UiElement.Scroll scroll = assertInstanceOf(UiElement.Scroll.class, root.children().get(3));

        UiElement.Row columns = assertInstanceOf(UiElement.Row.class, scroll.content());

        UiElement.Column state = assertInstanceOf(UiElement.Column.class, columns.children().get(0));
        UiElement.Column attributes = assertInstanceOf(UiElement.Column.class, columns.children().get(2));
        assertEquals(UiElement.Align.START, state.align());
        assertEquals(UiElement.Align.START, attributes.align());
        assertTrue(assertInstanceOf(UiElement.Label.class, state.children().getFirst()).text().getString().contains("Durabilidad"));
        assertTrue(assertInstanceOf(UiElement.Label.class, attributes.children().getFirst()).text().getString().contains("Atributos"));
    }

    @Test
    void groupsOfLinesAreSeparatedByBlankLinesInAChest() {
        ItemStack sword = new ItemStack(Items.DIAMOND_SWORD);
        sword.setDamageValue(5);
        save("espada", sword);

        List<String> lore = lore(stack(detail("espada"), 4));

        int attributes = lore.indexOf(lore.stream().filter(line -> line.contains("Atributos")).findFirst().orElseThrow());
        assertEquals("", lore.get(attributes - 1), "a blank line sets the group off");
    }

    @Test
    void theControlsAreBackAndDeleteAndTheItemItselfGivesACopy() {
        save("espada", new ItemStack(Items.DIAMOND_SWORD));

        CustomItemDetailMenu menu = detail("espada");

        assertEquals(Items.OAK_DOOR, stack(menu, 9).getItem());
        assertEquals(Items.LAVA_BUCKET, stack(menu, 13).getItem());
        assertEquals(Items.STAINED_GLASS_PANE.black(), stack(menu, 12).getItem(), "there is no separate button to get a copy");
        assertEquals("◀ Clic Izq. para obtener copia", lore(stack(menu, 4)).getLast());
    }

    @Test
    void theClientCompanionCanPressTheItemAndCopyTheId() {
        save("espada", new ItemStack(Items.DIAMOND_SWORD));
        UiElement.Column root = assertInstanceOf(UiElement.Column.class, UiTesting.root(detail("espada")));

        UiElement.Detail detail = assertInstanceOf(UiElement.Detail.class, root.children().getFirst());
        UiElement.Row controls = assertInstanceOf(UiElement.Row.class, root.children().getLast());

        assertTrue(detail.iconId() != UiElement.Detail.NOT_PRESSABLE);
        Component id = detail.lines().getFirst();
        assertEquals("ID: enriquecraft:espada", id.getString());
        assertEquals(List.of("enriquecraft:espada -> enriquecraft:espada"), CopyTextTesting.clickable(id),
                "only the id copies, not the label before it");
        assertEquals(ButtonRole.DANGER, assertInstanceOf(UiElement.Button.class, controls.children().get(4)).role());
    }

    @Test
    void everyOtherSlotIsFiller() {
        save("espada", new ItemStack(Items.DIAMOND_SWORD));

        CustomItemDetailMenu menu = detail("espada");

        Item filler = Items.STAINED_GLASS_PANE.black();
        for (int slot : List.of(0, 3, 5, 8, 10, 11, 14, 15, 17)) {
            assertEquals(filler, stack(menu, slot).getItem(), "slot " + slot);
        }
    }

    @Test
    void showingTheDetailNeverChangesTheSavedItem() {
        save("espada", new ItemStack(Items.DIAMOND_SWORD));

        detail("espada");

        assertEquals(List.of(), service.find("espada").orElseThrow().stack()
                .getOrDefault(DataComponents.LORE, ItemLore.EMPTY).lines());
    }

    @Test
    void anItemRemovedWhileShownLeavesAMessageAndTheBackButtonWithoutResizing() {
        save("espada", new ItemStack(Items.DIAMOND_SWORD));
        CustomItemDetailMenu menu = detail("espada");

        service.remove("espada");
        menu.refresh();

        assertEquals(Items.BARRIER, stack(menu, 4).getItem());
        assertEquals(Items.OAK_DOOR, stack(menu, 9).getItem());
    }
}
