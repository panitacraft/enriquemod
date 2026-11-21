package com.panita.enriquecraft.staff.gui;

import com.panita.enriquecraft.staff.message.StaffMessages;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;

import java.util.List;

/**
 * A tool of the staff menu: the menu that opens, what it is called and which command grants access to it.
 * A section is offered only to players who may run that command, so the menu never gives more than the
 * commands do.
 */
enum StaffSection {

    COORDINATES(List.of("staff", "coords"), Items.COMPASS, StaffMessages.Staff.COORDINATES_NAME,
            StaffMessages.Staff.COORDINATES_LORE),
    ITEMS(List.of("staff", "item"), Items.ENCHANTED_BOOK, StaffMessages.Staff.ITEMS_NAME, StaffMessages.Staff.ITEMS_LORE),
    DEATHS(List.of("staff", "invrestore"), Items.SKELETON_SKULL, StaffMessages.Staff.DEATHS_NAME,
            StaffMessages.Staff.DEATHS_LORE);

    /** The command path that grants access, as the command tree names it. */
    private final List<String> command;
    private final Item icon;
    private final String title;
    private final String lore;

    StaffSection(List<String> command, Item icon, String name, String lore) {
        this.command = command;
        this.icon = icon;
        this.title = name;
        this.lore = lore;
    }

    List<String> command() {
        return command;
    }

    Item icon() {
        return icon;
    }

    String title() {
        return title;
    }

    String lore() {
        return lore;
    }
}
