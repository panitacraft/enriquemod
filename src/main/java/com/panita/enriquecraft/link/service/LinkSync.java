package com.panita.enriquecraft.link.service;

import com.panita.enriquecraft.link.client.LinkedPlayer;
import com.panita.enriquecraft.link.client.PanitaApi;
import com.panita.enriquecraft.link.data.LinkedSnapshot;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.function.IntSupplier;
import java.util.function.Supplier;

/**
 * Brings the whitelist in line with the list of the Panita API, and remembers the list it applied. Nothing here
 * runs by itself: the module asks for a sync at startup, on reload and when staff ask, and a sync never runs twice
 * at once. The work moves between threads on purpose: the answer of the API arrives on a network thread, the whitelist
 * may only be read and changed on the server thread, and the questions to Mojang that premium names need run on a
 * worker so the server never waits for them.
 */
public final class LinkSync {

    /** How the last sync ended, and when. */
    public record Status(Instant at, SyncOutcome outcome) {
    }

    private static final Logger LOGGER = LoggerFactory.getLogger(LinkSync.class);

    private final PanitaApi api;
    private final Supplier<LinkedSnapshot> read;
    private final Consumer<LinkedSnapshot> write;
    private final Supplier<Optional<String>> key;
    private final IntSupplier maxRemovalPercent;
    private final Executor worker;
    private final Supplier<Instant> clock;
    private final AtomicBoolean running = new AtomicBoolean();
    private volatile Optional<Status> last = Optional.empty();
    private volatile List<String> waiting = List.of();

    public LinkSync(PanitaApi api, Supplier<LinkedSnapshot> read, Consumer<LinkedSnapshot> write,
                    Supplier<Optional<String>> key, IntSupplier maxRemovalPercent, Executor worker, Supplier<Instant> clock) {
        this.api = api;
        this.read = read;
        this.write = write;
        this.key = key;
        this.maxRemovalPercent = maxRemovalPercent;
        this.worker = worker;
        this.clock = clock;
    }

    /** How the last sync ended; empty until one has. */
    public Optional<Status> last() {
        return last;
    }

    /** Whether some players' changes wait for them to go offline, so a sync is worth running when someone leaves. */
    public boolean hasWaiting() {
        return !waiting.isEmpty();
    }

    /** The players whose changes wait for them to go offline. */
    public List<String> waiting() {
        return waiting;
    }

    public int linkedCount() {
        return read.get().players().size();
    }

    /**
     * Runs a sync. Call it on the server thread. The future never fails; every problem is a {@link SyncOutcome}.
     */
    public CompletableFuture<SyncOutcome> sync(LinkTarget target) {
        if (key.get().isEmpty()) {
            return CompletableFuture.completedFuture(record(new SyncOutcome.NoKey()));
        }
        if (!running.compareAndSet(false, true)) {
            return CompletableFuture.completedFuture(new SyncOutcome.Busy());
        }
        CompletableFuture<SyncOutcome> result;
        try {
            LinkedSnapshot snapshot = read.get();
            result = api.players(snapshot.etag()).thenCompose(answer -> afterAnswer(target, snapshot, answer));
        } catch (RuntimeException e) {
            result = CompletableFuture.failedFuture(e);
        }
        return result.exceptionally(error -> {
            LOGGER.error("The link sync failed unexpectedly", error);
            return new SyncOutcome.Failed("unexpected error (" + error.getClass().getSimpleName() + ")");
        }).thenApply(outcome -> {
            running.set(false);
            return record(outcome);
        });
    }

    private SyncOutcome record(SyncOutcome outcome) {
        last = Optional.of(new Status(clock.get(), outcome));
        return outcome;
    }

    private CompletableFuture<SyncOutcome> afterAnswer(LinkTarget target, LinkedSnapshot snapshot, PanitaApi.Result answer) {
        return switch (answer) {
            case PanitaApi.Result.Failed failed -> CompletableFuture.completedFuture(new SyncOutcome.Failed(failed.reason()));
            case PanitaApi.Result.NotModified notModified -> plan(target, snapshot, snapshot.players(), snapshot.etag(), false);
            case PanitaApi.Result.Players players -> plan(target, snapshot, players.players(), players.etag(), true);
        };
    }

    private CompletableFuture<SyncOutcome> plan(LinkTarget target, LinkedSnapshot snapshot, List<LinkedPlayer> desired,
                                                Optional<String> etag, boolean fresh) {
        return onServerThread(target, () -> LinkPlan.compute(snapshot.players(), desired, target.whitelist()))
                .thenCompose(plan -> {
                    if (fresh && LinkPlan.removesTooMany(snapshot.players().size(), plan.leaving(), maxRemovalPercent.getAsInt())) {
                        return CompletableFuture.<SyncOutcome>completedFuture(new SyncOutcome.Refused(plan.leaving(),
                                snapshot.players().size(), maxRemovalPercent.getAsInt()));
                    }
                    return CompletableFuture.supplyAsync(() -> target.resolve(plan), worker)
                            .thenCompose(resolved -> onServerThread(target,
                                    () -> finish(target, snapshot, desired, etag, fresh, plan, resolved)));
                });
    }

    private SyncOutcome finish(LinkTarget target, LinkedSnapshot snapshot, List<LinkedPlayer> desired, Optional<String> etag,
                               boolean fresh, LinkPlan plan, List<LinkApplier.Resolved> resolved) {
        LinkReport report = target.apply(plan, resolved);
        waiting = report.waiting();
        if (fresh) {
            write.accept(remembered(snapshot, desired, etag, report));
        }
        return new SyncOutcome.Applied(report, fresh);
    }

    /**
     * What to remember of the list. A rename that waits for its player is not remembered yet: the old name stays, so the
     * next sync still knows which entry to replace, and the tag is dropped so the next sync asks for the whole list
     * instead of hearing that nothing changed.
     */
    private static LinkedSnapshot remembered(LinkedSnapshot previous, List<LinkedPlayer> desired, Optional<String> etag,
                                             LinkReport report) {
        if (report.waitingRenameIds().isEmpty()) {
            return new LinkedSnapshot(desired, etag);
        }
        Map<Integer, LinkedPlayer> before = new HashMap<>();
        previous.players().forEach(player -> before.put(player.userId(), player));
        List<LinkedPlayer> players = desired.stream()
                .map(player -> report.waitingRenameIds().contains(player.userId()) ? before.get(player.userId()) : player)
                .toList();
        return new LinkedSnapshot(players, Optional.empty());
    }

    private static <T> CompletableFuture<T> onServerThread(LinkTarget target, Supplier<T> task) {
        CompletableFuture<T> future = new CompletableFuture<>();
        target.onServerThread(() -> {
            try {
                future.complete(task.get());
            } catch (RuntimeException e) {
                future.completeExceptionally(e);
            }
        });
        return future;
    }
}
