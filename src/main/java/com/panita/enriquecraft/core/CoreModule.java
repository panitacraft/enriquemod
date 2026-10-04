package com.panita.enriquecraft.core;

import com.panita.enriquecraft.core.config.CoreConfig;
import com.panita.enriquecraft.core.framework.command.CommandCatalog;
import com.panita.enriquecraft.core.framework.inject.ServiceRegistry;
import com.panita.enriquecraft.core.framework.module.EnriquecraftModule;
import com.panita.enriquecraft.core.gui.MenuFactory;
import com.panita.enriquecraft.core.message.ConfigReportView;
import com.panita.enriquecraft.core.message.HelpView;
import com.panita.enriquecraft.core.message.MessageFormatter;
import com.panita.enriquecraft.core.message.Messenger;
import com.panita.enriquecraft.core.message.PlayerOnly;
import com.panita.enriquecraft.core.message.channel.BossBarChannel;
import com.panita.enriquecraft.core.message.channel.TitleChannel;
import com.panita.enriquecraft.core.network.ClientCapabilities;
import com.panita.enriquecraft.core.service.HelpService;
import com.panita.enriquecraft.core.service.ServerInfoService;
import com.panita.enriquecraft.core.ui.ChatPrompts;
import com.panita.enriquecraft.core.ui.UiService;
import com.panita.enriquecraft.core.ui.UiSessions;

/**
 * The core module: messaging, help, and the general commands every other module relies on.
 */
public final class CoreModule implements EnriquecraftModule {

    @Override
    public void registerServices(ServiceRegistry services) {
        ClientCapabilities capabilities = new ClientCapabilities();
        BossBarChannel bossBars = new BossBarChannel();
        MessageFormatter formatter = new MessageFormatter(services.get(CoreConfig.class));
        Messenger messenger = new Messenger(formatter, new TitleChannel(), bossBars);
        HelpService helpService = new HelpService(services.get(CommandCatalog.class));

        services.register(BossBarChannel.class, bossBars);
        services.register(Messenger.class, messenger);
        services.register(HelpService.class, helpService);
        services.register(HelpView.class, new HelpView(messenger, helpService));
        services.register(ConfigReportView.class, new ConfigReportView(messenger));
        MenuFactory menuFactory = new MenuFactory(formatter);
        services.register(MenuFactory.class, menuFactory);
        services.register(ClientCapabilities.class, capabilities);
        ChatPrompts chatPrompts = new ChatPrompts(messenger);
        services.register(ChatPrompts.class, chatPrompts);
        UiSessions uiSessions = new UiSessions();
        services.register(UiSessions.class, uiSessions);
        services.register(UiService.class, new UiService(menuFactory, capabilities, uiSessions, chatPrompts));
        services.register(PlayerOnly.class, new PlayerOnly(messenger));
        services.register(ServerInfoService.class, new ServerInfoService());
    }
}
