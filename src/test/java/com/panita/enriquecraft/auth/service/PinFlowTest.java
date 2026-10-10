package com.panita.enriquecraft.auth.service;

import com.panita.enriquecraft.MinecraftTestSupport;
import com.panita.enriquecraft.core.framework.data.WorldData;
import net.minecraft.nbt.NbtOps;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PinFlowTest {

    private static final UUID PLAYER = UUID.fromString("00000000-0000-0000-0000-000000000001");

    @TempDir
    Path directory;

    private AccountService accounts;
    private int accepted;
    private int lockedOut;

    private final PinFlow.Listener listener = new PinFlow.Listener() {
        @Override
        public void accepted() {
            accepted++;
        }

        @Override
        public void lockedOut() {
            lockedOut++;
        }
    };

    @BeforeEach
    void create() {
        MinecraftTestSupport.bootstrap();
        WorldData worldData = new WorldData();
        worldData.attach(directory, NbtOps.INSTANCE);
        accounts = new AccountService(worldData, new PinHasher(new byte[32]), Instant::now);
    }

    private PinFlow entering() {
        accounts.setPin(PLAYER, "Tezzlar", "1234");
        return new PinFlow(PLAYER, "Tezzlar", new PinEntry(4, 3, true), accounts, listener);
    }

    private PinFlow choosing() {
        return new PinFlow(PLAYER, "Tezzlar", new PinEntry(4, 3, false), accounts, listener);
    }

    private static void type(PinFlow flow, String digits) {
        digits.chars().forEach(digit -> flow.digit(digit - '0'));
    }

    private static void waitTicks(PinFlow flow, int ticks) {
        for (int tick = 0; tick < ticks; tick++) {
            flow.tick();
        }
    }

    @Test
    void aCorrectPinIsShownAndThenAccepted() {
        PinFlow flow = entering();

        type(flow, "1234");

        assertEquals(PinFlow.Phase.SUCCESS, flow.phase());
        assertTrue(flow.success());
        assertEquals(4, flow.shownDigits());
        assertEquals(0, accepted);
        waitTicks(flow, PinFlow.SUCCESS_TICKS);
        assertEquals(1, accepted);
        assertEquals(PinFlow.Phase.INPUT, flow.phase());
    }

    @Test
    void aWrongPinShakesAndShowsFullDotsUntilItClears() {
        PinFlow flow = entering();

        type(flow, "0000");

        assertEquals(PinFlow.Phase.WRONG, flow.phase());
        assertEquals(PinFlow.Failure.WRONG_PIN, flow.failure());
        assertEquals(1, flow.shakes());
        assertEquals(4, flow.shownDigits());
        assertEquals(2, flow.attemptsLeft());
        waitTicks(flow, PinFlow.WRONG_TICKS);
        assertEquals(PinFlow.Phase.INPUT, flow.phase());
        assertEquals(0, flow.shownDigits());
        assertEquals(0, lockedOut);
    }

    @Test
    void digitsAreIgnoredWhileAResultIsShown() {
        PinFlow flow = entering();
        type(flow, "0000");

        type(flow, "1234");
        flow.backspace();
        flow.clear();

        assertEquals(1, flow.shakes());
        assertEquals(PinFlow.Phase.WRONG, flow.phase());
        waitTicks(flow, PinFlow.WRONG_TICKS);
        assertEquals(0, accepted);
    }

    @Test
    void thirdWrongPinLocksOutAfterItIsShown() {
        PinFlow flow = entering();
        for (int attempt = 0; attempt < 3; attempt++) {
            type(flow, "0000");
            waitTicks(flow, PinFlow.WRONG_TICKS);
        }

        assertEquals(1, lockedOut);
        assertEquals(0, accepted);
    }

    @Test
    void aCorrectPinAfterMistakesIsAccepted() {
        PinFlow flow = entering();
        type(flow, "0000");
        waitTicks(flow, PinFlow.WRONG_TICKS);

        type(flow, "1234");
        waitTicks(flow, PinFlow.SUCCESS_TICKS);

        assertEquals(1, accepted);
        assertEquals(0, lockedOut);
    }

    @Test
    void choosingAPinStoresItOnlyOnceConfirmed() {
        PinFlow flow = choosing();

        type(flow, "4821");
        assertEquals(PinFlow.Phase.ADVANCE, flow.phase());
        assertFalse(accounts.verify(PLAYER, "4821"));
        waitTicks(flow, PinFlow.ADVANCE_TICKS);
        assertEquals(PinEntry.Step.CONFIRM, flow.step());

        type(flow, "4821");
        assertTrue(flow.success());
        assertTrue(flow.wasCreated());
        assertTrue(accounts.verify(PLAYER, "4821"));
        waitTicks(flow, PinFlow.SUCCESS_TICKS);
        assertEquals(1, accepted);
    }

    @Test
    void aMismatchShakesAndStartsChoosingAgainWithoutStoringAnything() {
        PinFlow flow = choosing();
        type(flow, "4821");
        waitTicks(flow, PinFlow.ADVANCE_TICKS);

        type(flow, "4822");

        assertEquals(PinFlow.Failure.MISMATCH, flow.failure());
        assertEquals(1, flow.shakes());
        waitTicks(flow, PinFlow.WRONG_TICKS);
        assertEquals(PinEntry.Step.CREATE, flow.step());
        assertTrue(accounts.pinLength(PLAYER).isEmpty());
        assertEquals(0, lockedOut);
    }

    @Test
    void choosingAPinShowsNoAttempts() {
        assertEquals(0, choosing().maxAttempts());
        assertEquals(3, entering().maxAttempts());
    }

    @Test
    void backspaceAndClearWorkWhileTyping() {
        PinFlow flow = entering();
        type(flow, "12");

        flow.backspace();
        assertEquals(1, flow.shownDigits());
        flow.clear();
        assertEquals(0, flow.shownDigits());
    }
}
