package com.panita.enriquecraft.auth.message;

import com.panita.enriquecraft.core.message.Message;
import com.panita.enriquecraft.core.message.Messenger;
import net.minecraft.commands.CommandSourceStack;

/**
 * What staff and players are told by the auth commands. Presentation only.
 */
public final class AdminView {

    private final Messenger messenger;

    public AdminView(Messenger messenger) {
        this.messenger = messenger;
    }

    public void resetDone(CommandSourceStack source, String name) {
        messenger.send(source, Message.success(AuthMessages.Commands.RESET_DONE).with("name", name).prefixed());
    }

    public void noAccount(CommandSourceStack source, String name) {
        messenger.send(source, Message.error(AuthMessages.Commands.NO_ACCOUNT).with("name", name).prefixed());
    }

    public void forced(CommandSourceStack source, String name) {
        messenger.send(source, Message.success(AuthMessages.Commands.FORCED).with("name", name).prefixed());
    }

    public void notWaiting(CommandSourceStack source, String name) {
        messenger.send(source, Message.error(AuthMessages.Commands.NOT_WAITING).with("name", name).prefixed());
    }

    public void premiumHasNoPin(CommandSourceStack source) {
        messenger.send(source, Message.info(AuthMessages.Commands.PREMIUM_HAS_NO_PIN).prefixed());
    }

    public void noPinYet(CommandSourceStack source) {
        messenger.send(source, Message.error(AuthMessages.Commands.NO_PIN_YET).prefixed());
    }

    public void premiumHasNoSession(CommandSourceStack source) {
        messenger.send(source, Message.info(AuthMessages.Commands.LOGGED_OUT_PREMIUM).prefixed());
    }
}
