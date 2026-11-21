package com.panita.enriquecraft.profile.service;

import com.panita.enriquecraft.profile.client.ApiEmblem;
import com.panita.enriquecraft.profile.client.EmblemCategory;
import com.panita.enriquecraft.profile.client.ModProfile;
import com.panita.enriquecraft.profile.config.ProfileConfig;
import com.panita.enriquecraft.profile.data.EmblemLook;
import com.panita.enriquecraft.profile.data.PrankLevel;
import com.panita.enriquecraft.profile.message.ProfileMessages;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Decides which emblems a player shows and in which place. The API only says which emblems of the edition a player holds; which of
 * them can be chosen and how many fit are this server's rules, in the config.
 * <p>
 * Every player shows one prank level before their name. The levels are the server's own, from the config, and the player may pick
 * any of them; the first is where they start. Every global or local emblem the API says they hold, the staff emblem included, is
 * theirs to choose to show after the name, except the emblems the API has for the prank levels, which would show the same thing twice.
 * The system emblems of the API (veteran, awards) are never shown by this server, except the nationality, which the profile shows
 * beside the name and the player cannot change in game.
 */
public final class EmblemResolver {

    /** The id of the stand-in for a place after the name the player has not filled; an entry of {@code profile.looks} may restyle it. */
    public static final String EMPTY_ID = "no_emblem";

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
        return assemble(List.of(), Optional.empty(), Optional.empty(), prank);
    }

    /**
     * @param chosen the ids the player chose, in order; empty when they never chose, in which case the first emblems they hold are
     *               shown
     * @param prank  the id of the prank level the player chose; empty when they never did, or when it is no longer a level, which
     *               shows the first one
     */
    public PlayerEmblems resolve(ModProfile profile, Optional<List<String>> chosen, Optional<String> prank) {
        List<ApiEmblem> selectable = profile.emblems().stream().filter(emblem -> isPlain(emblem) && !isPrank(emblem.id())).toList();
        String prefix = config.nationalityPrefix.get();
        Optional<ApiEmblem> nationality = prefix.isEmpty() ? Optional.empty() : profile.emblems().stream()
                .filter(emblem -> emblem.category() == EmblemCategory.SYSTEM && emblem.id().startsWith(prefix)).findFirst();
        return assemble(selectable, nationality, chosen, prank);
    }

    private PlayerEmblems assemble(List<ApiEmblem> selectable, Optional<ApiEmblem> held, Optional<List<String>> chosen, Optional<String> prank) {
        Optional<EmblemView> nationality = held.map(this::view);
        List<EmblemView> levels = levels();
        EmblemView shown = prank.flatMap(id -> levels.stream().filter(level -> level.id().equals(id)).findFirst()).orElse(levels.getFirst());
        List<Optional<ApiEmblem>> placed = placedFrom(selectable, chosen);
        List<EmblemView> slots = placed.stream().map(emblem -> emblem.map(this::view).orElseGet(this::emptySlot)).toList();
        List<EmblemView> shownEmblems = placed.stream().flatMap(Optional::stream).map(this::view).toList();
        return new PlayerEmblems(nationality, shown, levels, shownEmblems, slots, selectable.stream().map(this::view).toList());
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

    /**
     * What is in each place after the name. A player who never chose has their first emblems; one who did has exactly what they put in
     * each place, an empty string or an emblem they no longer hold leaving the place empty.
     */
    private List<Optional<ApiEmblem>> placedFrom(List<ApiEmblem> selectable, Optional<List<String>> chosen) {
        int max = config.loreMax.get();
        List<Optional<ApiEmblem>> placed = new ArrayList<>();
        Set<String> used = new LinkedHashSet<>();
        for (int place = 0; place < max; place++) {
            Optional<ApiEmblem> emblem;
            if (chosen.isEmpty()) {
                emblem = place < selectable.size() ? Optional.of(selectable.get(place)) : Optional.empty();
            } else {
                String id = place < chosen.get().size() ? chosen.get().get(place) : "";
                emblem = selectable.stream().filter(held -> held.id().equals(id) && !used.contains(id)).findFirst();
                emblem.ifPresent(held -> used.add(held.id()));
            }
            placed.add(emblem);
        }
        return placed;
    }
}
