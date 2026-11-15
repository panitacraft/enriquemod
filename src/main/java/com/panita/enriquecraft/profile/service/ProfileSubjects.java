package com.panita.enriquecraft.profile.service;

import com.mojang.authlib.GameProfile;
import com.panita.enriquecraft.auth.service.HeldPlayers;
import com.panita.enriquecraft.chat.service.SkinMemory;
import com.panita.enriquecraft.discord.service.PlayTimes;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.NameAndId;
import net.minecraft.server.players.UserWhiteListEntry;
import net.minecraft.stats.Stats;
import net.minecraft.world.level.storage.LevelResource;

import java.util.Locale;
import java.util.Optional;
import java.util.OptionalLong;

/**
 * Finds a player for a profile screen: those online, then everyone on the whitelist, which is who can play here. A name nobody on the
 * server has is no profile, whatever the Panita API may know.
 */
public final class ProfileSubjects {

    private final ProfileService profiles;
    private final ProfileDetailsService details;
    private final HeldPlayers held;
    private final SkinMemory skins;

    public ProfileSubjects(ProfileService profiles, ProfileDetailsService details, HeldPlayers held, SkinMemory skins) {
        this.profiles = profiles;
        this.details = details;
        this.held = held;
        this.skins = skins;
    }

    /** A player who is online, from what the server holds of them now. */
    public ProfileSubject of(ServerPlayer player) {
        long seconds = PlayTimes.secondsOfTicks(player.getStats().getValue(Stats.CUSTOM.get(Stats.PLAY_TIME)));
        return new ProfileSubject(player.getGameProfile().name(), player.getUUID(), true, Optional.of(player.getGameProfile()),
                profiles.profileOf(player.getUUID()), profiles.emblemsOf(player), details.detailsOf(player), OptionalLong.of(seconds));
    }

    /** The player with that name, whatever its capitalization; empty when nobody on the server has it. */
    public Optional<ProfileSubject> named(MinecraftServer server, String name) {
        Optional<ServerPlayer> online = server.getPlayerList().getPlayers().stream()
                .filter(player -> !held.isHeld(player) && player.getGameProfile().name().equalsIgnoreCase(name)).findFirst();
        if (online.isPresent()) {
            return online.map(this::of);
        }
        return whitelisted(server, name).map(user -> {
            Optional<GameProfile> skin = skins.profileOf(user.name());
            OptionalLong seconds = PlayTimes.secondsOf(server.getWorldPath(LevelResource.PLAYER_STATS_DIR), user.id());
            return new ProfileSubject(user.name(), user.id(), false, skin, profiles.profileOf(user.name()), profiles.emblemsOf(user.name()),
                    details.detailsOf(user.name()), seconds);
        });
    }

    private static Optional<NameAndId> whitelisted(MinecraftServer server, String name) {
        String wanted = name.toLowerCase(Locale.ROOT);
        for (UserWhiteListEntry entry : server.getPlayerList().getWhiteList().getEntries()) {
            NameAndId user = entry.getUser();
            if (user != null && user.name().toLowerCase(Locale.ROOT).equals(wanted)) {
                return Optional.of(user);
            }
        }
        return Optional.empty();
    }
}
