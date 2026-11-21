package com.panita.enriquecraft.core.framework.listener;

/**
 * A group of event subscriptions. Implementations inside a module's {@code listeners} package are
 * discovered automatically, built through constructor injection, and then asked to subscribe.
 */
public interface ModListener {

    /** Subscribes this listener to the events it handles. */
    void register();
}
