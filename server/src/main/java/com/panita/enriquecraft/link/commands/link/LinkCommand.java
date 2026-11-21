package com.panita.enriquecraft.link.commands.link;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.panita.enriquecraft.core.framework.command.CommandSpec;
import com.panita.enriquecraft.core.framework.command.ModCommand;
import com.panita.enriquecraft.core.message.HelpView;
import com.panita.enriquecraft.link.message.LinkMessages;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.permissions.PermissionLevel;

/**
 * {@code /link}: root of the tools that keep the whitelist in step with the Panita accounts. Running it alone lists them.
 */
@CommandSpec(name = "link", description = LinkMessages.Commands.LINK_DESCRIPTION, access = PermissionLevel.GAMEMASTERS)
public final class LinkCommand implements ModCommand {

    private static final String NAME = "link";

    private final HelpView helpView;

    public LinkCommand(HelpView helpView) {
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
