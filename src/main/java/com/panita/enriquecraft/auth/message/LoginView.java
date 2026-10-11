package com.panita.enriquecraft.auth.message;

import com.panita.enriquecraft.auth.service.PinEntry;
import com.panita.enriquecraft.auth.service.PinFlow;
import com.panita.enriquecraft.core.message.Durations;
import com.panita.enriquecraft.core.message.Message;
import com.panita.enriquecraft.core.message.Messenger;
import com.panita.enriquecraft.core.message.channel.TitleTimes;
import net.minecraft.server.level.ServerPlayer;

import java.time.Duration;

/**
 * What players are told around logging in: the title that welcomes them, the line about their session, and the
 * title that explains the PIN screen to clients that cannot show the companion's own. Presentation only.
 */
public final class LoginView {

    private static final TitleTimes WELCOME_TIMES =
            new TitleTimes(Duration.ofMillis(500), Duration.ofMillis(2000), Duration.ofMillis(1000));

    private final Messenger messenger;

    public LoginView(Messenger messenger) {
        this.messenger = messenger;
    }

    /** A premium player came in with their Microsoft account. */
    public void autoLogin(ServerPlayer player) {
        welcome(player);
        messenger.send(player, Message.plain(AuthMessages.Login.AUTOMATIC));
    }

    /** A player with a PIN is in, and their login stays valid for the given time. */
    public void sessionActive(ServerPlayer player, Duration remaining) {
        welcome(player);
        messenger.send(player, Message.plain(AuthMessages.Login.SESSION_ACTIVE).with("time", Durations.format(remaining)));
    }

    /** A player was let in without a PIN, as staff decided; nothing is remembered. */
    public void letIn(ServerPlayer player) {
        welcome(player);
    }

    /**
     * Tells a player who sees a chest instead of the companion's screen what they are about to be asked, so the
     * chest does not come as a surprise.
     */
    public void intro(ServerPlayer player, PinFlow flow, Duration stay) {
        TitleTimes times = new TitleTimes(Duration.ofMillis(300), stay, Duration.ofMillis(500));
        if (flow.step() == PinEntry.Step.ENTER) {
            messenger.showTitle(player, Message.plain(AuthMessages.Login.INTRO_ENTER_TITLE),
                    Message.plain(AuthMessages.Login.INTRO_ENTER_SUBTITLE), times);
        } else {
            messenger.showTitle(player, Message.plain(AuthMessages.Login.INTRO_CREATE_TITLE),
                    Message.plain(AuthMessages.Login.INTRO_CREATE_SUBTITLE).with("length", flow.length()), times);
        }
    }

    private void welcome(ServerPlayer player) {
        messenger.showTitle(player, Message.plain(AuthMessages.Login.WELCOME_TITLE),
                Message.plain(AuthMessages.Login.WELCOME_SUBTITLE), WELCOME_TIMES);
    }
}
