package com.panita.enriquecraft.discord.service;

import com.google.gson.JsonArray;
import com.panita.enriquecraft.auth.service.HeldPlayers;
import com.panita.enriquecraft.core.message.Message;
import com.panita.enriquecraft.core.message.Messenger;
import com.panita.enriquecraft.discord.message.DiscordMessages;
import com.panita.enriquecraft.discord.protocol.PlayerInfo;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.NameAndId;
import net.minecraft.server.players.UserWhiteListEntry;
import net.minecraft.stats.Stats;
import net.minecraft.world.level.storage.LevelResource;

import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.OptionalLong;

/**
 * Who is on the server and what it knows about each player, for the questions of the bot. A player who has not entered their
 * PIN yet is not counted, since they are not really in. The line the server shows for a player is the
 * {@link DiscordMessages.Players#ENTRY} text with the placeholders of that player (their rank prefix and suffix come from
 * LuckPerms through Placeholder API); one who is not online has only their name, since the rank is read from the connected
 * player.
 */
public final class PlayerRoster {

    private final PlayerInfos infos;
    private final HeldPlayers held;
    private final Messenger messenger;

    public PlayerRoster(PlayerInfos infos, HeldPlayers held, Messenger messenger) {
        this.infos = infos;
        this.held = held;
        this.messenger = messenger;
    }

    public List<Answers.Online> online(MinecraftServer server) {
        return server.getPlayerList().getPlayers().stream()
                .filter(player -> !held.isHeld(player))
                .map(player -> new Answers.Online(infos.of(player), player.connection.latency(), line(player)))
                .toList();
    }

    /** What the server knows about the player of that name: those online, then everyone on the whitelist. */
    public Optional<Answers.Info> info(MinecraftServer server, String name) {
        Optional<ServerPlayer> online = server.getPlayerList().getPlayers().stream()
                .filter(player -> !held.isHeld(player) && player.getGameProfile().name().equalsIgnoreCase(name)).findFirst();
        if (online.isPresent()) {
            ServerPlayer player = online.get();
            long seconds = PlayTimes.secondsOfTicks(player.getStats().getValue(Stats.CUSTOM.get(Stats.PLAY_TIME)));
            return Optional.of(new Answers.Info(infos.of(player), true, OptionalInt.of(player.connection.latency()), line(player),
                    OptionalLong.of(seconds)));
        }
        return whitelisted(server, name).map(user -> {
            PlayerInfo info = infos.offline(user.id(), user.name());
            return new Answers.Info(info, false, OptionalInt.empty(), plain(user.name()),
                    PlayTimes.secondsOf(server.getWorldPath(LevelResource.PLAYER_STATS_DIR), user.id()));
        });
    }

    private Optional<NameAndId> whitelisted(MinecraftServer server, String name) {
        String wanted = name.toLowerCase(Locale.ROOT);
        for (UserWhiteListEntry entry : server.getPlayerList().getWhiteList().getEntries()) {
            NameAndId user = entry.getUser();
            if (user != null && user.name().toLowerCase(Locale.ROOT).equals(wanted)) {
                return Optional.of(user);
            }
        }
        return Optional.empty();
    }

    private JsonArray line(ServerPlayer player) {
        return DisplayRuns.of(messenger.format(Message.plain(DiscordMessages.Players.ENTRY), player));
    }

    private static JsonArray plain(String name) {
        return DisplayRuns.of(Component.literal(name));
    }
}
