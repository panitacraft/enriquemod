package com.panita.enriquecraft.link;

import com.panita.enriquecraft.auth.service.PlayerDataMigration;
import com.panita.enriquecraft.auth.service.WhitelistAdmin;
import com.panita.enriquecraft.core.framework.data.WorldData;
import com.panita.enriquecraft.core.framework.inject.ServiceRegistry;
import com.panita.enriquecraft.core.framework.module.EnriquecraftModule;
import com.panita.enriquecraft.core.message.MessageFormatter;
import com.panita.enriquecraft.core.message.Messenger;
import com.panita.enriquecraft.link.client.ApiKey;
import com.panita.enriquecraft.link.client.HttpPanitaApi;
import com.panita.enriquecraft.link.config.LinkConfig;
import com.panita.enriquecraft.link.data.LinkedSnapshot;
import com.panita.enriquecraft.link.message.LinkMessages;
import com.panita.enriquecraft.link.message.LinkView;
import com.panita.enriquecraft.link.service.LinkApplier;
import com.panita.enriquecraft.link.service.LinkRunner;
import com.panita.enriquecraft.link.service.LinkSync;
import net.fabricmc.loader.api.FabricLoader;

import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.function.Supplier;

/**
 * Keeps the whitelist in step with the Panita accounts: a member who activated their account in Discord can join, and
 * one who left the Discord server can not. It reads who may play from the Panita API and never sends anything to it.
 */
public final class LinkModule implements EnriquecraftModule {

    private static final String KEY_FILE = "enriquecraft-link.key";
    private static final String SNAPSHOT_FILE = "linked_players.snbt";

    @Override
    public Optional<Class<?>> messages() {
        return Optional.of(LinkMessages.class);
    }

    @Override
    public void registerServices(ServiceRegistry services) {
        LinkConfig config = services.get(LinkConfig.class);
        Path keyFile = FabricLoader.getInstance().getConfigDir().resolve(KEY_FILE);
        // Read at each use, so a key put in place later is found by the next reload.
        Supplier<Optional<String>> key = () -> ApiKey.find(keyFile, System::getenv);
        Executor worker = Executors.newSingleThreadExecutor(task -> {
            Thread thread = new Thread(task, "enriquecraft-link");
            thread.setDaemon(true);
            return thread;
        });
        var snapshot = services.get(WorldData.class).register(SNAPSHOT_FILE, LinkedSnapshot.CODEC, LinkedSnapshot.EMPTY);
        LinkView view = new LinkView(services.get(Messenger.class), services.get(MessageFormatter.class));
        LinkApplier applier = new LinkApplier(services.get(WhitelistAdmin.class), services.get(PlayerDataMigration.class), view);
        LinkSync sync = new LinkSync(
                new HttpPanitaApi(config.apiUrl::get, () -> Duration.ofSeconds(config.requestTimeoutSeconds.get()), key),
                snapshot::get, snapshot::set, key, config.maxRemovalPercent::get, worker, Instant::now);

        services.register(LinkView.class, view);
        services.register(LinkSync.class, sync);
        services.register(LinkRunner.class, new LinkRunner(sync, applier, view));
    }
}
