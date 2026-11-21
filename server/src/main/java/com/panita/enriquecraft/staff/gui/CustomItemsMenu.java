package com.panita.enriquecraft.staff.gui;

import com.panita.enriquecraft.core.message.Message;
import com.panita.enriquecraft.core.network.UiElement;
import com.panita.enriquecraft.core.ui.ClickHints;
import com.panita.enriquecraft.core.ui.UiBuilder;
import com.panita.enriquecraft.core.ui.UiMenu;
import com.panita.enriquecraft.core.ui.UiPagedMenu;
import com.panita.enriquecraft.core.ui.UiService;
import com.panita.enriquecraft.staff.data.SavedItem;
import com.panita.enriquecraft.staff.message.CustomItemView;
import com.panita.enriquecraft.staff.message.StaffMessages;
import com.panita.enriquecraft.staff.service.CustomItemService;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.List;
import java.util.Optional;
import java.util.function.Function;

/**
 * Every saved custom item, shown as the real item, searchable by name and id. Hovering shows the
 * item as it is, then its id. Left click gives a copy (with shift, a full stack) and right click opens
 * everything about it.
 */
public final class CustomItemsMenu extends UiPagedMenu<SavedItem> {

    private final CustomItemService service;
    private final CustomItemView view;

    public CustomItemsMenu(UiService ui, CustomItemService service, CustomItemView view) {
        this(ui, service, view, null);
    }

    /** A list that goes back to the menu it was opened from. */
    public CustomItemsMenu(UiService ui, CustomItemService service, CustomItemView view, UiMenu previous) {
        super(ui, previous);
        this.service = service;
        this.view = view;
    }

    @Override
    protected ItemStack icon() {
        return new ItemStack(Items.ENCHANTED_BOOK);
    }

    @Override
    protected Component title() {
        return factory().text(StaffMessages.Items.MENU_TITLE);
    }

    @Override
    protected List<SavedItem> entries() {
        return service.all();
    }

    @Override
    protected Optional<Function<SavedItem, String>> searchText() {
        return Optional.of(item -> item.name() + " " + item.stack().getHoverName().getString());
    }

    @Override
    protected UiElement render(UiBuilder builder, SavedItem item) {
        // After the item's own tooltip: a gap, the id the way vanilla shows it, a gap, and the clicks.
        List<Component> details = List.of(
                factory().text(Message.plain("")),
                factory().text(Message.plain(StaffMessages.Items.ENTRY_ID_LINE).with("id", "enriquecraft:" + item.name())),
                factory().text(Message.plain("")),
                ClickHints.left(factory(), "obtener copia"),
                ClickHints.right(factory(), "info"));
        // No label: the button is the item itself, so it keeps its own name and lore.
        return builder.button(item.stack().copy(), Component.empty(), details, click -> {
            if (click.isLeft()) {
                view.give(click.player(), item, click.isShift());
            } else if (click.isRight()) {
                new CustomItemDetailMenu(ui(), service, view, item.name(), this).open(click.player());
            }
        });
    }
}
