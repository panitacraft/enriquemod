package com.panita.enriquecraft.staff.gui;

import com.panita.enriquecraft.core.gui.MenuFactory;
import com.panita.enriquecraft.core.gui.MenuItem;
import com.panita.enriquecraft.core.gui.PaginatedMenu;
import com.panita.enriquecraft.core.message.Message;
import com.panita.enriquecraft.core.message.Timestamps;
import com.panita.enriquecraft.staff.data.SavedItem;
import com.panita.enriquecraft.staff.message.CustomItemView;
import com.panita.enriquecraft.staff.message.StaffMessages;
import com.panita.enriquecraft.staff.service.CustomItemService;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

import java.util.List;

/**
 * Every saved custom item, shown as the real item with a few extra lines of lore. Left click gives
 * the staff member an exact copy, without those extra lines.
 */
public final class CustomItemsMenu extends PaginatedMenu<SavedItem> {

    private final CustomItemService service;
    private final CustomItemView view;

    public CustomItemsMenu(MenuFactory factory, CustomItemService service, CustomItemView view) {
        super(factory, null);
        this.service = service;
        this.view = view;
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
    protected MenuItem render(SavedItem item) {
        ItemStack shown = factory().item(item.stack())
                .lore(Message.plain(""),
                        Message.plain(StaffMessages.Items.ENTRY_ID).with("id", "enriquecraft:" + item.name()),
                        Message.plain(StaffMessages.Items.ENTRY_SAVED_BY).with("player", item.savedByName()),
                        Message.plain(StaffMessages.Items.ENTRY_DATE).with("date", Timestamps.format(item.savedAt())),
                        Message.plain(StaffMessages.Items.ENTRY_CLICK_HINT))
                .build();
        return MenuItem.button(shown, click -> {
            if (click.isLeft()) {
                view.give(click.player(), item);
            }
        });
    }
}
