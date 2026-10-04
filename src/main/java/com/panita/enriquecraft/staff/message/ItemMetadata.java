package com.panita.enriquecraft.staff.message;

import com.panita.enriquecraft.core.gui.MenuFactory;
import com.panita.enriquecraft.core.message.Message;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.component.TypedDataComponent;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomModelData;
import net.minecraft.world.item.component.DyedItemColor;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.ItemEnchantments;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Turns what an item carries beyond its name and lore into lines staff can read: enchantments,
 * attributes, model data, hidden tooltip parts and the like. Everything the item customizes that has no
 * line of its own is listed by name, so nothing is silently left out.
 */
public final class ItemMetadata {

    /** Components that are shown elsewhere, are internal, or have a line of their own below. */
    private static final Set<DataComponentType<?>> NOT_LISTED_BY_NAME = Set.of(
            DataComponents.CUSTOM_NAME, DataComponents.ITEM_NAME, DataComponents.LORE, DataComponents.CUSTOM_DATA,
            DataComponents.ENCHANTMENTS, DataComponents.STORED_ENCHANTMENTS, DataComponents.ATTRIBUTE_MODIFIERS,
            DataComponents.CUSTOM_MODEL_DATA, DataComponents.ITEM_MODEL, DataComponents.DYED_COLOR,
            DataComponents.UNBREAKABLE, DataComponents.DAMAGE, DataComponents.MAX_DAMAGE, DataComponents.REPAIR_COST,
            DataComponents.TOOLTIP_DISPLAY, DataComponents.RARITY);

    private final MenuFactory factory;
    private final List<Component> lines = new ArrayList<>();

    private ItemMetadata(MenuFactory factory) {
        this.factory = factory;
    }

    /** The lines for an item; a single line saying so when there is nothing to show. */
    public static List<Component> lines(ItemStack stack, MenuFactory factory) {
        ItemMetadata metadata = new ItemMetadata(factory);
        metadata.describe(stack);
        if (metadata.lines.isEmpty()) {
            metadata.lines.add(factory.text(StaffMessages.Items.META_NONE));
        }
        return List.copyOf(metadata.lines);
    }

    private void describe(ItemStack stack) {
        if (stack.getCount() > 1 || stack.getMaxStackSize() > 1) {
            value(StaffMessages.Items.META_COUNT, stack.getCount() + " / " + stack.getMaxStackSize());
        }
        if (stack.getMaxDamage() > 0) {
            value(StaffMessages.Items.META_DURABILITY, (stack.getMaxDamage() - stack.getDamageValue()) + " / " + stack.getMaxDamage());
        }
        enchantments(StaffMessages.Items.META_ENCHANTMENTS, stack.get(DataComponents.ENCHANTMENTS));
        enchantments(StaffMessages.Items.META_STORED_ENCHANTMENTS, stack.get(DataComponents.STORED_ENCHANTMENTS));
        attributes(stack.get(DataComponents.ATTRIBUTE_MODIFIERS));
        modelData(stack.get(DataComponents.CUSTOM_MODEL_DATA));
        Object model = customized(stack, DataComponents.ITEM_MODEL);
        if (model != null) {
            detail(StaffMessages.Items.META_ITEM_MODEL, String.valueOf(model));
        }
        DyedItemColor dye = customized(stack, DataComponents.DYED_COLOR);
        if (dye != null) {
            detail(StaffMessages.Items.META_DYE, String.format(Locale.ROOT, "#%06X", dye.rgb() & 0xFFFFFF));
        }
        if (stack.has(DataComponents.UNBREAKABLE)) {
            detail(StaffMessages.Items.META_UNBREAKABLE, StaffMessages.Items.META_YES);
        }
        Integer repairCost = stack.get(DataComponents.REPAIR_COST);
        if (repairCost != null && repairCost > 0) {
            detail(StaffMessages.Items.META_REPAIR_COST, String.valueOf(repairCost));
        }
        Object rarity = customized(stack, DataComponents.RARITY);
        if (rarity != null) {
            detail(StaffMessages.Items.META_RARITY, String.valueOf(rarity).toLowerCase(Locale.ROOT));
        }
        tooltip(stack.get(DataComponents.TOOLTIP_DISPLAY));
        otherComponents(stack);
    }

    /** The value only when the item overrides it, so what every item of the kind has is not repeated. */
    private static <T> T customized(ItemStack stack, DataComponentType<T> type) {
        return stack.getComponentsPatch().split().added().get(type);
    }

    private void enchantments(String label, ItemEnchantments enchantments) {
        if (enchantments == null || enchantments.isEmpty()) {
            return;
        }
        section(label);
        enchantments.entrySet().forEach(entry -> {
            Holder<Enchantment> enchantment = entry.getKey();
            // Plain text: the game's own enchantment name carries a color of its own that would hide ours.
            line(StaffMessages.Items.META_ENCHANTMENT, Enchantment.getFullname(enchantment, entry.getIntValue()).getString());
        });
    }

    private void attributes(ItemAttributeModifiers modifiers) {
        if (modifiers == null || modifiers.modifiers().isEmpty()) {
            return;
        }
        section(StaffMessages.Items.META_ATTRIBUTES);
        for (ItemAttributeModifiers.Entry entry : modifiers.modifiers()) {
            AttributeModifier modifier = entry.modifier();
            line(StaffMessages.Items.META_ATTRIBUTE, attributeName(entry.attribute()) + " " + amount(modifier) + " ("
                    + entry.slot().getSerializedName() + ")");
        }
    }

    private static String attributeName(Holder<Attribute> attribute) {
        return attribute.unwrapKey().map(key -> key.identifier().getPath()).orElse("?");
    }

    /** A bonus as the player reads it: +4, or +10% for the operations that multiply. */
    private static String amount(AttributeModifier modifier) {
        double value = modifier.amount();
        boolean multiplies = modifier.operation() != AttributeModifier.Operation.ADD_VALUE;
        double shown = multiplies ? value * 100 : value;
        String number = shown == Math.rint(shown) ? String.valueOf((long) shown) : String.format(Locale.ROOT, "%.2f", shown);
        return (shown >= 0 ? "+" : "") + number + (multiplies ? "%" : "");
    }

    private void modelData(CustomModelData data) {
        if (data == null) {
            return;
        }
        List<String> parts = new ArrayList<>();
        if (!data.floats().isEmpty()) {
            parts.add("floats " + data.floats());
        }
        if (!data.flags().isEmpty()) {
            parts.add("flags " + data.flags());
        }
        if (!data.strings().isEmpty()) {
            parts.add("strings " + data.strings());
        }
        if (!data.colors().isEmpty()) {
            parts.add("colors " + data.colors());
        }
        detail(StaffMessages.Items.META_MODEL_DATA, parts.isEmpty() ? "-" : String.join(", ", parts));
    }

    private void tooltip(TooltipDisplay display) {
        if (display == null) {
            return;
        }
        if (display.hideTooltip()) {
            detail(StaffMessages.Items.META_TOOLTIP_HIDDEN, StaffMessages.Items.META_YES);
        }
        if (!display.hiddenComponents().isEmpty()) {
            detail(StaffMessages.Items.META_HIDDEN_COMPONENTS, display.hiddenComponents().stream()
                    .map(ItemMetadata::name).collect(Collectors.joining(", ")));
        }
    }

    private void otherComponents(ItemStack stack) {
        List<String> others = new ArrayList<>();
        for (TypedDataComponent<?> component : stack.getComponentsPatch().split().added()) {
            if (!NOT_LISTED_BY_NAME.contains(component.type())) {
                others.add(name(component.type()));
            }
        }
        if (!others.isEmpty()) {
            detail(StaffMessages.Items.META_OTHER, String.join(", ", others));
        }
    }

    private static String name(DataComponentType<?> type) {
        var key = BuiltInRegistries.DATA_COMPONENT_TYPE.getKey(type);
        return key == null ? "?" : key.toString();
    }

    private void value(String label, String value) {
        lines.add(factory.text(Message.plain(StaffMessages.Items.META_LINE).with("label", label).with("value", value)));
    }

    private void detail(String label, String value) {
        lines.add(factory.text(Message.plain(StaffMessages.Items.META_LINE_DETAIL).with("label", label).with("value", value)));
    }

    private void section(String label) {
        lines.add(factory.text(Message.plain(StaffMessages.Items.META_SECTION).with("label", label)));
    }

    /** A child of a section, in the color the template gives its kind. */
    private void line(String template, String value) {
        lines.add(factory.text(Message.plain(template).with("value", value)));
    }
}
