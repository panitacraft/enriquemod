package com.panita.enriquecraft.chat.commands;

import com.panita.enriquecraft.chat.channel.ChannelDefinition;
import com.panita.enriquecraft.chat.channel.ChannelRegistry;
import com.panita.enriquecraft.chat.message.ChatMessages;
import com.panita.enriquecraft.chat.service.ChatService;
import com.panita.enriquecraft.core.framework.command.CommandSpec;
import com.panita.enriquecraft.core.message.PlayerOnly;

/**
 * {@code /ac <message>}: one message to the global chat, whatever channel the player writes in.
 */
@CommandSpec(name = "ac", description = ChatMessages.Channel.ALL_DESCRIPTION)
public final class AllChatCommand extends ChannelShortcutCommand {

    private final ChannelRegistry channels;

    public AllChatCommand(PlayerOnly playerOnly, ChatService chat, ChannelRegistry channels) {
        super(playerOnly, chat);
        this.channels = channels;
    }

    @Override
    ChannelDefinition channel() {
        return channels.global();
    }
}
