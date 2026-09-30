package com.panita.enriquecraft.core.commands.enriquecraft;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.panita.enriquecraft.core.framework.command.CommandEntry;
import com.panita.enriquecraft.core.framework.command.CommandMetadata;
import com.panita.enriquecraft.core.framework.command.CommandSuggestions;
import com.panita.enriquecraft.core.framework.command.ModCommand;
import com.panita.enriquecraft.core.message.Message;
import com.panita.enriquecraft.core.message.Messages;
import com.panita.enriquecraft.core.message.Messenger;
import com.panita.enriquecraft.core.service.HelpService;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.server.permissions.PermissionLevel;

import java.util.Optional;

/**
 * {@code /enriquecraft help [command]}: lists the commands the sender may run, or shows the usage
 * of one of them.
 */
public final class HelpSubcommand implements ModCommand {

    private static final String COMMAND_ARGUMENT = "command";

    private final Messenger messenger;
    private final HelpService helpService;

    public HelpSubcommand(Messenger messenger, HelpService helpService) {
        this.messenger = messenger;
        this.helpService = helpService;
    }

    @Override
    public CommandMetadata metadata() {
        return CommandMetadata.of("help", Messages.Help.DESCRIPTION, PermissionLevel.ALL);
    }

    @Override
    public void configure(LiteralArgumentBuilder<CommandSourceStack> builder) {
        builder.executes(this::showList)
                .then(Commands.argument(COMMAND_ARGUMENT, StringArgumentType.word())
                        .suggests(CommandSuggestions.forSource(helpService::visibleNames))
                        .executes(this::showUsage));
    }

    /** Also used as the executor of the bare {@code /enriquecraft} command. */
    int showList(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        messenger.send(source, Message.plain(Messages.Help.HEADER).prefixed());
        for (CommandEntry entry : helpService.visibleEntries(source)) {
            messenger.send(source, Message.plain(Messages.Help.ENTRY)
                    .with("command", entry.displayPath())
                    .with("description", entry.command().metadata().description()));
        }
        return Command.SINGLE_SUCCESS;
    }

    private int showUsage(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        String literal = StringArgumentType.getString(context, COMMAND_ARGUMENT);
        Optional<CommandEntry> entry = helpService.findTopLevel(source, literal);
        if (entry.isEmpty()) {
            messenger.send(source, Message.error(Messages.Help.UNKNOWN_COMMAND).prefixed().with("command", literal));
            return 0;
        }
        messenger.send(source, Message.plain(Messages.Help.USAGE_HEADER)
                .with("command", entry.get().displayPath())
                .with("description", entry.get().command().metadata().description()));
        for (String usage : helpService.usages(source, entry.get())) {
            messenger.send(source, Message.plain(Messages.Help.USAGE_LINE).with("usage", usage));
        }
        return Command.SINGLE_SUCCESS;
    }
}
