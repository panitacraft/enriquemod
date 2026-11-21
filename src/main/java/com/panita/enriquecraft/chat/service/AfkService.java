package com.panita.enriquecraft.chat.service;

import com.panita.enriquecraft.chat.ChatPermissions;
import com.panita.enriquecraft.chat.text.AfkColor;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextColor;
import net.minecraft.server.level.ServerPlayer;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.BooleanSupplier;
import java.util.function.IntSupplier;
import java.util.function.LongSupplier;
import java.util.function.Supplier;

/**
 * Knows who is AFK: a player whose last sign of activity is older than the timeout. Nothing is announced and nothing runs when the
 * state changes; the state is worked out when a line is built, so the rank and name simply come out in the AFK color while it lasts.
 * Only the server thread uses it.
 */
public final class AfkService {

    /** Smaller turns of the view are noise, not a player moving the mouse. */
    private static final float ROTATION_EPSILON = 0.01f;

    private static final class Activity {
        long lastActiveMillis;
        float yaw;
        float pitch;
        boolean rotationKnown;

        Activity(long now) {
            this.lastActiveMillis = now;
        }
    }

    private final Map<UUID, Activity> seen = new HashMap<>();
    private final LongSupplier clockMillis;
    private final BooleanSupplier enabled;
    private final IntSupplier timeoutSeconds;
    private final Supplier<String> color;

    public AfkService(LongSupplier clockMillis, BooleanSupplier enabled, IntSupplier timeoutSeconds, Supplier<String> color) {
        this.clockMillis = clockMillis;
        this.enabled = enabled;
        this.timeoutSeconds = timeoutSeconds;
        this.color = color;
    }

    /** The player did something: joined, spoke, used or attacked something. */
    public void touch(UUID id) {
        seen.computeIfAbsent(id, key -> new Activity(clockMillis.getAsLong())).lastActiveMillis = clockMillis.getAsLong();
    }

    /** Looks at where the player faces; turning the view counts as activity. Being pushed around does not, since it keeps the view. */
    public void observe(UUID id, float yaw, float pitch) {
        Activity activity = seen.computeIfAbsent(id, key -> new Activity(clockMillis.getAsLong()));
        if (activity.rotationKnown && (Math.abs(yaw - activity.yaw) > ROTATION_EPSILON || Math.abs(pitch - activity.pitch) > ROTATION_EPSILON)) {
            activity.lastActiveMillis = clockMillis.getAsLong();
        }
        activity.yaw = yaw;
        activity.pitch = pitch;
        activity.rotationKnown = true;
    }

    public void forget(UUID id) {
        seen.remove(id);
    }

    /** Whether the player has been inactive for the timeout. A player never seen is not idle. */
    public boolean isIdle(UUID id) {
        Activity activity = seen.get(id);
        return enabled.getAsBoolean() && activity != null
                && clockMillis.getAsLong() - activity.lastActiveMillis >= timeoutSeconds.getAsInt() * 1000L;
    }

    /** Whether the player shows as AFK: idle, and not one of those who never do. */
    public boolean isAfk(ServerPlayer player) {
        return isIdle(player.getUUID()) && !ChatPermissions.canBypassAfk(player);
    }

    /** The text in the AFK color. */
    public Component shade(Component text) {
        TextColor shade = AfkColor.parse(color.get()).orElseGet(() -> AfkColor.parse(AfkColor.DEFAULT).orElseThrow());
        return AfkColor.paint(text, shade);
    }
}
