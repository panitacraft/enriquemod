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
import static org.junit.jupiter.api.Assertions.assertTrue;

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

    /** Resolves a player who chose no prank level. */
    private PlayerEmblems resolve(ModProfile profile, Optional<List<String>> chosen) {
        return resolver.resolve(profile, chosen, Optional.empty());
    }

    @Test
    void theFirstPrankLevelIsShownUntilAnotherIsChosenAndItsImageComesFromTheConfig() {
        PlayerEmblems emblems = resolve(profile(), Optional.empty());

        assertEquals("enrique6_prank_0", emblems.prank().id());
        assertEquals(Optional.of("https://res.cloudinary.com/panita/image/upload/v1794522697/panita-extra/pranks/prank_level_zero.png"),
                emblems.prank().iconUrl());
        assertEquals(new EmblemLook("☠", "white"), emblems.prank().look());
    }

    @Test
    void everyLevelCanBeChosenInTheConfigsOrder() {
        PlayerEmblems emblems = resolve(profile(), Optional.empty());

        assertEquals(List.of("enrique6_prank_0", "enrique6_prank_0_plus", "enrique6_prank_1", "enrique6_prank_1_plus", "enrique6_prank_2",
                "enrique6_prank_2_plus", "enrique6_prank_3", "enrique6_prank_3_plus"), ids(emblems.pranks()));
    }

    @Test
    void aChosenLevelIsShownWhateverTheApiSays() {
        PlayerEmblems emblems = resolver.resolve(profile(), Optional.empty(), Optional.of("enrique6_prank_2_plus"));

        assertEquals("enrique6_prank_2_plus", emblems.prank().id());
        assertEquals(new EmblemLook("☠⁺", "gold"), emblems.prank().look());
    }

    @Test
    void aLevelThatIsNotOneAnymoreShowsTheFirst() {
        assertEquals("enrique6_prank_0", resolver.resolve(profile(), Optional.empty(), Optional.of("enrique6_prank_9")).prank().id());
        assertEquals("enrique6_prank_0", resolver.resolve(profile(), Optional.empty(), Optional.of("hero")).prank().id());
    }

    @Test
    void aPlayerTheApiHasNothingOfStillHasALevelAndNoEmblems() {
        PlayerEmblems emblems = resolver.withoutProfile(Optional.of("enrique6_prank_1"));

        assertEquals("enrique6_prank_1", emblems.prank().id());
        assertEquals(List.of(), emblems.chosen());
        assertEquals(List.of(), emblems.selectable());
        assertEquals(3, emblems.empty().size());
    }

    @Test
    void theApisEmblemsForThePrankLevelsAreNeverOfferedAsOrdinaryOnes() {
        PlayerEmblems emblems = resolve(profile(
                emblem("enrique6_prank_3", EmblemCategory.LOCAL_EMBLEM, 1), emblem("enrique6_prank_0_plus", EmblemCategory.EMBLEM, 2),
                emblem("hero", EmblemCategory.EMBLEM, 3)), Optional.empty());

        assertEquals(List.of("hero"), ids(emblems.selectable()));
        assertEquals(List.of("hero"), ids(emblems.chosen()));
        assertEquals("enrique6_prank_0", emblems.prank().id());
    }

    @Test
    void theStaffEmblemAndTheNationalityAreChosenLikeAnyOther() {
        PlayerEmblems emblems = resolve(profile(
                emblem("nationality_co", EmblemCategory.SYSTEM, 1),
                emblem("enriquecraft6_staff", EmblemCategory.LOCAL_EMBLEM, 2),
                emblem("hero", EmblemCategory.EMBLEM, 3),
                emblem("extra", EmblemCategory.EMBLEM, 4)), Optional.empty());

        assertEquals(List.of("nationality_co", "enriquecraft6_staff", "hero", "extra"), ids(emblems.selectable()));
        assertEquals(List.of("nationality_co", "enriquecraft6_staff", "hero"), ids(emblems.chosen()));
    }

    @Test
    void aSystemEmblemTheServerDoesNotAcceptNeverShows() {
        PlayerEmblems emblems = resolve(profile(
                emblem("veterano", EmblemCategory.SYSTEM, 1),
                emblem("master_of_lens", EmblemCategory.SYSTEM, 2)), Optional.empty());

        assertEquals(List.of(), ids(emblems.selectable()));
        assertEquals(List.of(), ids(emblems.chosen()));
    }

    @Test
    void everyPlaceNotFilledHasAStandInWithItsImage() {
        PlayerEmblems one = resolve(profile(emblem("a", EmblemCategory.EMBLEM, 1)), Optional.of(List.of("a")));
        PlayerEmblems full = resolve(profile(emblem("a", EmblemCategory.EMBLEM, 1), emblem("b", EmblemCategory.EMBLEM, 2),
                emblem("c", EmblemCategory.EMBLEM, 3)), Optional.empty());

        assertEquals(List.of("no_emblem", "no_emblem"), ids(one.empty()));
        assertEquals(Optional.of("https://res.cloudinary.com/panita/image/upload/v1794522806/panita-extra/emblems/no_emblem.png"),
                one.empty().getFirst().iconUrl());
        assertEquals(new EmblemLook("○", "dark_gray"), one.empty().getFirst().look());
        assertTrue(full.empty().isEmpty());
    }

    @Test
    void withoutAChoiceTheFirstEmblemsShowUpToTheLimit() {
        PlayerEmblems emblems = resolve(profile(
                emblem("a", EmblemCategory.EMBLEM, 1), emblem("b", EmblemCategory.LOCAL_EMBLEM, 2),
                emblem("c", EmblemCategory.EMBLEM, 3), emblem("d", EmblemCategory.EMBLEM, 4)), Optional.empty());

        assertEquals(List.of("a", "b", "c"), ids(emblems.chosen()));
        assertEquals(List.of("a", "b", "c", "d"), ids(emblems.selectable()));
    }

    @Test
    void aChoiceIsShownInItsOrderAndAnEmptyChoiceShowsNothing() {
        ModProfile held = profile(emblem("a", EmblemCategory.EMBLEM, 1), emblem("b", EmblemCategory.EMBLEM, 2), emblem("c", EmblemCategory.EMBLEM, 3));

        assertEquals(List.of("c", "a"), ids(resolve(held, Optional.of(List.of("c", "a"))).chosen()));
        assertEquals(List.of(), ids(resolve(held, Optional.of(List.of())).chosen()));
    }

    @Test
    void whatThePlayerNoLongerHoldsDropsOutOfTheirChoice() {
        ModProfile held = profile(emblem("a", EmblemCategory.EMBLEM, 1), emblem("enrique6_prank_2", EmblemCategory.LOCAL_EMBLEM, 2));

        PlayerEmblems emblems = resolve(held, Optional.of(List.of("gone", "a", "enrique6_prank_2", "a")));

        assertEquals(List.of("a"), ids(emblems.chosen()));
    }

    @Test
    void theChoiceIsCappedByTheLimit() {
        ModProfile held = profile(emblem("a", EmblemCategory.EMBLEM, 1), emblem("b", EmblemCategory.EMBLEM, 2),
                emblem("c", EmblemCategory.EMBLEM, 3), emblem("d", EmblemCategory.EMBLEM, 4));

        assertEquals(List.of("d", "c", "b"), ids(resolve(held, Optional.of(List.of("d", "c", "b", "a"))).chosen()));
    }

    @Test
    void titlesAreNeverShownOrChosen() {
        PlayerEmblems emblems = resolve(profile(emblem("rey", EmblemCategory.TITLE, 1), emblem("a", EmblemCategory.EMBLEM, 2)),
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
