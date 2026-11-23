package com.panita.enriquecraft.qol.message;

import com.panita.enriquecraft.core.message.Message;
import com.panita.enriquecraft.core.message.Messenger;
import com.panita.enriquecraft.staff.data.Dimensions;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;

import java.util.Collection;
import java.util.List;

/**
 * What sharing coordinates shows. The position is read here, at the moment the line is built, so it is the one the player has
 * when they share. Presentation only.
 */
public final class CoordinateShareView {

    private final Messenger messenger;

    public CoordinateShareView(Messenger messenger) {
        this.messenger = messenger;
    }

    /** The line that tells every reader, and the console, where the sender is. */
    public void everyone(ServerPlayer sender, Collection<ServerPlayer> readers) {
        messenger.deliver(sender.level().getServer(), readers,
                messenger.format(located(QolMessages.ShareCoordinates.PUBLIC, sender), sender));
    }

    /** The line that tells the receiver where the sender is. */
    public void toPlayer(ServerPlayer sender, ServerPlayer receiver) {
        tell(receiver, sender, located(QolMessages.ShareCoordinates.PRIVATE, sender));
    }

    /** The line for a player who picked themselves. */
    public void self(ServerPlayer player) {
        tell(player, player, located(QolMessages.ShareCoordinates.SELF, player));
    }

    /** Tells the sender that the receiver now knows where they are. */
    public void sent(ServerPlayer sender, ServerPlayer receiver) {
        tell(sender, receiver, Message.plain(QolMessages.ShareCoordinates.SENT_CONFIRMATION).prefixed()
                .with("player", receiver.getDisplayName()));
    }

    public void notOnline(ServerPlayer sender, ServerPlayer receiver) {
        tell(sender, receiver, Message.plain(QolMessages.ShareCoordinates.NOT_ONLINE).prefixed()
                .with("player", receiver.getGameProfile().name()));
    }

    public void disabled(ServerPlayer player) {
        messenger.send(player, Message.error(QolMessages.ShareCoordinates.DISABLED).prefixed());
    }

    /** Sends the line to one player, with the placeholders of the template resolved for the player it is about. */
    private void tell(ServerPlayer receiver, ServerPlayer context, Message message) {
        messenger.deliver(List.of(receiver), messenger.format(message, context));
    }

    private static Message located(String template, ServerPlayer player) {
        BlockPos position = player.blockPosition();
        return Message.plain(template).prefixed()
                .with("player", player.getDisplayName())
                .with("x", position.getX())
                .with("y", position.getY())
                .with("z", position.getZ())
                .with("dimension", Dimensions.displayName(player.level().dimension()));
    }
}
