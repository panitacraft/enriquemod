package com.panita.enriquecraft.core.dialog;

import net.minecraft.core.Holder;
import net.minecraft.nbt.Tag;
import net.minecraft.network.protocol.common.ClientboundClearDialogPacket;
import net.minecraft.network.protocol.common.ClientboundShowDialogPacket;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.LongSupplier;
import java.util.function.Predicate;

/**
 * Forms for players without the client companion, built on the game's own dialogs: the server shows a dialog, the
 * player fills it in and presses a button, and the handler gets the values. Everything the client sends back is
 * checked (see {@link DialogPayloadReader}) and tied to the one dialog the player has open (see
 * {@link DialogTokens}); the game itself gives a reply no player, so the mixin hands it over here.
 */
public final class DialogService {

    private static final Logger LOGGER = LoggerFactory.getLogger(DialogService.class);

    /** How long a dialog can stay open before its buttons stop working; Escape tells the server nothing. */
    private static final Duration LIFETIME = Duration.ofMinutes(15);
    /** A person cannot press buttons faster than this; anything quicker is dropped. */
    private static final long MIN_REPLY_INTERVAL_NANOS = 100_000_000L;

    private final DialogTokens tokens;
    private final LongSupplier clock;
    private final List<Predicate<ServerPlayer>> refusals = new CopyOnWriteArrayList<>();
    private final Map<UUID, Long> lastReply = new ConcurrentHashMap<>();

    public DialogService() {
        this(System::nanoTime, new DialogTokens(System::nanoTime, LIFETIME.toNanos()));
    }

    DialogService(LongSupplier clock, DialogTokens tokens) {
        this.clock = clock;
        this.tokens = tokens;
    }

    /**
     * Makes the service refuse to show dialogs to, and read replies from, players the predicate matches. The auth
     * module uses it for players who have not entered their PIN yet.
     */
    public void refuseWhen(Predicate<ServerPlayer> refusal) {
        refusals.add(refusal);
    }

    /**
     * Shows a dialog to a player, replacing the one they had. Must be called on the server thread.
     *
     * @return whether the dialog was shown; false for a player the service refuses
     */
    public boolean show(ServerPlayer player, DialogSpec spec, DialogHandler handler) {
        if (isRefused(player)) {
            return false;
        }
        DialogTokens.Open open = tokens.issue(player.getUUID(), spec, handler);
        player.connection.send(new ClientboundShowDialogPacket(Holder.direct(DialogFactory.build(spec, open.token()))));
        return true;
    }

    /** Closes the player's dialog on their screen, if the server still knows of one. */
    public void close(ServerPlayer player) {
        if (tokens.drop(player.getUUID())) {
            player.connection.send(ClientboundClearDialogPacket.INSTANCE);
        }
    }

    /** The player left: their dialog and pace are forgotten. */
    public void forget(UUID player) {
        tokens.drop(player);
        lastReply.remove(player);
    }

    /**
     * Handles a custom click action a client reported.
     *
     * @return whether the action was a dialog's, which the game must not handle any further
     */
    public boolean handleClick(ServerPlayer player, Identifier id, Optional<Tag> payload) {
        if (!DialogFactory.CLICK_ID.equals(id)) {
            return false;
        }
        if (isRefused(player)) {
            LOGGER.warn("Ignoring a dialog reply of {}, who may not use dialogs now", player.getGameProfile().name());
            return true;
        }
        take(player.getUUID(), payload, player.getGameProfile().name()).ifPresent(reply -> {
            try {
                reply.handler().handle(player, reply.button(), reply.values());
            } catch (RuntimeException e) {
                LOGGER.error("A dialog action failed for {}", player.getGameProfile().name(), e);
            }
        });
        return true;
    }

    /** A reply that passed every check and ended its dialog. */
    record Reply(DialogHandler handler, String button, DialogValues values) {
    }

    /** Applies the token, pace and content rules to a reply; the dialog ends when its reply is taken or is hostile. */
    Optional<Reply> take(UUID player, Optional<Tag> payload, String name) {
        Optional<DialogTokens.Open> current = tokens.current(player);
        if (current.isEmpty()) {
            LOGGER.debug("{} answered a dialog that is gone", name);
            return Optional.empty();
        }
        DialogTokens.Open open = current.get();
        long now = clock.getAsLong();
        Long last = lastReply.put(player, now);
        if (last != null && now - last < MIN_REPLY_INTERVAL_NANOS) {
            LOGGER.warn("{} answered dialogs too fast", name);
            return Optional.empty();
        }
        return switch (DialogPayloadReader.read(open.spec(), open.token(), payload)) {
            case DialogPayloadReader.Accepted accepted -> {
                // One answer per dialog, so a reply cannot be repeated; the handler may show the next dialog.
                yield tokens.drop(player, open)
                        ? Optional.of(new Reply(open.handler(), accepted.button(), accepted.values()))
                        : Optional.empty();
            }
            case DialogPayloadReader.Stale ignored -> {
                LOGGER.debug("{} answered a dialog that was replaced", name);
                yield Optional.empty();
            }
            case DialogPayloadReader.Rejected rejected -> {
                LOGGER.warn("{} sent an invalid dialog reply: {}", name, rejected.reason());
                tokens.drop(player, open);
                yield Optional.empty();
            }
        };
    }

    private boolean isRefused(ServerPlayer player) {
        return refusals.stream().anyMatch(refusal -> refusal.test(player));
    }
}
