package com.panita.enriquecraft.staff.message;

import com.panita.enriquecraft.core.gui.MenuFactory;
import com.panita.enriquecraft.core.message.Message;
import com.panita.enriquecraft.core.message.Timestamps;
import com.panita.enriquecraft.staff.data.DeathRecord;
import com.panita.enriquecraft.staff.data.Dimensions;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * The readable lines about a death, shared by the list and the inspector so both say the same things the
 * same way: the list shows the summary, the inspector everything.
 */
public final class DeathInfo {

    private DeathInfo() {
    }

    /** What tells deaths apart at a glance: how, where, and whether the items were already returned. */
    public static List<Component> summary(DeathRecord record, MenuFactory factory) {
        List<Component> lines = new ArrayList<>();
        lines.add(factory.text(Message.plain(StaffMessages.Deaths.ENTRY_CAUSE).with("cause", record.cause())));
        lines.add(factory.text(Message.plain(StaffMessages.Deaths.ENTRY_DIMENSION)
                .with("dimension", Dimensions.coloredName(record.dimension()))));
        lines.add(factory.text(Message.plain(StaffMessages.Deaths.ENTRY_POSITION)
                .with("x", CoordinateView.block(record.x()))
                .with("y", CoordinateView.block(record.y()))
                .with("z", CoordinateView.block(record.z()))));
        restored(record, factory).ifPresent(lines::add);
        return lines;
    }

    /** The summary plus how much the player carried; the experience level only when there was some. */
    public static List<Component> full(DeathRecord record, MenuFactory factory) {
        List<Component> lines = new ArrayList<>(summary(record, factory));
        // Keep the restored line last, after the details it qualifies.
        Component restored = record.isRestored() ? lines.removeLast() : null;
        lines.add(factory.text(Message.plain(StaffMessages.Deaths.ENTRY_ITEMS).with("count", record.nonEmptyItems().size())));
        if (record.xpLevel() > 0) {
            lines.add(factory.text(Message.plain(StaffMessages.Deaths.ENTRY_XP).with("level", record.xpLevel())));
        }
        if (restored != null) {
            lines.add(restored);
        }
        return lines;
    }

    /** The name of a death, led by the skull and carrying the date and time. */
    public static Component name(DeathRecord record, MenuFactory factory) {
        return factory.text(Message.plain(StaffMessages.Deaths.ENTRY_NAME).with("date", Timestamps.dateTime(record.diedAt())));
    }

    private static Optional<Component> restored(DeathRecord record, MenuFactory factory) {
        return record.restoredAt().map(moment -> factory.text(
                Message.plain(StaffMessages.Deaths.ENTRY_RESTORED).with("date", Timestamps.dateTime(moment))));
    }
}
