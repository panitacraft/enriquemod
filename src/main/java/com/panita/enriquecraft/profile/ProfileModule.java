package com.panita.enriquecraft.profile;

import com.panita.enriquecraft.core.art.InlineArts;
import com.panita.enriquecraft.core.framework.data.WorldData;
import com.panita.enriquecraft.core.framework.inject.ServiceRegistry;
import com.panita.enriquecraft.core.framework.module.EnriquecraftModule;
import com.panita.enriquecraft.core.message.Messenger;
import com.panita.enriquecraft.core.ui.UiService;
import com.panita.enriquecraft.link.client.ApiAccess;
import com.panita.enriquecraft.link.service.LinkedPlayers;
import com.panita.enriquecraft.profile.client.HttpEmblemApi;
import com.panita.enriquecraft.profile.config.ProfileConfig;
import com.panita.enriquecraft.profile.data.EmblemSelections;
import com.panita.enriquecraft.profile.gui.OpenMenus;
import com.panita.enriquecraft.profile.gui.ProfileMenus;
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
    /** How long opening the menu leaves the API alone, so opening it again and again does not read the profile each time. */
    private static final int MENU_REFRESH_SECONDS = 15;

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
        new EmblemPlaceholders(profiles, tooltip, services.get(InlineArts.class)).register();

        OpenMenus openMenus = new OpenMenus();
        profiles.onUpdated(player -> openMenus.refresh(player.getUUID()));

        services.register(EmblemTooltip.class, tooltip);
        services.register(EmblemResolver.class, resolver);
        services.register(ProfileService.class, profiles);
        services.register(RefreshCooldown.class, cooldown);
        ProfileRefresher refresher = new ProfileRefresher(loader, profiles, cooldown);
        ProfileView view = new ProfileView(services.get(Messenger.class));

        services.register(ProfileView.class, view);
        services.register(ProfileRefresher.class, refresher);
        services.register(ProfileMenus.class, new ProfileMenus(services.get(UiService.class), profiles, refresher, view, tooltip, config,
                services.get(Messenger.class), openMenus, new RefreshCooldown(() -> MENU_REFRESH_SECONDS, Instant::now)));
    }
}
