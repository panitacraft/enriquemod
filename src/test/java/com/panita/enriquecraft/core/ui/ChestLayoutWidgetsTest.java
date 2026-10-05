package com.panita.enriquecraft.core.ui;

import com.panita.enriquecraft.MinecraftTestSupport;
import com.panita.enriquecraft.core.gui.MenuClick;
import com.panita.enriquecraft.core.gui.MenuItem;
import com.panita.enriquecraft.core.network.ButtonRole;
import com.panita.enriquecraft.core.network.UiElement;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** How the scroll area, the dropdown, the badge and the pressable detail look and act as a chest. */
class ChestLayoutWidgetsTest {

    private static final class ScreenMenu extends UiMenu {
        private final Function<UiBuilder, UiElement> screen;

        ScreenMenu(UiService ui, Function<UiBuilder, UiElement> screen) {
            super(ui, null);
            this.screen = screen;
        }

        @Override
        protected Component title() {
            return Component.literal("Screen");
        }

        @Override
        protected UiElement describe(UiBuilder builder) {
            return screen.apply(builder);
        }
    }

    @TempDir
    Path directory;

    private UiService ui;

    @BeforeEach
    void createService() {
        ui = MinecraftTestSupport.uiService(directory);
    }

    private ChestLayout.Plan plan(Function<UiBuilder, UiElement> screen) {
        ScreenMenu menu = new ScreenMenu(ui, screen);
        return ChestLayout.plan(menu.layout(0), ChestStyle.PLAIN, ui.factory(), (player, field, answer) -> { });
    }

    private static void click(MenuItem item, int button) {
        item.action().accept(new MenuClick(null, 0, button, ContainerInput.PICKUP));
    }

    private static List<String> lore(ItemStack stack) {
        return stack.get(DataComponents.LORE).lines().stream().map(Component::getString).toList();
    }

    @Test
    void aScrollAreaIsShownAsItsContent() {
        ChestLayout.Plan plan = plan(b -> new UiElement.Column(List.of(
                new UiElement.Scroll(new UiElement.Row(List.of(new UiElement.Label(Component.literal("uno")))), 40),
                new UiElement.Row(List.of(new UiElement.Label(Component.literal("dos")))))));

        assertEquals(2, plan.rows());
        assertEquals("uno", plan.items().get(0).stack().get(DataComponents.CUSTOM_NAME).getString());
        assertEquals("dos", plan.items().get(9).stack().get(DataComponents.CUSTOM_NAME).getString());
    }

    private UiElement.Dropdown dropdown(UiBuilder builder, int selected, List<Integer> chosen) {
        return builder.dropdown(Component.literal("Filtrar"),
                List.of(Component.literal("a"), Component.literal("b"), Component.literal("c")), selected,
                select -> chosen.add(select.option()));
    }

    @Test
    void aDropdownIsAHopperThatListsItsOptionsAndMarksTheChosenOne() {
        ChestLayout.Plan plan = plan(b -> dropdown(b, 1, new ArrayList<>()));

        ItemStack shown = plan.items().get(4).stack();

        assertEquals(Items.HOPPER, shown.getItem());
        assertEquals("Filtrar", shown.get(DataComponents.CUSTOM_NAME).getString());
        assertEquals(List.of("  a", "▸ b", "  c", "", "◀ Clic Izq. para siguiente", "▶ Clic Der. para anterior"), lore(shown));
    }

    @Test
    void aLeftClickMovesToTheNextOptionAndWrapsAround() {
        List<Integer> chosen = new ArrayList<>();
        ChestLayout.Plan plan = plan(b -> dropdown(b, 2, chosen));

        click(plan.items().get(4), 0);

        assertEquals(List.of(0), chosen);
    }

    @Test
    void aRightClickMovesToThePreviousOptionAndWrapsAround() {
        List<Integer> chosen = new ArrayList<>();
        ChestLayout.Plan plan = plan(b -> dropdown(b, 0, chosen));

        click(plan.items().get(4), 1);

        assertEquals(List.of(2), chosen);
    }

    @Test
    void otherClicksOnADropdownDoNothing() {
        List<Integer> chosen = new ArrayList<>();
        ChestLayout.Plan plan = plan(b -> dropdown(b, 0, chosen));

        click(plan.items().get(4), 2);

        assertTrue(chosen.isEmpty());
    }

    @Test
    void aBadgeLeadsTheNameOfALabeledButton() {
        ChestLayout.Plan plan = plan(b -> b.button(ButtonRole.NONE, new ItemStack(Items.SKELETON_SKULL),
                Component.literal("Muerte"), List.of(), "✔", click -> { }));

        assertEquals("✔ Muerte", plan.items().get(4).stack().get(DataComponents.CUSTOM_NAME).getString());
    }

    @Test
    void aBadgeLeadsTheLoreOfAnItemThatKeepsItsOwnName() {
        ChestLayout.Plan plan = plan(b -> b.button(ButtonRole.NONE, new ItemStack(Items.SKELETON_SKULL),
                Component.empty(), List.of(Component.literal("detalle")), "✔", click -> { }));

        ItemStack shown = plan.items().get(4).stack();

        assertEquals(null, shown.get(DataComponents.CUSTOM_NAME));
        assertEquals(List.of("✔", "detalle"), lore(shown));
    }

    @Test
    void aPressableDetailRunsItsActionAndSaysWhatPressingDoes() {
        List<Integer> pressed = new ArrayList<>();
        ChestLayout.Plan plan = plan(b -> b.detail(new ItemStack(Items.DIAMOND), Component.literal("Base"),
                List.of(Component.literal("uno")), List.of(Component.literal("cambiar")), click -> pressed.add(1)));

        MenuItem item = plan.items().get(4);
        click(item, 0);

        assertEquals(List.of("uno", "", "cambiar"), lore(item.stack()));
        assertEquals(List.of(1), pressed);
    }

    @Test
    void aDetailThatCannotBePressedDoesNothingWhenClicked() {
        ChestLayout.Plan plan = plan(b -> new UiElement.Detail(new ItemStack(Items.DIAMOND), Component.literal("Base"),
                List.of(Component.literal("uno"))));

        click(plan.items().get(4), 0);

        assertEquals(List.of("uno"), lore(plan.items().get(4).stack()));
    }

    @Test
    void aDividerTakesNoSlotInAChest() {
        ChestLayout.Plan plan = plan(b -> new UiElement.Row(List.of(
                new UiElement.Label(Component.literal("uno")), new UiElement.Divider(), new UiElement.Label(Component.literal("dos")))));

        assertEquals(2, plan.items().size());
        assertTrue(plan.items().containsKey(0));
        assertTrue(plan.items().containsKey(2));
    }
}
