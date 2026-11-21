package com.panita.enriquecraft.core.ui;

import com.panita.enriquecraft.MinecraftTestSupport;
import com.panita.enriquecraft.core.gui.MenuClick;
import com.panita.enriquecraft.core.gui.MenuItem;
import com.panita.enriquecraft.core.network.UiElement;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.ContainerInput;
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

/** A text field shown as a chest: an item that asks for the value in chat. */
class ChestLayoutInputTest {

    private static final int MAX_LENGTH = 5;

    /** A screen with one text field holding {@code value}; what it receives is recorded. */
    private static final class FieldMenu extends UiMenu {
        private final String value;
        private final List<String> received = new ArrayList<>();

        FieldMenu(UiService ui, String value) {
            super(ui, null);
            this.value = value;
        }

        @Override
        protected Component title() {
            return Component.literal("Field");
        }

        @Override
        protected UiElement describe(UiBuilder builder) {
            return builder.input(Component.literal("Buscar"), value, MAX_LENGTH, submit -> received.add(submit.text()));
        }
    }

    /** Remembers who was asked for what, and answers when told to. */
    private static final class RecordingPrompter implements ChestLayout.InputPrompter {
        private final List<String> asked = new ArrayList<>();
        private Consumer<String> answer;

        @Override
        public void ask(ServerPlayer player, Component field,
                        Consumer<String> answer) {
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

    private MenuItem fieldItem(FieldMenu menu, RecordingPrompter prompter) {
        ChestLayout.Plan plan = ChestLayout.plan(menu.layout(0), ChestStyle.PLAIN, ui.factory(), prompter);
        return plan.items().get(4);
    }

    private static void click(MenuItem item, int button) {
        item.action().accept(new MenuClick(null, 4, button, ContainerInput.PICKUP));
    }

    @Test
    void aFieldIsANameTagNamedByItsHint() {
        MenuItem item = fieldItem(new FieldMenu(ui, ""), new RecordingPrompter());

        assertEquals(Items.NAME_TAG, item.stack().getItem());
        assertEquals("Buscar", item.stack().get(DataComponents.CUSTOM_NAME).getString());
    }

    @Test
    void theLoreShowsTheCurrentValueAndHowToChangeIt() {
        MenuItem item = fieldItem(new FieldMenu(ui, "base"), new RecordingPrompter());

        List<String> lore = item.stack().get(DataComponents.LORE).lines().stream().map(Component::getString).toList();
        assertEquals(List.of("Actual: base", "Clic izquierdo para escribir en el chat", "Clic derecho para borrar"), lore);
    }

    @Test
    void anEmptyFieldShowsNoCurrentValue() {
        MenuItem item = fieldItem(new FieldMenu(ui, ""), new RecordingPrompter());

        List<String> lore = item.stack().get(DataComponents.LORE).lines().stream().map(Component::getString).toList();
        assertEquals(2, lore.size());
    }

    @Test
    void aLeftClickAsksInChatAndTheAnswerReachesTheField() {
        FieldMenu menu = new FieldMenu(ui, "");
        RecordingPrompter prompter = new RecordingPrompter();
        MenuItem item = fieldItem(menu, prompter);

        click(item, 0);
        prompter.answer.accept("base");

        assertEquals(List.of("Buscar"), prompter.asked);
        assertEquals(List.of("base"), menu.received);
    }

    @Test
    void anAnswerLongerThanTheFieldKeepsWhatFits() {
        FieldMenu menu = new FieldMenu(ui, "");
        RecordingPrompter prompter = new RecordingPrompter();
        MenuItem item = fieldItem(menu, prompter);

        click(item, 0);
        prompter.answer.accept("abcdefgh");

        assertEquals(List.of("abcde"), menu.received);
    }

    @Test
    void aRightClickClearsTheFieldWithoutAsking() {
        FieldMenu menu = new FieldMenu(ui, "base");
        RecordingPrompter prompter = new RecordingPrompter();
        MenuItem item = fieldItem(menu, prompter);

        click(item, 1);

        assertEquals(List.of(""), menu.received);
        assertTrue(prompter.asked.isEmpty());
    }

    @Test
    void otherClickTypesDoNothing() {
        FieldMenu menu = new FieldMenu(ui, "base");
        RecordingPrompter prompter = new RecordingPrompter();
        MenuItem item = fieldItem(menu, prompter);

        click(item, 2);

        assertTrue(menu.received.isEmpty());
        assertTrue(prompter.asked.isEmpty());
    }
}
