package com.panita.enriquecraft;

import com.panita.enriquecraft.command.CommandModule;
import com.panita.enriquecraft.command.CommandRegistrar;
import com.panita.enriquecraft.command.CommandTreeBuilder;
import com.panita.enriquecraft.event.BossBarCleanupListener;
import com.panita.enriquecraft.message.MessageFormatter;
import com.panita.enriquecraft.message.Messenger;
import com.panita.enriquecraft.message.channel.BossBarChannel;
import com.panita.enriquecraft.message.channel.TitleChannel;
import net.fabricmc.api.ModInitializer;

public class Enriquecraft implements ModInitializer {

    public static final String MOD_ID = "enriquecraft";

    @Override
    public void onInitialize() {
        BossBarChannel bossBars = new BossBarChannel();
        Messenger messenger = new Messenger(new MessageFormatter(), new TitleChannel(), bossBars);

        new BossBarCleanupListener(bossBars).register();
        new CommandRegistrar(CommandModule.create(messenger), new CommandTreeBuilder()).register();
    }
}
