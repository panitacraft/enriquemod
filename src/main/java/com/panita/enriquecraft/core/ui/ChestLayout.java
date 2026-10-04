package com.panita.enriquecraft.core.ui;

import com.panita.enriquecraft.core.gui.ItemBuilder;
import com.panita.enriquecraft.core.gui.MenuClick;
import com.panita.enriquecraft.core.gui.MenuFactory;
import com.panita.enriquecraft.core.gui.MenuFrame;
import com.panita.enriquecraft.core.gui.MenuItem;
import com.panita.enriquecraft.core.message.Message;
import com.panita.enriquecraft.core.message.Messages;
import com.panita.enriquecraft.core.network.UiElement;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Turns a screen description into chest slots, for players without the client companion.
 * <p>
 * The root's children are laid out as bands from the top. A row fills one chest row from the left,
 * a grid takes as many rows as it declares, and any other element sits centered in a row of its own.
 * A {@link ChestStyle#FRAMED} screen keeps its outer edge for decoration: every band but the last
 * goes inside the frame, and the last band takes the bottom edge, where controls belong.
 * <p>
 * A chest cannot hold a text field, so a field becomes an item: a left click asks for the value in
 * chat, and a right click clears it.
 */
final class ChestLayout {

    private static final int MAX_ROWS = 6;
    private static final int FRAME = 1;

    /** The chest a description turns into. */
    record Plan(int rows, Map<Integer, MenuItem> items) {
    }

    /** Asks a player for a value, in a way that works without a text field. */
    @FunctionalInterface
    interface InputPrompter {
        void ask(ServerPlayer player, Component field, Consumer<String> answer);
    }

    private final UiLayout layout;
    private final MenuFactory factory;
    private final InputPrompter prompter;
    private final Map<Integer, MenuItem> items = new HashMap<>();

    private ChestLayout(UiLayout layout, MenuFactory factory, InputPrompter prompter) {
        this.layout = layout;
        this.factory = factory;
        this.prompter = prompter;
    }

    static Plan plan(UiLayout layout, ChestStyle style, MenuFactory factory, InputPrompter prompter) {
        return new ChestLayout(layout, factory, prompter).arrange(style == ChestStyle.FRAMED);
    }

    private Plan arrange(boolean framed) {
        List<UiElement> bands = new ArrayList<>();
        collectBands(layout.root(), bands);
        if (bands.isEmpty()) {
            throw new IllegalStateException("A screen needs at least one element");
        }

        int offset = framed ? FRAME : 0;
        int width = MenuFrame.COLUMNS - 2 * offset;
        int interiorBands = framed ? bands.size() - 1 : bands.size();
        int row = framed ? FRAME : 0;
        for (int band = 0; band < interiorBands; band++) {
            row += place(bands.get(band), row, offset, width);
        }
        if (framed) {
            row += place(bands.getLast(), row, 0, MenuFrame.COLUMNS);
        }
        if (row > MAX_ROWS) {
            throw new IllegalStateException("A chest has at most " + MAX_ROWS + " rows, but the screen needs " + row);
        }
        return new Plan(row, items);
    }

    /** Nested columns only group; their children are bands of the screen. */
    private static void collectBands(UiElement element, List<UiElement> bands) {
        if (element instanceof UiElement.Column column) {
            column.children().forEach(child -> collectBands(child, bands));
        } else {
            bands.add(element);
        }
    }

    /** Places one band and returns how many rows it takes. */
    private int place(UiElement band, int row, int offset, int width) {
        switch (band) {
            case UiElement.Row line -> {
                requireFits(line.children().size(), width);
                for (int index = 0; index < line.children().size(); index++) {
                    put(slot(row, offset + index), line.children().get(index));
                }
                return 1;
            }
            case UiElement.Grid grid -> {
                requireFits(grid.columns(), width);
                for (int index = 0; index < grid.children().size(); index++) {
                    put(slot(row + index / grid.columns(), offset + index % grid.columns()), grid.children().get(index));
                }
                return grid.rows();
            }
            default -> {
                put(slot(row, offset + width / 2), band);
                return 1;
            }
        }
    }

    private static void requireFits(int columns, int width) {
        if (columns > width) {
            throw new IllegalStateException("A chest row holds " + width + " elements here, but the screen needs " + columns);
        }
    }

    private static int slot(int row, int column) {
        return row * MenuFrame.COLUMNS + column;
    }

    private void put(int slot, UiElement element) {
        MenuItem item = switch (element) {
            case UiElement.Button button -> MenuItem.button(stack(button), click -> handler(button.id()).accept(UiClick.from(click)));
            case UiElement.TextInput input -> MenuItem.button(stack(input), click -> edit(input, click));
            case UiElement.Label label -> MenuItem.display(factory.item(Items.PAPER).name(label.text()).build());
            case UiElement.Spacer ignored -> null;
            case UiElement.Page ignored -> null;
            case UiElement.Detail detail -> MenuItem.display(factory.item(detail.icon().isEmpty() ? new ItemStack(Items.PAPER) : detail.icon())
                    .name(detail.title()).loreLines(detail.lines()).build());
            case UiElement.Row ignored -> throw containerInsideAnotherContainer();
            case UiElement.Column ignored -> throw containerInsideAnotherContainer();
            case UiElement.Grid ignored -> throw containerInsideAnotherContainer();
        };
        if (item != null) {
            items.put(slot, item);
        }
    }

    private ItemStack stack(UiElement.Button button) {
        ItemStack icon = button.icon().isEmpty() ? new ItemStack(Items.PAPER) : button.icon();
        ItemBuilder builder = factory.item(icon);
        if (!button.label().getString().isEmpty()) {
            builder.name(button.label());
        }
        return builder.loreLines(button.tooltip()).build();
    }

    private ItemStack stack(UiElement.TextInput input) {
        List<Component> lore = new ArrayList<>();
        if (!input.value().isEmpty()) {
            lore.add(factory.text(Message.plain(Messages.Gui.INPUT_CURRENT).with("value", input.value())));
        }
        lore.add(factory.text(Messages.Gui.INPUT_EDIT));
        lore.add(factory.text(Messages.Gui.INPUT_CLEAR));
        return factory.item(Items.NAME_TAG).name(input.hint()).loreLines(lore).build();
    }

    private void edit(UiElement.TextInput input, MenuClick click) {
        UiInputHandler handler = layout.inputs().get(input.id());
        if (handler == null) {
            throw new IllegalStateException("The field " + input.id() + " has no action");
        }
        ServerPlayer player = click.player();
        if (click.isRight()) {
            handler.action().accept(new UiSubmit(player, ""));
        } else if (click.isLeft()) {
            prompter.ask(player, input.hint(), answer -> handler.action().accept(new UiSubmit(player, clamp(answer, handler.maxLength()))));
        }
    }

    /** Chat messages can be longer than a field allows; the field keeps what fits. */
    private static String clamp(String text, int maxLength) {
        return text.length() > maxLength ? text.substring(0, maxLength) : text;
    }

    private Consumer<UiClick> handler(int id) {
        Consumer<UiClick> handler = layout.handlers().get(id);
        if (handler == null) {
            throw new IllegalStateException("The button " + id + " has no action");
        }
        return handler;
    }

    private static IllegalStateException containerInsideAnotherContainer() {
        return new IllegalStateException("A chest cannot show a row, column or grid inside a row or grid");
    }
}
