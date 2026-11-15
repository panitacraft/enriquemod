package com.panita.enriquecraft.core.dialog;

import java.security.SecureRandom;
import java.util.Base64;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.LongSupplier;

/**
 * The dialogs players have open, one per player, each tied to a token. The game says nothing when a dialog is
 * closed with Escape and a reply carries no player, so a reply is trusted only through its token: it must belong to
 * the player's current dialog, which ends when another dialog replaces it, when the player leaves, when the dialog is
 * answered or closed by the server, and after a time limit.
 */
final class DialogTokens {

    /** An open dialog: what it asked, what to do with the answer, and until when its token works. */
    record Open(String token, DialogSpec spec, DialogHandler handler, long expiresAtNanos) {
    }

    private static final int TOKEN_BYTES = 12;

    private final Map<UUID, Open> open = new ConcurrentHashMap<>();
    private final SecureRandom random = new SecureRandom();
    private final LongSupplier clock;
    private final long lifetimeNanos;

    DialogTokens(LongSupplier clock, long lifetimeNanos) {
        this.clock = clock;
        this.lifetimeNanos = lifetimeNanos;
    }

    /** Makes the dialog the player's current one, which ends whatever dialog they had. */
    Open issue(UUID player, DialogSpec spec, DialogHandler handler) {
        byte[] bytes = new byte[TOKEN_BYTES];
        random.nextBytes(bytes);
        Open dialog = new Open(Base64.getUrlEncoder().withoutPadding().encodeToString(bytes), spec, handler,
                clock.getAsLong() + lifetimeNanos);
        open.put(player, dialog);
        dropExpired();
        return dialog;
    }

    /** The player's current dialog, if it has not expired. */
    Optional<Open> current(UUID player) {
        Open dialog = open.get(player);
        if (dialog == null) {
            return Optional.empty();
        }
        if (isExpired(dialog)) {
            open.remove(player, dialog);
            return Optional.empty();
        }
        return Optional.of(dialog);
    }

    /** Ends the player's dialog; returns whether they had one. */
    boolean drop(UUID player) {
        return open.remove(player) != null;
    }

    /** Ends the dialog only if it is still this one, so a late answer cannot end the dialog that replaced it. */
    boolean drop(UUID player, Open dialog) {
        return open.remove(player, dialog);
    }

    private boolean isExpired(Open dialog) {
        // Compared as a difference because the clock is a nanoTime, which may be negative.
        return clock.getAsLong() - dialog.expiresAtNanos() >= 0;
    }

    /** Players who close a dialog with Escape and never open another would otherwise keep their entry forever. */
    private void dropExpired() {
        open.values().removeIf(this::isExpired);
    }
}
