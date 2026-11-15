package com.panita.enriquecraft.core.dialog;

import java.util.Set;
import java.util.regex.Pattern;

/**
 * The names of fields and buttons. The game accepts only letters, digits and underscores as an input key, and the
 * reply carries the token and the pressed button next to the fields, so those two names are taken.
 */
final class DialogNames {

    static final String TOKEN_KEY = "token";
    static final String BUTTON_KEY = "button";

    private static final Set<String> RESERVED = Set.of(TOKEN_KEY, BUTTON_KEY);
    private static final Pattern VALID = Pattern.compile("[A-Za-z0-9_]{1,32}");

    private DialogNames() {
    }

    static void require(String name) {
        if (!VALID.matcher(name).matches()) {
            throw new IllegalArgumentException("'" + name + "' must be 1 to 32 letters, digits or underscores");
        }
        if (RESERVED.contains(name)) {
            throw new IllegalArgumentException("'" + name + "' is reserved by the dialog protocol");
        }
    }
}
