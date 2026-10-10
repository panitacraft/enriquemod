package com.panita.enriquecraft.auth.gui;

import com.panita.enriquecraft.auth.config.AuthConfig;
import com.panita.enriquecraft.auth.message.PinView;
import com.panita.enriquecraft.auth.service.PinFlow;
import com.panita.enriquecraft.auth.service.PinScreens;
import com.panita.enriquecraft.core.ui.UiService;
import net.minecraft.server.level.ServerPlayer;

/**
 * Opens the PIN screen for the players who have to enter one.
 */
public final class PinMenus implements PinScreens {

    private final UiService ui;
    private final PinView view;
    private final AuthConfig config;

    public PinMenus(UiService ui, PinView view, AuthConfig config) {
        this.ui = ui;
        this.view = view;
        this.config = config;
    }

    @Override
    public Screen open(ServerPlayer player, PinFlow flow, Runnable onDismissed) {
        PinMenu menu = new PinMenu(ui, player, flow, view, ui.factory().text(config.screenTitle.get()), onDismissed);
        menu.open(player);
        return menu;
    }
}
