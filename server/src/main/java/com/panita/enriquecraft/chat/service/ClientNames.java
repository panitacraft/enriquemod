package com.panita.enriquecraft.chat.service;

import com.panita.enriquecraft.chat.mixin.ChunkMapAccessor;
import com.panita.enriquecraft.chat.mixin.TrackedEntityInvoker;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientboundPlayerInfoRemovePacket;
import net.minecraft.network.protocol.game.ClientboundPlayerInfoUpdatePacket;
import net.minecraft.network.protocol.game.ClientboundResetScorePacket;
import net.minecraft.network.protocol.game.ClientboundSetScorePacket;
import net.minecraft.server.ServerScoreboard;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.scores.Objective;
import net.minecraft.world.scores.PlayerScoreEntry;

import java.util.List;
import java.util.Optional;

/**
 * Makes the clients learn the name of a player again after it changed. A client reads the name from the player's entry in
 * its list when the entity of the player appears, so the entry is replaced and the entity is shown again, as if the player had
 * just come into view. What the clients are told is {@code PlayerInfoEntryMixin}'s doing. A client also finds the scores of a player (the
 * health in the tab list) by that name, so they are sent again under the new one; {@code ServerScoreboardMixin} does it from then on.
 */
public final class ClientNames {

    private final NicknameService nicknames;

    public ClientNames(NicknameService nicknames) {
        this.nicknames = nicknames;
    }

    /** The name the clients know the player by: the nickname without colors or styles, else the real name. */
    public String shownName(ServerPlayer player) {
        return nicknames.plainNicknameOf(player.getUUID()).orElseGet(() -> player.getGameProfile().name());
    }

    /** @param previousName what {@link #shownName} was before the change */
    public void refresh(ServerPlayer player, String previousName) {
        List<ServerPlayer> everyone = player.level().getServer().getPlayerList().getPlayers();
        List<ServerPlayer> others = everyone.stream().filter(other -> other != player).toList();
        Object tracked = ((ChunkMapAccessor) player.level().getChunkSource().chunkMap).enriquecraft$trackedEntities().get(player.getId());

        if (tracked != null) {
            others.forEach(viewer -> ((TrackedEntityInvoker) tracked).enriquecraft$stopShowingTo(viewer));
        }
        ClientboundPlayerInfoRemovePacket remove = new ClientboundPlayerInfoRemovePacket(List.of(player.getUUID()));
        ClientboundPlayerInfoUpdatePacket add = ClientboundPlayerInfoUpdatePacket.createPlayerInitializing(List.of(player));
        everyone.forEach(viewer -> {
            viewer.connection.send(remove);
            viewer.connection.send(add);
        });
        if (tracked != null) {
            others.forEach(viewer -> ((TrackedEntityInvoker) tracked).enriquecraft$showToIfInView(viewer));
        }
        resendScores(player, previousName, everyone);
    }

    private void resendScores(ServerPlayer player, String previousName, List<ServerPlayer> everyone) {
        ServerScoreboard scoreboard = player.level().getServer().getScoreboard();
        String shown = shownName(player);
        String realName = player.getGameProfile().name();
        for (Objective objective : scoreboard.getObjectives()) {
            // Only the objectives the clients were told about: they have none of the others, and a score for one is a warning in their log
            if (scoreboard.getObjectiveDisplaySlotCount(objective) == 0) {
                continue;
            }
            if (!previousName.equals(shown)) {
                broadcast(everyone, new ClientboundResetScorePacket(previousName, objective.getName()));
            }
            for (PlayerScoreEntry entry : scoreboard.listPlayerScores(objective)) {
                if (entry.owner().equals(realName)) {
                    broadcast(everyone, new ClientboundSetScorePacket(shown, objective.getName(), entry.value(),
                            Optional.ofNullable(entry.display()), Optional.ofNullable(entry.numberFormatOverride())));
                }
            }
        }
    }

    private static void broadcast(List<ServerPlayer> everyone, Packet<?> packet) {
        everyone.forEach(viewer -> viewer.connection.send(packet));
    }
}
