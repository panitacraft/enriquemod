package com.panita.enriquecraft.chat.service;

import com.panita.enriquecraft.Enriquecraft;
import com.panita.enriquecraft.chat.channel.ChannelSelection;
import com.panita.enriquecraft.core.art.InlineArts;
import com.panita.enriquecraft.core.message.Message;
import com.panita.enriquecraft.core.message.Messenger;
import eu.pb4.placeholders.api.PlaceholderResult;
import eu.pb4.placeholders.api.Placeholders;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;

import java.util.function.Supplier;

/**
 * The placeholders of the chat module, for every text that resolves them and for mods that read Placeholder API's, such as
 * a tab list mod: {@code %enriquecraft:nickname%}, {@code %enriquecraft:channel%} and {@code %enriquecraft:rank%}.
 */
public final class ChatPlaceholders {

    private final NicknameService nicknames;
    private final ChannelSelection selection;
    private final Messenger messenger;
    private final InlineArts arts;
    private final Supplier<String> rankIconMeta;

    public ChatPlaceholders(NicknameService nicknames, ChannelSelection selection, Messenger messenger, InlineArts arts,
                            Supplier<String> rankIconMeta) {
        this.nicknames = nicknames;
        this.selection = selection;
        this.messenger = messenger;
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
                ? PlaceholderResult.value(rank(context.serverPlayer()))
                : PlaceholderResult.invalid("No player"));
        Placeholders.registerServer(id("channel"), (context, argument) -> context.hasServerPlayer()
                ? PlaceholderResult.value(selection.current(context.serverPlayer()).id())
                : PlaceholderResult.invalid("No player"));
    }

    /**
     * The rank of a player: the prefix LuckPerms gives them, and in place of it, for a client that draws art, the image whose address is
     * in the meta of their group. A rank with no image is only the prefix.
     */
    private Component rank(ServerPlayer player) {
        Component prefix = messenger.format(Message.plain("%luckperms:prefix%"), player);
        String icon = messenger.format(Message.plain("%luckperms:meta " + rankIconMeta.get() + "%"), player).getString().strip();
        return icon.isEmpty() ? prefix : arts.wrap(prefix, icon);
    }

    private static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(Enriquecraft.MOD_ID, path);
    }
}
