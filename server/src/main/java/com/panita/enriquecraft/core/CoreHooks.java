package com.panita.enriquecraft.core;

import com.panita.enriquecraft.core.dialog.DialogService;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import org.jspecify.annotations.Nullable;

import java.util.Optional;

/**
 * The way the core module's mixins reach its services. A mixin is instantiated by Mixin, not by the service
 * registry, so this is the one static door, installed once when the module loads. Without it every mixin leaves
 * vanilla behavior untouched.
 */
public final class CoreHooks {

    private static volatile @Nullable CoreHooks instance;

    private final DialogService dialogs;

    private CoreHooks(DialogService dialogs) {
        this.dialogs = dialogs;
    }

    static void install(DialogService dialogs) {
        instance = new CoreHooks(dialogs);
    }

    public static @Nullable CoreHooks get() {
        return instance;
    }

    /**
     * A client reported a custom click action, which the game gives no player. Returns whether it was a dialog's
     * answer, in which case the game must not handle it further.
     */
    public boolean customClickAction(ServerPlayer player, Identifier id, Optional<Tag> payload) {
        return dialogs.handleClick(player, id, payload);
    }
}
