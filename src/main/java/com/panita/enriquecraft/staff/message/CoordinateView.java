package com.panita.enriquecraft.staff.message;

import com.panita.enriquecraft.core.message.Message;
import com.panita.enriquecraft.core.message.Messenger;
import com.panita.enriquecraft.staff.data.Dimensions;
import com.panita.enriquecraft.staff.data.SavedCoordinate;
import com.panita.enriquecraft.staff.service.CoordinateService;
import com.panita.enriquecraft.staff.service.Teleporter;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.level.ServerPlayer;

import java.util.Locale;

/**
 * Tells staff what happened when they use saved coordinates, whether from a command or a menu.
 */
public final class CoordinateView {

    private final Messenger messenger;
    private final CoordinateService service;

    public CoordinateView(Messenger messenger, CoordinateService service) {
        this.messenger = messenger;
        this.service = service;
    }

    /** Formats a coordinate value with two decimals, the same everywhere it is shown. */
    public static String number(double value) {
        return String.format(Locale.ROOT, "%.2f", value);
    }

    public void saved(CommandSourceStack source, SavedCoordinate coordinate) {
        messenger.send(source, Message.success(StaffMessages.Coordinates.SAVED).prefixed()
                .with("name", coordinate.name())
                .with("dimension", Dimensions.displayName(coordinate.dimension()))
                .with("x", number(coordinate.x()))
                .with("y", number(coordinate.y()))
                .with("z", number(coordinate.z())));
    }

    public void rejected(CommandSourceStack source, String name, CoordinateService.AddResult result) {
        String template = result == CoordinateService.AddResult.DUPLICATE
                ? StaffMessages.Coordinates.DUPLICATE
                : StaffMessages.Coordinates.INVALID_NAME;
        messenger.send(source, Message.error(template).prefixed().with("name", name));
    }

    public void removed(CommandSourceStack source, String name) {
        messenger.send(source, Message.success(StaffMessages.Coordinates.REMOVED).prefixed().with("name", name));
    }

    public void notFound(CommandSourceStack source, String name) {
        messenger.send(source, Message.error(StaffMessages.Coordinates.NOT_FOUND).prefixed().with("name", name));
    }

    /** Moves the player to the coordinate and tells them the outcome. */
    public void teleport(ServerPlayer player, SavedCoordinate coordinate) {
        if (service.teleport(player, coordinate) == Teleporter.Result.TELEPORTED) {
            messenger.send(player, Message.success(StaffMessages.Coordinates.TELEPORTED).prefixed()
                    .with("name", coordinate.name()));
        } else {
            messenger.send(player, Message.error(StaffMessages.Coordinates.DIMENSION_UNAVAILABLE).prefixed()
                    .with("name", coordinate.name())
                    .with("dimension", Dimensions.displayName(coordinate.dimension())));
        }
    }
}
