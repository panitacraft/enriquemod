package com.panita.enriquecraft.core.item;

import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;

import java.util.Optional;
import java.util.regex.Pattern;

/**
 * Marks items as custom items of this mod. The mark is the {@code custom_item} entry of the
 * item's custom data, with the value {@code enriquecraft:<name>}, so it travels with the item
 * and other data on the item is left alone.
 */
public final class CustomItemTag {

    private static final String KEY = "custom_item";
    private static final String NAMESPACE_PREFIX = "enriquecraft:";
    private static final Pattern NAME = Pattern.compile("[a-z0-9_]{1,32}");

    private CustomItemTag() {
    }

    /** Whether a name can be used for a custom item: 1 to 32 lowercase letters, digits or underscores. */
    public static boolean isValidName(String name) {
        return NAME.matcher(name).matches();
    }

    /**
     * Marks a stack as the custom item with this name, replacing any earlier mark of this mod.
     *
     * @throws IllegalArgumentException if the name is not valid
     */
    public static void apply(ItemStack stack, String name) {
        if (!isValidName(name)) {
            throw new IllegalArgumentException("Invalid custom item name '" + name + "'");
        }
        CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> tag.putString(KEY, NAMESPACE_PREFIX + name));
    }

    /** The custom item name of a stack, or empty if it is not marked or is marked by something else. */
    public static Optional<String> nameOf(ItemStack stack) {
        CustomData data = stack.get(DataComponents.CUSTOM_DATA);
        if (data == null) {
            return Optional.empty();
        }
        return data.copyTag().getString(KEY)
                .filter(value -> value.startsWith(NAMESPACE_PREFIX))
                .map(value -> value.substring(NAMESPACE_PREFIX.length()));
    }

    public static boolean is(ItemStack stack, String name) {
        return nameOf(stack).filter(name::equals).isPresent();
    }
}
