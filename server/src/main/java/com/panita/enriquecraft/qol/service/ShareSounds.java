package com.panita.enriquecraft.qol.service;

import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.protocol.game.ClientboundSoundPacket;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import org.jspecify.annotations.Nullable;

/**
 * Plays a configured sound to one player, at their own position so they hear it at full volume wherever they are.
 */
public final class ShareSounds {

    private static final float VOLUME = 1.0f;
    private static final float PITCH = 1.0f;

    /** Whether the text is the id of a sound the game knows, which is what the settings accept. */
    public static boolean isValid(String id) {
        Identifier parsed = Identifier.tryParse(id);
        return parsed != null && BuiltInRegistries.SOUND_EVENT.containsKey(parsed);
    }

    /** Plays the sound to the player; a text that is not a known sound plays nothing. */
    public void play(ServerPlayer player, String id) {
        @Nullable Identifier parsed = Identifier.tryParse(id);
        if (parsed == null) {
            return;
        }
        BuiltInRegistries.SOUND_EVENT.get(parsed).ifPresent(sound -> send(player, sound));
    }

    private static void send(ServerPlayer player, Holder<SoundEvent> sound) {
        player.connection.send(new ClientboundSoundPacket(sound, SoundSource.PLAYERS, player.getX(), player.getY(), player.getZ(),
                VOLUME, PITCH, player.getRandom().nextLong()));
    }
}
