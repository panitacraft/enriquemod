package com.panita.enriquecraft.core.commands.enriquecraft;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.panita.enriquecraft.core.framework.command.CommandSpec;
import com.panita.enriquecraft.core.framework.command.ModCommand;
import com.panita.enriquecraft.core.message.HelpView;
import com.panita.enriquecraft.core.message.Messages;
import net.minecraft.commands.CommandSourceStack;

/**
 * {@code /enriquecraft} (alias {@code /enrique}): root of the general commands. Running it without
 * a subcommand shows the help list.
 */
@CommandSpec(name = "enriquecraft", aliases = "enrique", description = Messages.Enriquecraft.DESCRIPTION)
public final class EnriquecraftCommand implements ModCommand {

    private final HelpView helpView;

    public EnriquecraftCommand(HelpView helpView) {
        this.helpView = helpView;
    }

    @Override
    public void configure(LiteralArgumentBuilder<CommandSourceStack> builder) {
        builder.executes(context -> {
            helpView.sendOverview(context.getSource());
            return Command.SINGLE_SUCCESS;
        });
    }
}
