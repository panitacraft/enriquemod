package com.panita.enriquecraft.chat.service;

import com.panita.enriquecraft.auth.service.HeldPlayers;
import com.panita.enriquecraft.chat.config.ChatConfig;
import com.panita.enriquecraft.chat.message.ChatMessages;
import com.panita.enriquecraft.core.art.InlineArts;
import com.panita.enriquecraft.core.message.Message;
import com.panita.enriquecraft.core.message.Messenger;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundPlayerInfoUpdatePacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import org.jspecify.annotations.Nullable;

import java.util.EnumSet;
import java.util.List;
import java.util.UUID;

/**
 * The name a player has in the tab list: the same line as in chat (prank level, rank, name, suffix, emblems), built by the server as one
 * finished text, so a client that draws art shows the images and every other client shows their text. A tab list mod cannot do this,
 * since it turns the text into its own string and loses the images. The name is read again each second and sent only when it changed
 * (a rank, an emblem, the AFK color).
 */
public final class TabNames {

    private static final int REFRESH_INTERVAL_TICKS = 20;
    /** The height of the text of a line of the tab list: images are drawn this tall there, not as tall as in chat. */
    private static final int TEXT_HEIGHT = 8;

    private final Messenger messenger;
    private final InlineArts arts;
    private final ChatConfig config;
    private final HeldPlayers held;
    private final NameChanges changes;
    private int ticks;

    public TabNames(Messenger messenger, InlineArts arts, ChatConfig config, HeldPlayers held, NameChanges changes) {
        this.messenger = messenger;
        this.arts = arts;
        this.config = config;
        this.held = held;
        this.changes = changes;
    }

    /** The name of the player in the tab list, or null when the game's own entry stays: the setting is off, or the player is on the PIN screen. */
    public @Nullable Component nameOf(ServerPlayer player) {
        if (!config.tabListEnabled.get() || held.isHeld(player)) {
            return null;
        }
        return arts.withHeight(messenger.format(Message.plain(ChatMessages.Format.TAB_NAME), player), TEXT_HEIGHT);
    }

    /** Called every tick; once a second it sends to everyone the names that changed. */
    public void tick(MinecraftServer server) {
        if (++ticks % REFRESH_INTERVAL_TICKS != 0 || !config.tabListEnabled.get()) {
            return;
        }
        List<ServerPlayer> players = server.getPlayerList().getPlayers();
        List<ServerPlayer> changed = players.stream().filter(player -> {
            Component name = nameOf(player);
            if (name == null) {
                changes.forget(player.getUUID());
                return false;
            }
            // Readers must know the images before they get the name that holds them
            arts.announce(players, name);
            return changes.changed(player.getUUID(), name.toString());
        }).toList();
        if (!changed.isEmpty()) {
            server.getPlayerList().broadcastAll(new ClientboundPlayerInfoUpdatePacket(
                    EnumSet.of(ClientboundPlayerInfoUpdatePacket.Action.UPDATE_DISPLAY_NAME), changed));
        }
    }

    public void forget(UUID player) {
        changes.forget(player);
    }
}
