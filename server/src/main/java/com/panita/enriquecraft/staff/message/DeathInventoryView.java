package com.panita.enriquecraft.staff.message;

import com.panita.enriquecraft.core.item.ItemGiving;
import com.panita.enriquecraft.core.message.Message;
import com.panita.enriquecraft.core.message.Messenger;
import com.panita.enriquecraft.staff.data.DeathRecord;
import com.panita.enriquecraft.staff.data.Dimensions;
import com.panita.enriquecraft.staff.service.Teleporter;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

/**
 * Does what staff ask of a death inventory and tells them the outcome.
 */
public final class DeathInventoryView {

    private final Messenger messenger;

    public DeathInventoryView(Messenger messenger) {
        this.messenger = messenger;
    }

    public void singlePlayerRequired(CommandSourceStack source) {
        messenger.send(source, Message.error(StaffMessages.Deaths.SINGLE_PLAYER).prefixed());
    }

    /** Moves the staff member to the exact spot where the player died. */
    public void teleport(ServerPlayer staff, DeathRecord record) {
        Teleporter.Result result = Teleporter.teleport(staff, record.dimension(), record.x(), record.y(), record.z(),
                staff.getYRot(), staff.getXRot());
        if (result == Teleporter.Result.TELEPORTED) {
            messenger.send(staff, Message.success(StaffMessages.Deaths.TELEPORTED).prefixed()
                    .with("player", record.playerName()));
        } else {
            messenger.send(staff, Message.error(StaffMessages.Deaths.DIMENSION_UNAVAILABLE).prefixed()
                    .with("dimension", Dimensions.displayName(record.dimension())));
        }
    }

    /** Gives the staff member an exact copy of one stack of the inventory. */
    public void giveItem(ServerPlayer staff, ItemStack stack) {
        ItemGiving.give(staff, stack);
        messenger.send(staff, Message.success(StaffMessages.Deaths.ITEM_GIVEN).prefixed().with("item", stack.getHoverName()));
    }

    public void chestsGiven(ServerPlayer staff, DeathRecord record, int chests) {
        messenger.send(staff, Message.success(StaffMessages.Deaths.CHESTS_GIVEN).prefixed()
                .with("count", chests)
                .with("player", record.playerName()));
    }

    public void restored(ServerPlayer staff, ServerPlayer target, int stacks) {
        messenger.send(staff, Message.success(StaffMessages.Deaths.RESTORED).prefixed()
                .with("count", stacks)
                .with("player", target.getName().getString()));
        messenger.send(target, Message.info(StaffMessages.Deaths.RESTORED_TO_PLAYER).prefixed());
    }

    public void targetOffline(ServerPlayer staff, String playerName) {
        messenger.send(staff, Message.error(StaffMessages.Deaths.TARGET_OFFLINE).prefixed().with("player", playerName));
    }

    public void deleted(ServerPlayer staff) {
        messenger.send(staff, Message.success(StaffMessages.Deaths.DELETED).prefixed());
    }
}
