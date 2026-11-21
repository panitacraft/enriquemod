package com.panita.enriquecraft.discord.service;

import java.time.Duration;

/**
 * Runs something later. The connection asks for it through this, so its timing can be tested without waiting.
 */
public interface Scheduler {

    /** A task that was scheduled and may still be called off. */
    interface Handle {
        void cancel();
    }

    Handle schedule(Duration delay, Runnable task);
}
