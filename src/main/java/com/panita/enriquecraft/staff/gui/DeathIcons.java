package com.panita.enriquecraft.staff.gui;

import com.panita.enriquecraft.core.ui.PlayerHeads;
import com.panita.enriquecraft.staff.data.DeathRecord;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.time.Duration;
import java.time.Instant;

/**
 * The icon of a death, which ages: the player's head while it is recent, a skeleton skull after a day, and a
 * wither skeleton skull after three days, so old deaths read as old without opening them.
 */
final class DeathIcons {

    static final Duration SKULL_AFTER = Duration.ofHours(24);
    static final Duration WITHER_AFTER = Duration.ofHours(72);

    private DeathIcons() {
    }

    static ItemStack of(DeathRecord record, Instant now) {
        Duration age = Duration.between(record.diedAt(), now);
        if (age.compareTo(WITHER_AFTER) >= 0) {
            return new ItemStack(Items.WITHER_SKELETON_SKULL);
        }
        if (age.compareTo(SKULL_AFTER) >= 0) {
            return new ItemStack(Items.SKELETON_SKULL);
        }
        return PlayerHeads.item(record.player());
    }
}
