package com.panita.enriquecraft.profile.service;

import com.panita.enriquecraft.profile.client.ApiEmblem;
import com.panita.enriquecraft.profile.client.EmblemCategory;
import com.panita.enriquecraft.profile.client.ModProfile;
import com.panita.enriquecraft.profile.config.ProfileConfig;
import com.panita.enriquecraft.profile.data.EmblemLook;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Decides which of the emblems the API says a player holds this server shows, and in which place. The API only filters by edition:
 * which system emblems count, which emblem is shown with them and how many fit are this server's rules, in the config.
 */
public final class EmblemResolver {

    private static final EmblemLook FALLBACK_LOOK = new EmblemLook("○", "gray");

    private final ProfileConfig config;

    public EmblemResolver(ProfileConfig config) {
        this.config = config;
    }

    /** How an emblem looks without the client mod: its own entry, or its category's. */
    public EmblemLook lookOf(ApiEmblem emblem) {
        EmblemLook own = config.looks.get().get(emblem.id());
        if (own != null) {
            return own;
        }
        // A title has no icon of its own, so it is drawn as the plain kind of emblem
        String category = emblem.category() == EmblemCategory.TITLE ? EmblemCategory.EMBLEM.id() : emblem.category().id();
        return config.categoryLooks.get().getOrDefault(category, FALLBACK_LOOK);
    }

    public EmblemView view(ApiEmblem emblem) {
        return new EmblemView(emblem.id(), emblem.name(), emblem.description(), emblem.iconUrl(), emblem.category(), lookOf(emblem));
    }

    /**
     * @param chosen the ids the player chose, in order; empty when they never chose, in which case the first emblems they hold are
     *               shown
     */
    public PlayerEmblems resolve(ModProfile profile, Optional<List<String>> chosen) {
        List<String> promotedIds = ProfileConfig.idsOf(config.systemIds.get());
        List<String> prefixes = ProfileConfig.idsOf(config.systemPrefixes.get());

        List<ApiEmblem> promoted = new ArrayList<>();
        for (String id : promotedIds) {
            profile.emblems().stream().filter(emblem -> emblem.id().equals(id) && emblem.category() != EmblemCategory.SYSTEM
                    && emblem.category() != EmblemCategory.TITLE).findFirst().ifPresent(promoted::add);
        }
        List<ApiEmblem> system = new ArrayList<>(promoted);
        profile.emblems().stream()
                .filter(emblem -> emblem.category() == EmblemCategory.SYSTEM
                        && prefixes.stream().anyMatch(prefix -> emblem.id().startsWith(prefix)))
                .forEach(system::add);

        List<ApiEmblem> selectable = profile.emblems().stream()
                .filter(emblem -> emblem.category() == EmblemCategory.EMBLEM || emblem.category() == EmblemCategory.LOCAL_EMBLEM)
                .filter(emblem -> !promotedIds.contains(emblem.id()))
                .toList();

        return new PlayerEmblems(
                system.stream().limit(config.systemMax.get()).map(this::view).toList(),
                chosenFrom(selectable, chosen).stream().map(this::view).toList(),
                selectable.stream().map(this::view).toList());
    }

    private List<ApiEmblem> chosenFrom(List<ApiEmblem> selectable, Optional<List<String>> chosen) {
        int max = config.loreMax.get();
        if (chosen.isEmpty()) {
            return selectable.stream().limit(max).toList();
        }
        // What the player no longer holds, or what is no longer selectable, simply drops out of their choice
        Set<String> ids = new LinkedHashSet<>(chosen.get());
        List<ApiEmblem> result = new ArrayList<>();
        for (String id : ids) {
            selectable.stream().filter(emblem -> emblem.id().equals(id)).findFirst().ifPresent(result::add);
        }
        return result.stream().limit(max).toList();
    }
}
