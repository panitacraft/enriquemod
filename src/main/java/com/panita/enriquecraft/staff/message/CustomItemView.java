package com.panita.enriquecraft.staff.message;

import com.panita.enriquecraft.core.item.ItemGiving;
import com.panita.enriquecraft.core.message.Message;
import com.panita.enriquecraft.core.message.Messenger;
import com.panita.enriquecraft.core.message.Timestamps;
import com.panita.enriquecraft.staff.data.SavedItem;
import com.panita.enriquecraft.staff.service.CustomItemService;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.level.ServerPlayer;

/**
 * Tells staff what happened when they use saved custom items, whether from a command or a menu.
 */
public final class CustomItemView {

    private final Messenger messenger;

    public CustomItemView(Messenger messenger) {
        this.messenger = messenger;
    }

    public void emptyHand(CommandSourceStack source) {
        messenger.send(source, Message.error(StaffMessages.Items.EMPTY_HAND).prefixed());
    }

    public void saved(CommandSourceStack source, SavedItem item) {
        messenger.send(source, Message.success(StaffMessages.Items.SAVED).prefixed()
                .with("name", item.name())
                .with("item", item.stack().getHoverName())
                .with("count", item.stack().getCount()));
    }

    public void rejected(CommandSourceStack source, String name, CustomItemService.AddResult result) {
        String template = result == CustomItemService.AddResult.DUPLICATE
                ? StaffMessages.Items.DUPLICATE
                : StaffMessages.Items.INVALID_NAME;
        messenger.send(source, Message.error(template).prefixed().with("name", name));
    }

    public void removed(CommandSourceStack source, String name) {
        messenger.send(source, Message.success(StaffMessages.Items.REMOVED).prefixed().with("name", name));
    }

    public void notFound(CommandSourceStack source, String name) {
        messenger.send(source, Message.error(StaffMessages.Items.NOT_FOUND).prefixed().with("name", name));
    }

    public void info(CommandSourceStack source, SavedItem item) {
        messenger.send(source, Message.plain(StaffMessages.Items.INFO_HEADER).prefixed().with("name", item.name()));
        messenger.send(source, Message.plain(StaffMessages.Items.INFO_ID).with("id", "enriquecraft:" + item.name()));
        messenger.send(source, Message.plain(StaffMessages.Items.INFO_ITEM)
                .with("item", item.stack().getHoverName())
                .with("count", item.stack().getCount()));
        messenger.send(source, Message.plain(StaffMessages.Items.INFO_SAVED_BY).with("player", item.savedByName()));
        messenger.send(source, Message.plain(StaffMessages.Items.INFO_DATE).with("date", Timestamps.dateTime(item.savedAt())));
    }

    /**
     * Gives the staff member an exact copy, or a full stack of it; whatever does not fit in the inventory
     * drops at their feet.
     */
    public void give(ServerPlayer player, SavedItem item, boolean fullStack) {
        ItemGiving.give(player, fullStack ? item.stack().copyWithCount(item.stack().getMaxStackSize()) : item.stack());
        messenger.send(player, Message.success(StaffMessages.Items.GIVEN).prefixed().with("name", item.name()));
    }
}
