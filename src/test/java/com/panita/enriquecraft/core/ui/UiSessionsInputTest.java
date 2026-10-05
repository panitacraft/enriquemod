package com.panita.enriquecraft.core.ui;

import com.panita.enriquecraft.MinecraftTestSupport;
import com.panita.enriquecraft.core.network.UiClickC2S;
import com.panita.enriquecraft.core.network.UiElement;
import com.panita.enriquecraft.core.network.UiSubmitC2S;
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
import static org.junit.jupiter.api.Assertions.assertTrue;

/** What the server accepts as the value of a text field. */
class UiSessionsInputTest {

    private static final long MILLISECOND = 1_000_000L;
    private static final int MAX_LENGTH = 5;

    /** A screen with one button (id 0) and one text field (id 1) that records what it receives. */
    private static final class FieldMenu extends UiMenu {
        private final List<String> received = new ArrayList<>();

        FieldMenu(UiService ui) {
            super(ui, null);
        }

        @Override
        protected Component title() {
            return Component.literal("Field");
        }

        @Override
        protected UiElement describe(UiBuilder builder) {
            return new UiElement.Row(List.of(
                    builder.button(new ItemStack(Items.STONE), Component.literal("B"), List.of(), click -> { }),
                    builder.input(Component.literal("Campo"), "", MAX_LENGTH, submit -> received.add(submit.text()))));
        }
    }

    @TempDir
    Path directory;

    private final AtomicLong clock = new AtomicLong();
    private final UiSessions sessions = new UiSessions(clock::get);
    private final UUID player = UUID.randomUUID();
    private FieldMenu menu;
    private int session;

    @BeforeEach
    void showTheMenu() {
        menu = new FieldMenu(MinecraftTestSupport.uiService(directory));
        menu.opened();
        session = sessions.begin(player, menu, menu.layout(0));
    }

    private UiSessions.ActionResult submit(int sessionId, int element, String text) {
        UiSessions.ActionResult result = sessions.submit(player, null, new UiSubmitC2S(sessionId, element, text));
        clock.addAndGet(100 * MILLISECOND);
        return result;
    }

    @Test
    void aValueRunsTheActionOfItsField() {
        assertEquals(UiSessions.ActionResult.HANDLED, submit(session, 1, "base"));

        assertEquals(List.of("base"), menu.received);
    }

    @Test
    void anEmptyValueIsHowAFieldIsCleared() {
        assertEquals(UiSessions.ActionResult.HANDLED, submit(session, 1, ""));

        assertEquals(List.of(""), menu.received);
    }

    @Test
    void aValueLongerThanTheFieldAllowsIsRejected() {
        assertEquals(UiSessions.ActionResult.INVALID, submit(session, 1, "x".repeat(MAX_LENGTH + 1)));

        assertTrue(menu.received.isEmpty());
    }

    @Test
    void aValueWithControlCharactersIsRejected() {
        assertEquals(UiSessions.ActionResult.INVALID, submit(session, 1, "a\nb"));
        assertEquals(UiSessions.ActionResult.INVALID, submit(session, 1, "a\u0000"));
        assertEquals(UiSessions.ActionResult.INVALID, submit(session, 1, "\u007f"));

        assertTrue(menu.received.isEmpty());
    }

    @Test
    void aValueSentToAButtonIsRejected() {
        assertEquals(UiSessions.ActionResult.UNKNOWN_ELEMENT, submit(session, 0, "base"));

        assertTrue(menu.received.isEmpty());
    }

    @Test
    void aPressSentToATextFieldIsRejected() {
        UiSessions.ActionResult result = sessions.click(player, null, new UiClickC2S(session, 1, 0, false));

        assertEquals(UiSessions.ActionResult.UNKNOWN_ELEMENT, result);
        assertTrue(menu.received.isEmpty());
    }

    @Test
    void aValueForAnUnknownFieldIsRejected() {
        assertEquals(UiSessions.ActionResult.UNKNOWN_ELEMENT, submit(session, 9, "base"));
    }

    @Test
    void aValueForAScreenThatIsGoneIsStale() {
        assertEquals(UiSessions.ActionResult.STALE, submit(session + 1, 1, "base"));

        sessions.end(player);

        assertEquals(UiSessions.ActionResult.STALE, submit(session, 1, "base"));
        assertTrue(menu.received.isEmpty());
    }

    @Test
    void valuesFasterThanAPersonCanTypeAreDropped() {
        sessions.submit(player, null, new UiSubmitC2S(session, 1, "uno"));

        UiSessions.ActionResult result = sessions.submit(player, null, new UiSubmitC2S(session, 1, "dos"));

        assertEquals(UiSessions.ActionResult.TOO_FAST, result);
        assertEquals(List.of("uno"), menu.received);
    }
}
