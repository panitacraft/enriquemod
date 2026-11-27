package com.panita.enriquecraft.moderation;

import com.panita.enriquecraft.auth.config.AuthConfig;
import com.panita.enriquecraft.core.framework.data.WorldData;
import com.panita.enriquecraft.core.framework.inject.ServiceRegistry;
import com.panita.enriquecraft.core.framework.module.EnriquecraftModule;
import com.panita.enriquecraft.core.message.Messenger;
import com.panita.enriquecraft.moderation.config.ModerationConfig;
import com.panita.enriquecraft.moderation.data.Mutes;
import com.panita.enriquecraft.moderation.message.BanView;
import com.panita.enriquecraft.moderation.message.ClientModView;
import com.panita.enriquecraft.moderation.message.KickView;
import com.panita.enriquecraft.moderation.message.ModerationMessages;
import com.panita.enriquecraft.moderation.message.MuteView;
import com.panita.enriquecraft.moderation.message.Reasons;
import com.panita.enriquecraft.moderation.message.VoiceChatView;
import com.panita.enriquecraft.moderation.service.BanService;
import com.panita.enriquecraft.moderation.service.ClientModPolicy;
import com.panita.enriquecraft.moderation.service.MuteBook;
import com.panita.enriquecraft.moderation.service.MuteService;
import com.panita.enriquecraft.moderation.service.VoiceChatService;

import java.time.Instant;
import java.util.Optional;
import java.util.function.Supplier;

/**
 * What staff do to players who break the rules: kicks and bans that show their screen under the mod prefix, bans that can end,
 * and mutes. Registered before the chat module, which asks {@link MuteService} before it lets a player speak.
 */
public final class ModerationModule implements EnriquecraftModule {

    private static final String MUTES_FILE = "mutes.snbt";

    @Override
    public Optional<Class<?>> messages() {
        return Optional.of(ModerationMessages.class);
    }

    @Override
    public void registerServices(ServiceRegistry services) {
        Supplier<Instant> clock = Instant::now;
        Messenger messenger = services.get(Messenger.class);
        Reasons reasons = new Reasons(messenger);
        var mutes = services.get(WorldData.class).register(MUTES_FILE, Mutes.CODEC, Mutes.EMPTY);

        BanService bans = new BanService(new BanView(messenger, reasons), clock);
        services.register(BanService.class, bans);
        services.register(MuteService.class, new MuteService(new MuteBook(mutes::get, mutes::set, clock), new MuteView(messenger, reasons), clock));
        services.register(VoiceChatService.class,
                new VoiceChatService(services.get(ModerationConfig.class), services.get(AuthConfig.class), new VoiceChatView(messenger)));
        services.register(ClientModPolicy.class, new ClientModPolicy(services.get(ModerationConfig.class)));
        services.register(ClientModView.class, new ClientModView(messenger));
        ModerationHooks.install(bans, new KickView(messenger, reasons));
    }
}
