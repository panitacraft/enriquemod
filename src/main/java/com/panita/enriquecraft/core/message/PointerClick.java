package com.panita.enriquecraft.core.message;

import com.panita.enriquecraft.Enriquecraft;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.resources.Identifier;

import java.util.Optional;

/**
 * A click that does nothing. The game shows a pointing cursor only over text that has a click event, so text that only has a tooltip
 * (an emblem, a rank) gets this one to be pointed at like something clickable. The client sends the id to the server, which ignores
 * it (the game only logs it at debug level), so it never fills the chat box, copies anything or runs anything.
 */
public final class PointerClick {

    public static final ClickEvent NOTHING = new ClickEvent.Custom(Identifier.fromNamespaceAndPath(Enriquecraft.MOD_ID, "none"), Optional.empty());

    private PointerClick() {
    }
}
