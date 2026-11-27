package com.panita.enriquecraft.chat.service;

import com.panita.enriquecraft.auth.service.HeldPlayers;
import com.panita.enriquecraft.chat.config.ChatConfig;
import com.panita.enriquecraft.chat.message.ChatMessages;
import com.panita.enriquecraft.core.art.InlineArts;
import com.panita.enriquecraft.core.message.Message;
import com.panita.enriquecraft.core.message.Messenger;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetPlayerTeamPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.scores.PlayerTeam;
import net.minecraft.world.scores.Scoreboard;
import org.jspecify.annotations.Nullable;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * The name above the head of a player, dressed like their line in chat: the prank level and rank before the name and the suffix and emblems
 * after it. A client draws the name of a player from their entry in its list and puts the prefix and suffix of the team they are in around
 * it, so each player gets a team of their own, known only to the clients (it is never in the scoreboard of the server, saved or listed by
 * a command). A player the server already put in a team keeps the team, since a client holds a player in one team only. The name between
 * the prefix and the suffix is the one the clients know the player by, the nickname when they have one. Everything is built again each
 * second and sent only when it changed.
 */
public final class NameTags {

    private static final int REFRESH_INTERVAL_TICKS = 20;
    /** The height of the name above a head: images are drawn this tall, as in the tab list. */
    private static final int TEXT_HEIGHT = 8;
    private static final String TEAM_PREFIX = "enriquecraft_tag_";

    private record Active(String signature, PlayerTeam team) {
    }

    private final Messenger messenger;
    private final InlineArts arts;
    private final ChatConfig config;
    private final HeldPlayers held;
    private final ClientNames names;
    /** Only holds the teams so they can be turned into packets; no server uses it. */
    private final Scoreboard packetsOnly = new Scoreboard();
    private final Map<UUID, Active> active = new HashMap<>();
    private int ticks;

    public NameTags(Messenger messenger, InlineArts arts, ChatConfig config, HeldPlayers held, ClientNames names) {
        this.messenger = messenger;
        this.arts = arts;
        this.config = config;
        this.held = held;
        this.names = names;
    }

    /** Called every tick; once a second it sends the teams that changed to everyone, or takes them all back when the setting is off. */
    public void tick(MinecraftServer server) {
        if (++ticks % REFRESH_INTERVAL_TICKS != 0) {
            return;
        }
        List<ServerPlayer> players = server.getPlayerList().getPlayers();
        boolean enabled = config.nameTagEnabled.get();
        for (ServerPlayer player : players) {
            PlayerTeam wanted = enabled ? teamOf(server, player) : null;
            Active current = active.get(player.getUUID());
            if (wanted == null) {
                if (current != null) {
                    active.remove(player.getUUID());
                    broadcast(players, ClientboundSetPlayerTeamPacket.createRemovePacket(current.team()));
                }
                continue;
            }
            String signature = signatureOf(wanted);
            if (current != null && current.signature().equals(signature)) {
                continue;
            }
            // Readers must know the images before they get the team that holds them
            arts.announce(players, wanted.getPlayerPrefix());
            arts.announce(players, wanted.getPlayerSuffix());
            if (current != null) {
                broadcast(players, ClientboundSetPlayerTeamPacket.createRemovePacket(current.team()));
            }
            broadcast(players, ClientboundSetPlayerTeamPacket.createAddOrModifyPacket(wanted, true));
            active.put(player.getUUID(), new Active(signature, wanted));
        }
    }

    /** Tells a player who just joined the teams everybody else already has. */
    public void introduce(ServerPlayer joined) {
        for (Active entry : active.values()) {
            arts.announce(List.of(joined), entry.team().getPlayerPrefix());
            arts.announce(List.of(joined), entry.team().getPlayerSuffix());
            joined.connection.send(ClientboundSetPlayerTeamPacket.createAddOrModifyPacket(entry.team(), true));
        }
    }

    /** Takes the team of a player back from everyone who is left. */
    public void forget(MinecraftServer server, UUID player) {
        Active current = active.remove(player);
        if (current != null) {
            broadcast(server.getPlayerList().getPlayers(), ClientboundSetPlayerTeamPacket.createRemovePacket(current.team()));
        }
    }

    /** The team the player should have, or null when the game's own entry stays: they are held, or already in a team of the server. */
    private @Nullable PlayerTeam teamOf(MinecraftServer server, ServerPlayer player) {
        if (held.isHeld(player) || server.getScoreboard().getPlayersTeam(player.getGameProfile().name()) != null) {
            return null;
        }
        PlayerTeam team = new PlayerTeam(packetsOnly, TEAM_PREFIX + player.getUUID());
        team.setPlayerPrefix(arts.withHeight(messenger.format(Message.plain(ChatMessages.Format.NAME_TAG_PREFIX), player), TEXT_HEIGHT));
        team.setPlayerSuffix(arts.withHeight(messenger.format(Message.plain(ChatMessages.Format.NAME_TAG_SUFFIX), player), TEXT_HEIGHT));
        team.getPlayers().add(names.shownName(player));
        return team;
    }

    private static String signatureOf(PlayerTeam team) {
        return team.getPlayerPrefix() + "|" + team.getPlayerSuffix() + "|" + team.getPlayers();
    }

    private static void broadcast(List<ServerPlayer> players, ClientboundSetPlayerTeamPacket packet) {
        players.forEach(player -> player.connection.send(packet));
    }
}
