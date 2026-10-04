package com.panita.enriquecraft.staff.gui;

import com.panita.enriquecraft.core.message.Message;
import com.panita.enriquecraft.core.message.Timestamps;
import com.panita.enriquecraft.core.network.UiElement;
import com.panita.enriquecraft.core.ui.UiBuilder;
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

/**
 * Every saved custom item, shown as the real item with a few extra lines of lore. Left click gives
 * the staff member an exact copy, without those extra lines.
 */
public final class CustomItemsMenu extends UiPagedMenu<SavedItem> {

    private final CustomItemService service;
    private final CustomItemView view;

    public CustomItemsMenu(UiService ui, CustomItemService service, CustomItemView view) {
        super(ui, null);
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
    protected UiElement render(UiBuilder builder, SavedItem item) {
        List<Component> details = List.of(
                factory().text(Message.plain("")),
                factory().text(Message.plain(StaffMessages.Items.ENTRY_ID).with("id", "enriquecraft:" + item.name())),
                factory().text(Message.plain(StaffMessages.Items.ENTRY_SAVED_BY).with("player", item.savedByName())),
                factory().text(Message.plain(StaffMessages.Items.ENTRY_DATE).with("date", Timestamps.dateTime(item.savedAt()))),
                factory().text(StaffMessages.Items.ENTRY_CLICK_HINT));
        // No label: the button is the item itself, so it keeps its own name and lore.
        return builder.button(item.stack().copy(), Component.empty(), details, click -> {
            if (click.isLeft()) {
                view.give(click.player(), item);
            }
        });
    }
}
