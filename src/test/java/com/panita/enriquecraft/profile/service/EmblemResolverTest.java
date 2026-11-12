package com.panita.enriquecraft.profile.service;

import com.panita.enriquecraft.core.framework.config.ConfigManager;
import com.panita.enriquecraft.profile.client.ApiEmblem;
import com.panita.enriquecraft.profile.client.EmblemCategory;
import com.panita.enriquecraft.profile.client.ModProfile;
import com.panita.enriquecraft.profile.config.ProfileConfig;
import com.panita.enriquecraft.profile.data.EmblemLook;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;

class EmblemResolverTest {

    @TempDir
    Path directory;

    private EmblemResolver resolver;

    private static ApiEmblem emblem(String id, EmblemCategory category, int position) {
        return new ApiEmblem(id, "Titulo " + id, Optional.of("Descripcion " + id), Optional.of("https://res.cloudinary.com/x/" + id + ".png"),
                category, position, category == EmblemCategory.SYSTEM ? Optional.empty() : Optional.of("enriquecraft6"));
    }

    private static ModProfile profile(ApiEmblem... emblems) {
        return new ModProfile(7, Optional.of("Alex"), "alex", List.of(emblems));
    }

    private static List<String> ids(List<EmblemView> views) {
        return views.stream().map(EmblemView::id).toList();
    }

    @BeforeEach
    void bind() {
        resolver = new EmblemResolver(new ConfigManager(directory).bind("profile", ProfileConfig.class));
    }

    @Test
    void theStaffEmblemIsPromotedAndShownBeforeTheNationality() {
        PlayerEmblems emblems = resolver.resolve(profile(
                emblem("nationality_co", EmblemCategory.SYSTEM, 1),
                emblem("enriquecraft6_staff", EmblemCategory.LOCAL_EMBLEM, 2),
                emblem("hero", EmblemCategory.EMBLEM, 3)), Optional.empty());

        assertEquals(List.of("enriquecraft6_staff", "nationality_co"), ids(emblems.system()));
        assertEquals(List.of("hero"), ids(emblems.selectable()));
    }

    @Test
    void aSystemEmblemTheServerDoesNotAcceptNeverShows() {
        PlayerEmblems emblems = resolver.resolve(profile(
                emblem("veterano", EmblemCategory.SYSTEM, 1),
                emblem("master_of_lens", EmblemCategory.SYSTEM, 2)), Optional.empty());

        assertEquals(List.of(), ids(emblems.system()));
    }

    @Test
    void theSystemEmblemsAreCappedByTheSetting() {
        PlayerEmblems emblems = resolver.resolve(profile(
                emblem("enriquecraft6_staff", EmblemCategory.LOCAL_EMBLEM, 1),
                emblem("nationality_co", EmblemCategory.SYSTEM, 2),
                emblem("nationality_mx", EmblemCategory.SYSTEM, 3)), Optional.empty());

        assertEquals(List.of("enriquecraft6_staff", "nationality_co"), ids(emblems.system()));
    }

    @Test
    void withoutAChoiceTheFirstEmblemsShowUpToTheLimit() {
        PlayerEmblems emblems = resolver.resolve(profile(
                emblem("a", EmblemCategory.EMBLEM, 1), emblem("b", EmblemCategory.LOCAL_EMBLEM, 2),
                emblem("c", EmblemCategory.EMBLEM, 3), emblem("d", EmblemCategory.EMBLEM, 4)), Optional.empty());

        assertEquals(List.of("a", "b", "c"), ids(emblems.chosen()));
        assertEquals(List.of("a", "b", "c", "d"), ids(emblems.selectable()));
    }

    @Test
    void aChoiceIsShownInItsOrderAndAnEmptyChoiceShowsNothing() {
        ModProfile held = profile(emblem("a", EmblemCategory.EMBLEM, 1), emblem("b", EmblemCategory.EMBLEM, 2), emblem("c", EmblemCategory.EMBLEM, 3));

        assertEquals(List.of("c", "a"), ids(resolver.resolve(held, Optional.of(List.of("c", "a"))).chosen()));
        assertEquals(List.of(), ids(resolver.resolve(held, Optional.of(List.of())).chosen()));
    }

    @Test
    void whatThePlayerNoLongerHoldsDropsOutOfTheirChoice() {
        ModProfile held = profile(emblem("a", EmblemCategory.EMBLEM, 1), emblem("enriquecraft6_staff", EmblemCategory.LOCAL_EMBLEM, 2));

        PlayerEmblems emblems = resolver.resolve(held, Optional.of(List.of("gone", "a", "enriquecraft6_staff", "a")));

        assertEquals(List.of("a"), ids(emblems.chosen()));
    }

    @Test
    void theChoiceIsCappedByTheLimit() {
        ModProfile held = profile(emblem("a", EmblemCategory.EMBLEM, 1), emblem("b", EmblemCategory.EMBLEM, 2),
                emblem("c", EmblemCategory.EMBLEM, 3), emblem("d", EmblemCategory.EMBLEM, 4));

        assertEquals(List.of("d", "c", "b"), ids(resolver.resolve(held, Optional.of(List.of("d", "c", "b", "a"))).chosen()));
    }

    @Test
    void titlesAreNeverShownOrChosen() {
        PlayerEmblems emblems = resolver.resolve(profile(emblem("rey", EmblemCategory.TITLE, 1), emblem("a", EmblemCategory.EMBLEM, 2)),
                Optional.empty());

        assertEquals(List.of("a"), ids(emblems.chosen()));
        assertEquals(List.of("a"), ids(emblems.selectable()));
    }

    @Test
    void anEmblemLooksAsItsOwnEntryOrElseAsItsCategory() {
        assertEquals(new EmblemLook("★", "#ff5555"), resolver.lookOf(emblem("enriquecraft6_staff", EmblemCategory.LOCAL_EMBLEM, 1)));
        assertEquals(new EmblemLook("●", "gold"), resolver.lookOf(emblem("nationality_co", EmblemCategory.SYSTEM, 1)));
        assertEquals(new EmblemLook("○", "gray"), resolver.lookOf(emblem("hero", EmblemCategory.EMBLEM, 1)));
        assertEquals(new EmblemLook("○", "gray"), resolver.lookOf(emblem("rey", EmblemCategory.TITLE, 1)));
    }
}
