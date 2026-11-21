package com.panita.enriquecraft.core.event;

import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;

/**
 * What happens in the core module, for the modules that follow it.
 */
public final class CoreEvents {

    private CoreEvents() {
    }

    /**
     * A message was sent to everyone: staff's {@code /broadcast}, or a module's announcement such as a prank level change. Called on
     * the server thread, with the text as it was written, which may carry text tags.
     */
    public static final Event<Broadcast> BROADCAST = EventFactory.createArrayBacked(Broadcast.class,
            listeners -> text -> {
                for (Broadcast listener : listeners) {
                    listener.onBroadcast(text);
                }
            });

    @FunctionalInterface
    public interface Broadcast {

        void onBroadcast(String text);
    }
}
