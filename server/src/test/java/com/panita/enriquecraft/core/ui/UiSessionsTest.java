package com.panita.enriquecraft.core.ui;

import com.panita.enriquecraft.MinecraftTestSupport;
import com.panita.enriquecraft.core.network.UiClickC2S;
import com.panita.enriquecraft.core.network.UiElement;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UiSessionsTest {

    private static final long MILLISECOND = 1_000_000L;

    /** A screen of {@code buttons} buttons; pressing button n records n. */
    private static final class ButtonsMenu extends UiMenu {
        private final int buttons;
        private final List<Integer> pressed = new ArrayList<>();
        private int closes;
        private boolean failing;

        ButtonsMenu(UiService ui, int buttons) {
            super(ui, null);
            this.buttons = buttons;
        }

        @Override
        protected Component title() {
            return Component.literal("Buttons");
        }

        @Override
        protected UiElement describe(UiBuilder builder) {
            List<UiElement> children = new ArrayList<>();
            for (int index = 0; index < buttons; index++) {
                int number = index;
                children.add(builder.button(new ItemStack(Items.STONE), Component.literal("B" + index), List.of(), click -> {
                    if (failing) {
                        throw new IllegalStateException("boom");
                    }
                    pressed.add(number);
                }));
            }
            return new UiElement.Row(children);
        }

        @Override
        protected void onClose() {
            closes++;
        }
    }

    @TempDir
    Path directory;

    private final AtomicLong clock = new AtomicLong();
    private final UiSessions sessions = new UiSessions(clock::get);
    private final UUID player = UUID.randomUUID();
    private UiService ui;

    @BeforeEach
    void createService() {
        ui = MinecraftTestSupport.uiService(directory);
    }

    private ButtonsMenu menu(int buttons) {
        ButtonsMenu menu = new ButtonsMenu(ui, buttons);
        menu.opened();
        return menu;
    }

    private int show(UUID viewer, ButtonsMenu menu) {
        return sessions.begin(viewer, menu, menu.layout(0));
    }

    private UiSessions.ActionResult press(UUID viewer, int session, int element) {
        return sessions.click(viewer, null, new UiClickC2S(session, element, 0, false));
    }

    @Test
    void aPressRunsTheActionOfItsButton() {
        ButtonsMenu menu = menu(3);
        int session = show(player, menu);

        assertEquals(UiSessions.ActionResult.HANDLED, press(player, session, 1));

        assertEquals(List.of(1), menu.pressed);
    }

    @Test
    void aPressWithoutASessionIsStale() {
        assertEquals(UiSessions.ActionResult.STALE, press(player, 1, 0));
    }

    @Test
    void aPressForAnotherSessionIsStale() {
        ButtonsMenu menu = menu(1);
        int session = show(player, menu);

        assertEquals(UiSessions.ActionResult.STALE, press(player, session + 1, 0));
        assertTrue(menu.pressed.isEmpty());
    }

    @Test
    void aPlayerCannotUseTheSessionOfAnotherPlayer() {
        ButtonsMenu menu = menu(1);
        int session = show(player, menu);
        UUID intruder = UUID.randomUUID();
        show(intruder, menu(1));

        assertEquals(UiSessions.ActionResult.STALE, press(intruder, session, 0));
        assertTrue(menu.pressed.isEmpty());
    }

    @Test
    void aPressOnAButtonThatDoesNotExistIsRejected() {
        ButtonsMenu menu = menu(2);
        int session = show(player, menu);

        assertEquals(UiSessions.ActionResult.UNKNOWN_ELEMENT, press(player, session, 7));
        clock.addAndGet(100 * MILLISECOND);
        assertEquals(UiSessions.ActionResult.UNKNOWN_ELEMENT, press(player, session, -1));
        assertTrue(menu.pressed.isEmpty());
    }

    @Test
    void aMouseButtonOtherThanLeftOrRightIsRejected() {
        ButtonsMenu menu = menu(1);
        int session = show(player, menu);

        assertEquals(UiSessions.ActionResult.INVALID,
                sessions.click(player, null, new UiClickC2S(session, 0, 2, false)));
        clock.addAndGet(100 * MILLISECOND);
        assertEquals(UiSessions.ActionResult.INVALID,
                sessions.click(player, null, new UiClickC2S(session, 0, -1, false)));
        assertTrue(menu.pressed.isEmpty());
    }

    @Test
    void pressesFasterThanAPersonCanMakeAreDropped() {
        ButtonsMenu menu = menu(1);
        int session = show(player, menu);

        assertEquals(UiSessions.ActionResult.HANDLED, press(player, session, 0));
        clock.addAndGet(10 * MILLISECOND);
        assertEquals(UiSessions.ActionResult.TOO_FAST, press(player, session, 0));
        clock.addAndGet(60 * MILLISECOND);
        assertEquals(UiSessions.ActionResult.HANDLED, press(player, session, 0));

        assertEquals(2, menu.pressed.size());
    }

    @Test
    void theRateLimitOfOnePlayerDoesNotAffectAnother() {
        ButtonsMenu first = menu(1);
        ButtonsMenu second = menu(1);
        UUID other = UUID.randomUUID();
        int firstSession = show(player, first);
        int secondSession = show(other, second);

        press(player, firstSession, 0);

        assertEquals(UiSessions.ActionResult.HANDLED, press(other, secondSession, 0));
    }

    @Test
    void aFailingActionDoesNotEscapeAndTheSessionSurvives() {
        ButtonsMenu menu = menu(1);
        int session = show(player, menu);
        menu.failing = true;

        assertEquals(UiSessions.ActionResult.HANDLED, press(player, session, 0));

        menu.failing = false;
        clock.addAndGet(100 * MILLISECOND);
        assertEquals(UiSessions.ActionResult.HANDLED, press(player, session, 0));
        assertEquals(List.of(0), menu.pressed);
    }

    @Test
    void showingAnotherMenuClosesThePreviousOneAndInvalidatesItsSession() {
        ButtonsMenu first = menu(1);
        ButtonsMenu second = menu(1);
        int firstSession = show(player, first);

        int secondSession = show(player, second);

        assertEquals(1, first.closes);
        assertNotEquals(firstSession, secondSession);
        assertEquals(UiSessions.ActionResult.STALE, press(player, firstSession, 0));
        assertEquals(0, second.closes);
    }

    @Test
    void showingTheSameMenuAgainDoesNotCloseIt() {
        ButtonsMenu menu = menu(1);
        show(player, menu);

        show(player, menu);

        assertEquals(0, menu.closes);
    }

    @Test
    void whenTheClientClosesTheScreenTheMenuIsClosedOnce() {
        ButtonsMenu menu = menu(1);
        int session = show(player, menu);

        sessions.closedByClient(player, session);
        sessions.closedByClient(player, session);

        assertEquals(1, menu.closes);
        assertEquals(UiSessions.ActionResult.STALE, press(player, session, 0));
    }

    @Test
    void aClientReportOfAScreenThatAlreadyEndedIsIgnored() {
        ButtonsMenu old = menu(1);
        ButtonsMenu current = menu(1);
        int oldSession = show(player, old);
        show(player, current);

        sessions.closedByClient(player, oldSession);

        assertEquals(0, current.closes, "the screen that replaced it stays open");
        assertTrue(sessions.showing(player, current).isPresent());
    }

    @Test
    void endingASessionClosesTheMenuAndReturnsItsId() {
        ButtonsMenu menu = menu(1);
        int session = show(player, menu);

        assertEquals(session, sessions.end(player).getAsInt());

        assertEquals(1, menu.closes);
        assertTrue(sessions.end(player).isEmpty());
        assertEquals(1, menu.closes);
    }

    @Test
    void anUpdateReplacesTheButtonsAndKeepsNumberingAhead() {
        ButtonsMenu menu = menu(2);
        int session = show(player, menu);
        UiSessions.Showing showing = sessions.showing(player, menu).orElseThrow();

        UiLayout next = menu.layout(showing.nextElementId());
        sessions.update(player, next);

        assertEquals(session, showing.sessionId());
        assertEquals(2, showing.nextElementId());
        assertEquals(UiSessions.ActionResult.UNKNOWN_ELEMENT, press(player, session, 0),
                "a press meant for the previous description cannot land on a new button");
        clock.addAndGet(100 * MILLISECOND);
        assertEquals(UiSessions.ActionResult.HANDLED, press(player, session, 2));
        assertEquals(List.of(0), menu.pressed, "button 2 of the new description is the first button");
    }

    @Test
    void aMenuThatIsNotBeingShownHasNoSession() {
        ButtonsMenu shown = menu(1);
        ButtonsMenu other = menu(1);
        show(player, shown);

        assertTrue(sessions.showing(player, other).isEmpty());
        assertFalse(sessions.showing(player, shown).isEmpty());
    }
}
