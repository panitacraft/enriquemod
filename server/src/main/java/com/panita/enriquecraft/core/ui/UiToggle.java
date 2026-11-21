package com.panita.enriquecraft.core.ui;

import net.minecraft.server.level.ServerPlayer;

/**
 * The state a player gave a toggle of a {@link UiMenu}, the same whether the box was pressed in the client
 * companion's screen or the item was clicked in a chest.
 *
 * @param checked the state the toggle should have now, not the one it had
 */
public record UiToggle(ServerPlayer player, boolean checked) {
}
