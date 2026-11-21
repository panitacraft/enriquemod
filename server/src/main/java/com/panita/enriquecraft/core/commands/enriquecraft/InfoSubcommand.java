package com.panita.enriquecraft.core.commands.enriquecraft;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.panita.enriquecraft.core.framework.command.CommandSpec;
import com.panita.enriquecraft.core.framework.command.ModCommand;
import com.panita.enriquecraft.core.message.Message;
import com.panita.enriquecraft.core.message.Messages;
import com.panita.enriquecraft.core.message.Messenger;
import com.panita.enriquecraft.core.service.ServerInfoService;
import com.panita.enriquecraft.core.service.ServerInfoService.ServerInfo;
import net.minecraft.commands.CommandSourceStack;

/**
 * {@code /enriquecraft info}: shows mod and server basics.
 */
@CommandSpec(name = "info", parent = EnriquecraftCommand.class, description = Messages.Info.DESCRIPTION)
public final class InfoSubcommand implements ModCommand {

    private final Messenger messenger;
    private final ServerInfoService infoService;

    public InfoSubcommand(Messenger messenger, ServerInfoService infoService) {
        this.messenger = messenger;
        this.infoService = infoService;
    }

    @Override
    public void configure(LiteralArgumentBuilder<CommandSourceStack> builder) {
        builder.executes(this::execute);
    }

    private int execute(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        ServerInfo info = infoService.collect(source.getServer());
        messenger.send(source, Message.plain(Messages.Info.BODY)
                .with("name", info.modName())
                .with("version", info.modVersion())
                .with("minecraft", info.minecraftVersion())
                .with("loader", info.loaderVersion())
                .with("players", info.playerCount())
                .with("maxPlayers", info.maxPlayers()));
        return Command.SINGLE_SUCCESS;
    }
}
