package com.panita.enriquecraft.discord.service;

import com.panita.enriquecraft.discord.protocol.Protocol;

import java.util.regex.Pattern;

/**
 * Turns text into what travels over the bridge: plain, without control characters, and not longer than a frame allows.
 */
public final class PlainText {

    private static final Pattern TAG = Pattern.compile("<[^<>]*>");
    private static final Pattern LEGACY_CODE = Pattern.compile("[&§][0-9a-fk-orA-FK-OR]");
    private static final Pattern CONTROL = Pattern.compile("\\p{Cntrl}");
    private static final Pattern INVISIBLE = Pattern.compile("[\\p{Cntrl}\\p{Cf}§]");
    private static final int MAX_EVENT_TEXT = 1024;

    private PlainText() {
    }

    /** What an administrator wrote with text tags and color codes, as plain words. */
    public static String withoutMarkup(String raw) {
        return LEGACY_CODE.matcher(TAG.matcher(raw).replaceAll("")).replaceAll("").strip();
    }

    /** A line of chat: no control characters, and at most as long as the protocol allows. */
    public static String chat(String text) {
        return limited(CONTROL.matcher(text).replaceAll("").strip(), Protocol.MAX_CHAT_LENGTH);
    }

    /** What a player wrote to go with a screenshot: no control or invisible formatting characters, and at most {@code max} long. */
    public static String caption(String text, int max) {
        return limited(INVISIBLE.matcher(text).replaceAll("").strip(), max);
    }

    /** The text of an event: no control characters, and at most as long as the protocol allows. */
    public static String event(String text) {
        return limited(CONTROL.matcher(text).replaceAll("").strip(), MAX_EVENT_TEXT);
    }

    private static String limited(String text, int max) {
        return text.length() <= max ? text : text.substring(0, max);
    }
}
