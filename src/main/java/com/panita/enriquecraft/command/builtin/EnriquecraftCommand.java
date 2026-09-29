package com.panita.enriquecraft.command.builtin;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.panita.enriquecraft.command.CommandMetadata;
import com.panita.enriquecraft.command.ModCommand;
import com.panita.enriquecraft.message.Messages;
import com.panita.enriquecraft.message.Messenger;
import com.panita.enriquecraft.service.HelpService;
import com.panita.enriquecraft.service.ServerInfoService;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.permissions.PermissionLevel;

import java.util.List;

/**
 * {@code /enriquecraft} (alias {@code /enrique}): root of the general commands. Running it without
 * a subcommand shows the help list.
 */
public final class EnriquecraftCommand implements ModCommand {

    private final HelpSubcommand help;
    private final InfoSubcommand info;

    public EnriquecraftCommand(Messenger messenger, HelpService helpService, ServerInfoService infoService) {
        this.help = new HelpSubcommand(messenger, helpService);
        this.info = new InfoSubcommand(messenger, infoService);
    }

    @Override
    public CommandMetadata metadata() {
        return CommandMetadata.of("enriquecraft", Messages.Enriquecraft.DESCRIPTION, PermissionLevel.ALL)
                .withAliases("enrique");
    }

    @Override
    public void configure(LiteralArgumentBuilder<CommandSourceStack> builder) {
        builder.executes(help::showList);
    }

    @Override
    public List<ModCommand> subcommands() {
        return List.of(help, info);
    }
}
