package com.panita.enriquecraft.core.art;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.OptionalInt;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Which addresses of art (an emblem's image, a rank's image) a client companion may fetch on its own, and how an address is named
 * between the server and the client. The hosts are fixed in the code, not a setting: the player's address goes to them, so neither
 * the administrator nor the API may widen the list, and a local host is never one of them.
 */
public final class ArtLinks {

    private static final List<String> TRUSTED = List.of("res.cloudinary.com");
    private static final int ID_BYTES = 6;
    private static final Pattern ID = Pattern.compile("[0-9a-f]{" + ID_BYTES * 2 + "}");
    private static final Pattern SIZE = Pattern.compile("size=(\\d{1,2})");

    /** The longest address the server announces and the client accepts. */
    public static final int MAX_URL = 1024;
    /** The smallest and the tallest an address may ask its art to be drawn, in GUI pixels; text is 8 and ordinary art 10. */
    public static final int MIN_SIZE = 2;
    public static final int MAX_SIZE = 12;

    private ArtLinks() {
    }

    /**
     * Whether the address is {@code https}, on a trusted host and not longer than allowed. It may end in {@code #size=<n>}, which says
     * how tall the art is drawn (see {@link #sizeOf}); any other fragment makes it untrusted.
     */
    public static boolean isTrusted(String url) {
        if (url == null || url.length() > MAX_URL) {
            return false;
        }
        try {
            URI address = URI.create(url);
            String host = address.getHost();
            return "https".equals(address.getScheme()) && host != null && address.getUserInfo() == null
                    && TRUSTED.stream().anyMatch(trusted -> host.toLowerCase(Locale.ROOT).equals(trusted))
                    && (address.getRawFragment() == null || sizeOf(url).isPresent());
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    /**
     * The height in GUI pixels the address asks its art to be drawn at when it ends in {@code #size=<n>}, from {@link #MIN_SIZE} to
     * {@link #MAX_SIZE}; empty when it asks for none. The fragment is never sent to the host, so the client alone reads it.
     */
    public static OptionalInt sizeOf(String url) {
        int hash = url.indexOf('#');
        Matcher size = SIZE.matcher(hash < 0 ? "" : url.substring(hash + 1));
        if (!size.matches()) {
            return OptionalInt.empty();
        }
        int pixels = Integer.parseInt(size.group(1));
        return pixels >= MIN_SIZE && pixels <= MAX_SIZE ? OptionalInt.of(pixels) : OptionalInt.empty();
    }

    /** The address to fetch: the same one without the fragment that only says how the art is drawn. */
    public static String withoutSize(String url) {
        int hash = url.indexOf('#');
        return hash < 0 ? url : url.substring(0, hash);
    }

    /** The short name of an address, which a font and a packet use instead of the address itself. */
    public static String idOf(String url) {
        try {
            byte[] hash = MessageDigest.getInstance("SHA-1").digest(url.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash, 0, ID_BYTES);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("Every Java has SHA-1", e);
        }
    }

    /** Whether the text has the shape of an id made by {@link #idOf}. */
    public static boolean isId(String text) {
        return ID.matcher(text).matches();
    }
}
