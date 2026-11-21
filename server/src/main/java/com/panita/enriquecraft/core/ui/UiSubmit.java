package com.panita.enriquecraft.core.ui;

import net.minecraft.server.level.ServerPlayer;

/**
 * A value a player confirmed in a text field of a {@link UiMenu}, the same whether it was typed in
 * the client companion's screen or in chat.
 *
 * @param text what the player typed; never longer than the field allows
 */
public record UiSubmit(ServerPlayer player, String text) {
}
