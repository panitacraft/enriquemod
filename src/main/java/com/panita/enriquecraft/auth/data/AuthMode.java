package com.panita.enriquecraft.auth.data;

import com.mojang.serialization.Codec;
import net.minecraft.server.players.NameAndId;
import net.minecraft.util.StringRepresentable;

import java.util.Arrays;
import java.util.Optional;
import java.util.UUID;

/**
 * How a player proves who they are: with their Microsoft account, or with a PIN.
 */
public enum AuthMode implements StringRepresentable {

    /** The name belongs to a Microsoft account; Mojang's session servers verify the player. */
    PREMIUM("premium"),
    /** No account stands behind the name; the player enters a PIN. */
    CRACKED("cracked");

    public static final Codec<AuthMode> CODEC = StringRepresentable.fromEnum(AuthMode::values);

    private final String id;

    AuthMode(String id) {
        this.id = id;
    }

    @Override
    public String getSerializedName() {
        return id;
    }

    public static Optional<AuthMode> byId(String id) {
        return Arrays.stream(values()).filter(mode -> mode.id.equals(id)).findFirst();
    }

    /**
     * The mode a whitelist or op entry stands for. Entries carry it in their UUID: the offline UUID of
     * the name means a cracked account, any other UUID is the real one of a Microsoft account.
     */
    public static AuthMode of(UUID id, String name) {
        return NameAndId.createOffline(name).id().equals(id) ? CRACKED : PREMIUM;
    }

    public static AuthMode of(NameAndId entry) {
        return of(entry.id(), entry.name());
    }
}
