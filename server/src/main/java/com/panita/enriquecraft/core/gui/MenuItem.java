package com.panita.enriquecraft.core.gui;

import net.minecraft.world.item.ItemStack;

import java.util.function.Consumer;

/**
 * What a menu slot shows and what happens when it is clicked.
 */
public record MenuItem(ItemStack stack, Consumer<MenuClick> action) {

    /** An item that only decorates: clicking it does nothing. */
    public static MenuItem display(ItemStack stack) {
        return new MenuItem(stack, click -> {
        });
    }

    public static MenuItem button(ItemStack stack, Consumer<MenuClick> action) {
        return new MenuItem(stack, action);
    }
}
