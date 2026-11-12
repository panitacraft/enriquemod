package com.panita.enriquecraft.discord.message;

import com.panita.enriquecraft.Enriquecraft;
import com.panita.enriquecraft.chat.service.ChatHeads;
import com.panita.enriquecraft.core.message.Message;
import com.panita.enriquecraft.core.message.MessageFormatter;
import com.panita.enriquecraft.discord.config.DiscordConfig;
import com.panita.enriquecraft.discord.protocol.InFrame;
import com.panita.enriquecraft.profile.service.EmblemPlaceholders;
import com.panita.enriquecraft.profile.service.PlayerEmblems;
import com.panita.enriquecraft.profile.service.ProfileService;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FontDescription;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
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

    private final MessageFormatter formatter;
    private final DiscordConfig config;
    private final ReplyView replies;
    private final ChatHeads heads;
    private final ProfileService profiles;
    private final EmblemPlaceholders emblems;

    public AuthorView(MessageFormatter formatter, DiscordConfig config, ReplyView replies, ChatHeads heads, ProfileService profiles,
                      EmblemPlaceholders emblems) {
        this.formatter = formatter;
        this.config = config;
        this.replies = replies;
        this.heads = heads;
        this.profiles = profiles;
        this.emblems = emblems;
    }

    /** The author as the start of a line, hovering shows where it came from. Called on the server thread. */
    public Component sender(MinecraftServer server, String ign, Optional<InFrame.Reply> reply) {
        PlayerEmblems author = profiles.emblemsOf(ign);
        Component line = compose(heads.of(server, ign).map(AuthorView::withBotHead), emblems.system(author.prank()),
                formatter.format(Message.plain(config.deliveryPrefix.get())), ign, emblems.lore(author), reply.map(replies::line));
        Component hover = formatter.format(Message.plain(DiscordMessages.Delivery.HOVER));
        return line.copy().withStyle(style -> style.withHoverEvent(new HoverEvent.ShowText(hover)));
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
    static Component withBotHead(Component skinHead) {
        MutableComponent glyph = Component.literal(HEAD_GLYPH)
                .withStyle(style -> style.withFont(new FontDescription.Resource(HEAD_FONT)).withColor(0xFFFFFF));
        // The space is outside the glyph, which would give it the font of the icon
        Component icon = Component.empty().append(glyph).append(Component.literal(" "));
        return Component.translatableWithFallback(HEAD_KEY, "%1$s", skinHead, icon);
    }
}
