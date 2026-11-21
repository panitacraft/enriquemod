package com.panita.enriquecraft.chat.service;

import com.panita.enriquecraft.auth.service.HeldPlayers;
import com.panita.enriquecraft.chat.channel.ChannelSelection;
import com.panita.enriquecraft.chat.message.SudoView;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.permissions.Permission;
import net.minecraft.server.permissions.PermissionLevel;
import net.minecraft.server.permissions.PermissionSet;

import java.util.List;

/**
 * Makes a player do something: a text that starts with a slash is a command they run, anything else is a message they say in the
 * channel they write in. The command runs with what the player may do, so it is only allowed on a player who may not do more
 * than the one who asks, or it would be a way to get permissions one does not have.
 */
public final class SudoService {

    private final ChatService chat;
    private final ChannelSelection selection;
    private final HeldPlayers held;
    private final SudoView view;

    public SudoService(ChatService chat, ChannelSelection selection, HeldPlayers held, SudoView view) {
        this.chat = chat;
        this.selection = selection;
        this.held = held;
        this.view = view;
    }

    /** @return how a command reports it: 1 when it was done */
    public int run(CommandSourceStack source, ServerPlayer target, String text) {
        if (held.isHeld(target)) {
            view.notAvailable(source);
            return 0;
        }
        if (levelOf(target.permissions()).ordinal() > levelOf(source.permissions()).ordinal()) {
            view.tooPowerful(source);
            return 0;
        }
        if (text.startsWith("/")) {
            source.getServer().getCommands().performPrefixedCommand(target.createCommandSourceStack(), text.substring(1));
        } else {
            chat.speak(target, selection.current(target), text);
        }
        view.done(source, target.getGameProfile().name(), text);
        return 1;
    }

    /** The highest command level the permissions reach. */
    static PermissionLevel levelOf(PermissionSet permissions) {
        List<PermissionLevel> fromHighest = List.of(PermissionLevel.values()).reversed();
        return fromHighest.stream()
                .filter(level -> permissions.hasPermission(new Permission.HasCommandLevel(level)))
                .findFirst()
                .orElse(PermissionLevel.ALL);
    }
}
