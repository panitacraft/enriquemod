package com.panita.enriquecraft.core.message;

import com.panita.enriquecraft.core.service.TpsMonitor;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;

import java.util.Locale;

/**
 * Shows the ticks per second as Spigot's {@code /tps} does: each average rounded to two decimals and never above 20, in green
 * above 18, yellow above 16 and red below, with an asterisk when the server ran clearly faster than 20. Presentation only.
 */
public final class TpsView {

    private static final double GREEN_ABOVE = 18.0;
    private static final double YELLOW_ABOVE = 16.0;
    private static final double ASTERISK_ABOVE = 21.0;

    private final Messenger messenger;

    public TpsView(Messenger messenger) {
        this.messenger = messenger;
    }

    public void show(CommandSourceStack source, TpsMonitor.Averages averages) {
        messenger.send(source, Message.plain(Messages.Tps.RESULT).prefixed()
                .with("one", value(averages.oneMinute()))
                .with("five", value(averages.fiveMinutes()))
                .with("fifteen", value(averages.fifteenMinutes())));
    }

    static Component value(double tps) {
        double shown = Math.min(Math.round(tps * 100.0) / 100.0, TpsMonitor.TARGET);
        String text = (tps > ASTERISK_ABOVE ? "*" : "") + String.format(Locale.ROOT, "%.2f", shown);
        return Component.literal(text).withStyle(colorOf(tps));
    }

    private static ChatFormatting colorOf(double tps) {
        if (tps > GREEN_ABOVE) {
            return ChatFormatting.GREEN;
        }
        return tps > YELLOW_ABOVE ? ChatFormatting.YELLOW : ChatFormatting.RED;
    }
}
