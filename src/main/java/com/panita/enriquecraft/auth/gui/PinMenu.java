package com.panita.enriquecraft.auth.gui;

import com.panita.enriquecraft.auth.message.PinView;
import com.panita.enriquecraft.auth.service.PinFlow;
import com.panita.enriquecraft.auth.service.PinScreens;
import com.panita.enriquecraft.core.network.UiElement;
import com.panita.enriquecraft.core.ui.PinKeys;
import com.panita.enriquecraft.core.ui.PlayerHeads;
import com.panita.enriquecraft.core.ui.UiBuilder;
import com.panita.enriquecraft.core.ui.UiMenu;
import com.panita.enriquecraft.core.ui.UiService;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.item.ItemStack;

/**
 * The PIN screen. The client companion shows the keypad element; everyone else gets the same keypad laid
 * out as a crafting table. Both press the same keys of the same {@link PinFlow}, which is what holds the
 * digits, and both are drawn again whenever it changes.
 */
final class PinMenu extends UiMenu implements PinScreens.Screen {

    private final ServerPlayer player;
    private final PinFlow flow;
    private final PinView view;
    private final Component title;
    private final Runnable onDismissed;
    private final PinKeys keys = new PinKeys() {
        @Override
        public void digit(int digit) {
            flow.digit(digit);
            redraw();
        }

        @Override
        public void backspace() {
            flow.backspace();
            redraw();
        }

        @Override
        public void clear() {
            flow.clear();
            redraw();
        }
    };
    private PinKeypadMenu keypad;

    PinMenu(UiService ui, ServerPlayer player, PinFlow flow, PinView view, Component title, Runnable onDismissed) {
        super(ui, null);
        this.player = player;
        this.flow = flow;
        this.view = view;
        this.title = title;
        this.onDismissed = onDismissed;
    }

    @Override
    protected Component title() {
        return title;
    }

    @Override
    protected ItemStack icon() {
        return PlayerHeads.item(player.getUUID());
    }

    @Override
    protected UiElement describe(UiBuilder builder) {
        PinView.Texts texts = view.of(flow);
        int maxAttempts = flow.maxAttempts();
        return builder.pinPad(texts.title(), texts.hint(), flow.shownLength(), flow.shownDigits(),
                maxAttempts == 0 ? 0 : flow.attemptsLeft(), maxAttempts, flow.shakes(), flow.success(), keys);
    }

    @Override
    protected boolean openFallback(ServerPlayer opening) {
        opening.openMenu(new SimpleMenuProvider((containerId, inventory, ignored) -> {
            keypad = new PinKeypadMenu(containerId, keys, this::keypadClosed);
            keypad.show(PinKeypadLayout.render(ui().factory(), view.of(flow), flow));
            return keypad;
        }, title));
        return true;
    }

    private void keypadClosed() {
        keypad = null;
        onDismissed.run();
    }

    @Override
    protected void onClose() {
        onDismissed.run();
    }

    @Override
    public void redraw() {
        refresh();
        if (keypad != null) {
            keypad.show(PinKeypadLayout.render(ui().factory(), view.of(flow), flow));
            keypad.broadcastChanges();
        }
    }

    @Override
    public void close() {
        ui().close(player);
    }
}
