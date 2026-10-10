package com.panita.enriquecraft.auth.service;

import java.util.UUID;

/**
 * One player typing their PIN, from the first key to the verdict. It wraps {@link PinEntry} with what a
 * screen needs on top: wrong PINs and a correct one are shown for a moment before anything happens, so
 * the player sees the result, and keys are ignored meanwhile. Screens read its state and press its keys;
 * what the verdict leads to (letting the player in, kicking them) is told to the {@link Listener}.
 */
public final class PinFlow {

    /** What the screen is showing right now. */
    public enum Phase {
        /** Waiting for digits. */
        INPUT,
        /** A wrong PIN, or a confirmation that differed. */
        WRONG,
        /** The first typing of a new PIN was accepted; the confirmation comes next. */
        ADVANCE,
        /** The PIN was accepted. */
        SUCCESS
    }

    /** How the last wrong thing was wrong. */
    public enum Failure {
        NONE, WRONG_PIN, MISMATCH
    }

    /** What the verdict leads to. */
    public interface Listener {
        /** The PIN was accepted and its celebration is over. */
        void accepted();

        /** The last wrong PIN was shown and no attempt is left. */
        void lockedOut();
    }

    static final int WRONG_TICKS = 14;
    static final int ADVANCE_TICKS = 6;
    static final int SUCCESS_TICKS = 20;

    private final UUID id;
    private final String name;
    private final PinEntry entry;
    private final AccountService accounts;
    private final Listener listener;
    private Phase phase = Phase.INPUT;
    private Failure failure = Failure.NONE;
    private int phaseTicks;
    private int shakes;
    private boolean lockedOut;
    private boolean created;

    public PinFlow(UUID id, String name, PinEntry entry, AccountService accounts, Listener listener) {
        this.id = id;
        this.name = name;
        this.entry = entry;
        this.accounts = accounts;
        this.listener = listener;
    }

    public void digit(int digit) {
        if (phase == Phase.INPUT) {
            apply(entry.press(digit, pin -> accounts.verify(id, pin)));
        }
    }

    public void backspace() {
        if (phase == Phase.INPUT) {
            entry.backspace();
        }
    }

    public void clear() {
        if (phase == Phase.INPUT) {
            entry.clear();
        }
    }

    private void apply(PinEntry.Outcome outcome) {
        switch (outcome) {
            case INCOMPLETE -> { }
            case VERIFIED -> succeed();
            case CREATED -> {
                accounts.setPin(id, name, entry.createdPin());
                created = true;
                succeed();
            }
            case CONFIRM_NEEDED, OLD_VERIFIED -> enter(Phase.ADVANCE, ADVANCE_TICKS);
            case WRONG -> fail(Failure.WRONG_PIN);
            case MISMATCH -> fail(Failure.MISMATCH);
            case LOCKED_OUT -> {
                lockedOut = true;
                fail(Failure.WRONG_PIN);
            }
        }
    }

    private void succeed() {
        failure = Failure.NONE;
        enter(Phase.SUCCESS, SUCCESS_TICKS);
    }

    private void fail(Failure kind) {
        failure = kind;
        shakes++;
        enter(Phase.WRONG, WRONG_TICKS);
    }

    private void enter(Phase next, int ticks) {
        phase = next;
        phaseTicks = ticks;
    }

    /**
     * Advances the moment a result is shown for.
     *
     * @return whether what the screen shows changed
     */
    public boolean tick() {
        if (phase == Phase.INPUT || --phaseTicks > 0) {
            return false;
        }
        Phase ended = phase;
        phase = Phase.INPUT;
        if (ended == Phase.SUCCESS) {
            listener.accepted();
        } else if (ended == Phase.WRONG) {
            failure = Failure.NONE;
            if (lockedOut) {
                listener.lockedOut();
            }
        }
        return true;
    }

    public Phase phase() {
        return phase;
    }

    public Failure failure() {
        return failure;
    }

    public PinEntry.Step step() {
        return entry.step();
    }

    public int length() {
        return entry.length();
    }

    /** How many dots are filled: while a result is shown, all of them. */
    public int shownDigits() {
        return phase == Phase.INPUT ? entry.filled() : entry.length();
    }

    public int attemptsLeft() {
        return entry.attemptsLeft();
    }

    /** How many attempts there were; zero when the player is choosing a PIN, which has no attempts. */
    public int maxAttempts() {
        return entry.step() == PinEntry.Step.ENTER ? entry.maxAttempts() : 0;
    }

    /** How many times the PIN was wrong; screens shake once for every increase. */
    public int shakes() {
        return shakes;
    }

    public boolean success() {
        return phase == Phase.SUCCESS;
    }

    /** Whether the PIN that was accepted was just chosen, as opposed to entered. */
    public boolean wasCreated() {
        return created;
    }

    /** Whether the player is changing their PIN rather than logging in. */
    public boolean changing() {
        return entry.isChanging();
    }

    public String playerName() {
        return name;
    }
}
