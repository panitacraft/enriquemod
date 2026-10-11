package com.panita.enriquecraft.chat.service;

import com.panita.enriquecraft.Enriquecraft;
import com.panita.enriquecraft.chat.channel.ChannelSelection;
import eu.pb4.placeholders.api.PlaceholderResult;
import eu.pb4.placeholders.api.Placeholders;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

/**
 * The placeholders of the chat module, for every text that resolves them and for mods that read Placeholder API's, such as
 * a tab list mod: {@code %enriquecraft:nickname%} and {@code %enriquecraft:channel%}.
 */
public final class ChatPlaceholders {

    private final NicknameService nicknames;
    private final ChannelSelection selection;

    public ChatPlaceholders(NicknameService nicknames, ChannelSelection selection) {
        this.nicknames = nicknames;
        this.selection = selection;
    }

    public void register() {
        Placeholders.registerServer(id("nickname"), (context, argument) -> {
            if (!context.hasServerPlayer()) {
                return PlaceholderResult.invalid("No player");
            }
            return PlaceholderResult.value(nicknames.nicknameOf(context.serverPlayer().getUUID())
                    .orElseGet(() -> Component.literal(context.serverPlayer().getGameProfile().name())));
        });
        Placeholders.registerServer(id("channel"), (context, argument) -> context.hasServerPlayer()
                ? PlaceholderResult.value(selection.current(context.serverPlayer()).id())
                : PlaceholderResult.invalid("No player"));
    }

    private static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(Enriquecraft.MOD_ID, path);
    }
}
