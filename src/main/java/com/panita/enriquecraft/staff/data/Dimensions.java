package com.panita.enriquecraft.staff.data;

import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

/**
 * Readable names for dimensions.
 */
public final class Dimensions {

    private Dimensions() {
    }

    /** The name of a vanilla dimension; other dimensions show their identifier. */
    public static String displayName(ResourceKey<Level> dimension) {
        if (dimension.equals(Level.OVERWORLD)) {
            return "Overworld";
        }
        if (dimension.equals(Level.NETHER)) {
            return "Nether";
        }
        if (dimension.equals(Level.END)) {
            return "End";
        }
        return dimension.identifier().toString();
    }
}
