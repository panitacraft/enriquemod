package com.panita.enriquecraft.link.service;

import com.panita.enriquecraft.link.message.LinkView;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.MinecraftServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;

/**
 * Runs a sync for whatever asked for it (the server starting, a reload, a player leaving, a command), logs how it
 * ended and tells whoever asked, if they are a person. A problem that goes on is logged once, not on every attempt.
 */
public final class LinkRunner {

    private static final Logger LOGGER = LoggerFactory.getLogger(LinkRunner.class);

    private final LinkSync sync;
    private final LinkApplier applier;
    private final LinkView view;
    private volatile Optional<SyncOutcome> lastProblem = Optional.empty();

    public LinkRunner(LinkSync sync, LinkApplier applier, LinkView view) {
        this.sync = sync;
        this.applier = applier;
        this.view = view;
    }

    /**
     * @param reportTo who to tell how it ended, besides the log
     */
    public void run(MinecraftServer server, Optional<CommandSourceStack> reportTo) {
        sync.sync(applier.on(server)).thenAccept(outcome -> server.execute(() -> {
            log(outcome);
            reportTo.ifPresent(source -> view.outcome(source, outcome));
        }));
    }

    private void log(SyncOutcome outcome) {
        switch (outcome) {
            case SyncOutcome.Applied applied -> {
                lastProblem = Optional.empty();
                logApplied(applied.report());
            }
            case SyncOutcome.Refused refused -> problem(outcome, "The list of the Panita API would remove {} of {} linked players, over the {}% allowed, so it was not applied",
                    refused.leaving(), refused.linked(), refused.maxPercent());
            case SyncOutcome.Failed failed -> problem(outcome, "Could not sync the whitelist with the Panita API: {}", failed.reason());
            case SyncOutcome.NoKey noKey -> problem(outcome, "There is no API key, so the whitelist is not synced with the Panita API. Put it in config/enriquecraft-link.key");
            case SyncOutcome.Busy busy -> LOGGER.debug("A link sync was asked for while another was running");
        }
    }

    /** Warns the first time a problem shows up, and again only when it is a different one; repeats go to the debug log. */
    private void problem(SyncOutcome outcome, String message, Object... arguments) {
        if (lastProblem.equals(Optional.of(outcome))) {
            LOGGER.debug(message, arguments);
        } else {
            LOGGER.warn(message, arguments);
            lastProblem = Optional.of(outcome);
        }
    }

    private void logApplied(LinkReport report) {
        if (report.changedAnything()) {
            LOGGER.info("Whitelist synced with the Panita API: {} added, {} switched, {} renamed, {} removed",
                    report.added(), report.replaced(), report.renamed(), report.removed());
        }
        report.problems().forEach(problem -> LOGGER.warn("Link problem with {}: {}", problem.name(), problem.reason()));
        if (!report.waiting().isEmpty()) {
            LOGGER.info("Waiting for these players to go offline: {}", String.join(", ", report.waiting()));
        }
    }
}
