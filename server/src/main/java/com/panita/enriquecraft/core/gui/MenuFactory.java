package com.panita.enriquecraft.core.gui;

import com.panita.enriquecraft.core.message.Message;
import com.panita.enriquecraft.core.message.MessageFormatter;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * Creates the texts and items that menus are made of. Texts use the same tags and color codes as
 * chat messages, and never appear in italics, which Minecraft would otherwise apply to item names
 * and lore.
 */
public final class MenuFactory {

    private final MessageFormatter formatter;
    private ItemStack filler;

    public MenuFactory(MessageFormatter formatter) {
        this.formatter = formatter;
    }

    public Component text(String template) {
        return text(Message.plain(template));
    }

    public Component text(Message message) {
        MutableComponent upright = Component.empty().withStyle(Style.EMPTY.withItalic(false));
        return upright.append(formatter.format(message));
    }

    public ItemBuilder item(Item item) {
        return new ItemBuilder(this, new ItemStack(item));
    }

    /** Starts from a copy of an existing stack, keeping all its components. */
    public ItemBuilder item(ItemStack stack) {
        return new ItemBuilder(this, stack.copy());
    }

    /** The decorative item used for frames: a pane with no name and no tooltip. */
    ItemStack filler() {
        if (filler == null) {
            filler = item(Items.STAINED_GLASS_PANE.black()).hideTooltip().build();
        }
        return filler;
    }
}
