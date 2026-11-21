package com.panita.enriquecraft.discord.service;

import java.time.Duration;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * The scheduler of the real server: one daemon thread, which never keeps the server from stopping.
 */
public final class ExecutorScheduler implements Scheduler {

    private final ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor(task -> {
        Thread thread = new Thread(task, "enriquecraft-discord");
        thread.setDaemon(true);
        return thread;
    });

    @Override
    public Handle schedule(Duration delay, Runnable task) {
        ScheduledFuture<?> future = executor.schedule(task, delay.toMillis(), TimeUnit.MILLISECONDS);
        return () -> future.cancel(false);
    }
}
