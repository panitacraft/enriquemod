package com.panita.enriquecraft.link.service;

import com.panita.enriquecraft.auth.data.AuthMode;
import com.panita.enriquecraft.link.client.LinkedPlayer;
import net.minecraft.server.players.NameAndId;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LinkPlanTest {

    private static final LinkedPlayer JAVI = new LinkedPlayer(12, "Javithor360", AuthMode.PREMIUM, "1");
    private static final LinkedPlayer TEZ = new LinkedPlayer(30, "Tezzlar", AuthMode.CRACKED, "2");

    private static NameAndId premium(String name) {
        return new NameAndId(UUID.nameUUIDFromBytes(("real:" + name).getBytes()), name);
    }

    private static NameAndId cracked(String name) {
        return NameAndId.createOffline(name);
    }

    private static LinkPlan plan(List<LinkedPlayer> previous, List<LinkedPlayer> desired, NameAndId... whitelist) {
        return LinkPlan.compute(previous, desired, List.of(whitelist));
    }

    @Test
    void everyoneOfANewListWithAnEmptyWhitelistIsAddedInOrder() {
        LinkPlan plan = plan(List.of(), List.of(JAVI, TEZ));

        assertEquals(List.of(new LinkPlan.Action.Add(JAVI), new LinkPlan.Action.Add(TEZ)), plan.actions());
        assertEquals(0, plan.unchanged());
    }

    @Test
    void playersThatAlreadyHaveTheRightEntryNeedNothing() {
        LinkPlan plan = plan(List.of(JAVI, TEZ), List.of(JAVI, TEZ), premium("Javithor360"), cracked("Tezzlar"));

        assertTrue(plan.actions().isEmpty());
        assertEquals(2, plan.unchanged());
    }

    @Test
    void theNameIsFoundWhateverItsCapitalization() {
        LinkPlan plan = plan(List.of(JAVI), List.of(JAVI), premium("javithor360"));

        assertTrue(plan.actions().isEmpty());
    }

    @Test
    void anEntryOfTheOtherModeIsReplaced() {
        NameAndId wrongJavi = cracked("Javithor360");
        NameAndId wrongTez = premium("Tezzlar");

        LinkPlan plan = plan(List.of(JAVI, TEZ), List.of(JAVI, TEZ), wrongJavi, wrongTez);

        assertEquals(List.of(new LinkPlan.Action.Replace(JAVI, wrongJavi), new LinkPlan.Action.Replace(TEZ, wrongTez)), plan.actions());
    }

    @Test
    void anOfflineNameWithAnotherCapitalizationIsReplacedBecauseItsUuidDiffers() {
        NameAndId lowercase = cracked("tezzlar");

        LinkPlan plan = plan(List.of(TEZ), List.of(TEZ), lowercase);

        assertEquals(List.of(new LinkPlan.Action.Replace(TEZ, lowercase)), plan.actions());
    }

    @Test
    void anEntryStaffUndidByHandIsAddedAgain() {
        LinkPlan plan = plan(List.of(JAVI), List.of(JAVI));

        assertEquals(List.of(new LinkPlan.Action.Add(JAVI)), plan.actions());
    }

    @Test
    void entriesStaffAddedByHandAreNeverTouched() {
        LinkPlan plan = plan(List.of(JAVI), List.of(JAVI), premium("Javithor360"), cracked("Tester"), premium("Friend"));

        assertTrue(plan.actions().isEmpty());
    }

    @Test
    void aPlayerWhoChangedTheirNameIsRenamedFromTheirPreviousName() {
        LinkedPlayer renamed = new LinkedPlayer(30, "TezzlarNew", AuthMode.CRACKED, "2");

        LinkPlan plan = plan(List.of(TEZ), List.of(renamed), cracked("Tezzlar"));

        assertEquals(List.of(new LinkPlan.Action.Rename(renamed, "Tezzlar")), plan.actions());
        assertEquals(0, plan.leaving());
    }

    @Test
    void aChangeOfCapitalizationAloneIsARename() {
        LinkedPlayer recased = new LinkedPlayer(30, "tezzlar", AuthMode.CRACKED, "2");

        LinkPlan plan = plan(List.of(TEZ), List.of(recased), cracked("Tezzlar"));

        assertEquals(List.of(new LinkPlan.Action.Rename(recased, "Tezzlar")), plan.actions());
    }

    @Test
    void aPlayerWhoLeftTheListAndStillHasTheirEntryIsRemoved() {
        NameAndId entry = cracked("Tezzlar");

        LinkPlan plan = plan(List.of(JAVI, TEZ), List.of(JAVI), premium("Javithor360"), entry);

        assertEquals(List.of(new LinkPlan.Action.Remove(TEZ, entry)), plan.actions());
        assertEquals(1, plan.leaving());
    }

    @Test
    void aPlayerWhoLeftTheListWithoutAnEntryIsOnlyCounted() {
        LinkPlan plan = plan(List.of(JAVI, TEZ), List.of(JAVI), premium("Javithor360"));

        assertTrue(plan.actions().isEmpty());
        assertEquals(1, plan.leaving());
    }

    @Test
    void aNameThatPassedToAnotherPlayerKeepsItsEntry() {
        LinkedPlayer newOwner = new LinkedPlayer(99, "Tezzlar", AuthMode.CRACKED, "9");

        LinkPlan plan = plan(List.of(TEZ), List.of(newOwner), cracked("Tezzlar"));

        assertTrue(plan.actions().isEmpty());
        assertEquals(1, plan.leaving());
    }

    @Test
    void theFirstPlayerOfADuplicatedNameWinsAndTheOtherIsReported() {
        LinkedPlayer twin = new LinkedPlayer(40, "TEZZLAR", AuthMode.CRACKED, "4");

        LinkPlan plan = plan(List.of(), List.of(TEZ, twin));

        assertEquals(List.of(new LinkPlan.Action.Add(TEZ)), plan.actions());
        assertEquals(List.of(twin), plan.duplicates());
    }

    @Test
    void nothingChangesWhenThereIsNothingToDo() {
        LinkPlan plan = plan(List.of(), List.of());

        assertTrue(plan.actions().isEmpty());
        assertEquals(0, plan.unchanged());
        assertEquals(0, plan.leaving());
    }

    @Test
    void aListThatRemovesMoreThanTheLimitIsRefused() {
        assertFalse(LinkPlan.removesTooMany(10, 5, 50));
        assertTrue(LinkPlan.removesTooMany(10, 6, 50));
    }

    @Test
    void anEmptyListAfterANonEmptyOneIsRefusedUnlessTheLimitIsAll() {
        assertTrue(LinkPlan.removesTooMany(3, 3, 50));
        assertFalse(LinkPlan.removesTooMany(3, 3, 100));
    }

    @Test
    void thereIsNothingToRefuseWhenNobodyWasLinkedBefore() {
        assertFalse(LinkPlan.removesTooMany(0, 0, 0));
    }

    @Test
    void aLimitOfZeroRefusesEveryRemoval() {
        assertTrue(LinkPlan.removesTooMany(100, 1, 0));
        assertFalse(LinkPlan.removesTooMany(100, 0, 0));
    }
}
