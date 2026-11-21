package com.panita.enriquecraft.core.ui;

import com.panita.enriquecraft.MinecraftTestSupport;
import com.panita.enriquecraft.core.network.UiDropC2S;
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
import static org.junit.jupiter.api.Assertions.assertTrue;

/** What the server accepts as one button dragged onto another. */
class UiSessionsDropTest {

    private static final long MILLISECOND = 1_000_000L;

    /** A screen with two draggable buttons (ids 0 and 1) and an ordinary one (id 2). */
    private static final class DragMenu extends UiMenu {
        private final List<String> drops = new ArrayList<>();

        DragMenu(UiService ui) {
            super(ui, null);
        }

        @Override
        protected Component title() {
            return Component.literal("Drag");
        }

        @Override
        protected UiElement describe(UiBuilder builder) {
            UiElement first = builder.draggable(builder.button(new ItemStack(Items.STONE), Component.literal("A"), List.of(), click -> { }));
            UiElement second = builder.draggable(builder.button(new ItemStack(Items.DIRT), Component.literal("B"), List.of(), click -> { }));
            UiElement plain = builder.button(new ItemStack(Items.SAND), Component.literal("C"), List.of(), click -> { });
            builder.onDrop(drop -> drops.add(drop.draggedId() + ">" + drop.targetId()));
            return new UiElement.Row(List.of(first, second, plain));
        }
    }

    @TempDir
    Path directory;

    private final AtomicLong clock = new AtomicLong();
    private final UiSessions sessions = new UiSessions(clock::get);
    private final UUID player = UUID.randomUUID();
    private DragMenu menu;
    private int session;

    @BeforeEach
    void showTheMenu() {
        menu = new DragMenu(MinecraftTestSupport.uiService(directory));
        menu.opened();
        session = sessions.begin(player, menu, menu.layout(0));
    }

    private UiSessions.ActionResult drop(int sessionId, int dragged, int target) {
        UiSessions.ActionResult result = sessions.drop(player, null, new UiDropC2S(sessionId, dragged, target));
        clock.addAndGet(100 * MILLISECOND);
        return result;
    }

    @Test
    void droppingOneDraggableButtonOnAnotherRunsTheDropAction() {
        assertEquals(UiSessions.ActionResult.HANDLED, drop(session, 0, 1));

        assertEquals(List.of("0>1"), menu.drops);
    }

    @Test
    void aButtonThatIsNotDraggableCannotBeDraggedOrDroppedOn() {
        assertEquals(UiSessions.ActionResult.UNKNOWN_ELEMENT, drop(session, 2, 1));
        assertEquals(UiSessions.ActionResult.UNKNOWN_ELEMENT, drop(session, 0, 2));
        assertEquals(UiSessions.ActionResult.UNKNOWN_ELEMENT, drop(session, 0, 9));

        assertTrue(menu.drops.isEmpty());
    }

    @Test
    void aButtonDroppedOnItselfIsRejected() {
        assertEquals(UiSessions.ActionResult.INVALID, drop(session, 1, 1));

        assertTrue(menu.drops.isEmpty());
    }

    @Test
    void aDropForAScreenThatIsGoneIsStale() {
        sessions.end(player);

        assertEquals(UiSessions.ActionResult.STALE, drop(session, 0, 1));
        assertTrue(menu.drops.isEmpty());
    }

    @Test
    void dropsFasterThanAPersonCanDragAreDropped() {
        sessions.drop(player, null, new UiDropC2S(session, 0, 1));

        assertEquals(UiSessions.ActionResult.TOO_FAST, sessions.drop(player, null, new UiDropC2S(session, 1, 0)));
        assertEquals(List.of("0>1"), menu.drops);
    }

    @Test
    void theClientCompanionSeesWhichButtonsCanBeDragged() {
        UiElement.Row row = (UiElement.Row) menu.layout(0).root();

        assertEquals(List.of(true, true, false), row.children().stream()
                .map(child -> ((UiElement.Button) child).draggable()).toList());
    }
}
