package com.panita.enriquecraft.auth;

import com.panita.enriquecraft.auth.config.AuthConfig;
import com.panita.enriquecraft.auth.message.RefusalView;
import com.panita.enriquecraft.auth.service.AccountService;
import com.panita.enriquecraft.auth.service.AttemptLimiter;
import com.panita.enriquecraft.auth.service.LoginGate;
import com.panita.enriquecraft.auth.service.PinHasher;
import com.panita.enriquecraft.auth.service.PremiumLookup;
import com.panita.enriquecraft.core.config.CoreConfig;
import com.panita.enriquecraft.core.framework.data.WorldData;
import com.panita.enriquecraft.core.framework.inject.ServiceRegistry;
import com.panita.enriquecraft.core.framework.module.EnriquecraftModule;
import com.panita.enriquecraft.core.message.MessageFormatter;
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
        PinHasher hasher = PinHasher.withPepperFile(FabricLoader.getInstance().getConfigDir().resolve(PEPPER_FILE));
        AttemptLimiter limiter = new AttemptLimiter(clock);
        PremiumLookup lookup = new PremiumLookup(PremiumLookup.mojang(), clock,
                () -> Duration.ofMinutes(config.lookupCacheMinutes.get()), config.lookupMaxRequestsPerTenMinutes::get);
        LoginGate gate = new LoginGate(limiter, lookup);
        RefusalView refusals = new RefusalView(new MessageFormatter(services.get(CoreConfig.class)));

        services.register(PinHasher.class, hasher);
        services.register(AccountService.class, new AccountService(services.get(WorldData.class), hasher, clock));
        services.register(AttemptLimiter.class, limiter);
        services.register(LoginGate.class, gate);
        services.register(RefusalView.class, refusals);
        AuthHooks.install(gate, refusals);
    }
}
