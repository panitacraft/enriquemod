package com.panita.enriquecraft.core.commands;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.panita.enriquecraft.core.framework.command.CommandSpec;
import com.panita.enriquecraft.core.framework.command.ModCommand;
import com.panita.enriquecraft.core.message.Messages;
import com.panita.enriquecraft.core.message.TpsView;
import com.panita.enriquecraft.core.service.TpsMonitor;
import net.minecraft.commands.CommandSourceStack;

/**
 * {@code /tps}: the ticks per second of the server over the last 1, 5 and 15 minutes, for players and the console, like the
 * command of Spigot.
 */
@CommandSpec(name = "tps", description = Messages.Tps.DESCRIPTION)
public final class TpsCommand implements ModCommand {

    private final TpsMonitor monitor;
    private final TpsView view;

    public TpsCommand(TpsMonitor monitor, TpsView view) {
        this.monitor = monitor;
        this.view = view;
    }

    @Override
    public void configure(LiteralArgumentBuilder<CommandSourceStack> builder) {
        builder.executes(context -> {
            view.show(context.getSource(), monitor.averages());
            return Command.SINGLE_SUCCESS;
        });
    }
}
