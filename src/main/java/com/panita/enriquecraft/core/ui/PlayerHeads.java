package com.panita.enriquecraft.core.ui;

import com.mojang.authlib.GameProfile;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.contents.objects.PlayerSprite;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ResolvableProfile;

import java.util.UUID;

/**
 * A player's head, as an item and inside a line of text. The skin is looked up by whoever shows it,
 * so it follows the player's current skin, and a player whose skin cannot be found shows the default one.
 */
public final class PlayerHeads {

    private PlayerHeads() {
    }

    /** A player head item showing the skin of the given player. */
    public static ItemStack item(UUID player) {
        ItemStack head = new ItemStack(Items.PLAYER_HEAD);
        head.set(DataComponents.PROFILE, ResolvableProfile.createUnresolved(player));
        return head;
    }

    /** A player head item showing the skin in a profile the server holds, which needs no lookup by whoever shows it. */
    public static ItemStack item(GameProfile profile) {
        ItemStack head = new ItemStack(Items.PLAYER_HEAD);
        head.set(DataComponents.PROFILE, ResolvableProfile.createResolved(profile));
        return head;
    }

    /** The head, a space and then the name, to put in front of a player's name in a line of text. */
    public static MutableComponent inline(UUID player, Component name) {
        return Component.object(new PlayerSprite(ResolvableProfile.createUnresolved(player), true))
                .append(Component.literal(" "))
                .append(name);
    }
}
