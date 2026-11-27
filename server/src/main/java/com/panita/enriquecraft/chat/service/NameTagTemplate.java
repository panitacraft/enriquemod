package com.panita.enriquecraft.chat.service;

/**
 * The format of a name tag, cut where the name goes: what a team puts before the name and what it puts after it. A tag opened before
 * the name is not closed by what comes after, since each side is formatted on its own.
 */
public record NameTagTemplate(String before, String after) {

    /** Where the format puts the name. */
    public static final String NAME = "{name}";

    /** A format without {@link #NAME} puts everything before the name. */
    public static NameTagTemplate of(String format) {
        int at = format.indexOf(NAME);
        if (at < 0) {
            return new NameTagTemplate(format, "");
        }
        return new NameTagTemplate(format.substring(0, at), format.substring(at + NAME.length()));
    }
}
