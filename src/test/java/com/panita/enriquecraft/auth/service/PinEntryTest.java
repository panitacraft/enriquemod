package com.panita.enriquecraft.auth.service;

import org.junit.jupiter.api.Test;

import static com.panita.enriquecraft.auth.service.PinEntry.Outcome;
import static com.panita.enriquecraft.auth.service.PinEntry.Step;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PinEntryTest {

    private static Outcome type(PinEntry entry, String digits, String correct) {
        Outcome last = Outcome.INCOMPLETE;
        for (char digit : digits.toCharArray()) {
            last = entry.press(digit - '0', correct::equals);
        }
        return last;
    }

    @Test
    void aCorrectPinIsVerifiedOnTheLastDigit() {
        PinEntry entry = new PinEntry(4, 3, true);

        assertEquals(Outcome.INCOMPLETE, type(entry, "123", "1234"));
        assertEquals(3, entry.filled());
        assertEquals(Outcome.VERIFIED, entry.press(4, "1234"::equals));
    }

    @Test
    void wrongPinsCostAttemptsAndTheLastOneLocksOut() {
        PinEntry entry = new PinEntry(4, 3, true);

        assertEquals(Outcome.WRONG, type(entry, "9999", "1234"));
        assertEquals(2, entry.attemptsLeft());
        assertEquals(0, entry.filled());
        assertEquals(Outcome.WRONG, type(entry, "1111", "1234"));
        assertEquals(Outcome.LOCKED_OUT, type(entry, "2222", "1234"));
        assertEquals(0, entry.attemptsLeft());
    }

    @Test
    void aCorrectPinAfterMistakesStillWorks() {
        PinEntry entry = new PinEntry(4, 3, true);
        type(entry, "9999", "1234");

        assertEquals(Outcome.VERIFIED, type(entry, "1234", "1234"));
    }

    @Test
    void choosingAPinNeedsTheSamePinTwice() {
        PinEntry entry = new PinEntry(4, 3, false);
        assertEquals(Step.CREATE, entry.step());

        assertEquals(Outcome.CONFIRM_NEEDED, type(entry, "4821", ""));
        assertEquals(Step.CONFIRM, entry.step());
        assertEquals(Outcome.CREATED, type(entry, "4821", ""));
        assertEquals("4821", entry.createdPin());
    }

    @Test
    void aMismatchStartsChoosingAgainWithoutCostingAttempts() {
        PinEntry entry = new PinEntry(4, 3, false);
        type(entry, "4821", "");

        assertEquals(Outcome.MISMATCH, type(entry, "4822", ""));
        assertEquals(Step.CREATE, entry.step());
        assertEquals(3, entry.attemptsLeft());
        assertEquals(Outcome.CONFIRM_NEEDED, type(entry, "1357", ""));
        assertEquals(Outcome.CREATED, type(entry, "1357", ""));
        assertEquals("1357", entry.createdPin());
    }

    @Test
    void backspaceAndClearRemoveTypedDigits() {
        PinEntry entry = new PinEntry(6, 3, true);
        type(entry, "123", "999999");

        entry.backspace();
        assertEquals(2, entry.filled());
        entry.clear();
        assertEquals(0, entry.filled());
        entry.backspace();
        assertEquals(0, entry.filled());
    }

    @Test
    void thePinHasTheRequestedLength() {
        PinEntry entry = new PinEntry(6, 3, true);

        assertEquals(Outcome.INCOMPLETE, type(entry, "12345", "123456"));
        assertEquals(Outcome.VERIFIED, entry.press(6, "123456"::equals));
    }

    @Test
    void changingAPinEntersTheCurrentOneThenChoosesANewOneTwice() {
        PinEntry entry = PinEntry.changing(4, 6, 3);
        assertEquals(Step.ENTER, entry.step());

        assertEquals(Outcome.OLD_VERIFIED, type(entry, "1234", "1234"));
        assertEquals(Step.CREATE, entry.step());
        assertEquals(6, entry.length());
        assertEquals(Outcome.CONFIRM_NEEDED, type(entry, "482159", ""));
        assertEquals(Outcome.CREATED, type(entry, "482159", ""));
        assertEquals("482159", entry.createdPin());
    }

    @Test
    void aWrongCurrentPinWhileChangingCostsAttemptsLikeAnyOther() {
        PinEntry entry = PinEntry.changing(4, 4, 2);

        assertEquals(Outcome.WRONG, type(entry, "9999", "1234"));
        assertEquals(Outcome.LOCKED_OUT, type(entry, "9999", "1234"));
        assertTrue(entry.isChanging());
    }

    @Test
    void anOrdinaryEntryIsNotAChange() {
        assertFalse(new PinEntry(4, 3, true).isChanging());
    }

    @Test
    void zeroIsNotADigitOfAPinAndOtherThingsThatAreNotDigitsAreIgnored() {
        PinEntry entry = new PinEntry(4, 3, true);

        assertEquals(Outcome.INCOMPLETE, entry.press(0, pin -> true));
        assertEquals(Outcome.INCOMPLETE, entry.press(10, pin -> true));
        assertEquals(Outcome.INCOMPLETE, entry.press(-1, pin -> true));
        assertEquals(0, entry.filled());
    }
}
