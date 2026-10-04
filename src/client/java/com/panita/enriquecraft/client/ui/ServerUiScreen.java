package com.panita.enriquecraft.client.ui;

import com.panita.enriquecraft.core.network.UiClickC2S;
import com.panita.enriquecraft.core.network.UiClosedC2S;
import com.panita.enriquecraft.core.network.UiElement;
import com.panita.enriquecraft.core.network.UiSubmitC2S;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.StringWidget;
import net.minecraft.client.gui.layouts.FrameLayout;
import net.minecraft.client.gui.layouts.LinearLayout;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/**
 * A screen the server described. It shows what it was sent and reports presses and typed values;
 * everything that happens next is decided by the server.
 */
final class ServerUiScreen extends Screen implements UiActions {

    private static final int PADDING = 8;
    private static final int TITLE_SPACING = 8;
    private static final int PANEL_COLOR = 0xE0101010;
    private static final int BORDER_COLOR = 0xFF8B8B8B;

    private final int sessionId;
    private UiElement root;
    private LinearLayout content;

    ServerUiScreen(int sessionId, Component title, UiElement root) {
        super(title);
        this.sessionId = sessionId;
        this.root = root;
    }

    int sessionId() {
        return sessionId;
    }

    /** Shows a new description of the same screen. */
    void update(UiElement root) {
        this.root = root;
        rebuildWidgets();
    }

    @Override
    protected void init() {
        content = LinearLayout.vertical().spacing(TITLE_SPACING);
        content.addChild(new StringWidget(title, font), settings -> settings.alignHorizontallyCenter());
        content.addChild(new UiLayouts(font, this).build(root), settings -> settings.alignHorizontallyCenter());
        content.arrangeElements();
        FrameLayout.centerInRectangle(content, 0, 0, width, height);
        content.visitWidgets(this::addRenderableWidget);
        // Whoever opens a screen with a text field wants to type in it, and an update rebuilds the field.
        content.visitWidgets(widget -> {
            if (widget instanceof UiInputWidget && getFocused() == null) {
                setInitialFocus(widget);
            }
        });
    }

    @Override
    public void press(int elementId, int button, boolean shift) {
        ClientPlayNetworking.send(new UiClickC2S(sessionId, elementId, button, shift));
    }

    @Override
    public void submit(int elementId, String text) {
        ClientPlayNetworking.send(new UiSubmitC2S(sessionId, elementId, text));
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        int left = content.getX() - PADDING;
        int top = content.getY() - PADDING;
        int panelWidth = content.getWidth() + 2 * PADDING;
        int panelHeight = content.getHeight() + 2 * PADDING;
        graphics.fill(left, top, left + panelWidth, top + panelHeight, PANEL_COLOR);
        graphics.outline(left, top, panelWidth, panelHeight, BORDER_COLOR);
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
    }

    /** The world keeps running behind a server screen, as it does behind a chest. */
    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void removed() {
        super.removed();
        // Also runs when another screen replaces this one, and then the server ignores it as stale.
        if (minecraft.getConnection() != null && ClientPlayNetworking.canSend(UiClosedC2S.TYPE)) {
            ClientPlayNetworking.send(new UiClosedC2S(sessionId));
        }
    }
}
