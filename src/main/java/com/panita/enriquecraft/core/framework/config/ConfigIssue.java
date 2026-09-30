package com.panita.enriquecraft.core.framework.config;

/**
 * A problem found while loading the config file.
 *
 * @param kind   what went wrong
 * @param path   the full path of the value, such as {@code core.messages.prefix}; empty for the file itself
 * @param detail the reason, in English, meant for the console
 */
public record ConfigIssue(Kind kind, String path, String detail) {

    public enum Kind {
        /** The file could not be read or parsed; nothing from it was applied. */
        SYNTAX_ERROR,
        /** A value has the wrong type or breaks its rule; its default is used. */
        INVALID_VALUE,
        /** A key that no module declares, possibly a typo; it is kept but has no effect. */
        UNKNOWN_KEY
    }
}
