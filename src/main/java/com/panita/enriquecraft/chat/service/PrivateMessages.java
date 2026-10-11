package com.panita.enriquecraft.chat.service;

import com.panita.enriquecraft.auth.service.HeldPlayers;
import com.panita.enriquecraft.chat.ChatPermissions;
import com.panita.enriquecraft.chat.message.PrivateMessageView;
import com.panita.enriquecraft.chat.text.ChatCapability;
import com.panita.enriquecraft.chat.text.ChatTextParser;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import org.jspecify.annotations.Nullable;

import java.util.Collection;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

/**
 * Private messages: {@code /msg}, {@code /tell} and {@code /w} go through here, and so does {@code /r}. The text is read as
 * far as the sender's permissions allow, like chat, and a player who has not entered their PIN yet receives nothing.
 */
public final class PrivateMessages {

    private final ChatTextParser parser;
    private final PrivateMessageView view;
    private final ReplyTracker replies;
    private final HeldPlayers held;

    public PrivateMessages(ChatTextParser parser, PrivateMessageView view, ReplyTracker replies, HeldPlayers held) {
        this.parser = parser;
        this.view = view;
        this.replies = replies;
        this.held = held;
    }

    /**
     * @param source the sender: a player, or the console
     * @param raw    what they typed
     * @return how many players the message reached, as a command reports
     */
    public int send(CommandSourceStack source, Collection<ServerPlayer> targets, String raw) {
        List<ServerPlayer> reachable = targets.stream().filter(target -> !held.isHeld(target)).toList();
        if (reachable.isEmpty()) {
            view.notAvailable(source);
            return 0;
        }
        ServerPlayer sender = source.getPlayer();
        Set<ChatCapability> capabilities = sender == null ? EnumSet.allOf(ChatCapability.class) : ChatPermissions.capabilitiesOf(sender);
        Component text = parser.parse(raw, capabilities);
        if (text.getString().isBlank()) {
            return 0;
        }
        for (ServerPlayer target : reachable) {
            view.incoming(source, target, text);
            view.outgoing(source, target, text);
            if (sender != null) {
                replies.exchanged(sender.getUUID(), target.getUUID());
            }
        }
        return reachable.size();
    }

    /** {@code /r}: answers the last player the sender exchanged a message with, if they are still here. */
    public int reply(CommandSourceStack source, ServerPlayer sender, String raw) {
        @Nullable ServerPlayer target = replies.lastContactOf(sender.getUUID())
                .map(id -> sender.level().getServer().getPlayerList().getPlayer(id))
                .orElse(null);
        if (target == null) {
            view.nobodyToReplyTo(source);
            return 0;
        }
        return send(source, List.of(target), raw);
    }
}
