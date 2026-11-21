package com.panita.enriquecraft.staff.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.UUIDUtil;

import java.util.UUID;

/**
 * A player picked by name, kept with the id the server found for it: the skin is looked up by id, which
 * works where a lookup by name does not.
 *
 * @param name the player's name as the server knew it
 * @param id   the player's id
 */
public record PlayerRef(String name, UUID id) {

    public static final Codec<PlayerRef> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.STRING.fieldOf("name").forGetter(PlayerRef::name),
            UUIDUtil.STRING_CODEC.fieldOf("id").forGetter(PlayerRef::id)
    ).apply(instance, PlayerRef::new));
}
