package com.panita.enriquecraft.link.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.panita.enriquecraft.link.client.LinkedPlayer;

import java.util.List;
import java.util.Optional;

/**
 * The last list of players the server applied. It is what the server starts from when the API cannot be
 * reached, and what tells a player who left the list from one the whitelist was always given by hand.
 *
 * @param players the players of that list
 * @param etag    the tag the API gave that version, to ask whether it changed
 */
public record LinkedSnapshot(List<LinkedPlayer> players, Optional<String> etag) {

    public static final LinkedSnapshot EMPTY = new LinkedSnapshot(List.of(), Optional.empty());

    public static final Codec<LinkedSnapshot> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            LinkedPlayer.CODEC.listOf().fieldOf("players").forGetter(LinkedSnapshot::players),
            Codec.STRING.optionalFieldOf("etag").forGetter(LinkedSnapshot::etag)
    ).apply(instance, LinkedSnapshot::new));
}
