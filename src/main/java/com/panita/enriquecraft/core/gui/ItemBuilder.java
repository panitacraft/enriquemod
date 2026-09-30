package com.panita.enriquecraft.core.gui;

import com.panita.enriquecraft.core.message.Message;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemLore;
import net.minecraft.world.item.component.TooltipDisplay;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;

/**
 * Fluent builder for the items shown in menus. Get one from {@link MenuFactory}.
 */
public final class ItemBuilder {

    private final MenuFactory factory;
    private final ItemStack stack;
    private final List<Component> lore = new ArrayList<>();
    private boolean loreChanged;

    ItemBuilder(MenuFactory factory, ItemStack stack) {
        this.factory = factory;
        this.stack = stack;
    }

    public ItemBuilder name(String template) {
        return name(Message.plain(template));
    }

    public ItemBuilder name(Message message) {
        stack.set(DataComponents.CUSTOM_NAME, factory.text(message));
        return this;
    }

    /** Adds one line of lore per template, after any lore already present. */
    public ItemBuilder lore(String... templates) {
        for (String template : templates) {
            addLore(factory.text(template));
        }
        return this;
    }

    public ItemBuilder lore(Message... messages) {
        for (Message message : messages) {
            addLore(factory.text(message));
        }
        return this;
    }

    public ItemBuilder lore(List<String> templates) {
        return lore(templates.toArray(String[]::new));
    }

    public ItemBuilder amount(int amount) {
        stack.setCount(amount);
        return this;
    }

    public ItemBuilder glint() {
        stack.set(DataComponents.ENCHANTMENT_GLINT_OVERRIDE, true);
        return this;
    }

    /** Hides the whole tooltip, name included. */
    public ItemBuilder hideTooltip() {
        stack.set(DataComponents.TOOLTIP_DISPLAY, new TooltipDisplay(true, new LinkedHashSet<>()));
        return this;
    }

    public ItemStack build() {
        if (loreChanged) {
            List<Component> existing = new ArrayList<>(stack.getOrDefault(DataComponents.LORE, ItemLore.EMPTY).lines());
            existing.addAll(lore);
            stack.set(DataComponents.LORE, new ItemLore(existing));
        }
        return stack;
    }

    private void addLore(Component line) {
        lore.add(line);
        loreChanged = true;
    }
}
