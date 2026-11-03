package com.panita.enriquecraft.chat.service;

import com.panita.enriquecraft.Enriquecraft;
import com.panita.enriquecraft.chat.channel.ChannelSelection;
import com.panita.enriquecraft.core.art.InlineArts;
import eu.pb4.placeholders.api.PlaceholderResult;
import eu.pb4.placeholders.api.Placeholders;
import eu.pb4.placeholders.api.ServerPlaceholderContext;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;

import java.util.Optional;
import java.util.function.Supplier;

/**
 * The placeholders of the chat module, for every text that resolves them and for mods that read Placeholder API's, such as
 * a tab list mod: {@code %enriquecraft:nickname%}, {@code %enriquecraft:channel%} and {@code %enriquecraft:rank%}.
 */
public final class ChatPlaceholders {

    private final NicknameService nicknames;
    private final ChannelSelection selection;
    private final InlineArts arts;
    private final Supplier<String> rankIconMeta;

    public ChatPlaceholders(NicknameService nicknames, ChannelSelection selection, InlineArts arts, Supplier<String> rankIconMeta) {
        this.nicknames = nicknames;
        this.selection = selection;
        this.arts = arts;
        this.rankIconMeta = rankIconMeta;
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
     * in the meta of their group. A rank with no image is only the prefix.
     */
    private Component rank(ServerPlaceholderContext context) {
        Component prefix = luckperms(context, "prefix", null).orElse(Component.empty());
        String icon = luckperms(context, "meta", rankIconMeta.get()).map(value -> value.getString().strip()).orElse("");
        return icon.isEmpty() ? prefix : arts.wrap(prefix, icon);
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
