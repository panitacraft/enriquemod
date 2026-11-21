package com.panita.enriquecraft.moderation.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.panita.enriquecraft.core.framework.data.TimeCodecs;
import net.minecraft.core.UUIDUtil;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

/**
 * A player who may not write in the chat.
 *
 * @param id      the player, by the id the server knows them with
 * @param name    their name when they were muted, for lists and for unmuting by name
 * @param created when they were muted
 * @param expires when the mute ends; empty for a permanent one
 * @param source  who muted them
 * @param reason  why, when staff said
 */
public record MuteRecord(UUID id, String name, Instant created, Optional<Instant> expires, String source, Optional<String> reason) {

    public static final Codec<MuteRecord> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            UUIDUtil.CODEC.fieldOf("id").forGetter(MuteRecord::id),
            Codec.STRING.fieldOf("name").forGetter(MuteRecord::name),
            TimeCodecs.INSTANT.fieldOf("created").forGetter(MuteRecord::created),
            TimeCodecs.INSTANT.optionalFieldOf("expires").forGetter(MuteRecord::expires),
            Codec.STRING.fieldOf("source").forGetter(MuteRecord::source),
            Codec.STRING.optionalFieldOf("reason").forGetter(MuteRecord::reason)
    ).apply(instance, MuteRecord::new));

    public boolean isActiveAt(Instant now) {
        return expires.map(now::isBefore).orElse(true);
    }

    /** How long the mute still lasts; empty when it is permanent. */
    public Optional<Duration> remainingAt(Instant now) {
        return expires.map(end -> Duration.between(now, end));
    }
}
