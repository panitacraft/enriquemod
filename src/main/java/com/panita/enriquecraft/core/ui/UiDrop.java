package com.panita.enriquecraft.core.ui;

import net.minecraft.server.level.ServerPlayer;

/**
 * A draggable button dropped onto another, as the server's action sees it: the two buttons by the ids
 * {@link UiBuilder} gave them, which are valid for the description that was showing.
 */
public record UiDrop(ServerPlayer player, int draggedId, int targetId) {
}
