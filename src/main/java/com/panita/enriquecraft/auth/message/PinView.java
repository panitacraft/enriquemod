package com.panita.enriquecraft.auth.message;

import com.panita.enriquecraft.auth.config.AuthConfig;
import com.panita.enriquecraft.auth.service.PinEntry;
import com.panita.enriquecraft.auth.service.PinFlow;
import com.panita.enriquecraft.core.gui.MenuFactory;
import com.panita.enriquecraft.core.message.Message;
import net.minecraft.network.chat.Component;

/**
 * The words of the PIN screen for each moment of a {@link PinFlow}, the same for every kind of screen.
 * Presentation only.
 */
public final class PinView {

    /** What a screen says: a headline and a smaller line under it. */
    public record Texts(Component title, Component hint) {
    }

    private final MenuFactory factory;
    private final AuthConfig config;

    public PinView(MenuFactory factory, AuthConfig config) {
        this.factory = factory;
        this.config = config;
    }

    public Texts of(PinFlow flow) {
        return switch (flow.phase()) {
            case SUCCESS -> texts(AuthMessages.Pin.SUCCESS_TITLE,
                    Message.plain(flow.wasCreated() ? AuthMessages.Pin.CREATED_HINT : AuthMessages.Pin.SUCCESS_HINT)
                            .with("hours", config.sessionHours.get()));
            case WRONG -> flow.failure() == PinFlow.Failure.MISMATCH
                    ? texts(AuthMessages.Pin.MISMATCH_TITLE, Message.plain(AuthMessages.Pin.MISMATCH_HINT))
                    : texts(AuthMessages.Pin.WRONG_TITLE, flow.attemptsLeft() > 0
                            ? Message.plain(AuthMessages.Pin.WRONG_HINT).with("attempts", flow.attemptsLeft())
                            : Message.plain(AuthMessages.Pin.LOCKED_HINT));
            case INPUT, ADVANCE -> ofStep(flow);
        };
    }

    private Texts ofStep(PinFlow flow) {
        PinEntry.Step step = flow.step();
        return switch (step) {
            case ENTER -> texts(AuthMessages.Pin.ENTER_TITLE,
                    Message.plain(AuthMessages.Pin.ENTER_HINT).with("name", flow.playerName()));
            case CREATE -> texts(AuthMessages.Pin.CREATE_TITLE,
                    Message.plain(AuthMessages.Pin.CREATE_HINT).with("length", flow.length()));
            case CONFIRM -> texts(AuthMessages.Pin.CONFIRM_TITLE, Message.plain(AuthMessages.Pin.CONFIRM_HINT));
        };
    }

    private Texts texts(String title, Message hint) {
        return new Texts(factory.text(title), factory.text(hint));
    }
}
