package com.panita.enriquecraft.core.item;

import com.panita.enriquecraft.MinecraftTestSupport;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CustomItemTagTest {

    @BeforeAll
    static void startMinecraft() {
        MinecraftTestSupport.bootstrap();
    }

    @Test
    void applyMarksTheItemWithTheNamespacedName() {
        ItemStack stack = new ItemStack(Items.DIAMOND_SWORD);

        CustomItemTag.apply(stack, "soulbound_relic");

        CompoundTag tag = stack.get(DataComponents.CUSTOM_DATA).copyTag();
        assertEquals(Optional.of("enriquecraft:soulbound_relic"), tag.getString("custom_item"));
    }

    @Test
    void nameOfAndIsReadTheMark() {
        ItemStack stack = new ItemStack(Items.DIAMOND_SWORD);
        CustomItemTag.apply(stack, "relic");

        assertEquals(Optional.of("relic"), CustomItemTag.nameOf(stack));
        assertTrue(CustomItemTag.is(stack, "relic"));
        assertFalse(CustomItemTag.is(stack, "other"));
    }

    @Test
    void applyingAgainReplacesTheMark() {
        ItemStack stack = new ItemStack(Items.DIAMOND_SWORD);
        CustomItemTag.apply(stack, "first");

        CustomItemTag.apply(stack, "second");

        assertEquals(Optional.of("second"), CustomItemTag.nameOf(stack));
    }

    @Test
    void otherCustomDataOnTheItemIsKept() {
        ItemStack stack = new ItemStack(Items.DIAMOND_SWORD);
        CompoundTag existing = new CompoundTag();
        existing.putString("other_mod", "value");
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(existing));

        CustomItemTag.apply(stack, "relic");

        CompoundTag tag = stack.get(DataComponents.CUSTOM_DATA).copyTag();
        assertEquals(Optional.of("value"), tag.getString("other_mod"));
        assertEquals(Optional.of("relic"), CustomItemTag.nameOf(stack));
    }

    @Test
    void itemsWithoutAMarkHaveNoName() {
        assertEquals(Optional.empty(), CustomItemTag.nameOf(new ItemStack(Items.DIAMOND)));
        assertFalse(CustomItemTag.is(new ItemStack(Items.DIAMOND), "relic"));
    }

    @Test
    void aMarkFromAnotherNamespaceIsIgnored() {
        ItemStack stack = new ItemStack(Items.DIAMOND);
        CompoundTag foreign = new CompoundTag();
        foreign.putString("custom_item", "othermod:relic");
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(foreign));

        assertEquals(Optional.empty(), CustomItemTag.nameOf(stack));
    }

    @Test
    void copiesKeepTheMark() {
        ItemStack stack = new ItemStack(Items.DIAMOND);
        CustomItemTag.apply(stack, "relic");

        assertTrue(CustomItemTag.is(stack.copy(), "relic"));
    }

    @Test
    void nameRules() {
        assertTrue(CustomItemTag.isValidName("relic"));
        assertTrue(CustomItemTag.isValidName("a_1"));
        assertTrue(CustomItemTag.isValidName("a".repeat(32)));
        assertFalse(CustomItemTag.isValidName(""));
        assertFalse(CustomItemTag.isValidName("a".repeat(33)));
        assertFalse(CustomItemTag.isValidName("Upper"));
        assertFalse(CustomItemTag.isValidName("with space"));
        assertFalse(CustomItemTag.isValidName("colon:name"));
        assertFalse(CustomItemTag.isValidName("dash-name"));
    }

    @Test
    void applyRejectsInvalidNames() {
        assertThrows(IllegalArgumentException.class, () -> CustomItemTag.apply(new ItemStack(Items.DIAMOND), "Bad Name"));
    }
}
