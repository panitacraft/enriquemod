package com.panita.enriquecraft.auth;

import com.panita.enriquecraft.auth.config.AuthConfig;
import com.panita.enriquecraft.auth.gui.PinMenus;
import com.panita.enriquecraft.auth.message.PinView;
import com.panita.enriquecraft.auth.message.RefusalView;
import com.panita.enriquecraft.auth.service.AccountService;
import com.panita.enriquecraft.auth.service.AttemptLimiter;
import com.panita.enriquecraft.auth.service.HeldPlayers;
import com.panita.enriquecraft.auth.service.LoginGate;
import com.panita.enriquecraft.auth.service.PinHasher;
import com.panita.enriquecraft.auth.service.PremiumLookup;
import com.panita.enriquecraft.core.config.CoreConfig;
import com.panita.enriquecraft.core.framework.data.WorldData;
import com.panita.enriquecraft.core.framework.inject.ServiceRegistry;
import com.panita.enriquecraft.core.framework.module.EnriquecraftModule;
import com.panita.enriquecraft.core.message.MessageFormatter;
import com.panita.enriquecraft.core.message.Messenger;
import com.panita.enriquecraft.core.network.ClientCapabilities;
import com.panita.enriquecraft.core.ui.UiService;
import net.fabricmc.loader.api.FabricLoader;

import java.time.Duration;
import java.time.Instant;
import java.util.function.Supplier;

/**
 * Who may play and how they prove it: players with a Microsoft account join without any step, the
 * others enter a PIN, and a correct PIN is remembered for a while.
 */
public final class AuthModule implements EnriquecraftModule {

    private static final String PEPPER_FILE = "enriquecraft-auth.key";

    @Override
    public void registerServices(ServiceRegistry services) {
        Supplier<Instant> clock = Instant::now;
        AuthConfig config = services.get(AuthConfig.class);
        UiService ui = services.get(UiService.class);
        PinHasher hasher = PinHasher.withPepperFile(FabricLoader.getInstance().getConfigDir().resolve(PEPPER_FILE));
        AccountService accounts = new AccountService(services.get(WorldData.class), hasher, clock);
        AttemptLimiter limiter = new AttemptLimiter(clock);
        PremiumLookup lookup = new PremiumLookup(PremiumLookup.mojang(), clock,
                () -> Duration.ofMinutes(config.lookupCacheMinutes.get()), config.lookupMaxRequestsPerTenMinutes::get);
        LoginGate gate = new LoginGate(limiter, lookup);
        RefusalView refusals = new RefusalView(new MessageFormatter(services.get(CoreConfig.class)));
        PinMenus screens = new PinMenus(ui, new PinView(ui.factory(), config), config);
        HeldPlayers held = new HeldPlayers(accounts, limiter, config, refusals, screens,
                services.get(ClientCapabilities.class), services.get(Messenger.class));

        services.register(PinHasher.class, hasher);
        services.register(AccountService.class, accounts);
        services.register(AttemptLimiter.class, limiter);
        services.register(LoginGate.class, gate);
        services.register(RefusalView.class, refusals);
        services.register(HeldPlayers.class, held);
        AuthHooks.install(gate, refusals, held);
    }
}
