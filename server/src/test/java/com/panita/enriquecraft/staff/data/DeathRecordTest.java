package com.panita.enriquecraft.staff.data;

import com.panita.enriquecraft.MinecraftTestSupport;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.level.Level;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DeathRecordTest {

    private static final UUID PLAYER = UUID.fromString("11111111-2222-3333-4444-555555555555");

    @BeforeAll
    static void startMinecraft() {
        MinecraftTestSupport.bootstrap();
    }

    /** A full 41-slot inventory: a special sword in slot 0, a shulker with contents in slot 20, armor, and gaps. */
    static DeathRecord sample() {
        List<ItemStack> items = new ArrayList<>();
        for (int slot = 0; slot < DeathRecord.SLOT_COUNT; slot++) {
            items.add(ItemStack.EMPTY);
        }
        ItemStack sword = new ItemStack(Items.DIAMOND_SWORD);
        sword.set(DataComponents.CUSTOM_NAME, Component.literal("Espada <especial>"));
        CompoundTag data = new CompoundTag();
        data.putInt("nivel", 5);
        data.putByte("activo", (byte) 1);
        data.putLong("serie", 5_000_000_000L);
        sword.set(DataComponents.CUSTOM_DATA, CustomData.of(data));
        items.set(0, sword);

        ItemStack shulker = new ItemStack(Items.SHULKER_BOX);
        shulker.set(DataComponents.CONTAINER, ItemContainerContents.fromItems(List.of(new ItemStack(Items.DIAMOND, 12))));
        items.set(20, shulker);

        items.set(36, new ItemStack(Items.IRON_BOOTS));
        items.set(39, new ItemStack(Items.IRON_HELMET));
        items.set(40, new ItemStack(Items.SHIELD));
        return new DeathRecord(UUID.fromString("aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee"), PLAYER, "Ana",
                Instant.parse("2026-09-30T04:12:00Z"), Level.NETHER, 10.5, 64.0, -20.25,
                "Ana was slain by Zombie", 17, items);
    }

    @Test
    void aDeathIsNotRestoredUntilItIsMarked() {
        assertFalse(sample().isRestored());
        assertTrue(sample().markRestored(Instant.parse("2026-10-01T00:00:00Z")).isRestored());
    }

    @Test
    void theRestoredMomentSurvivesTheFileAndIsOmittedWhenThereIsNone() {
        var ops = MinecraftTestSupport.ops();
        Instant moment = Instant.parse("2026-10-01T10:30:00Z");

        Tag restored = DeathRecord.CODEC.encodeStart(ops, sample().markRestored(moment)).getOrThrow();
        Tag untouched = DeathRecord.CODEC.encodeStart(ops, sample()).getOrThrow();

        assertEquals(java.util.Optional.of(moment), DeathRecord.CODEC.parse(ops, restored).getOrThrow().restoredAt());
        assertFalse(((CompoundTag) untouched).contains("restoredAt"));
        assertEquals(java.util.Optional.empty(), DeathRecord.CODEC.parse(ops, untouched).getOrThrow().restoredAt());
    }

    @Test
    void roundTripKeepsEverySlotExactlyIncludingGapsAndNumberTypes() {
        var ops = MinecraftTestSupport.ops();
        Tag tag = DeathRecord.CODEC.encodeStart(ops, sample()).getOrThrow();

        DeathRecord decoded = DeathRecord.CODEC.parse(ops, tag).getOrThrow();

        assertEquals(DeathRecord.SLOT_COUNT, decoded.items().size());
        for (int slot = 0; slot < DeathRecord.SLOT_COUNT; slot++) {
            assertTrue(ItemStack.matches(sample().items().get(slot), decoded.items().get(slot)), "slot " + slot);
        }
        assertEquals("{activo:1b,nivel:5,serie:5000000000L}",
                decoded.items().get(0).get(DataComponents.CUSTOM_DATA).copyTag().toString());
        assertEquals(sample().id(), decoded.id());
        assertEquals("Ana", decoded.playerName());
        assertEquals(Level.NETHER, decoded.dimension());
        assertEquals(17, decoded.xpLevel());
        assertEquals("Ana was slain by Zombie", decoded.cause());
    }

    @Test
    void nestedContainerContentsSurvive() {
        var ops = MinecraftTestSupport.ops();
        Tag tag = DeathRecord.CODEC.encodeStart(ops, sample()).getOrThrow();

        DeathRecord decoded = DeathRecord.CODEC.parse(ops, tag).getOrThrow();

        ItemContainerContents contents = decoded.items().get(20).get(DataComponents.CONTAINER);
        assertEquals(12, contents.nonEmptyItemCopyStream().findFirst().orElseThrow().getCount());
    }

    @Test
    void nonEmptyItemsKeepInventoryOrderAndSkipGaps() {
        List<ItemStack> present = sample().nonEmptyItems();

        assertEquals(5, present.size());
        assertEquals(Items.DIAMOND_SWORD, present.get(0).getItem());
        assertEquals(Items.SHULKER_BOX, present.get(1).getItem());
        assertEquals(Items.IRON_BOOTS, present.get(2).getItem());
        assertEquals(Items.SHIELD, present.get(4).getItem());
    }

    @Test
    void recordWithOnlyEmptySlotsHasNoItems() {
        List<ItemStack> empty = new ArrayList<>();
        for (int slot = 0; slot < DeathRecord.SLOT_COUNT; slot++) {
            empty.add(ItemStack.EMPTY);
        }
        DeathRecord record = new DeathRecord(UUID.randomUUID(), PLAYER, "Ana", Instant.now(), Level.OVERWORLD, 0, 0, 0,
                "x", 0, empty);

        assertFalse(record.hasItems());
        assertTrue(sample().hasItems());
    }

    @Test
    void onlyOccupiedSlotsAreWrittenToTheFile() {
        CompoundTag tag = (CompoundTag) DeathRecord.CODEC.encodeStart(MinecraftTestSupport.ops(), sample()).getOrThrow();

        var items = tag.getListOrEmpty("items");

        assertEquals(5, items.size(), "one entry per occupied slot, no entries for empty slots");
        assertEquals(0, items.getCompoundOrEmpty(0).getIntOr("slot", -1));
        assertEquals(20, items.getCompoundOrEmpty(1).getIntOr("slot", -1));
        assertEquals("minecraft:shield", items.getCompoundOrEmpty(4).getCompoundOrEmpty("item").getStringOr("id", ""));
    }

    @Test
    void aSlotOutsideTheInventoryIsAnError() {
        CompoundTag tag = (CompoundTag) DeathRecord.CODEC.encodeStart(MinecraftTestSupport.ops(), sample()).getOrThrow();
        tag.getListOrEmpty("items").getCompoundOrEmpty(0).putInt("slot", DeathRecord.SLOT_COUNT);

        assertTrue(DeathRecord.CODEC.parse(MinecraftTestSupport.ops(), tag).error().isPresent());
    }

    @Test
    void twoEntriesForTheSameSlotAreAnError() {
        CompoundTag tag = (CompoundTag) DeathRecord.CODEC.encodeStart(MinecraftTestSupport.ops(), sample()).getOrThrow();
        tag.getListOrEmpty("items").getCompoundOrEmpty(1).putInt("slot", 0);

        assertTrue(DeathRecord.CODEC.parse(MinecraftTestSupport.ops(), tag).error().isPresent());
    }

    @Test
    void aMissingFieldIsAnErrorNotAPartialRecord() {
        CompoundTag tag = (CompoundTag) DeathRecord.CODEC.encodeStart(MinecraftTestSupport.ops(), sample()).getOrThrow();
        tag.remove("items");

        assertTrue(DeathRecord.CODEC.parse(MinecraftTestSupport.ops(), tag).error().isPresent());
    }
}
