package com.panita.enriquecraft.chat.message;

import com.panita.enriquecraft.core.message.Message;
import com.panita.enriquecraft.core.message.Messenger;
import net.minecraft.commands.CommandSourceStack;

/**
 * Reports what {@code /sudo} did. Presentation only.
 */
public final class SudoView {

    private final Messenger messenger;

    public SudoView(Messenger messenger) {
        this.messenger = messenger;
    }

    public void done(CommandSourceStack source, String player, String text) {
        messenger.send(source, Message.success(ChatMessages.Sudo.DONE).prefixed().with("player", player).with("text", text));
    }

    public void tooPowerful(CommandSourceStack source) {
        messenger.send(source, Message.error(ChatMessages.Sudo.TOO_POWERFUL).prefixed());
    }

    public void notAvailable(CommandSourceStack source) {
        messenger.send(source, Message.error(ChatMessages.Sudo.NOT_AVAILABLE).prefixed());
    }
}
