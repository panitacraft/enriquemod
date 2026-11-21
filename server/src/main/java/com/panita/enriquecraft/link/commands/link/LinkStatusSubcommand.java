package com.panita.enriquecraft.link.commands.link;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.panita.enriquecraft.core.framework.command.CommandSpec;
import com.panita.enriquecraft.core.framework.command.ModCommand;
import com.panita.enriquecraft.link.config.LinkConfig;
import com.panita.enriquecraft.link.message.LinkMessages;
import com.panita.enriquecraft.link.message.LinkView;
import com.panita.enriquecraft.link.service.LinkSync;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.permissions.PermissionLevel;

import java.time.Instant;

/**
 * {@code /link status}: whether the key was found, which API the server asks, how many players are linked and how the
 * last sync ended.
 */
@CommandSpec(name = "status", parent = LinkCommand.class, description = LinkMessages.Commands.STATUS_DESCRIPTION,
        access = PermissionLevel.GAMEMASTERS)
public final class LinkStatusSubcommand implements ModCommand {

    private final LinkSync sync;
    private final LinkConfig config;
    private final LinkView view;

    public LinkStatusSubcommand(LinkSync sync, LinkConfig config, LinkView view) {
        this.sync = sync;
        this.config = config;
        this.view = view;
    }

    @Override
    public void configure(LiteralArgumentBuilder<CommandSourceStack> builder) {
        builder.executes(this::status);
    }

    private int status(CommandContext<CommandSourceStack> context) {
        view.status(context.getSource(), sync.hasKey(), config.apiUrl.get(), sync.linkedCount(), sync.last(), sync.waiting(),
                Instant.now());
        return Command.SINGLE_SUCCESS;
    }
}
