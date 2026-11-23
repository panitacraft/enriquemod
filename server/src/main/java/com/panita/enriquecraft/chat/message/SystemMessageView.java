package com.panita.enriquecraft.chat.message;

import com.panita.enriquecraft.chat.channel.ChannelRegistry;
import com.panita.enriquecraft.chat.event.SystemEvent;
import com.panita.enriquecraft.chat.service.ChatHeads;
import com.panita.enriquecraft.core.message.Message;
import com.panita.enriquecraft.core.message.Messenger;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import java.util.Collection;

/**
 * Builds and sends the lines that replace the game's messages about players. Presentation only: what is shown is decided by
 * {@code SystemMessages}.
 */
public final class SystemMessageView {

    private final Messenger messenger;
    private final ChatHeads heads;
    private final SenderNameView names;
    private final ChannelRegistry channels;

    public SystemMessageView(Messenger messenger, ChatHeads heads, SenderNameView names, ChannelRegistry channels) {
        this.messenger = messenger;
        this.heads = heads;
        this.names = names;
        this.channels = channels;
    }

    /**
     * The line starts with the head of the player, as a line they write does; the head is left out when {@code chat.heads} is off. The
     * player is named as the global channel names its sender (rank, emblems, name, suffix), wherever the line says who they are.
     *
     * @param announceFirst whether a join is the first one of the player and has the message for that
     */
    public Component line(SystemEvent event, boolean announceFirst) {
        Component player = sender(event.player());
        Message message = switch (event) {
            case SystemEvent.Joined joined when announceFirst -> Message.plain(ChatMessages.Events.JOIN_FIRST).with("player", player);
            case SystemEvent.Joined joined when joined.oldName().isPresent() -> Message.plain(ChatMessages.Events.JOIN_RENAMED)
                    .with("player", player).with("oldName", joined.oldName().get());
            case SystemEvent.Joined joined -> Message.plain(ChatMessages.Events.JOIN).with("player", player);
            case SystemEvent.Left left -> Message.plain(ChatMessages.Events.LEAVE).with("player", player);
            case SystemEvent.Died died -> Message.plain(ChatMessages.Events.DEATH)
                    .with("player", player).with("message", named(died.message(), event.player(), player));
            case SystemEvent.Advanced advanced -> Message.plain(ChatMessages.Events.ADVANCEMENT)
                    .with("player", player).with("message", named(advanced.message(), event.player(), player));
        };
        Component line = messenger.format(message, event.player());
        return heads.of(event.player()).<Component>map(head -> Component.empty().append(head).append(line)).orElse(line);
    }

    /** The player as the global channel shows its sender, read from the channel at each use so a change of its format is followed. */
    private Component sender(ServerPlayer player) {
        return messenger.format(Message.plain(SenderTag.of(channels.global().format())).with("sender", names.of(player)), player);
    }

    /**
     * The game's sentence with the player in it replaced by the dressed name. The game puts the player first among the arguments of its
     * messages about them; a message of another shape is left as it is.
     */
    private static Component named(Component message, ServerPlayer player, Component dressed) {
        if (message.getContents() instanceof TranslatableContents translatable && translatable.getArgs().length > 0
                && translatable.getArgs()[0] instanceof Component first && first.getString().equals(player.getDisplayName().getString())) {
            Object[] arguments = translatable.getArgs().clone();
            arguments[0] = dressed;
            MutableComponent replaced = MutableComponent.create(new TranslatableContents(translatable.getKey(), translatable.getFallback(), arguments))
                    .setStyle(message.getStyle());
            message.getSiblings().forEach(replaced::append);
            return replaced;
        }
        return message;
    }

    public void deliver(MinecraftServer server, Collection<ServerPlayer> readers, Component line) {
        messenger.deliver(server, readers, line);
    }
}
