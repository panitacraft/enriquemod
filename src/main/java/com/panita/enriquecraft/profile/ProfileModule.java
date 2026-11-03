package com.panita.enriquecraft.profile;

import com.panita.enriquecraft.core.framework.data.WorldData;
import com.panita.enriquecraft.core.framework.inject.ServiceRegistry;
import com.panita.enriquecraft.core.framework.module.EnriquecraftModule;
import com.panita.enriquecraft.core.message.Messenger;
import com.panita.enriquecraft.link.client.ApiAccess;
import com.panita.enriquecraft.link.service.LinkedPlayers;
import com.panita.enriquecraft.profile.client.HttpEmblemApi;
import com.panita.enriquecraft.profile.config.ProfileConfig;
import com.panita.enriquecraft.profile.data.EmblemSelections;
import com.panita.enriquecraft.profile.message.ProfileMessages;
import com.panita.enriquecraft.profile.message.ProfileView;
import com.panita.enriquecraft.profile.message.EmblemTooltip;
import com.panita.enriquecraft.profile.service.EmblemPlaceholders;
import com.panita.enriquecraft.profile.service.EmblemResolver;
import com.panita.enriquecraft.profile.service.ProfileCache;
import com.panita.enriquecraft.profile.service.ProfileLoader;
import com.panita.enriquecraft.profile.service.ProfileRefresher;
import com.panita.enriquecraft.profile.service.ProfileService;
import com.panita.enriquecraft.profile.service.RefreshCooldown;

import java.time.Instant;
import java.util.Optional;

/**
 * What a player shows of their Panita profile on this server: the emblems of the edition next to their name, and the menu where
 * they choose them. It only reads the Panita API; what a player chooses to show is kept by the mod.
 */
public final class ProfileModule implements EnriquecraftModule {

    private static final String SELECTIONS_FILE = "emblem_selections.snbt";

    @Override
    public Optional<Class<?>> messages() {
        return Optional.of(ProfileMessages.class);
    }

    @Override
    public void registerServices(ServiceRegistry services) {
        ProfileConfig config = services.get(ProfileConfig.class);
        var selections = services.get(WorldData.class).register(SELECTIONS_FILE, EmblemSelections.CODEC, EmblemSelections.EMPTY);

        ProfileCache cache = new ProfileCache();
        EmblemResolver resolver = new EmblemResolver(config);
        LinkedPlayers linked = services.get(LinkedPlayers.class);
        ProfileService profiles = new ProfileService(cache, resolver, linked, selections::get, selections::set);
        ProfileLoader loader = new ProfileLoader(new HttpEmblemApi(services.get(ApiAccess.class)), linked, cache, config.editionId::get);
        RefreshCooldown cooldown = new RefreshCooldown(config.refreshCooldownSeconds::get, Instant::now);

        EmblemTooltip tooltip = new EmblemTooltip(config.tooltipWidth::get);
        new EmblemPlaceholders(profiles, tooltip).register();

        services.register(EmblemTooltip.class, tooltip);
        services.register(EmblemResolver.class, resolver);
        services.register(ProfileService.class, profiles);
        services.register(RefreshCooldown.class, cooldown);
        services.register(ProfileView.class, new ProfileView(services.get(Messenger.class)));
        services.register(ProfileRefresher.class, new ProfileRefresher(loader, profiles, cooldown));
    }
}
