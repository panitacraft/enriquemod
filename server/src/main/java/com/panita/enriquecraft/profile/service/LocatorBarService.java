package com.panita.enriquecraft.profile.service;

import com.panita.enriquecraft.Enriquecraft;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;

/**
 * Hides a player from the locator bar of the others, or shows them again. It zeroes the player's waypoint transmit range, the
 * way the game does for a mob head; the game itself then stops tracking the player (it checks dirty attributes on the player's
 * next tick), so nothing has to be untracked here. The modifier is transient: a new player entity (join, respawn) starts without
 * it, so the choice is applied again from {@link #refresh}.
 */
public final class LocatorBarService {

    static final Identifier HIDDEN_ID = Identifier.fromNamespaceAndPath(Enriquecraft.MOD_ID, "hidden_from_locator");
    static final AttributeModifier HIDDEN = new AttributeModifier(HIDDEN_ID, -1.0, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);

    private final LocatorBarPreference preference;

    public LocatorBarService(LocatorBarPreference preference) {
        this.preference = preference;
    }

    /** Shows the player on the locator bar of the others when {@code shared} is true, hides them otherwise. Safe to repeat. */
    public void apply(ServerPlayer player, boolean shared) {
        AttributeInstance range = player.getAttribute(Attributes.WAYPOINT_TRANSMIT_RANGE);
        if (range != null) {
            applyTo(range, shared);
        }
    }

    static void applyTo(AttributeInstance range, boolean shared) {
        if (shared) {
            range.removeModifier(HIDDEN_ID);
        } else {
            range.addOrUpdateTransientModifier(HIDDEN);
        }
    }

    /** Applies the choice the player has saved. */
    public void refresh(ServerPlayer player) {
        apply(player, preference.isShared(player));
    }
}
