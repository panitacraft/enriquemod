package com.panita.enriquecraft.core.ui;

import com.panita.enriquecraft.core.message.Messages;
import com.panita.enriquecraft.core.network.ButtonRole;
import com.panita.enriquecraft.core.network.UiElement;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.List;
import java.util.function.Consumer;

/**
 * Asks the player to confirm something that cannot be undone, such as deleting a record. Every such
 * question looks the same: what is about to happen, then confirm or cancel (red and green in the client
 * companion, which draws them as labels without an item; a chest shows the items). Cancelling, and the back
 * navigation of the client companion, return to the screen it was opened from. What follows a
 * confirmation, such as where to go next, is up to the action.
 */
public final class ConfirmMenu extends UiMenu {

    private final Component title;
    private final ItemStack icon;
    private final Component headline;
    private final List<Component> lines;
    private final Consumer<ServerPlayer> onConfirm;

    /**
     * @param previous  where cancelling goes back to
     * @param icon      the item shown large and beside the title
     * @param headline  what is about to happen, in a few words
     * @param lines     details of what will be lost
     * @param onConfirm runs when the player confirms
     */
    public ConfirmMenu(UiService ui, UiMenu previous, Component title, ItemStack icon, Component headline,
                       List<Component> lines, Consumer<ServerPlayer> onConfirm) {
        super(ui, previous);
        this.title = title;
        this.icon = icon;
        this.headline = headline;
        this.lines = List.copyOf(lines);
        this.onConfirm = onConfirm;
    }

    @Override
    protected Component title() {
        return title;
    }

    @Override
    protected ItemStack icon() {
        return icon;
    }

    @Override
    protected ChestStyle chestStyle() {
        return ChestStyle.FILLED;
    }

    @Override
    protected UiElement describe(UiBuilder builder) {
        UiElement cancel = builder.button(ButtonRole.CANCEL, new ItemStack(Items.OAK_DOOR), factory().text(Messages.Gui.CANCEL),
                List.of(), click -> previous().open(click.player()));
        UiElement confirm = builder.button(ButtonRole.CONFIRM, new ItemStack(Items.EMERALD_BLOCK),
                factory().text(Messages.Gui.CONFIRM), List.of(), click -> {
                    if (click.isLeft()) {
                        onConfirm.accept(click.player());
                    }
                });
        UiElement none = new UiElement.Spacer();
        return new UiElement.Column(List.of(
                new UiElement.Detail(icon, headline, lines),
                new UiElement.Row(List.of(cancel, none, none, none, none, confirm, none, none, none))));
    }
}
