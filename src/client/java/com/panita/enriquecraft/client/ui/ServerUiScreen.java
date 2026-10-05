package com.panita.enriquecraft.client.ui;

import com.mojang.blaze3d.platform.InputConstants;
import com.panita.enriquecraft.core.network.UiClickC2S;
import com.panita.enriquecraft.core.network.UiClosedC2S;
import com.panita.enriquecraft.core.network.UiDropC2S;
import com.panita.enriquecraft.core.network.UiElement;
import com.panita.enriquecraft.core.network.UiSelectC2S;
import com.panita.enriquecraft.core.network.UiSubmitC2S;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.ScrollableLayout;
import net.minecraft.client.gui.layouts.FrameLayout;
import net.minecraft.client.gui.layouts.Layout;
import net.minecraft.client.gui.layouts.LayoutElement;
import net.minecraft.client.gui.layouts.LinearLayout;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;

import java.util.List;

/**
 * A screen the server described, laid out like a modal: a header with the title and a close button,
 * the body, and a footer with the search field on the left and the page buttons on the right. It
 * shows what it was sent and reports presses and typed values; everything that happens next is
 * decided by the server.
 * <p>
 * It fits whatever window it is in: when the panel is taller than the window it first shortens its scroll
 * areas, then draws large icons and the title smaller, and as a last resort scrolls the whole body.
 * Moving between screens does not pop: a new screen's panel grows from the shape of the one before it while
 * its content fades in, and a refresh eases the panel to its new size.
 */
final class ServerUiScreen extends Screen implements UiActions {

    /** Where and how large the panel is, in screen pixels. */
    record Bounds(int left, int top, int width, int height) {

        static Bounds lerp(Bounds from, Bounds to, float amount) {
            return new Bounds(
                    Math.round(from.left + (to.left - from.left) * amount),
                    Math.round(from.top + (to.top - from.top) * amount),
                    Math.round(from.width + (to.width - from.width) * amount),
                    Math.round(from.height + (to.height - from.height) * amount));
        }

        /** The same panel shrunk around its center. */
        Bounds scaled(float scale) {
            int newWidth = Math.round(width * scale);
            int newHeight = Math.round(height * scale);
            return new Bounds(left + (width - newWidth) / 2, top + (height - newHeight) / 2, newWidth, newHeight);
        }
    }

    private static final int PADDING = 12;
    private static final int SECTION_SPACING = 12;
    private static final int GROUP_GAP = 16;
    private static final int MIN_WIDTH = 180;
    private static final int HEADER_HEIGHT = 24;
    private static final int FOOTER_HEIGHT = 20;
    private static final int ICON_SIZE = 20;
    private static final float TITLE_SCALE = 1.4F;
    private static final float COMPACT_TITLE_SCALE = 1.0F;
    /** How much of the window is left empty around the panel, in total on each axis. */
    private static final int SCREEN_MARGIN = 8;
    private static final int MIN_BODY_HEIGHT = 40;
    private static final long OPEN_MILLIS = 180;
    private static final long REFRESH_MILLIS = 120;
    private static final float OPEN_SCALE_FROM = 0.94F;
    private static final double DRAG_THRESHOLD = 4.0;

    private final int sessionId;
    private final ItemStack icon;
    private UiElement root;
    private ScreenParts parts = ScreenParts.split(new UiElement.Spacer());
    private List<UiDropdownWidget> dropdowns = List.of();
    private List<UiTitleEditWidget> titleEdits = List.of();
    private LinearLayout content;
    private FrameLayout header;
    private FrameLayout footer;
    private int naturalBodyHeight;

    // The panel eases from one shape to the next. Started when the screen is made or refreshed, never when it is
    // rebuilt, so resizing the window does not replay it.
    private @Nullable Bounds from;
    private long startedAt = System.currentTimeMillis();
    private long duration = OPEN_MILLIS;
    private boolean fadeIn = true;

    // A draggable button pressed and not yet released: it is a click unless the mouse moves far enough.
    private @Nullable UiButtonWidget dragCandidate;
    private double dragStartX;
    private double dragStartY;
    private boolean dragging;

    /**
     * @param previous the panel of the screen this one replaces, so it can grow from that shape, or null when
     *                 the screen opens over the game
     */
    ServerUiScreen(int sessionId, Component title, ItemStack icon, UiElement root, @Nullable Bounds previous) {
        super(title);
        this.sessionId = sessionId;
        this.icon = icon;
        this.root = root;
        this.from = previous;
    }

    int sessionId() {
        return sessionId;
    }

    /** The panel as it is drawn right now, or null before it has been laid out. */
    @Nullable Bounds panelBounds() {
        return content == null ? null : shown();
    }

    /** Shows a new description of the same screen; the panel eases to its new size. */
    void update(UiElement root) {
        this.from = content == null ? null : shown();
        this.startedAt = System.currentTimeMillis();
        this.duration = REFRESH_MILLIS;
        this.fadeIn = false;
        this.root = root;
        rebuildWidgets();
    }

    @Override
    protected void init() {
        parts = ScreenParts.split(root);
        UiLayouts layouts = layout(0, false, 0);
        // A panel taller than the window gives the difference back through its scroll areas; when that is not
        // enough it draws the large icons and the title smaller; and when even that is not enough the body
        // scrolls as a whole.
        int overflow = overflow();
        int reduction = 0;
        boolean compact = false;
        if (overflow > 0 && layouts.hasScroll()) {
            reduction = overflow;
            layouts = layout(reduction, false, 0);
            overflow = overflow();
        }
        if (overflow > 0) {
            compact = true;
            reduction = 0;
            layouts = layout(0, true, 0);
            overflow = overflow();
            if (overflow > 0 && layouts.hasScroll()) {
                reduction = overflow;
                layouts = layout(reduction, true, 0);
                overflow = overflow();
            }
        }
        if (overflow > 0) {
            layouts = layout(reduction, compact, Math.max(MIN_BODY_HEIGHT, naturalBodyHeight - overflow));
        }
        content.visitWidgets(this::addRenderableWidget);
        dropdowns = layouts.dropdowns();
        titleEdits = layouts.titleEdits();
    }

    private UiLayouts layout(int scrollReduction, boolean compact, int bodyLimit) {
        UiLayouts layouts = new UiLayouts(font, this, scrollReduction, compact, bodyLimit > 0);
        assemble(layouts, compact, bodyLimit);
        return layouts;
    }

    /** How far the panel reaches beyond the window, or less than zero when it fits. */
    private int overflow() {
        return content.getHeight() + 2 * PADDING - (height - SCREEN_MARGIN);
    }

    /**
     * Builds the whole panel, header to footer, and centers it.
     *
     * @param bodyLimit when above zero, the most height the body may take before it scrolls as a whole
     */
    private void assemble(UiLayouts layouts, boolean compact, int bodyLimit) {
        LinearLayout headerStart = LinearLayout.horizontal().spacing(8);
        UiElement.Button goBack = goBack();
        if (goBack != null) {
            headerStart.addChild(UiButtonWidget.glyph(font, "←", goBack.label(), false, true,
                    (mouse, shift) -> press(goBack.id(), mouse, shift)), settings -> settings.alignVerticallyMiddle());
        }
        if (!icon.isEmpty()) {
            headerStart.addChild(new UiItemWidget(icon, ICON_SIZE), settings -> settings.alignVerticallyMiddle());
        }
        headerStart.addChild(new UiTitleWidget(font, title, compact ? COMPACT_TITLE_SCALE : TITLE_SCALE),
                settings -> settings.alignVerticallyMiddle());
        headerStart.arrangeElements();
        UiButtonWidget close = UiButtonWidget.glyph(font, "✕", null, true, true, (mouse, shift) -> closeAll());

        LayoutElement body = layouts.build(parts.body());
        arrange(body);
        naturalBodyHeight = body.getHeight();
        if (bodyLimit > 0 && body.getHeight() > bodyLimit) {
            LinearLayout wrapper = LinearLayout.vertical();
            wrapper.addChild(body);
            wrapper.arrangeElements();
            ScrollableLayout scrollable = new ScrollableLayout(minecraft, wrapper, bodyLimit);
            scrollable.arrangeElements();
            body = scrollable;
        }

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
        pager.addChild(new UiLayouts(font, this, 0, false, false).text(Component.literal(parts.page().page() + " / " + parts.page().pages())),
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

    @Override
    public void drop(int draggedId, int targetId) {
        ClientPlayNetworking.send(new UiDropC2S(sessionId, draggedId, targetId));
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

    private @Nullable UiButtonWidget draggableAt(double mouseX, double mouseY) {
        for (var child : children()) {
            if (child instanceof UiButtonWidget button && button.draggable() && button.isMouseOver(mouseX, mouseY)) {
                return button;
            }
        }
        return null;
    }

    /**
     * An open list takes its clicks before anything under it. A draggable button is not pressed when the mouse
     * goes down but when it comes up, because only then is it known whether it was a click or a drag.
     */
    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        for (UiDropdownWidget dropdown : dropdowns) {
            if (dropdown.clickList(event)) {
                return true;
            }
        }
        if (event.button() == InputConstants.MOUSE_BUTTON_LEFT) {
            UiButtonWidget button = draggableAt(event.x(), event.y());
            if (button != null) {
                dragCandidate = button;
                dragStartX = event.x();
                dragStartY = event.y();
                dragging = false;
                return true;
            }
        }
        return super.mouseClicked(event, doubleClick);
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double dragX, double dragY) {
        if (dragCandidate != null) {
            if (!dragging && Math.hypot(event.x() - dragStartX, event.y() - dragStartY) > DRAG_THRESHOLD) {
                dragging = true;
            }
            return true;
        }
        return super.mouseDragged(event, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        if (dragCandidate != null && event.button() == InputConstants.MOUSE_BUTTON_LEFT) {
            UiButtonWidget picked = dragCandidate;
            boolean wasDragged = dragging;
            dragCandidate = null;
            dragging = false;
            if (wasDragged) {
                UiButtonWidget target = draggableAt(event.x(), event.y());
                if (target != null && target != picked) {
                    drop(picked.elementId(), target.elementId());
                }
            } else {
                picked.firePress(0, event.hasShiftDown());
            }
            return true;
        }
        return super.mouseReleased(event);
    }

    /** Escape drops an edit in progress, or closes an open list, before it goes back. */
    @Override
    public boolean keyPressed(KeyEvent event) {
        if (event.isEscape() && dragging) {
            dragCandidate = null;
            dragging = false;
            return true;
        }
        if (event.isEscape() && titleEdits.stream().anyMatch(UiTitleEditWidget::stopEditing)) {
            return true;
        }
        if (event.isEscape() && dropdowns.stream().anyMatch(UiDropdownWidget::isOpen)) {
            dropdowns.forEach(UiDropdownWidget::close);
            return true;
        }
        return super.keyPressed(event);
    }

    private Bounds finalBounds() {
        return new Bounds(content.getX() - PADDING, content.getY() - PADDING, content.getWidth() + 2 * PADDING,
                content.getHeight() + 2 * PADDING);
    }

    /** How far the easing has come, from 0 to 1, slowing down as it arrives so it settles softly. */
    private float progress() {
        float time = Math.min(1.0F, (System.currentTimeMillis() - startedAt) / (float) duration);
        float remaining = 1.0F - time;
        return 1.0F - remaining * remaining * remaining;
    }

    /** The panel as it is at this moment of the easing. */
    private Bounds shown() {
        Bounds end = finalBounds();
        Bounds start = from != null ? from : end.scaled(OPEN_SCALE_FROM);
        return Bounds.lerp(start, end, progress());
    }

    /** The game draws the dimming behind the panel before this, and it never changes between screens. */
    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        float eased = progress();
        Bounds panel = shown();
        UiTheme.pill(graphics, panel.left(), panel.top(), panel.width(), panel.height(), UiTheme.PANEL, UiTheme.PANEL_BORDER);

        // Everything inside is clipped to the panel as it is now, so content never shows outside a panel that is
        // still growing; and it fades in from the panel's own color, so nothing pops.
        graphics.enableScissor(panel.left() + 1, panel.top() + 1, panel.left() + panel.width() - 1, panel.top() + panel.height() - 1);
        int left = finalBounds().left();
        int panelWidth = finalBounds().width();
        int dividerY = header.getY() + header.getHeight() + SECTION_SPACING / 2;
        // The whole line under the header is the accent color, so the bar reads as one piece.
        graphics.fill(left + 1, dividerY, left + panelWidth - 1, dividerY + 1, UiTheme.ACCENT);
        if (footer != null) {
            int footerDividerY = footer.getY() - SECTION_SPACING / 2;
            graphics.fill(left + 1, footerDividerY, left + panelWidth - 1, footerDividerY + 1, UiTheme.DIVIDER);
        }
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
        if (fadeIn && eased < 1.0F) {
            int alpha = Math.round((1.0F - eased) * 255);
            graphics.fill(panel.left() + 1, panel.top() + 1, panel.left() + panel.width() - 1, panel.top() + panel.height() - 1,
                    (alpha << 24) | (UiTheme.PANEL & 0xFFFFFF));
        }
        graphics.disableScissor();

        dropdowns.forEach(dropdown -> dropdown.extractList(graphics, mouseX, mouseY));
        extractDrag(graphics, mouseX, mouseY);
    }

    /** While dragging: the picked button dims, the one it would land on is outlined, and its item follows the mouse. */
    private void extractDrag(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        if (!dragging || dragCandidate == null) {
            return;
        }
        UiButtonWidget picked = dragCandidate;
        graphics.fill(picked.getX(), picked.getY(), picked.getX() + picked.getWidth(), picked.getY() + picked.getHeight(), 0xAA181818);
        UiButtonWidget target = draggableAt(mouseX, mouseY);
        if (target != null && target != picked) {
            int x = target.getX();
            int y = target.getY();
            int right = x + target.getWidth();
            int bottom = y + target.getHeight();
            graphics.fill(x, y, right, y + 1, UiTheme.ACCENT);
            graphics.fill(x, bottom - 1, right, bottom, UiTheme.ACCENT);
            graphics.fill(x, y, x + 1, bottom, UiTheme.ACCENT);
            graphics.fill(right - 1, y, right, bottom, UiTheme.ACCENT);
        }
        graphics.item(picked.icon(), mouseX - 8, mouseY - 8);
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
