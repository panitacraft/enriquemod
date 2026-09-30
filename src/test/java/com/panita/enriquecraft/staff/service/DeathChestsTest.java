package com.panita.enriquecraft.staff.service;

import com.panita.enriquecraft.MinecraftTestSupport;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.ItemContainerContents;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DeathChestsTest {

    private static final DeathChests.Namer NAMER = (number, total) -> Component.literal(number + "/" + total);

    @BeforeAll
    static void startMinecraft() {
        MinecraftTestSupport.bootstrap();
    }

    private static List<ItemStack> stacks(int count) {
        List<ItemStack> items = new ArrayList<>();
        for (int i = 1; i <= count; i++) {
            items.add(new ItemStack(Items.DIAMOND, i));
        }
        return items;
    }

    private static List<ItemStack> contentsOf(ItemStack chest) {
        return chest.get(DataComponents.CONTAINER).nonEmptyItemCopyStream().toList();
    }

    @Test
    void noItemsMakeNoChests() {
        assertEquals(List.of(), DeathChests.pack(List.of(), NAMER));
    }

    @Test
    void upToTwentySevenStacksFitInOneChest() {
        List<ItemStack> chests = DeathChests.pack(stacks(27), NAMER);

        assertEquals(1, chests.size());
        assertEquals(Items.CHEST, chests.get(0).getItem());
        assertEquals(27, contentsOf(chests.get(0)).size());
    }

    @Test
    void aFullInventoryOfFortyOneStacksMakesTwoChestsWithNothingLost() {
        List<ItemStack> chests = DeathChests.pack(stacks(41), NAMER);

        assertEquals(2, chests.size());
        assertEquals(27, contentsOf(chests.get(0)).size());
        assertEquals(14, contentsOf(chests.get(1)).size());
        List<ItemStack> all = new ArrayList<>(contentsOf(chests.get(0)));
        all.addAll(contentsOf(chests.get(1)));
        for (int i = 0; i < 41; i++) {
            assertEquals(i + 1, all.get(i).getCount(), "stack " + i + " keeps its place and count");
        }
    }

    @Test
    void chestsAreNamedWithTheirPosition() {
        List<ItemStack> chests = DeathChests.pack(stacks(30), NAMER);

        assertEquals("1/2", chests.get(0).get(DataComponents.CUSTOM_NAME).getString());
        assertEquals("2/2", chests.get(1).get(DataComponents.CUSTOM_NAME).getString());
    }

    @Test
    void packedItemsAreCopies() {
        List<ItemStack> items = stacks(3);

        List<ItemStack> chests = DeathChests.pack(items, NAMER);
        items.get(0).setCount(64);

        assertEquals(1, contentsOf(chests.get(0)).get(0).getCount());
        assertEquals(64, items.get(0).getCount());
    }

    @Test
    void exactComponentsSurviveInsideTheChestAndAfterSavingIt() {
        ItemStack special = new ItemStack(Items.DIAMOND_SWORD);
        CompoundTag data = new CompoundTag();
        data.putInt("nivel", 5);
        data.putByte("activo", (byte) 1);
        special.set(DataComponents.CUSTOM_DATA, CustomData.of(data));
        ItemStack chest = DeathChests.pack(List.of(special), NAMER).get(0);

        var ops = MinecraftTestSupport.ops();
        Tag tag = ItemStack.CODEC.encodeStart(ops, chest).getOrThrow();
        ItemStack reloaded = ItemStack.CODEC.parse(ops, tag).getOrThrow();

        ItemContainerContents contents = reloaded.get(DataComponents.CONTAINER);
        ItemStack inside = contents.nonEmptyItemCopyStream().findFirst().orElseThrow();
        assertTrue(ItemStack.matches(special, inside));
        assertEquals("{activo:1b,nivel:5}", inside.get(DataComponents.CUSTOM_DATA).copyTag().toString());
    }
}
