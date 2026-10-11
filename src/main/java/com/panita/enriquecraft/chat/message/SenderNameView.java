package com.panita.enriquecraft.chat.message;

import com.panita.enriquecraft.chat.service.NicknameService;
import com.panita.enriquecraft.core.message.Message;
import com.panita.enriquecraft.core.message.Messenger;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.server.level.ServerPlayer;

/**
 * The name of a player as chat lines show it: their nickname, with a tooltip that tells who is behind it, and a click that
 * starts a private message to them.
 */
public final class SenderNameView {

    private final Messenger messenger;
    private final NicknameService nicknames;

    public SenderNameView(Messenger messenger, NicknameService nicknames) {
        this.messenger = messenger;
        this.nicknames = nicknames;
    }

    public Component of(ServerPlayer player) {
        String name = player.getGameProfile().name();
        Component shown = nicknames.nicknameOf(player.getUUID()).orElseGet(() -> Component.literal(name));
        Component tooltip = messenger.format(Message.plain(ChatMessages.Format.NAME_HOVER)
                .with("nickname", shown)
                .with("name", name), player);
        return shown.copy().withStyle(style -> style
                .withHoverEvent(new HoverEvent.ShowText(tooltip))
                .withClickEvent(new ClickEvent.SuggestCommand("/msg " + name + " ")));
    }
}
