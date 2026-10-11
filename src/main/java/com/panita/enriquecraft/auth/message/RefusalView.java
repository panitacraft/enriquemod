package com.panita.enriquecraft.auth.message;

import com.panita.enriquecraft.auth.service.LoginGate;
import com.panita.enriquecraft.core.message.Durations;
import com.panita.enriquecraft.core.message.Message;
import com.panita.enriquecraft.core.message.MessageFormatter;
import net.minecraft.network.chat.Component;

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
                    Message.plain(AuthMessages.Refusal.BLOCKED).with("time", Durations.format(blocked.remaining()))));
            case LoginGate.Verdict.AlreadyOnline alreadyOnline ->
                    Optional.of(formatter.format(Message.plain(AuthMessages.Refusal.ALREADY_ONLINE)));
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
}
