package com.panita.enriquecraft.staff.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.panita.enriquecraft.core.framework.data.TimeCodecs;
import net.minecraft.core.UUIDUtil;
import net.minecraft.world.item.ItemStack;

import java.time.Instant;
import java.util.UUID;

/**
 * A custom item saved by a staff member: the item exactly as it was held, with all its
 * components, marked as {@code enriquecraft:<name>}.
 *
 * @param name        the name it is looked up by
 * @param stack       the item; never modify it, give out copies
 * @param savedBy     who saved it
 * @param savedByName that person's name when it was saved
 * @param savedAt     when it was saved
 */
public record SavedItem(String name, ItemStack stack, UUID savedBy, String savedByName, Instant savedAt) {

    public static final Codec<SavedItem> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.STRING.fieldOf("name").forGetter(SavedItem::name),
            ItemStack.CODEC.fieldOf("item").forGetter(SavedItem::stack),
            UUIDUtil.STRING_CODEC.fieldOf("savedBy").forGetter(SavedItem::savedBy),
            Codec.STRING.fieldOf("savedByName").forGetter(SavedItem::savedByName),
            TimeCodecs.INSTANT.fieldOf("savedAt").forGetter(SavedItem::savedAt)
    ).apply(instance, SavedItem::new));
}
