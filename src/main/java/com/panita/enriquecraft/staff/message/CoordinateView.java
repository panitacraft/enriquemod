package com.panita.enriquecraft.staff.message;

import com.panita.enriquecraft.core.message.Message;
import com.panita.enriquecraft.core.message.Messenger;
import com.panita.enriquecraft.core.ui.CopyText;
import com.panita.enriquecraft.staff.data.Dimensions;
import com.panita.enriquecraft.staff.data.SavedCoordinate;
import com.panita.enriquecraft.staff.service.CoordinateService;
import com.panita.enriquecraft.staff.service.Teleporter;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;

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

    /** The block a position is in, which is what staff read in menus; the exact position stays in the data. */
    public static int block(double value) {
        return (int) Math.floor(value);
    }

    /** The block position as staff read it, which a click copies as "x y z", ready to paste into a command. */
    public static Component position(double x, double y, double z) {
        int blockX = block(x);
        int blockY = block(y);
        int blockZ = block(z);
        return CopyText.of(Component.literal(blockX + ", " + blockY + ", " + blockZ), blockX + " " + blockY + " " + blockZ);
    }

    /** The dimension in its color, which a click copies as its id. */
    public static Component dimension(ResourceKey<Level> dimension) {
        return CopyText.of(Dimensions.coloredName(dimension), dimension.identifier().toString());
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
