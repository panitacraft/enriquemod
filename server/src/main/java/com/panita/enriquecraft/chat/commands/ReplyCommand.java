package com.panita.enriquecraft.chat.commands;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.panita.enriquecraft.chat.message.ChatMessages;
import com.panita.enriquecraft.chat.service.PrivateMessages;
import com.panita.enriquecraft.core.framework.command.CommandSpec;
import com.panita.enriquecraft.core.framework.command.ModCommand;
import com.panita.enriquecraft.core.message.PlayerOnly;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;

/**
 * {@code /reply <message>} and {@code /r <message>}: a private message to the last player the sender exchanged one with.
 */
@CommandSpec(name = "reply", aliases = "r", description = ChatMessages.Private.REPLY_DESCRIPTION)
public final class ReplyCommand implements ModCommand {

    private static final String MESSAGE_ARGUMENT = "message";

    private final PlayerOnly playerOnly;
    private final PrivateMessages privateMessages;

    public ReplyCommand(PlayerOnly playerOnly, PrivateMessages privateMessages) {
        this.playerOnly = playerOnly;
        this.privateMessages = privateMessages;
    }

    @Override
    public void configure(LiteralArgumentBuilder<CommandSourceStack> builder) {
        builder.then(Commands.argument(MESSAGE_ARGUMENT, StringArgumentType.greedyString())
                .executes(playerOnly.executes((context, player) ->
                        privateMessages.reply(context.getSource(), player, StringArgumentType.getString(context, MESSAGE_ARGUMENT)))));
    }
}
