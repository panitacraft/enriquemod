package com.panita.enriquecraft.core.ui;

import com.panita.enriquecraft.MinecraftTestSupport;
import com.panita.enriquecraft.core.network.UiClickC2S;
import com.panita.enriquecraft.core.network.UiElement;
import com.panita.enriquecraft.core.network.UiSelectC2S;
import com.panita.enriquecraft.core.network.UiSubmitC2S;
import net.minecraft.network.chat.Component;
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

/** What the server accepts for a text area and a toggle, which reach it by the submit and select paths. */
class UiSessionsFormTest {

    private static final long MILLISECOND = 1_000_000L;
    private static final int AREA_LENGTH = 600;
    private static final int FIELD_LENGTH = 40;

    /** A screen with a one-line field (id 0), a text area (id 1) and a toggle (id 2). */
    private static final class FormMenu extends UiMenu {
        private final List<String> fieldValues = new ArrayList<>();
        private final List<String> areaValues = new ArrayList<>();
        private final List<Boolean> toggled = new ArrayList<>();

        FormMenu(UiService ui) {
            super(ui, null);
        }

        @Override
        protected Component title() {
            return Component.literal("Form");
        }

        @Override
        protected UiElement describe(UiBuilder builder) {
            return new UiElement.Column(List.of(
                    builder.input(Component.literal("Campo"), "", FIELD_LENGTH, submit -> fieldValues.add(submit.text())),
                    builder.textArea(Component.literal("Biografía"), "", AREA_LENGTH, 4, 200, submit -> areaValues.add(submit.text())),
                    builder.toggle(Component.literal("Mostrar"), false, toggle -> toggled.add(toggle.checked()))));
        }
    }

    @TempDir
    Path directory;

    private final AtomicLong clock = new AtomicLong();
    private final UiSessions sessions = new UiSessions(clock::get);
    private final UUID player = UUID.randomUUID();
    private FormMenu menu;
    private int session;

    @BeforeEach
    void showTheMenu() {
        menu = new FormMenu(MinecraftTestSupport.uiService(directory));
        menu.opened();
        session = sessions.begin(player, menu, menu.layout(0));
    }

    private UiSessions.ActionResult submit(int element, String text) {
        UiSessions.ActionResult result = sessions.submit(player, null, new UiSubmitC2S(session, element, text));
        clock.addAndGet(100 * MILLISECOND);
        return result;
    }

    private UiSessions.ActionResult select(int element, int option) {
        UiSessions.ActionResult result = sessions.select(player, null, new UiSelectC2S(session, element, option));
        clock.addAndGet(100 * MILLISECOND);
        return result;
    }

    @Test
    void aTextAreaKeepsTheNewlinesOfItsValue() {
        assertEquals(UiSessions.ActionResult.HANDLED, submit(1, "uno\ndos\n\ntres"));

        assertEquals(List.of("uno\ndos\n\ntres"), menu.areaValues);
    }

    @Test
    void aTextAreaAcceptsMoreThanAOneLineFieldEverCould() {
        String value = "x".repeat(AREA_LENGTH);

        assertEquals(UiSessions.ActionResult.HANDLED, submit(1, value));

        assertEquals(List.of(value), menu.areaValues);
    }

    @Test
    void aTextAreaRejectsAValueLongerThanItsOwnLimit() {
        assertEquals(UiSessions.ActionResult.INVALID, submit(1, "x".repeat(AREA_LENGTH + 1)));

        assertTrue(menu.areaValues.isEmpty());
    }

    @Test
    void aTextAreaRejectsControlCharactersOtherThanNewline() {
        assertEquals(UiSessions.ActionResult.INVALID, submit(1, "a\rb"));
        assertEquals(UiSessions.ActionResult.INVALID, submit(1, "a\tb"));
        assertEquals(UiSessions.ActionResult.INVALID, submit(1, "a\u0000"));

        assertTrue(menu.areaValues.isEmpty());
    }

    @Test
    void aOneLineFieldStillRejectsNewlinesAndKeepsItsOwnLimit() {
        assertEquals(UiSessions.ActionResult.INVALID, submit(0, "a\nb"));
        assertEquals(UiSessions.ActionResult.INVALID, submit(0, "x".repeat(FIELD_LENGTH + 1)));
        assertEquals(UiSessions.ActionResult.INVALID, submit(0, "x".repeat(UiElement.TextInput.MAX_LENGTH + 1)));

        assertTrue(menu.fieldValues.isEmpty());
    }

    @Test
    void anEmptyValueIsHowATextAreaIsCleared() {
        assertEquals(UiSessions.ActionResult.HANDLED, submit(1, ""));

        assertEquals(List.of(""), menu.areaValues);
    }

    @Test
    void aToggleReportsTheStateThePlayerAskedFor() {
        assertEquals(UiSessions.ActionResult.HANDLED, select(2, 1));
        assertEquals(UiSessions.ActionResult.HANDLED, select(2, 0));

        assertEquals(List.of(true, false), menu.toggled);
    }

    @Test
    void aToggleRejectsAnyStateButCheckedOrUnchecked() {
        assertEquals(UiSessions.ActionResult.INVALID, select(2, 2));
        assertEquals(UiSessions.ActionResult.INVALID, select(2, -1));

        assertTrue(menu.toggled.isEmpty());
    }

    @Test
    void aToggleIsNotAFieldAndAFieldIsNotAToggle() {
        assertEquals(UiSessions.ActionResult.UNKNOWN_ELEMENT, submit(2, "x"));
        assertEquals(UiSessions.ActionResult.UNKNOWN_ELEMENT, select(1, 1));
        assertEquals(UiSessions.ActionResult.UNKNOWN_ELEMENT, select(0, 1));

        UiSessions.ActionResult press = sessions.click(player, null, new UiClickC2S(session, 2, 0, false));
        assertEquals(UiSessions.ActionResult.UNKNOWN_ELEMENT, press);
        assertTrue(menu.toggled.isEmpty());
        assertTrue(menu.areaValues.isEmpty());
    }

    @Test
    void aToggleForAScreenThatIsGoneIsStale() {
        sessions.end(player);

        assertEquals(UiSessions.ActionResult.STALE, select(2, 1));
        assertTrue(menu.toggled.isEmpty());
    }
}
