package com.panita.enriquecraft.chat.service;

import com.panita.enriquecraft.Enriquecraft;
import com.panita.enriquecraft.chat.channel.ChannelSelection;
import com.panita.enriquecraft.chat.config.ChatConfig;
import com.panita.enriquecraft.chat.message.RankTooltip;
import com.panita.enriquecraft.core.art.InlineArts;
import eu.pb4.placeholders.api.PlaceholderResult;
import eu.pb4.placeholders.api.Placeholders;
import eu.pb4.placeholders.api.ServerPlaceholderContext;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;

import java.util.Optional;

/**
 * The placeholders of the chat module, for every text that resolves them and for mods that read Placeholder API's, such as
 * a tab list mod: {@code %enriquecraft:nickname%}, {@code %enriquecraft:channel%} and {@code %enriquecraft:rank%}.
 */
public final class ChatPlaceholders {

    private final NicknameService nicknames;
    private final ChannelSelection selection;
    private final InlineArts arts;
    private final ChatConfig config;
    private final RankTooltip tooltips;

    public ChatPlaceholders(NicknameService nicknames, ChannelSelection selection, InlineArts arts, ChatConfig config, RankTooltip tooltips) {
        this.nicknames = nicknames;
        this.selection = selection;
        this.arts = arts;
        this.config = config;
        this.tooltips = tooltips;
    }

    public void register() {
        Placeholders.registerServer(id("nickname"), (context, argument) -> {
            if (!context.hasServerPlayer()) {
                return PlaceholderResult.invalid("No player");
            }
            return PlaceholderResult.value(nicknames.nicknameOf(context.serverPlayer().getUUID())
                    .orElseGet(() -> Component.literal(context.serverPlayer().getGameProfile().name())));
        });
        Placeholders.registerServer(id("rank"), (context, argument) -> context.hasServerPlayer()
                ? PlaceholderResult.value(rank(context))
                : PlaceholderResult.invalid("No player"));
        Placeholders.registerServer(id("channel"), (context, argument) -> context.hasServerPlayer()
                ? PlaceholderResult.value(selection.current(context.serverPlayer()).id())
                : PlaceholderResult.invalid("No player"));
    }

    /**
     * The rank of a player: the prefix LuckPerms gives them, and in place of it, for a client that draws art, the image whose address is
     * in the meta of their group. A rank with no image is only the prefix. Like the emblem placeholders it brings its own space, so a
     * player with no rank leaves no gap before their name.
     */
    private Component rank(ServerPlaceholderContext context) {
        Component prefix = luckperms(context, "prefix", null).orElse(Component.empty());
        String icon = meta(context, config.rankIconMeta.get());
        if (icon.isEmpty() && prefix.getString().isBlank()) {
            return Component.empty();
        }
        Component drawn = icon.isEmpty() ? prefix : arts.wrap(prefix, icon);
        // Pointing at the rank shows its title and description, when the group has them. LuckPerms gives them already read, with the colors
        // and styles their text asks for, so they are kept as they are and not turned back into plain text.
        Optional<Style> tooltip = tooltips.styleOf(luckperms(context, "meta", config.rankTitleMeta.get()).orElse(Component.empty()),
                luckperms(context, "meta", config.rankDescriptionMeta.get()).orElse(Component.empty()));
        return Component.empty().append(tooltip.<Component>map(style -> drawn.copy().withStyle(style)).orElse(drawn)).append(Component.literal(" "));
    }

    /** The value of a LuckPerms meta key for the player of the context, as written; empty when the group has none. */
    private static String meta(ServerPlaceholderContext context, String key) {
        return luckperms(context, "meta", key).map(value -> value.getString().strip()).orElse("");
    }

    /** What a LuckPerms placeholder says for the player of the context, which Placeholder API holds under its name; empty when it says nothing. */
    private static Optional<Component> luckperms(ServerPlaceholderContext context, String name, String argument) {
        PlaceholderResult result = Placeholders.parseServerPlaceholder(Identifier.fromNamespaceAndPath("luckperms", name), argument, context);
        return result.isValid() ? Optional.ofNullable(result.component()) : Optional.empty();
    }

    private static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(Enriquecraft.MOD_ID, path);
    }
}
