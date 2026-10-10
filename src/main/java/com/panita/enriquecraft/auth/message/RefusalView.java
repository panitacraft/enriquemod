package com.panita.enriquecraft.auth.message;

import com.panita.enriquecraft.auth.service.LoginGate;
import com.panita.enriquecraft.core.message.Message;
import com.panita.enriquecraft.core.message.MessageFormatter;
import net.minecraft.network.chat.Component;

import java.time.Duration;
import java.util.Optional;

/**
 * The texts of the disconnect screens the auth module causes. Presentation only.
 */
public final class RefusalView {

    private final MessageFormatter formatter;

    public RefusalView(MessageFormatter formatter) {
        this.formatter = formatter;
    }

    /** Why a player may not continue, or empty when the verdict lets them in. */
    public Optional<Component> of(LoginGate.Verdict verdict) {
        return switch (verdict) {
            case LoginGate.Verdict.Proceed proceed -> Optional.empty();
            case LoginGate.Verdict.Blocked blocked -> Optional.of(formatter.format(
                    Message.plain(AuthMessages.Refusal.BLOCKED).with("minutes", minutes(blocked.remaining()))));
            case LoginGate.Verdict.NotWhitelisted notWhitelisted ->
                    Optional.of(formatter.format(Message.plain(AuthMessages.Refusal.NOT_WHITELISTED)));
            case LoginGate.Verdict.Unavailable unavailable ->
                    Optional.of(formatter.format(Message.plain(AuthMessages.Refusal.UNAVAILABLE)));
        };
    }

    public Component tooManyAttempts() {
        return formatter.format(Message.plain(AuthMessages.Refusal.TOO_MANY_ATTEMPTS));
    }

    public Component timeout() {
        return formatter.format(Message.plain(AuthMessages.Refusal.TIMEOUT));
    }

    public Component loggedOut() {
        return formatter.format(Message.plain(AuthMessages.Refusal.LOGGED_OUT));
    }

    private static int minutes(Duration remaining) {
        return (int) Math.max(1, (remaining.toSeconds() + 59) / 60);
    }
}
