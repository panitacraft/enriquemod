package com.panita.enriquecraft.core.ui;

import net.minecraft.server.level.ServerPlayer;

/**
 * An option a player chose in a dropdown of a {@link UiMenu}, the same whether it was picked in the
 * client companion's list or by clicking through the options of a chest item.
 *
 * @param option the position of the chosen option, always one the dropdown has
 */
public record UiSelect(ServerPlayer player, int option) {
}
