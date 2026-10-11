package com.panita.enriquecraft.chat.commands;

import com.panita.enriquecraft.chat.channel.ChannelDefinition;
import com.panita.enriquecraft.chat.channel.ChannelRegistry;
import com.panita.enriquecraft.chat.message.ChatMessages;
import com.panita.enriquecraft.chat.service.ChatService;
import com.panita.enriquecraft.core.framework.command.CommandSpec;
import com.panita.enriquecraft.core.message.PlayerOnly;

/**
 * {@code /sc <message>}: one message to the staff chat, whatever channel the player writes in. Who may use the staff
 * channel is the channel's own permission, so the command itself is open and answers that a player may not.
 */
@CommandSpec(name = "sc", description = ChatMessages.Channel.STAFF_DESCRIPTION)
public final class StaffChatCommand extends ChannelShortcutCommand {

    private final ChannelRegistry channels;

    public StaffChatCommand(PlayerOnly playerOnly, ChatService chat, ChannelRegistry channels) {
        super(playerOnly, chat);
        this.channels = channels;
    }

    @Override
    ChannelDefinition channel() {
        return channels.staff();
    }
}
