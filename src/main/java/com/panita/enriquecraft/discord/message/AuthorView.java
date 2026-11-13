package com.panita.enriquecraft.discord.message;

import com.panita.enriquecraft.Enriquecraft;
import com.panita.enriquecraft.auth.data.AuthMode;
import com.panita.enriquecraft.chat.message.RankTooltip;
import com.panita.enriquecraft.chat.service.ChatHeads;
import com.panita.enriquecraft.core.message.Message;
import com.panita.enriquecraft.core.message.MessageFormatter;
import com.panita.enriquecraft.discord.config.DiscordConfig;
import com.panita.enriquecraft.discord.protocol.InFrame;
import com.panita.enriquecraft.link.client.LinkedPlayer;
import com.panita.enriquecraft.link.service.LinkedPlayers;
import com.panita.enriquecraft.profile.service.EmblemPlaceholders;
import com.panita.enriquecraft.profile.service.PlayerEmblems;
import com.panita.enriquecraft.profile.service.ProfileService;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FontDescription;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;

import java.util.Optional;

/**
 * How the author of a Discord message is shown in the game, in the order of every other line: the head, the prank level, the
 * Discord rank, the name, the emblems the author chose and, for a reply, whose message it answers. The author is a linked member,
 * so their prank level and emblems are those of their Panita account whether or not they are online; one the server knows nothing
 * of has the first prank level and empty places.
 * <p>
 * The head is the skin of the author for every client, and a client companion draws the icon of the bot in its place: one component
 * carries both, as the images of {@code InlineArts} do, so there is no line per reader.
 */
public final class AuthorView {

    /** The translation a client without the companion does not have, so it shows the skin head, and the companion's language turns into the icon. */
    static final String HEAD_KEY = "enriquecraft.discord_head.v1";
    /** The font of the companion that has the icon of the bot as its one glyph. */
    static final Identifier HEAD_FONT = Identifier.fromNamespaceAndPath(Enriquecraft.MOD_ID, "discord_head");
    private static final String HEAD_GLYPH = "";
    /** A glyph with an advance of -1: a bitmap font leaves a pixel after each glyph that the head of a player does not, which this takes back. */
    private static final String HEAD_PULL_BACK = "";

    private final MessageFormatter formatter;
    private final DiscordConfig config;
    private final ReplyView replies;
    private final ChatHeads heads;
    private final ProfileService profiles;
    private final EmblemPlaceholders emblems;
    private final LinkedPlayers linked;
    private final RankTooltip rankTooltip;

    public AuthorView(MessageFormatter formatter, DiscordConfig config, ReplyView replies, ChatHeads heads, ProfileService profiles,
                      EmblemPlaceholders emblems, LinkedPlayers linked, RankTooltip rankTooltip) {
        this.formatter = formatter;
        this.config = config;
        this.replies = replies;
        this.heads = heads;
        this.profiles = profiles;
        this.emblems = emblems;
        this.linked = linked;
        this.rankTooltip = rankTooltip;
    }

    /**
     * The author as the start of a line. Pointing at the head says whether the account is premium, at the rank says what it is, and
     * at anything else where the message came from. Called on the server thread.
     */
    public Component sender(MinecraftServer server, String ign, Optional<InFrame.Reply> reply) {
        PlayerEmblems author = profiles.emblemsOf(ign);
        Optional<AuthMode> mode = linked.byName(ign).flatMap(LinkedPlayer::mode);
        // The icon of the bot is drawn by the companion in place of the skin, so it needs the tooltip of the head too
        Style headStyle = heads.tooltipStyle(server, ign, mode);
        Optional<Component> head = heads.of(server, ign, mode).map(skin -> withBotHead(skin).withStyle(headStyle));
        Component prefix = formatter.format(Message.plain(config.deliveryPrefix.get()));
        Component rank = rankTooltip.styleOf(prefix, Optional.empty(), formatter.format(Message.plain(DiscordMessages.Delivery.RANK_TITLE)),
                formatter.format(Message.plain(DiscordMessages.Delivery.RANK_DESCRIPTION))).<Component>map(style -> prefix.copy().withStyle(style)).orElse(prefix);
        Component line = compose(head, emblems.system(author.prank()), rank, ign, emblems.lore(author), reply.map(replies::line));
        Component hover = formatter.format(Message.plain(DiscordMessages.Delivery.HOVER));
        // What a part already shows stays as it is; the rest tells where the message came from
        return Component.empty().withStyle(style -> style.withHoverEvent(new HoverEvent.ShowText(hover))).append(line);
    }

    /** The pieces in their places, each bringing its own space as the emblem placeholders do. */
    static Component compose(Optional<Component> head, Component prank, Component rank, String ign, Component emblems, Optional<Component> reply) {
        MutableComponent line = Component.empty();
        head.ifPresent(line::append);
        line.append(prank).append(rank).append(Component.literal(" ")).append(Component.literal(ign).withStyle(ChatFormatting.WHITE)).append(emblems);
        reply.ifPresent(answered -> line.append(Component.literal(" ")).append(answered));
        return line;
    }

    /** The skin head for a client without the companion, which has no such translation, and the icon of the bot for the companion. */
    static MutableComponent withBotHead(Component skinHead) {
        MutableComponent glyph = inHeadFont(HEAD_GLYPH);
        // The space is outside the glyph, which would give it the font of the icon
        Component icon = Component.empty().append(glyph).append(inHeadFont(HEAD_PULL_BACK)).append(Component.literal(" "));
        return Component.translatableWithFallback(HEAD_KEY, "%1$s", skinHead, icon);
    }

    private static MutableComponent inHeadFont(String text) {
        return Component.literal(text).withStyle(style -> style.withFont(new FontDescription.Resource(HEAD_FONT)).withColor(0xFFFFFF));
    }
}
