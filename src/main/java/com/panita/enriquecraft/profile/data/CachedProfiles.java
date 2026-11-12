package com.panita.enriquecraft.profile.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.panita.enriquecraft.core.framework.data.TimeCodecs;
import com.panita.enriquecraft.profile.client.ApiEmblem;
import com.panita.enriquecraft.profile.client.EmblemCategory;
import com.panita.enriquecraft.profile.client.ModProfile;
import net.minecraft.core.UUIDUtil;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * What the Panita API last said of each player and when, kept in the world folder so a restart does not ask the API again.
 *
 * @param entries one entry per player the API was asked about
 */
public record CachedProfiles(Map<UUID, Entry> entries) {

    public static final CachedProfiles EMPTY = new CachedProfiles(Map.of());

    /**
     * @param profile what the API said; empty when it has no profile for the player, which is an answer too
     * @param readAt  when the API said it
     */
    public record Entry(Optional<ModProfile> profile, Instant readAt) {
    }

    private static final Codec<EmblemCategory> CATEGORY = Codec.STRING.comapFlatMap(
            id -> EmblemCategory.byId(id).map(DataResult::success).orElseGet(() -> DataResult.error(() -> "unknown emblem category " + id)),
            EmblemCategory::id);

    private static final Codec<ApiEmblem> EMBLEM = RecordCodecBuilder.create(instance -> instance.group(
            Codec.STRING.fieldOf("id").forGetter(ApiEmblem::id),
            Codec.STRING.fieldOf("name").forGetter(ApiEmblem::name),
            Codec.STRING.optionalFieldOf("description").forGetter(ApiEmblem::description),
            Codec.STRING.optionalFieldOf("iconUrl").forGetter(ApiEmblem::iconUrl),
            CATEGORY.fieldOf("category").forGetter(ApiEmblem::category),
            Codec.INT.fieldOf("position").forGetter(ApiEmblem::position),
            Codec.STRING.optionalFieldOf("editionId").forGetter(ApiEmblem::editionId)
    ).apply(instance, ApiEmblem::new));

    private static final Codec<ModProfile> PROFILE = RecordCodecBuilder.create(instance -> instance.group(
            Codec.INT.fieldOf("userId").forGetter(ModProfile::userId),
            Codec.STRING.optionalFieldOf("ign").forGetter(ModProfile::ign),
            Codec.STRING.fieldOf("discordName").forGetter(ModProfile::discordName),
            EMBLEM.listOf().fieldOf("emblems").forGetter(ModProfile::emblems)
    ).apply(instance, ModProfile::new));

    private static final Codec<Entry> ENTRY = RecordCodecBuilder.create(instance -> instance.group(
            PROFILE.optionalFieldOf("profile").forGetter(Entry::profile),
            TimeCodecs.INSTANT.fieldOf("readAt").forGetter(Entry::readAt)
    ).apply(instance, Entry::new));

    public static final Codec<CachedProfiles> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.unboundedMap(UUIDUtil.STRING_CODEC, ENTRY).fieldOf("entries").forGetter(CachedProfiles::entries)
    ).apply(instance, CachedProfiles::new));

    public CachedProfiles {
        entries = Map.copyOf(entries);
    }
}
