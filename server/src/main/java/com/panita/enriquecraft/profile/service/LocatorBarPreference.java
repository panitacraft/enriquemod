package com.panita.enriquecraft.profile.service;

import net.minecraft.server.level.ServerPlayer;

/** Where the saved choice of a player about the locator bar comes from, so the service does not depend on the store behind it. */
@FunctionalInterface
public interface LocatorBarPreference {

    /** Whether the player lets others see them on their locator bar. */
    boolean isShared(ServerPlayer player);
}
