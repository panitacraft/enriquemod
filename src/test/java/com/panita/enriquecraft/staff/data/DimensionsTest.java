package com.panita.enriquecraft.staff.data;

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
}
