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
import java.util.function.Function;

/**
 * Decides which of the emblems the API says a player holds this server shows, and in which place. The API only filters by edition:
 * which emblem is the prank level, which system emblems can be chosen and how many fit are this server's rules, in the config.
 * <p>
 * Every player shows one prank emblem before their name: the level they chose among those they hold, or level zero, which is where
 * everyone starts and is not a choice itself. It does not count toward the emblems after the name, which are everything else they
 * hold, the nationality and the staff emblem included.
 */
public final class EmblemResolver {

    private static final EmblemLook FALLBACK_LOOK = new EmblemLook("○", "gray");

    private final ProfileConfig config;
    private final Function<String, Optional<ApiEmblem>> catalog;

    /**
     * @param catalog finds an emblem by id among everything the API has said of anyone, so the default prank level has its image even
     *                for a player who does not hold it
     */
    public EmblemResolver(ProfileConfig config, Function<String, Optional<ApiEmblem>> catalog) {
        this.config = config;
        this.catalog = catalog;
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
        return new PlayerEmblems(defaultPrank(), List.of(), List.of(), List.of());
    }

    /**
     * @param chosen      the ids the player chose, in order; empty when they never chose, in which case the first emblems they hold are
     *                    shown
     * @param chosenPrank the prank emblem the player chose; empty when they never did, or when they no longer hold it, which shows the
     *                    default level
     */
    public PlayerEmblems resolve(ModProfile profile, Optional<List<String>> chosen, Optional<String> chosenPrank) {
        List<String> prefixes = ProfileConfig.idsOf(config.systemPrefixes.get());

        // The default level is what everyone starts at, so it is shown when nothing else is chosen and is not a choice itself
        List<ApiEmblem> pranks = profile.emblems().stream()
                .filter(emblem -> prankOf(emblem).filter(level -> !level.equals(PrankLevel.DEFAULT)).isPresent())
                .sorted(Comparator.comparing(emblem -> prankOf(emblem).orElse(PrankLevel.DEFAULT)))
                .toList();
        EmblemView prank = chosenPrank.flatMap(id -> pranks.stream().filter(emblem -> emblem.id().equals(id)).findFirst())
                .or(() -> profile.emblems().stream().filter(emblem -> prankOf(emblem).filter(PrankLevel.DEFAULT::equals).isPresent()).findFirst())
                .map(this::view)
                .orElseGet(this::defaultPrank);

        List<ApiEmblem> selectable = profile.emblems().stream()
                .filter(emblem -> (isPlain(emblem) && prankOf(emblem).isEmpty())
                        || (emblem.category() == EmblemCategory.SYSTEM && prefixes.stream().anyMatch(emblem.id()::startsWith)))
                .toList();

        return new PlayerEmblems(prank, pranks.stream().map(this::view).toList(), chosenFrom(selectable, chosen).stream().map(this::view).toList(),
                selectable.stream().map(this::view).toList());
    }

    private static boolean isPlain(ApiEmblem emblem) {
        return emblem.category() == EmblemCategory.EMBLEM || emblem.category() == EmblemCategory.LOCAL_EMBLEM;
    }

    /** The prank level of an emblem, when it is one: a global or local emblem whose id is a prank id. */
    private Optional<PrankLevel> prankOf(ApiEmblem emblem) {
        return isPlain(emblem) ? PrankLevel.parse(emblem.id(), config.prankPrefix.get()) : Optional.empty();
    }

    /** The default level as the API describes it, when anyone's profile has it; otherwise its text alone. */
    private EmblemView defaultPrank() {
        String id = PrankLevel.DEFAULT.id(config.prankPrefix.get());
        return catalog.apply(id).map(this::view).orElseGet(() -> new EmblemView(id, ProfileMessages.Prank.DEFAULT_TITLE, Optional.empty(),
                Optional.empty(), EmblemCategory.LOCAL_EMBLEM, lookOf(id, EmblemCategory.LOCAL_EMBLEM)));
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
