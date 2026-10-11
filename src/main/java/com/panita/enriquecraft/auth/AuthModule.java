package com.panita.enriquecraft.auth;

import com.panita.enriquecraft.auth.commands.WhitelistHandler;
import com.panita.enriquecraft.auth.config.AuthConfig;
import com.panita.enriquecraft.auth.gui.PinMenus;
import com.panita.enriquecraft.auth.message.AdminView;
import com.panita.enriquecraft.auth.message.AuthMessages;
import com.panita.enriquecraft.auth.message.PinView;
import com.panita.enriquecraft.auth.message.LoginView;
import com.panita.enriquecraft.auth.message.RefusalView;
import com.panita.enriquecraft.auth.message.WhitelistView;
import com.panita.enriquecraft.auth.service.AccountService;
import com.panita.enriquecraft.auth.service.AttemptLimiter;
import com.panita.enriquecraft.auth.service.HeldPlayers;
import com.panita.enriquecraft.auth.service.LoginGate;
import com.panita.enriquecraft.auth.service.PinChanges;
import com.panita.enriquecraft.auth.service.PinHasher;
import com.panita.enriquecraft.auth.service.PlayerDataMigration;
import com.panita.enriquecraft.auth.service.PremiumLookup;
import com.panita.enriquecraft.auth.service.WhitelistAdmin;
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
import java.util.Optional;
import java.util.function.Supplier;

/**
 * Who may play and how they prove it: players with a Microsoft account join without any step, the
 * others enter a PIN, and a correct PIN is remembered for a while.
 */
public final class AuthModule implements EnriquecraftModule {

    @Override
    public Optional<Class<?>> messages() {
        return Optional.of(AuthMessages.class);
    }

    private static final String PEPPER_FILE = "enriquecraft-auth.key";

    @Override
    public void registerServices(ServiceRegistry services) {
        Supplier<Instant> clock = Instant::now;
        AuthConfig config = services.get(AuthConfig.class);
        UiService ui = services.get(UiService.class);
        PinHasher hasher = PinHasher.withPepperFile(FabricLoader.getInstance().getConfigDir().resolve(PEPPER_FILE));
        AccountService accounts = new AccountService(services.get(WorldData.class), hasher, clock);
        // A lockout is `maxAttempts` wrong PINs, so that many times `kicksBeforeBlock` of them block an address.
        AttemptLimiter limiter = new AttemptLimiter(clock, () -> config.kicksBeforeBlock.get() * config.maxAttempts.get(),
                () -> Duration.ofSeconds(config.windowSeconds.get()), () -> Duration.ofSeconds(config.blockSeconds.get()));
        PremiumLookup lookup = new PremiumLookup(PremiumLookup.mojang(), clock,
                () -> Duration.ofMinutes(config.lookupCacheMinutes.get()), config.lookupMaxRequestsPerTenMinutes::get);
        RefusalView refusals = new RefusalView(services.get(MessageFormatter.class));
        PinMenus screens = new PinMenus(ui, new PinView(ui.factory(), config));
        HeldPlayers held = new HeldPlayers(accounts, limiter, config, refusals, screens,
                services.get(ClientCapabilities.class), new LoginView(services.get(Messenger.class)));

        LoginGate gate = new LoginGate(limiter, lookup, held::isHeld);
        WhitelistHandler whitelist = new WhitelistHandler(new WhitelistAdmin(lookup), new PlayerDataMigration(),
                new WhitelistView(services.get(Messenger.class)));

        services.register(PinHasher.class, hasher);
        services.register(AccountService.class, accounts);
        services.register(AttemptLimiter.class, limiter);
        services.register(LoginGate.class, gate);
        services.register(RefusalView.class, refusals);
        services.register(HeldPlayers.class, held);
        services.register(PinChanges.class, new PinChanges(accounts, limiter, config, screens, services.get(Messenger.class)));
        services.register(WhitelistHandler.class, whitelist);
        services.register(AdminView.class, new AdminView(services.get(Messenger.class)));
        AuthHooks.install(gate, refusals, held, whitelist);
    }
}
