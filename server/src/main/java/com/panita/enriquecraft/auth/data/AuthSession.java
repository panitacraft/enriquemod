package com.panita.enriquecraft.auth.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.panita.enriquecraft.core.framework.data.TimeCodecs;

import java.time.Instant;

/**
 * A login that is remembered, so the PIN is not asked again from the same address until it ends.
 *
 * @param expiresAt when the session stops being valid
 * @param ipHash    a keyed hash of the address the PIN was entered from; the address itself is never stored
 */
public record AuthSession(Instant expiresAt, String ipHash) {

    public static final Codec<AuthSession> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            TimeCodecs.INSTANT.fieldOf("expiresAt").forGetter(AuthSession::expiresAt),
            Codec.STRING.fieldOf("ipHash").forGetter(AuthSession::ipHash)
    ).apply(instance, AuthSession::new));

    public boolean isValidFor(String ipHash, Instant now) {
        return this.ipHash.equals(ipHash) && now.isBefore(expiresAt);
    }
}
