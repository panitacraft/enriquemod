package com.panita.enriquecraft.core.framework.reload;

import net.minecraft.commands.CommandSourceStack;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

/**
 * Lets a module do something when staff run {@code /enriquecraft reload}, such as the link module syncing the
 * whitelist. The framework reloads its own files first, then calls the listeners with whoever ran the command.
 */
public final class ReloadHooks {

    private final List<Consumer<CommandSourceStack>> listeners = new CopyOnWriteArrayList<>();

    public void onReload(Consumer<CommandSourceStack> listener) {
        listeners.add(listener);
    }

    public void fire(CommandSourceStack source) {
        listeners.forEach(listener -> listener.accept(source));
    }
}
