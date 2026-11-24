package com.panita.enriquecraft.chat.message;

import com.panita.enriquecraft.chat.channel.ChannelRegistry;
import com.panita.enriquecraft.chat.service.AfkService;
import com.panita.enriquecraft.chat.service.NicknameService;
import com.panita.enriquecraft.core.message.Message;
import com.panita.enriquecraft.core.message.Messenger;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.server.level.ServerPlayer;

import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.function.Predicate;

/**
 * The name of a player as chat lines show it, with a tooltip that tells who is behind it and a click that opens their profile on
 * the website.
 */
public final class SenderNameView {

    private static final String PROFILE_URL = "https://www.panitacraft.com/profile/";

    private final Messenger messenger;
    private final NicknameService nicknames;
    private final AfkService afk;
    private final Predicate<ServerPlayer> hasRank;
    private final ChannelRegistry channels;

    /**
     * @param hasRank whether a player has a rank; an AFK player without one shows their name in the AFK color, since there is no rank
     *                to carry it
     */
    public SenderNameView(Messenger messenger, NicknameService nicknames, AfkService afk, Predicate<ServerPlayer> hasRank,
                          ChannelRegistry channels) {
        this.messenger = messenger;
        this.nicknames = nicknames;
        this.afk = afk;
        this.hasRank = hasRank;
        this.channels = channels;
    }

    /** The nickname of the player, or their name when they have none. */
    public Component of(ServerPlayer player) {
        return decorated(player, nicknames.nicknameOf(player.getUUID()).orElseGet(() -> Component.literal(realName(player))));
    }

    /** The real name of the player, for someone who addressed them by it. */
    public Component ofRealName(ServerPlayer player) {
        return decorated(player, Component.literal(realName(player)));
    }

    /**
     * The player as the global channel shows its sender (prank level, rank, name, suffix, emblems), with the given name in it. It is
     * read from the channel at each use, so a change of that format is followed.
     */
    public Component tagged(ServerPlayer player, Component name) {
        return messenger.format(Message.plain(SenderTag.of(channels.global().format())).with("sender", name), player);
    }

    /** The player as the global channel shows its sender. */
    public Component tagged(ServerPlayer player) {
        return tagged(player, of(player));
    }

    private Component decorated(ServerPlayer player, Component shown) {
        String name = realName(player);
        Component tooltip = messenger.format(Message.plain(ChatMessages.Format.NAME_HOVER)
                .with("nickname", nicknames.nicknameOf(player.getUUID()).orElse(shown))
                .with("name", name)
                .with("level", player.experienceLevel), player);
        return afk.shadeName(player, shown, hasRank).copy().withStyle(style -> style
                .withHoverEvent(new HoverEvent.ShowText(tooltip))
                .withClickEvent(new ClickEvent.OpenUrl(profileUri(name))));
    }

    /** The web profile of the real player, never of a nickname; the name is encoded since offline names are not always plain. */
    static URI profileUri(String name) {
        // URLEncoder writes a space as "+", which in a path is a literal plus.
        return URI.create(PROFILE_URL + URLEncoder.encode(name, StandardCharsets.UTF_8).replace("+", "%20"));
    }

    private static String realName(ServerPlayer player) {
        return player.getGameProfile().name();
    }
}
