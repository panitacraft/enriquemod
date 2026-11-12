package com.panita.enriquecraft.profile.service;

import com.panita.enriquecraft.profile.client.ApiEmblem;
import com.panita.enriquecraft.profile.client.EmblemCategory;
import com.panita.enriquecraft.profile.client.ModProfile;
import com.panita.enriquecraft.profile.config.ProfileConfig;
import com.panita.enriquecraft.profile.data.EmblemLook;
import com.panita.enriquecraft.profile.message.ProfileMessages;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Decides which of the emblems the API says a player holds this server shows, and in which place. The API only filters by edition:
 * which emblem is the prank level, which system emblems can be chosen and how many fit are this server's rules, in the config.
 * <p>
 * Every player has a prank emblem, the only one that is not up to them: the highest level they hold, or level zero when they hold none.
 * Everything else they hold, the nationality and the staff emblem included, is theirs to choose.
 */
public final class EmblemResolver {

    private static final EmblemLook FALLBACK_LOOK = new EmblemLook("○", "gray");

    private final ProfileConfig config;

    public EmblemResolver(ProfileConfig config) {
        this.config = config;
    }

    /** How an emblem looks without the client mod: its own entry, its prank level, or its category's. */
    public EmblemLook lookOf(ApiEmblem emblem) {
        return lookOf(emblem.id(), emblem.category());
    }

    private EmblemLook lookOf(String id, EmblemCategory category) {
        EmblemLook own = config.looks.get().get(id);
        if (own != null) {
            return own;
        }
        Optional<PrankLevel> prank = PrankLevel.parse(id, config.prankPrefix.get());
        if (prank.isPresent()) {
            return prank.get().look();
        }
        // A title has no icon of its own, so it is drawn as the plain kind of emblem
        String key = category == EmblemCategory.TITLE ? EmblemCategory.EMBLEM.id() : category.id();
        return config.categoryLooks.get().getOrDefault(key, FALLBACK_LOOK);
    }

    public EmblemView view(ApiEmblem emblem) {
        return new EmblemView(emblem.id(), emblem.name(), emblem.description(), emblem.iconUrl(), emblem.category(), lookOf(emblem));
    }

    /** What a player the API has said nothing of shows: the default prank level, as text only, and nothing to choose from. */
    public PlayerEmblems withoutProfile() {
        return new PlayerEmblems(defaultPrank(), List.of(), List.of());
    }

    /**
     * @param chosen the ids the player chose, in order; empty when they never chose, in which case the first emblems they hold are
     *               shown
     */
    public PlayerEmblems resolve(ModProfile profile, Optional<List<String>> chosen) {
        List<String> prefixes = ProfileConfig.idsOf(config.systemPrefixes.get());

        EmblemView prank = profile.emblems().stream()
                .filter(emblem -> prankOf(emblem).isPresent())
                .max(Comparator.comparing(emblem -> prankOf(emblem).orElse(PrankLevel.DEFAULT)))
                .map(this::view)
                .orElseGet(this::defaultPrank);

        List<ApiEmblem> selectable = profile.emblems().stream()
                .filter(emblem -> (isPlain(emblem) && prankOf(emblem).isEmpty())
                        || (emblem.category() == EmblemCategory.SYSTEM && prefixes.stream().anyMatch(emblem.id()::startsWith)))
                .toList();

        return new PlayerEmblems(prank, chosenFrom(selectable, chosen).stream().map(this::view).toList(),
                selectable.stream().map(this::view).toList());
    }

    private static boolean isPlain(ApiEmblem emblem) {
        return emblem.category() == EmblemCategory.EMBLEM || emblem.category() == EmblemCategory.LOCAL_EMBLEM;
    }

    /** The prank level of an emblem, when it is one: a global or local emblem whose id is a prank id. */
    private Optional<PrankLevel> prankOf(ApiEmblem emblem) {
        return isPlain(emblem) ? PrankLevel.parse(emblem.id(), config.prankPrefix.get()) : Optional.empty();
    }

    private EmblemView defaultPrank() {
        String id = PrankLevel.DEFAULT.id(config.prankPrefix.get());
        return new EmblemView(id, ProfileMessages.Prank.DEFAULT_TITLE, Optional.empty(), Optional.empty(), EmblemCategory.LOCAL_EMBLEM,
                lookOf(id, EmblemCategory.LOCAL_EMBLEM));
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
