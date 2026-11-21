package com.panita.enriquecraft.moderation.service;

import com.panita.enriquecraft.core.message.Durations;

import java.time.Duration;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;

/**
 * What staff wrote after the player in {@code /ban} and {@code /mute}: optionally how long, then why. The first word is the
 * time when it is {@code permanent} or a span such as {@code 1w1d1m1s}; otherwise there is no time (the punishment is
 * permanent) and everything written is the reason. A reason that starts with a word that looks like a span has to be
 * preceded by {@code permanent}.
 *
 * @param duration how long it lasts; empty for permanent
 * @param reason   why; empty when staff gave none
 */
public record PunishmentTerms(Optional<Duration> duration, Optional<String> reason) {

    public static final PunishmentTerms NONE = new PunishmentTerms(Optional.empty(), Optional.empty());

    /** The word that stands for no end, which the command suggests. */
    public static final String PERMANENT = "permanent";
    private static final Set<String> PERMANENT_WORDS = Set.of(PERMANENT, "permanente");

    public static PunishmentTerms parse(String written) {
        String text = written.strip();
        int end = text.indexOf(' ');
        String first = end < 0 ? text : text.substring(0, end);
        String rest = end < 0 ? "" : text.substring(end);
        if (PERMANENT_WORDS.contains(first.toLowerCase(Locale.ROOT))) {
            return new PunishmentTerms(Optional.empty(), reasonOf(rest));
        }
        Optional<Duration> duration = Durations.parse(first);
        if (duration.isPresent()) {
            return new PunishmentTerms(duration, reasonOf(rest));
        }
        return new PunishmentTerms(Optional.empty(), reasonOf(text));
    }

    private static Optional<String> reasonOf(String text) {
        String reason = text.strip();
        return reason.isEmpty() ? Optional.empty() : Optional.of(reason);
    }
}
