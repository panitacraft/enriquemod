package com.panita.enriquecraft.core.event;

import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;

/**
 * What happens in the core module, for the modules that follow it.
 */
public final class CoreEvents {

    private CoreEvents() {
    }

    /** Staff sent a message to everyone with {@code /broadcast}. Called on the server thread, with the text as they wrote it. */
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
