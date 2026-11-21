package com.panita.enriquecraft.auth.commands.pin;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.panita.enriquecraft.auth.message.AuthMessages;
import com.panita.enriquecraft.core.framework.command.CommandSpec;
import com.panita.enriquecraft.core.framework.command.ModCommand;
import com.panita.enriquecraft.core.message.HelpView;
import net.minecraft.commands.CommandSourceStack;

/**
 * {@code /pin}: root of what players can do with their own PIN. Running it alone lists it.
 */
@CommandSpec(name = "pin", description = AuthMessages.Commands.PIN_DESCRIPTION)
public final class PinCommand implements ModCommand {

    private static final String NAME = "pin";

    private final HelpView helpView;

    public PinCommand(HelpView helpView) {
        this.helpView = helpView;
    }

    @Override
    public void configure(LiteralArgumentBuilder<CommandSourceStack> builder) {
        builder.executes(context -> {
            helpView.sendUsage(context.getSource(), NAME);
            return Command.SINGLE_SUCCESS;
        });
    }
}
