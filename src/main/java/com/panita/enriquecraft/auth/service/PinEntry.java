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

    private final int length;
    private final int maxAttempts;
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
        this.length = length;
        this.maxAttempts = maxAttempts;
        this.attemptsLeft = maxAttempts;
        this.step = hasPin ? Step.ENTER : Step.CREATE;
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
        if (digit < 0 || digit > 9 || typed.length() >= length) {
            return Outcome.INCOMPLETE;
        }
        typed.append((char) ('0' + digit));
        if (typed.length() < length) {
            return Outcome.INCOMPLETE;
        }
        String pin = typed.toString();
        typed.setLength(0);
        return switch (step) {
            case ENTER -> verifier.test(pin) ? Outcome.VERIFIED : failedAttempt();
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
