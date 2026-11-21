package com.panita.enriquecraft.staff.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.panita.enriquecraft.core.framework.data.TimeCodecs;
import net.minecraft.core.UUIDUtil;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * The inventory a player had when they died, and where and how it happened.
 *
 * @param id         identifies this record
 * @param player     who died
 * @param playerName that player's name when they died
 * @param diedAt     when they died
 * @param dimension  where they died
 * @param x          the exact position
 * @param y          the exact position
 * @param z          the exact position
 * @param cause      the death message
 * @param xpLevel    their experience level
 * @param items      the {@link #SLOT_COUNT} inventory slots in inventory order, empty stacks included
 * @param restoredAt when the items were given back to the player, if they were
 */
public record DeathRecord(UUID id, UUID player, String playerName, Instant diedAt, ResourceKey<Level> dimension,
                          double x, double y, double z, String cause, int xpLevel, List<ItemStack> items,
                          Optional<Instant> restoredAt) {

    /** A death whose items have not been given back. */
    public DeathRecord(UUID id, UUID player, String playerName, Instant diedAt, ResourceKey<Level> dimension,
                       double x, double y, double z, String cause, int xpLevel, List<ItemStack> items) {
        this(id, player, playerName, diedAt, dimension, x, y, z, cause, xpLevel, items, Optional.empty());
    }

    /** Slots 0 to 35 are the main inventory and hotbar, 36 to 39 the armor (feet to head), 40 the offhand. */
    public static final int SLOT_COUNT = 41;

    /** In the file only the occupied slots are written; they are put back into all 41 slots on load. */
    private static final Codec<List<ItemStack>> SLOTS = SlotStack.CODEC.listOf()
            .comapFlatMap(DeathRecord::toAllSlots, DeathRecord::toOccupiedSlots);

    public static final Codec<DeathRecord> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            UUIDUtil.STRING_CODEC.fieldOf("id").forGetter(DeathRecord::id),
            UUIDUtil.STRING_CODEC.fieldOf("player").forGetter(DeathRecord::player),
            Codec.STRING.fieldOf("playerName").forGetter(DeathRecord::playerName),
            TimeCodecs.INSTANT.fieldOf("diedAt").forGetter(DeathRecord::diedAt),
            Level.RESOURCE_KEY_CODEC.fieldOf("dimension").forGetter(DeathRecord::dimension),
            Codec.DOUBLE.fieldOf("x").forGetter(DeathRecord::x),
            Codec.DOUBLE.fieldOf("y").forGetter(DeathRecord::y),
            Codec.DOUBLE.fieldOf("z").forGetter(DeathRecord::z),
            Codec.STRING.fieldOf("cause").forGetter(DeathRecord::cause),
            Codec.INT.fieldOf("xpLevel").forGetter(DeathRecord::xpLevel),
            SLOTS.fieldOf("items").forGetter(DeathRecord::items),
            TimeCodecs.INSTANT.optionalFieldOf("restoredAt").forGetter(DeathRecord::restoredAt)
    ).apply(instance, DeathRecord::new));

    private static DataResult<List<ItemStack>> toAllSlots(List<SlotStack> occupied) {
        List<ItemStack> items = new ArrayList<>(Collections.nCopies(SLOT_COUNT, ItemStack.EMPTY));
        for (SlotStack entry : occupied) {
            if (!items.get(entry.slot()).isEmpty()) {
                return DataResult.error(() -> "Slot " + entry.slot() + " appears twice");
            }
            items.set(entry.slot(), entry.stack());
        }
        return DataResult.success(items);
    }

    private static List<SlotStack> toOccupiedSlots(List<ItemStack> items) {
        List<SlotStack> occupied = new ArrayList<>();
        for (int slot = 0; slot < items.size(); slot++) {
            if (!items.get(slot).isEmpty()) {
                occupied.add(new SlotStack(slot, items.get(slot)));
            }
        }
        return occupied;
    }

    /** Takes a snapshot of a player who is dying; the stacks are copies. */
    public static DeathRecord of(ServerPlayer player, DamageSource source, Instant diedAt) {
        List<ItemStack> items = new ArrayList<>(SLOT_COUNT);
        for (int slot = 0; slot < SLOT_COUNT; slot++) {
            items.add(player.getInventory().getItem(slot).copy());
        }
        return new DeathRecord(UUID.randomUUID(), player.getUUID(), player.getName().getString(), diedAt,
                player.level().dimension(), player.getX(), player.getY(), player.getZ(),
                source.getLocalizedDeathMessage(player).getString(), player.experienceLevel, items);
    }

    /** The same death marked as given back at that moment. */
    public DeathRecord markRestored(Instant moment) {
        return new DeathRecord(id, player, playerName, diedAt, dimension, x, y, z, cause, xpLevel, items, Optional.of(moment));
    }

    public boolean isRestored() {
        return restoredAt.isPresent();
    }

    /** The stacks that are not empty, in inventory order. */
    public List<ItemStack> nonEmptyItems() {
        return items.stream().filter(stack -> !stack.isEmpty()).toList();
    }

    public boolean hasItems() {
        return items.stream().anyMatch(stack -> !stack.isEmpty());
    }

    private record SlotStack(int slot, ItemStack stack) {
        static final Codec<SlotStack> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.intRange(0, SLOT_COUNT - 1).fieldOf("slot").forGetter(SlotStack::slot),
                ItemStack.CODEC.fieldOf("item").forGetter(SlotStack::stack)
        ).apply(instance, SlotStack::new));
    }
}
