package com.panita.enriquecraft.staff.gui;

import com.panita.enriquecraft.MinecraftTestSupport;
import com.panita.enriquecraft.core.framework.data.WorldData;
import com.panita.enriquecraft.core.network.ButtonRole;
import com.panita.enriquecraft.core.network.UiElement;
import com.panita.enriquecraft.core.ui.UiService;
import com.panita.enriquecraft.core.ui.UiTesting;
import com.panita.enriquecraft.staff.data.PlayerRef;
import com.panita.enriquecraft.staff.data.SavedCoordinate;
import com.panita.enriquecraft.staff.message.CoordinateView;
import com.panita.enriquecraft.staff.service.CoordinateIcons;
import com.panita.enriquecraft.staff.service.CoordinateService;
import com.panita.enriquecraft.staff.service.PlayerLookup;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.NbtOps;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** The menu that asks for a player's name and the menu of structure maps, which end a coordinate's icon choice. */
class CoordinateHeadAndMapMenuTest {

    private static final PlayerRef NOTCH = new PlayerRef("Notch", UUID.fromString("069a79f4-44e9-4726-a5be-fca90e38aaf5"));

    @TempDir
    Path directory;

    private UiService ui;
    private CoordinateService service;
    private CoordinatesMenu parent;

    @BeforeEach
    void create() {
        ui = MinecraftTestSupport.uiService(directory.resolve("config"));
        WorldData worldData = new WorldData();
        worldData.attach(directory.resolve("world"), NbtOps.INSTANCE);
        service = new CoordinateService(worldData);
        service.add(new SavedCoordinate("base", Level.OVERWORLD, 0, 64, 0, 0, 0, UUID.randomUUID(), "Ana",
                Instant.parse("2026-09-30T04:12:00Z"), Items.COMPASS));
        parent = new CoordinatesMenu(ui, service, new CoordinateView(MinecraftTestSupport.messenger(directory.resolve("config")), service));
    }

    /** A menu whose server knows only Notch. */
    private CoordinateHeadMenu headMenu() {
        PlayerLookup knowsNotch = (asker, name) -> name.equalsIgnoreCase("notch") ? Optional.of(NOTCH) : Optional.empty();
        return new CoordinateHeadMenu(ui, service, "base", parent, parent, knowsNotch);
    }

    private static List<String> lore(ItemStack stack) {
        return stack.get(DataComponents.LORE).lines().stream().map(Component::getString).toList();
    }

    @Test
    void aNameTheServerKnowsMakesTheCoordinateShowThatPlayersHead() {
        assertEquals(CoordinateHeadMenu.Problem.NONE, headMenu().choose(" notch ", null));

        SavedCoordinate saved = service.find("base").orElseThrow();
        assertEquals(Items.PLAYER_HEAD, saved.icon());
        assertEquals(NOTCH, saved.iconPlayer().orElseThrow(), "the name as the server knows it, with the id the skin is found by");
        assertEquals(Items.PLAYER_HEAD, saved.iconStack().getItem());
    }

    @Test
    void aNameThatCannotBeAPlayersIsRefusedBeforeAskingTheServer() {
        CoordinateHeadMenu menu = headMenu();

        assertEquals(CoordinateHeadMenu.Problem.INVALID, menu.choose("no", null));
        assertEquals(CoordinateHeadMenu.Problem.INVALID, menu.choose("has space", null));
        assertEquals(CoordinateHeadMenu.Problem.INVALID, menu.choose("x".repeat(17), null));

        assertTrue(service.find("base").orElseThrow().iconPlayer().isEmpty());
    }

    @Test
    void aValidNameTheServerDoesNotKnowChangesNothing() {
        assertEquals(CoordinateHeadMenu.Problem.UNKNOWN, headMenu().choose("Nobody_Here", null));

        assertTrue(service.find("base").orElseThrow().iconPlayer().isEmpty());
        assertEquals(Items.COMPASS, service.find("base").orElseThrow().icon());
    }

    @Test
    void aProblemStaysOnTheMenuAndSaysWhatIsWrong() {
        CoordinateHeadMenu menu = headMenu();
        UiTesting.drawAsChest(menu);

        UiTesting.submit(menu, "no");
        assertEquals(List.of("Escribe el nombre del jugador cuya cabeza será el icono.",
                "Nombre no válido: de 3 a 16 letras, números o _."), lore(UiTesting.itemAt(menu, 4).stack()));

        UiTesting.submit(menu, "Nobody_Here");
        assertEquals(List.of("Escribe el nombre del jugador cuya cabeza será el icono.",
                "No se encontró a ningún jugador con ese nombre."), lore(UiTesting.itemAt(menu, 4).stack()));
    }

    @Test
    void choosingAPlainIconAfterAHeadForgetsThePlayer() {
        service.updateIconHead("base", NOTCH);

        service.updateIcon("base", Items.DIAMOND);

        SavedCoordinate saved = service.find("base").orElseThrow();
        assertTrue(saved.iconPlayer().isEmpty());
        assertEquals(Items.DIAMOND, saved.iconStack().getItem());
    }

    @Test
    void theClientCompanionShowsTheLabelWithTheFieldBelowAndCancelAndConfirmInTheFooter() {
        UiElement.Column root = assertInstanceOf(UiElement.Column.class, UiTesting.root(headMenu()));
        UiElement.Column form = assertInstanceOf(UiElement.Column.class, root.children().getFirst());
        UiElement.Row controls = assertInstanceOf(UiElement.Row.class, root.children().getLast());

        assertEquals("Nombre de Jugador", assertInstanceOf(UiElement.Label.class, form.children().get(0)).text().getString());
        UiElement.TextInput input = assertInstanceOf(UiElement.TextInput.class, form.children().get(1));
        assertEquals(SavedCoordinate.MAX_PLAYER_NAME, input.maxLength());
        assertEquals(2, form.children().size(), "nothing else is in the way between the label and the field");

        UiElement.Button cancel = assertInstanceOf(UiElement.Button.class, controls.children().get(0));
        UiElement.Button confirm = assertInstanceOf(UiElement.Button.class, controls.children().get(5));
        assertEquals(ButtonRole.CANCEL, cancel.role());
        assertEquals(ButtonRole.CONFIRM, confirm.role());
        assertEquals(input.id(), confirm.submits(), "confirming sends what the field holds");
    }

    @Test
    void aProblemShowsBelowTheFieldForTheClientCompanion() {
        CoordinateHeadMenu menu = headMenu();
        UiTesting.submit(menu, "no");

        UiElement.Column root = assertInstanceOf(UiElement.Column.class, UiTesting.root(menu));
        UiElement.Column form = assertInstanceOf(UiElement.Column.class, root.children().getFirst());

        assertEquals("Nombre no válido: de 3 a 16 letras, números o _.",
                assertInstanceOf(UiElement.Label.class, form.children().get(2)).text().getString());
    }

    @Test
    void aChestKeepsTheHeadWithItsExplanationAndTheFieldBesideTheBackDoor() {
        CoordinateHeadMenu menu = headMenu();
        UiTesting.drawAsChest(menu);

        assertEquals(Items.PLAYER_HEAD, UiTesting.itemAt(menu, 4).stack().getItem());
        assertEquals(Items.OAK_DOOR, UiTesting.itemAt(menu, 9).stack().getItem());
    }

    @Test
    void theMapMenuOffersOneMapPerStructureAndTheNewestIsAmongThem() {
        CoordinateMapMenu menu = new CoordinateMapMenu(ui, service, "base", parent, parent);
        UiTesting.drawAsChest(menu);

        List<Item> maps = CoordinateIcons.structureMaps();

        assertEquals(16, maps.size());
        assertTrue(maps.contains(Items.ABANDONED_CAMP_MAP));
        assertEquals(maps.get(0), UiTesting.itemAt(menu, 10).stack().getItem());
        assertEquals(maps.get(15), UiTesting.itemAt(menu, 10 + 2 * 9 + 1).stack().getItem());
    }

    @Test
    void theCurrentMapShinesInTheMapMenu() {
        service.updateIcon("base", Items.DESERT_PYRAMID_MAP);
        CoordinateMapMenu menu = new CoordinateMapMenu(ui, service, "base", parent, parent);
        UiTesting.drawAsChest(menu);

        int index = CoordinateIcons.structureMaps().indexOf(Items.DESERT_PYRAMID_MAP);
        int slot = 10 + (index / 7) * 9 + index % 7;

        assertEquals(true, UiTesting.itemAt(menu, slot).stack().get(DataComponents.ENCHANTMENT_GLINT_OVERRIDE));
        assertEquals("Icono actual", lore(UiTesting.itemAt(menu, slot).stack()).getFirst());
    }
}
