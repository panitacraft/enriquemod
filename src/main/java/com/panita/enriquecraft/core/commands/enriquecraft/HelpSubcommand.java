package com.panita.enriquecraft.core.commands.enriquecraft;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.panita.enriquecraft.core.framework.command.CommandSpec;
import com.panita.enriquecraft.core.framework.command.CommandSuggestions;
import com.panita.enriquecraft.core.framework.command.ModCommand;
import com.panita.enriquecraft.core.message.HelpView;
import com.panita.enriquecraft.core.message.Messages;
import com.panita.enriquecraft.core.service.HelpService;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;

/**
 * {@code /enriquecraft help [command]}: lists the commands the sender may run, or shows the usage
 * of one of them.
 */
@CommandSpec(name = "help", parent = EnriquecraftCommand.class, description = Messages.Help.DESCRIPTION)
public final class HelpSubcommand implements ModCommand {

    private static final String COMMAND_ARGUMENT = "command";

    private final HelpView helpView;
    private final HelpService helpService;

    public HelpSubcommand(HelpView helpView, HelpService helpService) {
        this.helpView = helpView;
        this.helpService = helpService;
    }

    @Override
    public void configure(LiteralArgumentBuilder<CommandSourceStack> builder) {
        builder.executes(this::showOverview)
                .then(Commands.argument(COMMAND_ARGUMENT, StringArgumentType.word())
                        .suggests(CommandSuggestions.forSource(helpService::visibleNames))
                        .executes(this::showUsage));
    }

    private int showOverview(CommandContext<CommandSourceStack> context) {
        helpView.sendOverview(context.getSource());
        return Command.SINGLE_SUCCESS;
    }

    private int showUsage(CommandContext<CommandSourceStack> context) {
        String literal = StringArgumentType.getString(context, COMMAND_ARGUMENT);
        return helpView.sendUsage(context.getSource(), literal) ? Command.SINGLE_SUCCESS : 0;
    }
}
