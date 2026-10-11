package com.panita.enriquecraft.chat.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.panita.enriquecraft.core.framework.data.TimeCodecs;
import net.minecraft.core.UUIDUtil;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

/**
 * What the chat module remembers about a player.
 *
 * @param id                the player's UUID
 * @param name              the name they last joined with, so nobody takes it as a nickname while they are away
 * @param nickname          the nickname as it was written, with its formatting, if they have one
 * @param nicknameChangedAt when they last changed it, for the cooldown
 */
public record ChatProfile(UUID id, String name, Optional<String> nickname, Optional<Instant> nicknameChangedAt) {

    public static final Codec<ChatProfile> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            UUIDUtil.STRING_CODEC.fieldOf("id").forGetter(ChatProfile::id),
            Codec.STRING.fieldOf("name").forGetter(ChatProfile::name),
            Codec.STRING.optionalFieldOf("nickname").forGetter(ChatProfile::nickname),
            TimeCodecs.INSTANT.optionalFieldOf("nicknameChangedAt").forGetter(ChatProfile::nicknameChangedAt)
    ).apply(instance, ChatProfile::new));

    public static ChatProfile create(UUID id, String name) {
        return new ChatProfile(id, name, Optional.empty(), Optional.empty());
    }

    public ChatProfile withName(String newName) {
        return new ChatProfile(id, newName, nickname, nicknameChangedAt);
    }

    public ChatProfile withNickname(Optional<String> newNickname, Optional<Instant> changedAt) {
        return new ChatProfile(id, name, newNickname, changedAt);
    }
}
