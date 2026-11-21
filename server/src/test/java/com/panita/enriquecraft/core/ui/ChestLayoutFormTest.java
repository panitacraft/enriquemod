package com.panita.enriquecraft.core.ui;

import com.panita.enriquecraft.MinecraftTestSupport;
import com.panita.enriquecraft.core.gui.MenuClick;
import com.panita.enriquecraft.core.gui.MenuItem;
import com.panita.enriquecraft.core.network.ButtonRole;
import com.panita.enriquecraft.core.network.UiElement;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * A text area, a toggle, the header roles and the large grid shown as a chest: the roles and the tile size are
 * ignored, the area is a name tag that asks in chat, the toggle is an item that flips.
 */
class ChestLayoutFormTest {

    /** A row of a text area, a toggle and an edit button, then a large grid of one button. */
    private static final class FormMenu extends UiMenu {
        private final List<String> areaValues = new ArrayList<>();
        private final List<Boolean> toggled = new ArrayList<>();
        private final List<String> pressed = new ArrayList<>();
        private final boolean checked;

        FormMenu(UiService ui, boolean checked) {
            super(ui, null);
            this.checked = checked;
        }

        @Override
        protected Component title() {
            return Component.literal("Form");
        }

        @Override
        protected UiElement describe(UiBuilder builder) {
            UiElement.Button edit = builder.button(ButtonRole.EDIT, new ItemStack(Items.WRITABLE_BOOK), Component.literal("Editar"),
                    List.of(), click -> pressed.add("edit"));
            UiElement.Button tile = builder.button(new ItemStack(Items.DIAMOND), Component.literal("Emblema"), List.of(),
                    click -> pressed.add("tile"));
            return new UiElement.Column(List.of(
                    new UiElement.Row(List.of(
                            builder.textArea(Component.literal("Biografía"), "uno\ndos", 100, 4, 200, submit -> areaValues.add(submit.text())),
                            builder.toggle(Component.literal("Mostrar"), checked, toggle -> toggled.add(toggle.checked())),
                            edit)),
                    new UiElement.Grid(1, 1, List.of(tile)).withLargeTiles()));
        }
    }

    private static final class RecordingPrompter implements ChestLayout.InputPrompter {
        private final List<String> asked = new ArrayList<>();
        private Consumer<String> answer;

        @Override
        public void ask(ServerPlayer player, Component field, Consumer<String> answer) {
            asked.add(field.getString());
            this.answer = answer;
        }
    }

    @TempDir
    Path directory;

    private UiService ui;

    @BeforeEach
    void createService() {
        ui = MinecraftTestSupport.uiService(directory);
    }

    private ChestLayout.Plan plan(FormMenu menu, RecordingPrompter prompter) {
        return ChestLayout.plan(menu.layout(0), ChestStyle.PLAIN, ui.factory(), prompter);
    }

    private static void click(MenuItem item, int slot, int button) {
        item.action().accept(new MenuClick(null, slot, button, ContainerInput.PICKUP));
    }

    private static List<String> lore(MenuItem item) {
        return item.stack().get(DataComponents.LORE).lines().stream().map(Component::getString).toList();
    }

    @Test
    void aTextAreaIsANameTagThatShowsItsLinesJoined() {
        MenuItem item = plan(new FormMenu(ui, false), new RecordingPrompter()).items().get(0);

        assertEquals(Items.NAME_TAG, item.stack().getItem());
        assertEquals("Biografía", item.stack().get(DataComponents.CUSTOM_NAME).getString());
        assertEquals("Actual: uno dos", lore(item).getFirst());
    }

    @Test
    void aLeftClickOnATextAreaAsksInChatAndARightClickClearsIt() {
        FormMenu menu = new FormMenu(ui, false);
        RecordingPrompter prompter = new RecordingPrompter();
        MenuItem item = plan(menu, prompter).items().get(0);

        click(item, 0, 0);
        prompter.answer.accept("hola");
        click(item, 0, 1);

        assertEquals(List.of("Biografía"), prompter.asked);
        assertEquals(List.of("hola", ""), menu.areaValues);
    }

    @Test
    void aTextAreaKeepsWhatFitsOfALongChatAnswer() {
        FormMenu menu = new FormMenu(ui, false);
        RecordingPrompter prompter = new RecordingPrompter();
        MenuItem item = plan(menu, prompter).items().get(0);

        click(item, 0, 0);
        prompter.answer.accept("x".repeat(150));

        assertEquals("x".repeat(100), menu.areaValues.getFirst());
    }

    @Test
    void anUncheckedToggleIsAnItemThatAsksToCheckItOnALeftClick() {
        FormMenu menu = new FormMenu(ui, false);
        MenuItem item = plan(menu, new RecordingPrompter()).items().get(1);

        assertEquals(Items.GUNPOWDER, item.stack().getItem());
        assertEquals("Mostrar", item.stack().get(DataComponents.CUSTOM_NAME).getString());
        assertEquals("Desactivado", lore(item).getFirst());
        click(item, 1, 0);

        assertEquals(List.of(true), menu.toggled);
    }

    @Test
    void aCheckedToggleAsksToUncheckItAndIgnoresARightClick() {
        FormMenu menu = new FormMenu(ui, true);
        MenuItem item = plan(menu, new RecordingPrompter()).items().get(1);

        assertEquals(Items.EMERALD, item.stack().getItem());
        assertEquals("Activado", lore(item).getFirst());
        click(item, 1, 1);
        assertTrue(menu.toggled.isEmpty());
        click(item, 1, 0);

        assertEquals(List.of(false), menu.toggled);
    }

    @Test
    void theHeaderRoleIsAnOrdinaryButtonInAChest() {
        FormMenu menu = new FormMenu(ui, false);
        ChestLayout.Plan plan = plan(menu, new RecordingPrompter());

        assertEquals(Items.WRITABLE_BOOK, plan.items().get(2).stack().getItem());
        click(plan.items().get(2), 2, 0);

        assertEquals(List.of("edit"), menu.pressed);
    }

    @Test
    void aLargeGridTakesTheSameChestSlotsAsAnOrdinaryOne() {
        FormMenu menu = new FormMenu(ui, false);
        ChestLayout.Plan plan = plan(menu, new RecordingPrompter());

        assertEquals(2, plan.rows());
        assertEquals(Items.DIAMOND, plan.items().get(9).stack().getItem());
        click(plan.items().get(9), 9, 0);
        assertEquals(List.of("tile"), menu.pressed);
    }
}
