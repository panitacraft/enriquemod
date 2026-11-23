package com.panita.enriquecraft;

import com.panita.enriquecraft.auth.AuthModule;
import com.panita.enriquecraft.chat.ChatModule;
import com.panita.enriquecraft.core.CoreModule;
import com.panita.enriquecraft.core.framework.module.ModuleManager;
import com.panita.enriquecraft.core.network.NetworkProtocol;
import com.panita.enriquecraft.discord.DiscordModule;
import com.panita.enriquecraft.link.LinkModule;
import com.panita.enriquecraft.moderation.ModerationModule;
import com.panita.enriquecraft.profile.ProfileModule;
import com.panita.enriquecraft.staff.StaffModule;
import net.fabricmc.api.ModInitializer;

public class Enriquecraft implements ModInitializer {

    public static final String MOD_ID = NetworkProtocol.NAMESPACE;

    @Override
    public void onInitialize() {
        NetworkProtocol.registerPayloads();
        ModuleManager modules = new ModuleManager(MOD_ID);
        modules.register(new CoreModule());
        modules.register(new StaffModule());
        modules.register(new AuthModule());
        modules.register(new ModerationModule());
        modules.register(new ChatModule());
        modules.register(new LinkModule());
        modules.register(new ProfileModule());
        modules.register(new DiscordModule());
    }
}
