package com.panita.enriquecraft.link.service;

import com.panita.enriquecraft.auth.data.AuthMode;
import com.panita.enriquecraft.link.client.LinkedPlayer;
import com.panita.enriquecraft.link.client.PanitaApi;
import com.panita.enriquecraft.link.data.LinkedSnapshot;
import net.minecraft.server.players.NameAndId;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LinkSyncTest {

    private static final LinkedPlayer A = new LinkedPlayer(1, "Alpha", AuthMode.PREMIUM, "1");
    private static final LinkedPlayer B = new LinkedPlayer(2, "Bravo", AuthMode.CRACKED, "2");
    private static final LinkedPlayer C = new LinkedPlayer(3, "Charlie", AuthMode.CRACKED, "3");
    private static final LinkedPlayer D = new LinkedPlayer(4, "Delta", AuthMode.CRACKED, "4");

    private final AtomicInteger asked = new AtomicInteger();
    private final AtomicInteger applied = new AtomicInteger();
    private CompletableFuture<PanitaApi.Result> answer = CompletableFuture.completedFuture(new PanitaApi.Result.Players(List.of(), Optional.empty()));
    private LinkedSnapshot snapshot = LinkedSnapshot.EMPTY;
    private final List<LinkedSnapshot> written = new ArrayList<>();
    private Optional<String> key = Optional.of("key");
    private int maxPercent = 50;
    private LinkReport report = new LinkReport(0, 0, 0, 0, 0, List.of(), List.of(), List.of());
    private RuntimeException applyFailure;

    private final PanitaApi api = etag -> {
        asked.incrementAndGet();
        return answer;
    };

    private final LinkTarget target = new LinkTarget() {
        @Override
        public void onServerThread(Runnable task) {
            task.run();
        }

        @Override
        public Collection<NameAndId> whitelist() {
            return List.of();
        }

        @Override
        public List<LinkApplier.Resolved> resolve(LinkPlan plan) {
            return List.of();
        }

        @Override
        public LinkReport apply(LinkPlan plan, List<LinkApplier.Resolved> resolved) {
            applied.incrementAndGet();
            if (applyFailure != null) {
                throw applyFailure;
            }
            return report;
        }
    };

    private final LinkSync sync = new LinkSync(api, () -> snapshot, value -> {
        snapshot = value;
        written.add(value);
    }, () -> key, () -> maxPercent, Runnable::run, () -> Instant.parse("2026-10-12T10:00:00Z"));

    private static PanitaApi.Result players(String etag, LinkedPlayer... players) {
        return new PanitaApi.Result.Players(List.of(players), Optional.of(etag));
    }

    @Test
    void withoutAKeyTheApiIsNotAsked() {
        key = Optional.empty();

        assertInstanceOf(SyncOutcome.NoKey.class, sync.sync(target).join());
        assertEquals(0, asked.get());
    }

    @Test
    void aFailureChangesNothingAndIsRemembered() {
        answer = CompletableFuture.completedFuture(new PanitaApi.Result.Failed("down"));

        SyncOutcome outcome = sync.sync(target).join();

        assertEquals(new SyncOutcome.Failed("down"), outcome);
        assertEquals(0, applied.get());
        assertTrue(written.isEmpty());
        assertEquals(outcome, sync.last().orElseThrow().outcome());
    }

    @Test
    void aNewListIsAppliedAndRememberedWithItsTag() {
        answer = CompletableFuture.completedFuture(players("\"v2\"", A, B));

        SyncOutcome outcome = sync.sync(target).join();

        assertEquals(new SyncOutcome.Applied(report, true), outcome);
        assertEquals(List.of(new LinkedSnapshot(List.of(A, B), Optional.of("\"v2\""))), written);
        assertEquals(2, sync.linkedCount());
    }

    @Test
    void aListThatHasNotChangedIsStillCheckedAgainstTheWhitelistButNotRewritten() {
        snapshot = new LinkedSnapshot(List.of(A), Optional.of("\"v1\""));
        answer = CompletableFuture.completedFuture(new PanitaApi.Result.NotModified());

        SyncOutcome outcome = sync.sync(target).join();

        assertEquals(new SyncOutcome.Applied(report, false), outcome);
        assertEquals(1, applied.get());
        assertTrue(written.isEmpty());
    }

    @Test
    void aListThatRemovesTooManyPlayersIsRefusedAndNothingIsTouched() {
        snapshot = new LinkedSnapshot(List.of(A, B, C, D), Optional.of("\"v1\""));
        answer = CompletableFuture.completedFuture(players("\"v2\"", A));

        SyncOutcome outcome = sync.sync(target).join();

        assertEquals(new SyncOutcome.Refused(3, 4, 50), outcome);
        assertEquals(0, applied.get());
        assertTrue(written.isEmpty());
    }

    @Test
    void aListThatRemovesJustUnderTheLimitIsApplied() {
        snapshot = new LinkedSnapshot(List.of(A, B, C, D), Optional.empty());
        answer = CompletableFuture.completedFuture(players("\"v2\"", A, B));

        assertInstanceOf(SyncOutcome.Applied.class, sync.sync(target).join());
    }

    @Test
    void aSecondSyncWhileOneRunsIsBusyAndTheNextOneRunsAgain() {
        answer = new CompletableFuture<>();
        CompletableFuture<SyncOutcome> first = sync.sync(target);

        assertInstanceOf(SyncOutcome.Busy.class, sync.sync(target).join());
        assertEquals(1, asked.get());

        answer.complete(players("\"v1\"", A));
        assertInstanceOf(SyncOutcome.Applied.class, first.join());
        answer = CompletableFuture.completedFuture(players("\"v1\"", A));
        assertInstanceOf(SyncOutcome.Applied.class, sync.sync(target).join());
    }

    @Test
    void aRenameThatWaitsKeepsTheOldNameAndDropsTheTagSoTheNextSyncAsksForTheWholeList() {
        LinkedPlayer renamed = new LinkedPlayer(2, "BravoNew", AuthMode.CRACKED, "2");
        snapshot = new LinkedSnapshot(List.of(A, B), Optional.of("\"v1\""));
        answer = CompletableFuture.completedFuture(players("\"v2\"", A, renamed));
        report = new LinkReport(0, 0, 0, 0, 1, List.of("BravoNew"), List.of(2), List.of());

        sync.sync(target).join();

        assertEquals(List.of(new LinkedSnapshot(List.of(A, B), Optional.empty())), written);
        assertTrue(sync.hasWaiting());
        assertEquals(List.of("BravoNew"), sync.waiting());
    }

    @Test
    void waitingClearsWhenTheNextSyncHasNobodyWaiting() {
        report = new LinkReport(0, 0, 0, 0, 1, List.of("Bravo"), List.of(), List.of());
        answer = CompletableFuture.completedFuture(players("\"v1\"", A));
        sync.sync(target).join();
        assertTrue(sync.hasWaiting());

        report = new LinkReport(0, 1, 0, 0, 0, List.of(), List.of(), List.of());
        sync.sync(target).join();

        assertFalse(sync.hasWaiting());
    }

    @Test
    void anUnexpectedErrorIsAFailureAndTheNextSyncStillRuns() {
        answer = CompletableFuture.completedFuture(players("\"v1\"", A));
        applyFailure = new IllegalStateException("boom");

        assertInstanceOf(SyncOutcome.Failed.class, sync.sync(target).join());
        assertTrue(written.isEmpty());

        applyFailure = null;
        assertInstanceOf(SyncOutcome.Applied.class, sync.sync(target).join());
    }
}
