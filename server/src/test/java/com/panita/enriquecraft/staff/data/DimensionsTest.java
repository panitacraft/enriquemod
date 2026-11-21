package com.panita.enriquecraft.staff.data;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.TextColor;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DimensionsTest {

    @Test
    void vanillaDimensionsHaveReadableNames() {
        assertEquals("Overworld", Dimensions.displayName(Level.OVERWORLD));
        assertEquals("Nether", Dimensions.displayName(Level.NETHER));
        assertEquals("End", Dimensions.displayName(Level.END));
    }

    @Test
    void otherDimensionsShowTheirIdentifier() {
        ResourceKey<Level> custom = ResourceKey.create(Registries.DIMENSION, Identifier.fromNamespaceAndPath("mymod", "moon"));

        assertEquals("mymod:moon", Dimensions.displayName(custom));
    }

    @Test
    void eachVanillaDimensionHasItsOwnColor() {
        assertEquals(TextColor.fromLegacyFormat(ChatFormatting.GREEN), Dimensions.coloredName(Level.OVERWORLD).getStyle().getColor());
        assertEquals(TextColor.fromLegacyFormat(ChatFormatting.RED), Dimensions.coloredName(Level.NETHER).getStyle().getColor());
        assertEquals(TextColor.fromLegacyFormat(ChatFormatting.LIGHT_PURPLE), Dimensions.coloredName(Level.END).getStyle().getColor());
    }

    @Test
    void otherDimensionsAreGrayAndShowTheirIdentifier() {
        ResourceKey<Level> custom = ResourceKey.create(Registries.DIMENSION, Identifier.fromNamespaceAndPath("mymod", "moon"));

        assertEquals("mymod:moon", Dimensions.coloredName(custom).getString());
        assertEquals(TextColor.fromLegacyFormat(ChatFormatting.GRAY), Dimensions.coloredName(custom).getStyle().getColor());
    }
}
