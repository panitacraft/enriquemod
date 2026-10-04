package com.panita.enriquecraft.client.ui;

import com.panita.enriquecraft.core.network.UiClickC2S;
import com.panita.enriquecraft.core.network.UiClosedC2S;
import com.panita.enriquecraft.core.network.UiElement;
import com.panita.enriquecraft.core.network.UiSubmitC2S;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.layouts.FrameLayout;
import net.minecraft.client.gui.layouts.LayoutElement;
import net.minecraft.client.gui.layouts.LinearLayout;
import net.minecraft.client.gui.layouts.Layout;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.List;

/**
 * A screen the server described, laid out like a modal: a header with the title and a close button,
 * the body, and a footer with the search field on the left and the page buttons on the right. It
 * shows what it was sent and reports presses and typed values; everything that happens next is
 * decided by the server.
 */
final class ServerUiScreen extends Screen implements UiActions {

    private static final int PADDING = 12;
    private static final int SECTION_SPACING = 12;
    private static final int GROUP_GAP = 16;
    private static final int MIN_WIDTH = 180;
    private static final int HEADER_HEIGHT = 24;
    private static final int FOOTER_HEIGHT = 20;
    private static final int ACCENT_WIDTH = 28;

    private final int sessionId;
    private UiElement root;
    private LinearLayout content;
    private FrameLayout header;
    private FrameLayout footer;

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
        ScreenParts parts = ScreenParts.split(root);
        UiLayouts layouts = new UiLayouts(font, this);

        LinearLayout headerStart = LinearLayout.horizontal().spacing(8);
        if (parts.back() != null) {
            headerStart.addChild(UiButtonWidget.glyph(font, "←", parts.back().label(), false, true,
                    (mouse, shift) -> press(parts.back().id(), mouse, shift)), settings -> settings.alignVerticallyMiddle());
        }
        headerStart.addChild(new UiTitleWidget(font, title), settings -> settings.alignVerticallyMiddle());
        headerStart.arrangeElements();
        UiButtonWidget close = UiButtonWidget.glyph(font, "✕", null, true, true, (mouse, shift) -> onClose());

        LayoutElement body = layouts.build(parts.body());
        arrange(body);

        LinearLayout footerStart = layouts.row(List.<UiElement>copyOf(parts.inputs()));
        LinearLayout footerMiddle = layouts.row(parts.actions());
        LinearLayout footerEnd = pager(parts);
        arrange(footerStart);
        arrange(footerMiddle);
        arrange(footerEnd);

        int footerWidth = footerStart.getWidth() + footerMiddle.getWidth() + footerEnd.getWidth() + 2 * GROUP_GAP;
        int headerWidth = headerStart.getWidth() + GROUP_GAP + close.getWidth();
        int contentWidth = Math.max(MIN_WIDTH, Math.max(body.getWidth(), Math.max(headerWidth, footerWidth)));

        header = new FrameLayout(contentWidth, HEADER_HEIGHT);
        header.addChild(headerStart, settings -> settings.alignHorizontallyLeft().alignVerticallyMiddle());
        header.addChild(close, settings -> settings.alignHorizontallyRight().alignVerticallyMiddle());

        FrameLayout bodyFrame = new FrameLayout(contentWidth, 0);
        bodyFrame.addChild(body, settings -> settings.alignHorizontallyCenter());

        content = LinearLayout.vertical().spacing(SECTION_SPACING);
        content.addChild(header);
        content.addChild(bodyFrame);
        footer = null;
        if (footerWidth > 2 * GROUP_GAP) {
            footer = new FrameLayout(contentWidth, FOOTER_HEIGHT);
            footer.addChild(footerStart, settings -> settings.alignHorizontallyLeft().alignVerticallyMiddle());
            footer.addChild(footerMiddle, settings -> settings.alignHorizontallyCenter().alignVerticallyMiddle());
            footer.addChild(footerEnd, settings -> settings.alignHorizontallyRight().alignVerticallyMiddle());
            content.addChild(footer);
        }
        content.arrangeElements();
        FrameLayout.centerInRectangle(content, 0, 0, width, height);
        content.visitWidgets(this::addRenderableWidget);
    }

    /** The page buttons with the page indicator between them; empty when the screen is not a list. */
    private LinearLayout pager(ScreenParts parts) {
        LinearLayout pager = LinearLayout.horizontal().spacing(6);
        if (parts.page() == null) {
            return pager;
        }
        UiElement.Button previous = parts.previous();
        UiElement.Button next = parts.next();
        pager.addChild(UiButtonWidget.glyph(font, "<", previous == null ? null : previous.label(), false, previous != null,
                (mouse, shift) -> press(previous.id(), mouse, shift)), settings -> settings.alignVerticallyMiddle());
        pager.addChild(new UiLayouts(font, this).text(Component.literal(parts.page().page() + " / " + parts.page().pages())),
                settings -> settings.alignVerticallyMiddle());
        pager.addChild(UiButtonWidget.glyph(font, ">", next == null ? null : next.label(), false, next != null,
                (mouse, shift) -> press(next.id(), mouse, shift)), settings -> settings.alignVerticallyMiddle());
        return pager;
    }

    private static void arrange(LayoutElement element) {
        if (element instanceof Layout layout) {
            layout.arrangeElements();
        }
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
        UiTheme.pill(graphics, left, top, panelWidth, panelHeight, UiTheme.PANEL, UiTheme.PANEL_BORDER);

        int dividerY = header.getY() + header.getHeight() + SECTION_SPACING / 2;
        graphics.fill(left + 1, dividerY, left + panelWidth - 1, dividerY + 1, UiTheme.DIVIDER);
        graphics.fill(content.getX(), dividerY, content.getX() + ACCENT_WIDTH, dividerY + 1, UiTheme.ACCENT);
        if (footer != null) {
            int footerDividerY = footer.getY() - SECTION_SPACING / 2;
            graphics.fill(left + 1, footerDividerY, left + panelWidth - 1, footerDividerY + 1, UiTheme.DIVIDER);
        }
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
