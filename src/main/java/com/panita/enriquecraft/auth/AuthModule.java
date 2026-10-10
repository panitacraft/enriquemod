package com.panita.enriquecraft.auth;

import com.panita.enriquecraft.auth.service.AccountService;
import com.panita.enriquecraft.auth.service.AttemptLimiter;
import com.panita.enriquecraft.auth.service.PinHasher;
import com.panita.enriquecraft.core.framework.data.WorldData;
import com.panita.enriquecraft.core.framework.inject.ServiceRegistry;
import com.panita.enriquecraft.core.framework.module.EnriquecraftModule;
import net.fabricmc.loader.api.FabricLoader;

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
        PinHasher hasher = PinHasher.withPepperFile(FabricLoader.getInstance().getConfigDir().resolve(PEPPER_FILE));
        services.register(PinHasher.class, hasher);
        services.register(AccountService.class, new AccountService(services.get(WorldData.class), hasher, clock));
        services.register(AttemptLimiter.class, new AttemptLimiter(clock));
    }
}
