package com.panita.enriquecraft.moderation.message;

import com.panita.enriquecraft.core.message.Message;
import com.panita.enriquecraft.core.message.Messenger;
import net.minecraft.commands.CommandSourceStack;

/** What the voice chat commands report to staff. Presentation only. */
public final class VoiceChatView {

    private final Messenger messenger;

    public VoiceChatView(Messenger messenger) {
        this.messenger = messenger;
    }

    public void mutedAll(CommandSourceStack source, int staff) {
        tell(source, Message.success(ModerationMessages.VoiceChat.MUTED_ALL).prefixed().with("staff", staff));
    }

    public void unmutedAll(CommandSourceStack source) {
        tell(source, Message.success(ModerationMessages.VoiceChat.UNMUTED_ALL).prefixed());
    }

    public void muted(CommandSourceStack source, String player) {
        tell(source, Message.success(ModerationMessages.VoiceChat.MUTED_PLAYER).prefixed().with("player", player));
    }

    public void unmuted(CommandSourceStack source, String player) {
        tell(source, Message.success(ModerationMessages.VoiceChat.UNMUTED_PLAYER).prefixed().with("player", player));
    }

    public void invalidPlayer(CommandSourceStack source) {
        messenger.send(source, Message.error(ModerationMessages.VoiceChat.INVALID_PLAYER).prefixed());
    }

    public void luckPermsMissing(CommandSourceStack source) {
        messenger.send(source, Message.error(ModerationMessages.VoiceChat.LUCKPERMS_MISSING).prefixed());
    }

    /** Like the other moderation results, it is shown to the operators who can see command feedback. */
    private void tell(CommandSourceStack source, Message message) {
        source.sendSuccess(() -> messenger.format(message), true);
    }
}
