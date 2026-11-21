package com.panita.enriquecraft.chat.message;

import com.panita.enriquecraft.chat.service.AfkService;
import com.panita.enriquecraft.chat.service.NicknameService;
import com.panita.enriquecraft.core.message.Message;
import com.panita.enriquecraft.core.message.Messenger;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.server.level.ServerPlayer;

import java.util.function.Predicate;

/**
 * The name of a player as chat lines show it, with a tooltip that tells who is behind it and a click that starts a private
 * message to them.
 */
public final class SenderNameView {

    private final Messenger messenger;
    private final NicknameService nicknames;
    private final AfkService afk;
    private final Predicate<ServerPlayer> hasRank;

    /**
     * @param hasRank whether a player has a rank; an AFK player without one shows their name in the AFK color, since there is no rank
     *                to carry it
     */
    public SenderNameView(Messenger messenger, NicknameService nicknames, AfkService afk, Predicate<ServerPlayer> hasRank) {
        this.messenger = messenger;
        this.nicknames = nicknames;
        this.afk = afk;
        this.hasRank = hasRank;
    }

    /** The nickname of the player, or their name when they have none. */
    public Component of(ServerPlayer player) {
        return decorated(player, nicknames.nicknameOf(player.getUUID()).orElseGet(() -> Component.literal(realName(player))));
    }

    /** The real name of the player, for someone who addressed them by it. */
    public Component ofRealName(ServerPlayer player) {
        return decorated(player, Component.literal(realName(player)));
    }


    private Component decorated(ServerPlayer player, Component shown) {
        String name = realName(player);
        Component tooltip = messenger.format(Message.plain(ChatMessages.Format.NAME_HOVER)
                .with("nickname", nicknames.nicknameOf(player.getUUID()).orElse(shown))
                .with("name", name), player);
        return afk.shadeName(player, shown, hasRank).copy().withStyle(style -> style
                .withHoverEvent(new HoverEvent.ShowText(tooltip))
                .withClickEvent(new ClickEvent.SuggestCommand("/msg " + name + " ")));
    }

    private static String realName(ServerPlayer player) {
        return player.getGameProfile().name();
    }
}
