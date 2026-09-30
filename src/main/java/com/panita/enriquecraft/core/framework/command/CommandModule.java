package com.panita.enriquecraft.core.framework.command;

import com.panita.enriquecraft.core.commands.BroadcastCommand;
import com.panita.enriquecraft.core.commands.enriquecraft.EnriquecraftCommand;
import com.panita.enriquecraft.core.commands.PingCommand;
import com.panita.enriquecraft.core.message.Messenger;
import com.panita.enriquecraft.core.service.HelpService;
import com.panita.enriquecraft.core.service.ServerInfoService;

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
