package com.panita.enriquecraft.auth.service;

import java.util.function.Predicate;

/**
 * The state of one player typing a PIN, digit by digit: entering an existing PIN, or choosing a new one
 * (typed twice). It decides nothing about the player; it only says what each key press led to, so the
 * screens, which differ for each kind of client, can show it.
 */
public final class PinEntry {

    public enum Step {
        /** The player has a PIN and types it. */
        ENTER,
        /** The player chooses a PIN. */
        CREATE,
        /** The player types the chosen PIN again. */
        CONFIRM
    }

    /** What a key press led to. */
    public enum Outcome {
        /** More digits are needed, or the press changed nothing. */
        INCOMPLETE,
        /** The PIN was right. */
        VERIFIED,
        /** The current PIN was right and the player is changing it: they now choose the new one. */
        OLD_VERIFIED,
        /** The PIN was wrong and the player may try again. */
        WRONG,
        /** The PIN was wrong and no attempt is left. */
        LOCKED_OUT,
        /** The chosen PIN was typed; now it must be typed again. */
        CONFIRM_NEEDED,
        /** The second typing differed from the first; the player starts choosing again. */
        MISMATCH,
        /** The chosen PIN was confirmed; see {@link #createdPin()}. */
        CREATED
    }

    private int length;
    private final int maxAttempts;
    private final int newLength;
    private final boolean changing;
    private final StringBuilder typed = new StringBuilder();
    private Step step;
    private int attemptsLeft;
    private String first = "";
    private String created = "";

    /**
     * @param length      how many digits the PIN has
     * @param maxAttempts how many wrong PINs are allowed before the player is locked out
     * @param hasPin      whether the player already has a PIN to enter, instead of choosing one
     */
    public PinEntry(int length, int maxAttempts, boolean hasPin) {
        this(length, length, maxAttempts, hasPin ? Step.ENTER : Step.CREATE, false);
    }

    private PinEntry(int length, int newLength, int maxAttempts, Step step, boolean changing) {
        this.length = length;
        this.newLength = newLength;
        this.maxAttempts = maxAttempts;
        this.attemptsLeft = maxAttempts;
        this.step = step;
        this.changing = changing;
    }

    /**
     * A player who changes their PIN: they enter the current one, which proves it is them, and then choose a
     * new one, typed twice. The new PIN may have another length than the old one.
     *
     * @param oldLength the digits of the current PIN
     * @param newLength the digits of the new PIN
     */
    public static PinEntry changing(int oldLength, int newLength, int maxAttempts) {
        return new PinEntry(oldLength, newLength, maxAttempts, Step.ENTER, true);
    }

    /** Whether this is a change of PIN, in which the current one is entered first and the player is already in. */
    public boolean isChanging() {
        return changing;
    }

    public Step step() {
        return step;
    }

    public int length() {
        return length;
    }

    /** How many digits are typed so far. */
    public int filled() {
        return typed.length();
    }

    public int attemptsLeft() {
        return attemptsLeft;
    }

    public int maxAttempts() {
        return maxAttempts;
    }

    /** The PIN the player chose, valid once {@link Outcome#CREATED} was returned. */
    public String createdPin() {
        return created;
    }

    /**
     * Types one digit. When it completes the PIN, the PIN is checked (against the verifier when entering,
     * against the first typing when confirming) and the typed digits start over.
     *
     * @param verifier tells whether a typed PIN is the player's own; only used when entering
     */
    public Outcome press(int digit, Predicate<String> verifier) {
        // A PIN has no zero: the keypads do not have the key, and a client that sends one is ignored.
        if (digit < 1 || digit > 9 || typed.length() >= length) {
            return Outcome.INCOMPLETE;
        }
        typed.append((char) ('0' + digit));
        if (typed.length() < length) {
            return Outcome.INCOMPLETE;
        }
        String pin = typed.toString();
        typed.setLength(0);
        return switch (step) {
            case ENTER -> verifier.test(pin) ? verified() : failedAttempt();
            case CREATE -> {
                first = pin;
                step = Step.CONFIRM;
                yield Outcome.CONFIRM_NEEDED;
            }
            case CONFIRM -> {
                if (pin.equals(first)) {
                    created = pin;
                    yield Outcome.CREATED;
                }
                first = "";
                step = Step.CREATE;
                yield Outcome.MISMATCH;
            }
        };
    }

    /** The current PIN was right: that is the end when logging in, and the start of choosing a new one when changing. */
    private Outcome verified() {
        if (!changing) {
            return Outcome.VERIFIED;
        }
        length = newLength;
        step = Step.CREATE;
        return Outcome.OLD_VERIFIED;
    }

    private Outcome failedAttempt() {
        attemptsLeft--;
        return attemptsLeft <= 0 ? Outcome.LOCKED_OUT : Outcome.WRONG;
    }

    public void backspace() {
        if (!typed.isEmpty()) {
            typed.setLength(typed.length() - 1);
        }
    }

    public void clear() {
        typed.setLength(0);
    }
}
