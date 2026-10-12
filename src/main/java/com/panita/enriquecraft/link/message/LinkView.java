package com.panita.enriquecraft.link.message;

import com.panita.enriquecraft.core.message.Durations;
import com.panita.enriquecraft.core.message.Message;
import com.panita.enriquecraft.core.message.MessageFormatter;
import com.panita.enriquecraft.core.message.Messenger;
import com.panita.enriquecraft.link.service.LinkReport;
import com.panita.enriquecraft.link.service.LinkSync;
import com.panita.enriquecraft.link.service.SyncOutcome;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * What the link module tells staff and players. Presentation only.
 */
public final class LinkView {

    private final Messenger messenger;
    private final MessageFormatter formatter;

    public LinkView(Messenger messenger, MessageFormatter formatter) {
        this.messenger = messenger;
        this.formatter = formatter;
    }

    /** The disconnect screen of a player whose account was deactivated while they were online. */
    public Component deactivated() {
        return formatter.format(Message.plain(LinkMessages.Kick.DEACTIVATED));
    }

    public void started(CommandSourceStack source) {
        messenger.send(source, Message.info(LinkMessages.Sync.STARTED).prefixed());
    }

    /** Tells whoever asked for a sync how it ended. */
    public void outcome(CommandSourceStack source, SyncOutcome outcome) {
        switch (outcome) {
            case SyncOutcome.Applied applied -> applied(source, applied.report());
            case SyncOutcome.Refused refused -> messenger.send(source, Message.error(LinkMessages.Sync.REFUSED)
                    .with("leaving", refused.leaving()).with("linked", refused.linked()).with("percent", refused.maxPercent()).prefixed());
            case SyncOutcome.Failed failed -> messenger.send(source, Message.error(LinkMessages.Sync.FAILED).prefixed());
            case SyncOutcome.NoKey noKey -> messenger.send(source, Message.error(LinkMessages.Sync.NO_KEY).prefixed());
            case SyncOutcome.Busy busy -> messenger.send(source, Message.warning(LinkMessages.Sync.BUSY).prefixed());
        }
    }

    private void applied(CommandSourceStack source, LinkReport report) {
        if (report.changedAnything()) {
            messenger.send(source, Message.success(LinkMessages.Sync.DONE_CHANGED).with("added", report.added())
                    .with("replaced", report.replaced()).with("renamed", report.renamed()).with("removed", report.removed()).prefixed());
        } else {
            messenger.send(source, Message.info(LinkMessages.Sync.DONE_UNCHANGED).with("unchanged", report.unchanged()).prefixed());
        }
        if (!report.waiting().isEmpty()) {
            messenger.send(source, Message.warning(LinkMessages.Sync.WAITING).with("names", String.join(", ", report.waiting())).prefixed());
        }
        for (LinkReport.Problem problem : report.problems()) {
            messenger.send(source, Message.warning(problemTemplate(problem.reason())).with("name", problem.name()).prefixed());
        }
    }

    private static String problemTemplate(LinkReport.Reason reason) {
        return switch (reason) {
            case NOT_PREMIUM -> LinkMessages.Sync.PROBLEM_NOT_PREMIUM;
            case LOOKUP_FAILED -> LinkMessages.Sync.PROBLEM_LOOKUP_FAILED;
            case INVALID_NAME -> LinkMessages.Sync.PROBLEM_INVALID_NAME;
            case DUPLICATE_NAME -> LinkMessages.Sync.PROBLEM_DUPLICATE_NAME;
        };
    }

    /**
     * The state of the module.
     *
     * @param keyFound whether the service key was found
     * @param url      the address of the API
     * @param linked   how many players the last applied list has
     * @param last     how the last sync ended, if one has
     * @param waiting  the players whose changes wait for them to go offline
     * @param now      the current moment, to say how long ago the last sync was
     */
    public void status(CommandSourceStack source, boolean keyFound, String url, int linked, Optional<LinkSync.Status> last,
                       List<String> waiting, Instant now) {
        messenger.send(source, Message.plain(keyFound ? LinkMessages.Status.KEY_FOUND : LinkMessages.Status.KEY_MISSING).prefixed());
        messenger.send(source, Message.plain(LinkMessages.Status.URL).with("url", url).prefixed());
        messenger.send(source, Message.plain(LinkMessages.Status.LINKED).with("count", linked).prefixed());
        if (last.isEmpty()) {
            messenger.send(source, Message.plain(LinkMessages.Status.LAST_NONE).prefixed());
        } else {
            String template = switch (last.get().outcome()) {
                case SyncOutcome.Applied applied -> LinkMessages.Status.LAST_APPLIED;
                case SyncOutcome.Refused refused -> LinkMessages.Status.LAST_REFUSED;
                case SyncOutcome.Failed failed -> LinkMessages.Status.LAST_FAILED;
                case SyncOutcome.NoKey noKey -> LinkMessages.Status.LAST_NO_KEY;
                case SyncOutcome.Busy busy -> LinkMessages.Status.LAST_NONE;
            };
            messenger.send(source, Message.plain(template).with("ago", Durations.format(Duration.between(last.get().at(), now))).prefixed());
        }
        if (!waiting.isEmpty()) {
            messenger.send(source, Message.plain(LinkMessages.Status.WAITING).with("names", String.join(", ", waiting)).prefixed());
        }
    }
}
