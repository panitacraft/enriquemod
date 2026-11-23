package com.panita.enriquecraft.chat.message;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * How a channel's format dresses the name of its sender: everything before the message, from the start up to the last placeholder or
 * {@code {sender}}, so the separator and the style of the message are left out. The messages about a player use it to show a name the
 * way chat does, and follow the channel's format when the administrator changes it.
 */
public final class SenderTag {

    private static final String SENDER = "{sender}";
    private static final String MESSAGE = "{message}";
    private static final Pattern PIECE = Pattern.compile(Pattern.quote(SENDER) + "|%[^%\\s]+%");

    private SenderTag() {
    }

    /** The part of the format that stands for the sender; just {@code {sender}} when the format has no such part. */
    public static String of(String format) {
        int message = format.indexOf(MESSAGE);
        String before = message < 0 ? format : format.substring(0, message);
        Matcher matcher = PIECE.matcher(before);
        int end = -1;
        while (matcher.find()) {
            end = matcher.end();
        }
        String tag = end < 0 ? "" : before.substring(0, end);
        return tag.contains(SENDER) ? tag : SENDER;
    }
}
