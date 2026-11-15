package com.panita.enriquecraft.profile.service;

import com.panita.enriquecraft.profile.data.ProfileDetails;

import java.util.Optional;
import java.util.regex.Pattern;

/**
 * Makes what a player wrote about themselves safe to show to everyone. It is plain text: the section sign, control characters
 * (except line breaks) and the invisible characters that change how text reads (such as the ones that flip its direction) are
 * removed, and it is never parsed for formatting.
 */
public final class BioText {

    private static final Pattern MANY_BREAKS = Pattern.compile("\n{3,}");

    private BioText() {
    }

    /**
     * @return the cleaned text, which is empty when the player wrote nothing; empty optional when it is longer than
     *         {@link ProfileDetails#BIO_MAX} characters, since cutting it would change what the player meant
     */
    public static Optional<String> clean(String raw) {
        StringBuilder text = new StringBuilder(raw.length());
        raw.replace("\r\n", "\n").replace('\r', '\n').codePoints()
                .filter(point -> point == '\n' || (point != '§' && !Character.isISOControl(point) && Character.getType(point) != Character.FORMAT))
                .forEach(text::appendCodePoint);
        String cleaned = MANY_BREAKS.matcher(text).replaceAll("\n\n").strip();
        return cleaned.length() > ProfileDetails.BIO_MAX ? Optional.empty() : Optional.of(cleaned);
    }
}
