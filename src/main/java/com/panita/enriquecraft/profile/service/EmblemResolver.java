package com.panita.enriquecraft.profile.service;

import com.panita.enriquecraft.profile.client.ApiEmblem;
import com.panita.enriquecraft.profile.client.EmblemCategory;
import com.panita.enriquecraft.profile.client.ModProfile;
import com.panita.enriquecraft.profile.config.ProfileConfig;
import com.panita.enriquecraft.profile.data.EmblemLook;
import com.panita.enriquecraft.profile.data.PrankLevel;
import com.panita.enriquecraft.profile.message.ProfileMessages;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Decides which emblems a player shows and in which place. The API only says which emblems of the edition a player holds; which of
 * them can be chosen and how many fit are this server's rules, in the config.
 * <p>
 * Every player shows one prank level before their name. The levels are the server's own, from the config, and the player may pick
 * any of them; the first is where they start. Everything the API says they hold, the nationality and the staff emblem included, is
 * theirs to choose to show after the name, except the emblems the API has for the prank levels, which would show the same thing twice.
 */
public final class EmblemResolver {

    /** The id of the stand-in for a place after the name the player has not filled; an entry of {@code profile.looks} may restyle it. */
    static final String EMPTY_ID = "no_emblem";

    private static final EmblemLook FALLBACK_LOOK = new EmblemLook("○", "gray");
    private static final EmblemLook EMPTY_LOOK = new EmblemLook("○", "dark_gray");

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

    private EmblemView view(PrankLevel level) {
        return new EmblemView(level.id(), level.title(), Optional.of(level.description()).filter(text -> !text.isBlank()),
                Optional.of(level.image()).filter(url -> !url.isEmpty()), EmblemCategory.LOCAL_EMBLEM, level.look());
    }

    /** Whether the id is one of the prank levels. */
    public boolean isPrank(String id) {
        return config.pranks.get().stream().anyMatch(level -> level.id().equals(id));
    }

    /** Every prank level, lowest first. */
    public List<EmblemView> levels() {
        return config.pranks.get().stream().map(this::view).toList();
    }

    /** The level a player is at until they choose one. */
    public String defaultPrankId() {
        return config.pranks.get().getFirst().id();
    }

    /** What a player the API has said nothing of shows: the prank level and the empty places, and nothing to choose from. */
    public PlayerEmblems withoutProfile(Optional<String> prank) {
        return assemble(List.of(), Optional.empty(), prank);
    }

    /**
     * @param chosen the ids the player chose, in order; empty when they never chose, in which case the first emblems they hold are
     *               shown
     * @param prank  the id of the prank level the player chose; empty when they never did, or when it is no longer a level, which
     *               shows the first one
     */
    public PlayerEmblems resolve(ModProfile profile, Optional<List<String>> chosen, Optional<String> prank) {
        List<String> prefixes = ProfileConfig.idsOf(config.systemPrefixes.get());
        List<ApiEmblem> selectable = profile.emblems().stream()
                .filter(emblem -> (isPlain(emblem) && !isPrank(emblem.id()))
                        || (emblem.category() == EmblemCategory.SYSTEM && prefixes.stream().anyMatch(emblem.id()::startsWith)))
                .toList();
        return assemble(selectable, chosen, prank);
    }

    private PlayerEmblems assemble(List<ApiEmblem> selectable, Optional<List<String>> chosen, Optional<String> prank) {
        List<EmblemView> levels = levels();
        EmblemView shown = prank.flatMap(id -> levels.stream().filter(level -> level.id().equals(id)).findFirst()).orElse(levels.getFirst());
        List<EmblemView> shownEmblems = chosenFrom(selectable, chosen).stream().map(this::view).toList();
        List<EmblemView> empty = Collections.nCopies(Math.max(0, config.loreMax.get() - shownEmblems.size()), emptySlot());
        return new PlayerEmblems(shown, levels, shownEmblems, empty, selectable.stream().map(this::view).toList());
    }

    private static boolean isPlain(ApiEmblem emblem) {
        return emblem.category() == EmblemCategory.EMBLEM || emblem.category() == EmblemCategory.LOCAL_EMBLEM;
    }

    /** What stands for a place after the name the player has not filled. */
    private EmblemView emptySlot() {
        EmblemLook look = config.looks.get().getOrDefault(EMPTY_ID, EMPTY_LOOK);
        return new EmblemView(EMPTY_ID, ProfileMessages.Emblems.EMPTY_TITLE, Optional.of(ProfileMessages.Emblems.EMPTY_DESCRIPTION),
                Optional.of(config.emptyEmblemImage.get()).filter(url -> !url.isEmpty()), EmblemCategory.LOCAL_EMBLEM, look);
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
