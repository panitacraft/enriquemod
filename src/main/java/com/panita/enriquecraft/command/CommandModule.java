package com.panita.enriquecraft.command;

import com.panita.enriquecraft.command.builtin.BroadcastCommand;
import com.panita.enriquecraft.command.builtin.EnriquecraftCommand;
import com.panita.enriquecraft.command.builtin.PingCommand;
import com.panita.enriquecraft.message.Messenger;
import com.panita.enriquecraft.service.HelpService;
import com.panita.enriquecraft.service.ServerInfoService;

/**
 * The single place where the mod's commands are listed. To add a command, create its class and
 * register one line here.
 */
public final class CommandModule {

    private CommandModule() {
    }

    public static CommandCatalog create(Messenger messenger) {
        CommandCatalog catalog = new CommandCatalog();
        HelpService helpService = new HelpService(catalog);
        ServerInfoService infoService = new ServerInfoService();

        catalog.register(new PingCommand(messenger));
        catalog.register(new EnriquecraftCommand(messenger, helpService, infoService));
        catalog.register(new BroadcastCommand(messenger));
        return catalog;
    }
}
