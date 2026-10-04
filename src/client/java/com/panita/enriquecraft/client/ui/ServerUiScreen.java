package com.panita.enriquecraft.client.ui;

import com.panita.enriquecraft.core.network.UiClickC2S;
import com.panita.enriquecraft.core.network.UiClosedC2S;
import com.panita.enriquecraft.core.network.UiElement;
import com.panita.enriquecraft.core.network.UiSelectC2S;
import com.panita.enriquecraft.core.network.UiSubmitC2S;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.layouts.FrameLayout;
import net.minecraft.client.gui.layouts.LayoutElement;
import net.minecraft.client.gui.layouts.LinearLayout;
import net.minecraft.client.gui.layouts.Layout;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import org.joml.Matrix3x2f;

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
    private static final int ICON_SIZE = 20;
    private static final float TITLE_SCALE = 1.4F;
    private static final int SCREEN_MARGIN = 16;
    private static final long OPEN_MILLIS = 160;
    private static final float OPEN_SCALE_FROM = 0.96F;
    private static final float OPEN_SLIDE = 6.0F;

    private final int sessionId;
    private final ItemStack icon;
    // Set when the screen is made, not when it is built, so a refresh does not play the opening again.
    private final long openedAt = System.currentTimeMillis();
    private UiElement root;
    private ScreenParts parts = ScreenParts.split(new UiElement.Spacer());
    private List<UiDropdownWidget> dropdowns = List.of();
    private List<UiTitleEditWidget> titleEdits = List.of();
    private LinearLayout content;
    private FrameLayout header;
    private FrameLayout footer;

    ServerUiScreen(int sessionId, Component title, ItemStack icon, UiElement root) {
        super(title);
        this.sessionId = sessionId;
        this.icon = icon;
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
        parts = ScreenParts.split(root);
        UiLayouts layouts = layout(0, false);
        // A panel taller than the window gives the difference back through its scroll areas, and when that is
        // not enough it draws the large icons smaller.
        int overflow = overflow();
        if (overflow > 0 && layouts.hasScroll()) {
            layouts = layout(overflow, false);
            overflow = overflow();
        }
        if (overflow > 0) {
            layouts = layout(0, true);
            overflow = overflow();
            if (overflow > 0 && layouts.hasScroll()) {
                layouts = layout(overflow, true);
            }
        }
        content.visitWidgets(this::addRenderableWidget);
        dropdowns = layouts.dropdowns();
        titleEdits = layouts.titleEdits();
    }

    private UiLayouts layout(int scrollReduction, boolean compact) {
        UiLayouts layouts = new UiLayouts(font, this, scrollReduction, compact);
        assemble(layouts);
        return layouts;
    }

    /** How far the panel reaches beyond the window, or less than zero when it fits. */
    private int overflow() {
        return content.getHeight() - (height - SCREEN_MARGIN);
    }

    /** Builds the whole panel, header to footer, and centers it. */
    private void assemble(UiLayouts layouts) {

        LinearLayout headerStart = LinearLayout.horizontal().spacing(8);
        UiElement.Button goBack = goBack();
        if (goBack != null) {
            headerStart.addChild(UiButtonWidget.glyph(font, "←", goBack.label(), false, true,
                    (mouse, shift) -> press(goBack.id(), mouse, shift)), settings -> settings.alignVerticallyMiddle());
        }
        if (!icon.isEmpty()) {
            headerStart.addChild(new UiItemWidget(icon, ICON_SIZE), settings -> settings.alignVerticallyMiddle());
        }
        headerStart.addChild(new UiTitleWidget(font, title, TITLE_SCALE), settings -> settings.alignVerticallyMiddle());
        headerStart.arrangeElements();
        UiButtonWidget close = UiButtonWidget.glyph(font, "✕", null, true, true, (mouse, shift) -> closeAll());

        LayoutElement body = layouts.build(parts.body());
        arrange(body);

        LinearLayout footerStart = layouts.row(parts.fields());
        LinearLayout footerMiddle = layouts.footerActions(parts.actions());
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
        pager.addChild(new UiLayouts(font, this, 0, false).text(Component.literal(parts.page().page() + " / " + parts.page().pages())),
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
    public void select(int elementId, int option) {
        ClientPlayNetworking.send(new UiSelectC2S(sessionId, elementId, option));
    }

    /** Closes the whole screen, whatever it was opened from. */
    private void closeAll() {
        super.onClose();
    }

    /** What goes back: the back button, or the cancel button of a question, which also leads back. */
    private UiElement.Button goBack() {
        return parts.back() != null ? parts.back() : parts.cancel();
    }

    /** Escape goes back to the screen this one was opened from, and closes only when there is none. */
    @Override
    public void onClose() {
        UiElement.Button goBack = goBack();
        if (goBack != null) {
            press(goBack.id(), 0, false);
        } else {
            closeAll();
        }
    }

    /** An open list takes its clicks before anything under it. */
    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        for (UiDropdownWidget dropdown : dropdowns) {
            if (dropdown.clickList(event)) {
                return true;
            }
        }
        return super.mouseClicked(event, doubleClick);
    }

    /** Escape drops an edit in progress, or closes an open list, before it goes back. */
    @Override
    public boolean keyPressed(KeyEvent event) {
        if (event.isEscape() && titleEdits.stream().anyMatch(UiTitleEditWidget::stopEditing)) {
            return true;
        }
        if (event.isEscape() && dropdowns.stream().anyMatch(UiDropdownWidget::isOpen)) {
            dropdowns.forEach(UiDropdownWidget::close);
            return true;
        }
        return super.keyPressed(event);
    }

    /** How far the opening has come, from 0 to 1, easing out so it settles softly. */
    private float openProgress() {
        float time = Math.min(1.0F, (System.currentTimeMillis() - openedAt) / (float) OPEN_MILLIS);
        float remaining = 1.0F - time;
        return 1.0F - remaining * remaining * remaining;
    }

    /** The dimming behind the panel stays put while the panel eases in. */
    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        Matrix3x2f transform = new Matrix3x2f(graphics.pose());
        graphics.pose().identity();
        super.extractBackground(graphics, mouseX, mouseY, partialTick);
        graphics.pose().set(transform);
    }

    /** The panel grows a little and rises into place when it opens, so moving between screens is gentle. */
    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        float opened = openProgress();
        graphics.pose().pushMatrix();
        if (opened < 1.0F) {
            float centerX = width / 2.0F;
            float centerY = height / 2.0F;
            float scale = OPEN_SCALE_FROM + (1.0F - OPEN_SCALE_FROM) * opened;
            graphics.pose().translate(centerX, centerY + (1.0F - opened) * OPEN_SLIDE);
            graphics.pose().scale(scale, scale);
            graphics.pose().translate(-centerX, -centerY);
        }
        extractPanel(graphics, mouseX, mouseY, partialTick);
        graphics.pose().popMatrix();
    }

    private void extractPanel(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        int left = content.getX() - PADDING;
        int top = content.getY() - PADDING;
        int panelWidth = content.getWidth() + 2 * PADDING;
        int panelHeight = content.getHeight() + 2 * PADDING;
        UiTheme.pill(graphics, left, top, panelWidth, panelHeight, UiTheme.PANEL, UiTheme.PANEL_BORDER);

        int dividerY = header.getY() + header.getHeight() + SECTION_SPACING / 2;
        // The whole line under the header is the accent color, so the bar reads as one piece.
        graphics.fill(left + 1, dividerY, left + panelWidth - 1, dividerY + 1, UiTheme.ACCENT);
        if (footer != null) {
            int footerDividerY = footer.getY() - SECTION_SPACING / 2;
            graphics.fill(left + 1, footerDividerY, left + panelWidth - 1, footerDividerY + 1, UiTheme.DIVIDER);
        }
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
        dropdowns.forEach(dropdown -> dropdown.extractList(graphics, mouseX, mouseY));
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
