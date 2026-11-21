package com.panita.enriquecraft.staff.gui;

import com.panita.enriquecraft.MinecraftTestSupport;
import com.panita.enriquecraft.core.framework.config.ConfigManager;
import com.panita.enriquecraft.core.framework.data.WorldData;
import com.panita.enriquecraft.core.network.ButtonRole;
import com.panita.enriquecraft.core.network.UiElement;
import com.panita.enriquecraft.core.ui.UiService;
import com.panita.enriquecraft.core.ui.UiTesting;
import com.panita.enriquecraft.staff.config.StaffConfig;
import com.panita.enriquecraft.staff.message.CoordinateView;
import com.panita.enriquecraft.staff.message.CustomItemView;
import com.panita.enriquecraft.staff.message.DeathInventoryView;
import com.panita.enriquecraft.staff.service.CoordinateService;
import com.panita.enriquecraft.staff.service.CustomItemService;
import com.panita.enriquecraft.staff.service.DeathInventoryService;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StaffMenuTest {

    @TempDir
    Path directory;

    private UiService ui;
    private StaffMenus menus;

    @BeforeEach
    void create() {
        Path config = directory.resolve("config");
        ui = MinecraftTestSupport.uiService(config);
        WorldData worldData = new WorldData();
        worldData.attach(directory.resolve("world"), MinecraftTestSupport.ops());
        var messenger = MinecraftTestSupport.messenger(config);
        CoordinateService coordinates = new CoordinateService(worldData);
        menus = new StaffMenus(ui, coordinates, new CoordinateView(messenger, coordinates),
                new CustomItemService(worldData), new CustomItemView(messenger),
                new DeathInventoryService(worldData, new ConfigManager(config).bind("staff", StaffConfig.class)),
                new DeathInventoryView(messenger));
    }

    private StaffMenu drawn(Set<StaffSection> allowed) {
        StaffMenu menu = new StaffMenu(ui, menus, allowed);
        UiTesting.drawAsChest(menu);
        return menu;
    }

    private static Item itemAt(StaffMenu menu, int slot) {
        return UiTesting.itemAt(menu, slot).stack().getItem();
    }

    @Test
    void everyToolIsOneIconSpreadAcrossTheFirstRow() {
        StaffMenu menu = drawn(EnumSet.allOf(StaffSection.class));

        assertEquals(Items.COMPASS, itemAt(menu, 1));
        assertEquals(Items.ENCHANTED_BOOK, itemAt(menu, 4));
        assertEquals(Items.SKELETON_SKULL, itemAt(menu, 7));
        assertEquals(Items.BARRIER, itemAt(menu, 13));
    }

    @Test
    void aToolIsNamedAndExplainedBeforeTheClickHint() {
        StaffMenu menu = drawn(EnumSet.allOf(StaffSection.class));

        ItemStack stack = UiTesting.itemAt(menu, 1).stack();

        assertEquals("Coordenadas", stack.get(DataComponents.CUSTOM_NAME).getString());
        assertEquals(List.of("Lugares guardados del servidor", "", "◀ Clic Izq. para abrir"),
                stack.get(DataComponents.LORE).lines().stream().map(Component::getString).toList());
    }

    @Test
    void onlyThePermittedToolsAreOffered() {
        StaffMenu menu = drawn(EnumSet.of(StaffSection.ITEMS));

        assertEquals(Items.ENCHANTED_BOOK, itemAt(menu, 4));
        assertEquals(Items.STAINED_GLASS_PANE.black(), itemAt(menu, 1));
        assertEquals(Items.STAINED_GLASS_PANE.black(), itemAt(menu, 7));
    }

    @Test
    void twoToolsAreCentered() {
        StaffMenu menu = drawn(EnumSet.of(StaffSection.COORDINATES, StaffSection.DEATHS));

        assertEquals(Items.COMPASS, itemAt(menu, 2));
        assertEquals(Items.SKELETON_SKULL, itemAt(menu, 6));
    }

    @Test
    void theClientCompanionSeesPressableIconsWithTitlesAndACloseControl() {
        StaffMenu menu = new StaffMenu(ui, menus, EnumSet.allOf(StaffSection.class));

        UiElement.Column root = assertInstanceOf(UiElement.Column.class, UiTesting.root(menu));
        UiElement.Row tools = assertInstanceOf(UiElement.Row.class, root.children().getFirst());
        List<UiElement.Detail> cards = tools.children().stream().filter(UiElement.Detail.class::isInstance)
                .map(UiElement.Detail.class::cast).toList();
        UiElement.Row controls = assertInstanceOf(UiElement.Row.class, root.children().getLast());

        assertEquals(List.of("Coordenadas", "Objetos", "Muertes"), cards.stream().map(card -> card.title().getString()).toList());
        cards.forEach(card -> assertTrue(card.iconId() != UiElement.Detail.NOT_PRESSABLE));
        assertEquals(ButtonRole.CLOSE, assertInstanceOf(UiElement.Button.class, controls.children().get(4)).role());
    }
}
