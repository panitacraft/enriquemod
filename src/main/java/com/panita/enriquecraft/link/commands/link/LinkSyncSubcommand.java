package com.panita.enriquecraft.link.commands.link;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.panita.enriquecraft.core.framework.command.CommandSpec;
import com.panita.enriquecraft.core.framework.command.ModCommand;
import com.panita.enriquecraft.link.message.LinkMessages;
import com.panita.enriquecraft.link.message.LinkView;
import com.panita.enriquecraft.link.service.LinkRunner;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.permissions.PermissionLevel;

import java.util.Optional;

/**
 * {@code /link sync}: asks the Panita API for the list of players and updates the whitelist now. The answer comes
 * when the API has replied, so the command only says that it started.
 */
@CommandSpec(name = "sync", parent = LinkCommand.class, description = LinkMessages.Commands.SYNC_DESCRIPTION,
        access = PermissionLevel.ADMINS)
public final class LinkSyncSubcommand implements ModCommand {

    private final LinkRunner runner;
    private final LinkView view;

    public LinkSyncSubcommand(LinkRunner runner, LinkView view) {
        this.runner = runner;
        this.view = view;
    }

    @Override
    public void configure(LiteralArgumentBuilder<CommandSourceStack> builder) {
        builder.executes(this::sync);
    }

    private int sync(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        view.started(source);
        runner.run(source.getServer(), Optional.of(source));
        return Command.SINGLE_SUCCESS;
    }
}
