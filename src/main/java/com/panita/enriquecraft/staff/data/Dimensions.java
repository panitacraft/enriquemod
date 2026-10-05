package com.panita.enriquecraft.staff.data;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
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

    /** The name of a dimension in its own color: green, red and purple for the three vanilla ones, gray otherwise. */
    public static Component coloredName(ResourceKey<Level> dimension) {
        ChatFormatting color = ChatFormatting.GRAY;
        if (dimension.equals(Level.OVERWORLD)) {
            color = ChatFormatting.GREEN;
        } else if (dimension.equals(Level.NETHER)) {
            color = ChatFormatting.RED;
        } else if (dimension.equals(Level.END)) {
            color = ChatFormatting.LIGHT_PURPLE;
        }
        return Component.literal(displayName(dimension)).withStyle(color);
    }
}
