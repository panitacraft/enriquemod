package com.panita.enriquecraft.core.ui;

import com.panita.enriquecraft.core.gui.MenuFactory;
import com.panita.enriquecraft.core.message.Message;
import com.panita.enriquecraft.core.message.Messages;
import net.minecraft.network.chat.Component;

/**
 * The lines of a tooltip that say what each click does, written the same way in every menu: a
 * pastel green for the left click and a pastel blue for the right click.
 */
public final class ClickHints {

    private ClickHints() {
    }

    /** "Clic Izq. para {action}". */
    public static Component left(MenuFactory factory, String action) {
        return factory.text(Message.plain(Messages.Gui.CLICK_LEFT).with("action", action));
    }

    /** "Clic Der. para {action}". */
    public static Component right(MenuFactory factory, String action) {
        return factory.text(Message.plain(Messages.Gui.CLICK_RIGHT).with("action", action));
    }
}
