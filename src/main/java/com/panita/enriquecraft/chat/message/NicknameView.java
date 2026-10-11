package com.panita.enriquecraft.chat.message;

import com.panita.enriquecraft.chat.config.ChatConfig;
import com.panita.enriquecraft.chat.service.NicknameService;
import com.panita.enriquecraft.core.message.Durations;
import com.panita.enriquecraft.core.message.Message;
import com.panita.enriquecraft.core.message.Messenger;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;

/**
 * Reports what happened to a nickname. Presentation only: the rules are the service's.
 */
public final class NicknameView {

    private final Messenger messenger;
    private final ChatConfig config;

    public NicknameView(Messenger messenger, ChatConfig config) {
        this.messenger = messenger;
        this.config = config;
    }

    public void changed(CommandSourceStack source, Component nickname) {
        messenger.send(source, Message.success(ChatMessages.Nickname.CHANGED).prefixed().with("nickname", nickname));
    }

    public void changedOther(CommandSourceStack source, String player, Component nickname) {
        messenger.send(source, Message.success(ChatMessages.Nickname.CHANGED_OTHER).prefixed()
                .with("player", player).with("nickname", nickname));
    }

    public void cleared(CommandSourceStack source) {
        messenger.send(source, Message.success(ChatMessages.Nickname.CLEARED).prefixed());
    }

    public void clearedOther(CommandSourceStack source, String player) {
        messenger.send(source, Message.success(ChatMessages.Nickname.CLEARED_OTHER).prefixed().with("player", player));
    }

    public void notSet(CommandSourceStack source) {
        messenger.send(source, Message.warning(ChatMessages.Nickname.NOT_SET).prefixed());
    }

    public void notSetOther(CommandSourceStack source, String player) {
        messenger.send(source, Message.warning(ChatMessages.Nickname.NOT_SET_OTHER).prefixed().with("player", player));
    }

    /** Says why a nickname was not accepted. */
    public void rejected(CommandSourceStack source, NicknameService.Outcome outcome) {
        Message message = switch (outcome.status()) {
            case TOO_SHORT -> Message.error(ChatMessages.Nickname.TOO_SHORT).with("min", config.nicknameMinLength.get());
            case TOO_LONG -> Message.error(ChatMessages.Nickname.TOO_LONG).with("max", config.nicknameMaxLength.get());
            case BAD_CHARACTERS -> Message.error(ChatMessages.Nickname.BAD_CHARACTERS);
            case TAKEN -> Message.error(ChatMessages.Nickname.TAKEN);
            case COOLDOWN -> Message.error(ChatMessages.Nickname.COOLDOWN).with("time", Durations.format(outcome.remaining()));
            case CHANGED, CLEARED, NOT_SET -> throw new IllegalArgumentException(outcome.status() + " is not a rejection");
        };
        messenger.send(source, message.prefixed());
    }

    public void realName(CommandSourceStack source, Component nickname, String name) {
        messenger.send(source, Message.info(ChatMessages.Nickname.REAL_NAME).prefixed()
                .with("nickname", nickname).with("name", name));
    }

    public void nobodyCalled(CommandSourceStack source, String nickname) {
        messenger.send(source, Message.warning(ChatMessages.Nickname.REAL_NAME_NONE).prefixed().with("nickname", nickname));
    }
}
