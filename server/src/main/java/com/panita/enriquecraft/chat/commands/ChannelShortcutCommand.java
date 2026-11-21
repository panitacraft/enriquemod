package com.panita.enriquecraft.chat.commands;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.panita.enriquecraft.chat.channel.ChannelDefinition;
import com.panita.enriquecraft.chat.service.ChatService;
import com.panita.enriquecraft.core.framework.command.ModCommand;
import com.panita.enriquecraft.core.message.PlayerOnly;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;

/**
 * A command that sends one message to a channel without changing the one the player writes in, so a message for the
 * staff does not need two switches.
 */
abstract class ChannelShortcutCommand implements ModCommand {

    private static final String MESSAGE_ARGUMENT = "message";

    private final PlayerOnly playerOnly;
    private final ChatService chat;

    ChannelShortcutCommand(PlayerOnly playerOnly, ChatService chat) {
        this.playerOnly = playerOnly;
        this.chat = chat;
    }

    /** The channel the message goes to; asked each time, since the config can change. */
    abstract ChannelDefinition channel();

    @Override
    public void configure(LiteralArgumentBuilder<CommandSourceStack> builder) {
        builder.then(Commands.argument(MESSAGE_ARGUMENT, StringArgumentType.greedyString())
                .executes(playerOnly.executes((context, player) -> {
                    chat.speak(player, channel(), StringArgumentType.getString(context, MESSAGE_ARGUMENT));
                    return Command.SINGLE_SUCCESS;
                })));
    }
}
