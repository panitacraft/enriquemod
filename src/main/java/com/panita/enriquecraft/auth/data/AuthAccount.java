package com.panita.enriquecraft.auth.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.UUIDUtil;

import java.util.Optional;
import java.util.UUID;

/**
 * What the server remembers about a player who logs in with a PIN.
 *
 * @param id      the player's UUID (the offline one, since no account stands behind the name)
 * @param name    the name they last joined with
 * @param pin     their PIN, once they have chosen one
 * @param session their remembered login, if any
 * @param heldAt  where they really were while waiting in the waiting spot, if they are or were waiting
 */
public record AuthAccount(UUID id, String name, Optional<PinCredential> pin, Optional<AuthSession> session,
                          Optional<HeldPosition> heldAt) {

    public static final Codec<AuthAccount> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            UUIDUtil.STRING_CODEC.fieldOf("id").forGetter(AuthAccount::id),
            Codec.STRING.fieldOf("name").forGetter(AuthAccount::name),
            PinCredential.CODEC.optionalFieldOf("pin").forGetter(AuthAccount::pin),
            AuthSession.CODEC.optionalFieldOf("session").forGetter(AuthAccount::session),
            HeldPosition.CODEC.optionalFieldOf("heldAt").forGetter(AuthAccount::heldAt)
    ).apply(instance, AuthAccount::new));

    public static AuthAccount create(UUID id, String name) {
        return new AuthAccount(id, name, Optional.empty(), Optional.empty(), Optional.empty());
    }

    public AuthAccount withName(String newName) {
        return new AuthAccount(id, newName, pin, session, heldAt);
    }

    public AuthAccount withPin(Optional<PinCredential> newPin) {
        return new AuthAccount(id, name, newPin, session, heldAt);
    }

    public AuthAccount withSession(Optional<AuthSession> newSession) {
        return new AuthAccount(id, name, pin, newSession, heldAt);
    }

    public AuthAccount withHeldAt(Optional<HeldPosition> newHeldAt) {
        return new AuthAccount(id, name, pin, session, newHeldAt);
    }
}
