package com.panita.enriquecraft.staff.data;

import com.panita.enriquecraft.MinecraftTestSupport;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SavedItemTest {

    @BeforeAll
    static void startMinecraft() {
        MinecraftTestSupport.bootstrap();
    }

    private static SavedItem sample() {
        ItemStack stack = new ItemStack(Items.DIAMOND_SWORD, 1);
        stack.set(DataComponents.CUSTOM_NAME, Component.literal("Espada <especial>"));
        CompoundTag data = new CompoundTag();
        data.putString("custom_item", "enriquecraft:espada");
        data.putInt("nivel", 5);
        data.putByte("activo", (byte) 1);
        data.putLong("serie", 5_000_000_000L);
        data.putDouble("poder", 1.5);
        data.putFloat("peso", 2.5F);
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(data));
        return new SavedItem("espada", stack, UUID.fromString("11111111-2222-3333-4444-555555555555"), "Ana",
                Instant.parse("2026-09-30T04:12:00Z"));
    }

    @Test
    void roundTripKeepsTheItemExactlyIncludingEveryNumberType() {
        var ops = MinecraftTestSupport.ops();
        Tag tag = SavedItem.CODEC.encodeStart(ops, sample()).getOrThrow();

        SavedItem decoded = SavedItem.CODEC.parse(ops, tag).getOrThrow();

        assertTrue(ItemStack.matches(sample().stack(), decoded.stack()));
        CompoundTag data = decoded.stack().get(DataComponents.CUSTOM_DATA).copyTag();
        assertEquals("{activo:1b,custom_item:\"enriquecraft:espada\",nivel:5,peso:2.5f,poder:1.5d,serie:5000000000L}",
                data.toString(), "number types must not change");
        assertEquals("espada", decoded.name());
        assertEquals("Ana", decoded.savedByName());
        assertEquals(Instant.parse("2026-09-30T04:12:00Z"), decoded.savedAt());
    }

    @Test
    void fileFormatIsReadable() {
        CompoundTag tag = (CompoundTag) SavedItem.CODEC.encodeStart(MinecraftTestSupport.ops(), sample()).getOrThrow();

        assertEquals("minecraft:diamond_sword", tag.getCompoundOrEmpty("item").getStringOr("id", ""));
        assertEquals("2026-09-30T04:12:00Z", tag.getStringOr("savedAt", ""));
        assertEquals("11111111-2222-3333-4444-555555555555", tag.getStringOr("savedBy", ""));
    }

    @Test
    void anEmptyItemIsRejected() {
        CompoundTag tag = (CompoundTag) SavedItem.CODEC.encodeStart(MinecraftTestSupport.ops(), sample()).getOrThrow();
        tag.getCompoundOrEmpty("item").putString("id", "minecraft:air");

        assertTrue(SavedItem.CODEC.parse(MinecraftTestSupport.ops(), tag).error().isPresent());
    }
}
