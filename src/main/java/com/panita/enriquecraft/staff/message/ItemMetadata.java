package com.panita.enriquecraft.staff.message;

import com.panita.enriquecraft.core.gui.MenuFactory;
import com.panita.enriquecraft.core.message.Message;
import com.panita.enriquecraft.core.ui.ItemIcons;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.component.TypedDataComponent;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
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
    private final List<List<Component>> groups = new ArrayList<>();
    private List<Component> group = new ArrayList<>();

    private ItemMetadata(MenuFactory factory) {
        this.factory = factory;
    }

    /**
     * What the item carries, in groups that belong together: how much of it there is, its enchantments, its
     * attributes, and the rest. A single group saying so when there is nothing to show.
     */
    public static List<List<Component>> groups(ItemStack stack, MenuFactory factory) {
        ItemMetadata metadata = new ItemMetadata(factory);
        metadata.describe(stack);
        metadata.groups.removeIf(List::isEmpty);
        if (metadata.groups.isEmpty()) {
            return List.of(List.of(factory.text(StaffMessages.Items.META_NONE)));
        }
        return metadata.groups.stream().map(List::copyOf).toList();
    }

    /** The same lines in one list, with a blank line between groups. */
    public static List<Component> lines(ItemStack stack, MenuFactory factory) {
        List<Component> lines = new ArrayList<>();
        for (List<Component> group : groups(stack, factory)) {
            if (!lines.isEmpty()) {
                lines.add(Component.empty());
            }
            lines.addAll(group);
        }
        return List.copyOf(lines);
    }

    /** Starts a new group; what follows is shown together. */
    private void newGroup() {
        group = new ArrayList<>();
        groups.add(group);
    }

    private void describe(ItemStack stack) {
        newGroup();
        if (stack.getCount() > 1 || stack.getMaxStackSize() > 1) {
            value(Items.BUNDLE, StaffMessages.Items.META_COUNT, stack.getCount() + " / " + stack.getMaxStackSize());
        }
        if (stack.getMaxDamage() > 0) {
            value(Items.DIAMOND_PICKAXE, StaffMessages.Items.META_DURABILITY, (stack.getMaxDamage() - stack.getDamageValue()) + " / " + stack.getMaxDamage());
        }
        newGroup();
        enchantments(StaffMessages.Items.META_ENCHANTMENTS, stack.get(DataComponents.ENCHANTMENTS));
        newGroup();
        enchantments(StaffMessages.Items.META_STORED_ENCHANTMENTS, stack.get(DataComponents.STORED_ENCHANTMENTS));
        newGroup();
        attributes(stack.get(DataComponents.ATTRIBUTE_MODIFIERS));
        newGroup();
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
        section(Items.ENCHANTED_BOOK, label);
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
        section(Items.ENCHANTED_GOLDEN_APPLE, StaffMessages.Items.META_ATTRIBUTES);
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

    /** A plain fact, led by the picture of an item that stands for it. */
    private void value(Item icon, String label, String value) {
        group.add(withIcon(icon, factory.text(Message.plain(StaffMessages.Items.META_LINE).with("label", label).with("value", value))));
    }

    private void detail(String label, String value) {
        group.add(factory.text(Message.plain(StaffMessages.Items.META_LINE_DETAIL).with("label", label).with("value", value)));
    }

    private void section(Item icon, String label) {
        group.add(withIcon(icon, factory.text(Message.plain(StaffMessages.Items.META_SECTION).with("label", label))));
    }

    private static Component withIcon(Item icon, Component line) {
        return Component.empty().append(ItemIcons.of(icon)).append(line);
    }

    /** A child of a section, in the color the template gives its kind. */
    private void line(String template, String value) {
        group.add(factory.text(Message.plain(template).with("value", value)));
    }
}
