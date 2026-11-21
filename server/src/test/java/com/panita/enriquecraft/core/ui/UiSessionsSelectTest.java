package com.panita.enriquecraft.core.ui;

import com.panita.enriquecraft.MinecraftTestSupport;
import com.panita.enriquecraft.core.network.UiClickC2S;
import com.panita.enriquecraft.core.network.UiElement;
import com.panita.enriquecraft.core.network.UiSelectC2S;
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

/** What the server accepts as the choice in a dropdown. */
class UiSessionsSelectTest {

    private static final long MILLISECOND = 1_000_000L;

    /** A screen with a button (id 0) and a dropdown of three options (id 1). */
    private static final class ChoiceMenu extends UiMenu {
        private final List<Integer> chosen = new ArrayList<>();

        ChoiceMenu(UiService ui) {
            super(ui, null);
        }

        @Override
        protected Component title() {
            return Component.literal("Choice");
        }

        @Override
        protected UiElement describe(UiBuilder builder) {
            return new UiElement.Row(List.of(
                    builder.button(new ItemStack(Items.STONE), Component.literal("B"), List.of(), click -> { }),
                    builder.dropdown(Component.literal("Filtrar"),
                            List.of(Component.literal("a"), Component.literal("b"), Component.literal("c")), 0,
                            select -> chosen.add(select.option()))));
        }
    }

    @TempDir
    Path directory;

    private final AtomicLong clock = new AtomicLong();
    private final UiSessions sessions = new UiSessions(clock::get);
    private final UUID player = UUID.randomUUID();
    private ChoiceMenu menu;
    private int session;

    @BeforeEach
    void showTheMenu() {
        menu = new ChoiceMenu(MinecraftTestSupport.uiService(directory));
        menu.opened();
        session = sessions.begin(player, menu, menu.layout(0));
    }

    private UiSessions.ActionResult select(int sessionId, int element, int option) {
        UiSessions.ActionResult result = sessions.select(player, null, new UiSelectC2S(sessionId, element, option));
        clock.addAndGet(100 * MILLISECOND);
        return result;
    }

    @Test
    void anOptionRunsTheActionOfItsDropdown() {
        assertEquals(UiSessions.ActionResult.HANDLED, select(session, 1, 2));

        assertEquals(List.of(2), menu.chosen);
    }

    @Test
    void anOptionTheDropdownDoesNotHaveIsRejected() {
        assertEquals(UiSessions.ActionResult.INVALID, select(session, 1, 3));
        assertEquals(UiSessions.ActionResult.INVALID, select(session, 1, -1));

        assertTrue(menu.chosen.isEmpty());
    }

    @Test
    void anOptionSentToAButtonIsRejected() {
        assertEquals(UiSessions.ActionResult.UNKNOWN_ELEMENT, select(session, 0, 0));
        assertEquals(UiSessions.ActionResult.UNKNOWN_ELEMENT, select(session, 9, 0));
    }

    @Test
    void aPressSentToADropdownIsRejected() {
        UiSessions.ActionResult result = sessions.click(player, null, new UiClickC2S(session, 1, 0, false));

        assertEquals(UiSessions.ActionResult.UNKNOWN_ELEMENT, result);
        assertTrue(menu.chosen.isEmpty());
    }

    @Test
    void anOptionForAScreenThatIsGoneIsStale() {
        sessions.end(player);

        assertEquals(UiSessions.ActionResult.STALE, select(session, 1, 0));
        assertTrue(menu.chosen.isEmpty());
    }

    @Test
    void optionsFasterThanAPersonCanChooseAreDropped() {
        sessions.select(player, null, new UiSelectC2S(session, 1, 0));

        assertEquals(UiSessions.ActionResult.TOO_FAST, sessions.select(player, null, new UiSelectC2S(session, 1, 1)));
        assertEquals(List.of(0), menu.chosen);
    }
}
