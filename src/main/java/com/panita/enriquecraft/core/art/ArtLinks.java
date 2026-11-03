package com.panita.enriquecraft.core.art;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
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

    /** The longest address the server announces and the client accepts. */
    public static final int MAX_URL = 1024;

    private ArtLinks() {
    }

    /** Whether the address is {@code https}, on a trusted host and not longer than allowed. */
    public static boolean isTrusted(String url) {
        if (url == null || url.length() > MAX_URL) {
            return false;
        }
        try {
            URI address = URI.create(url);
            String host = address.getHost();
            return "https".equals(address.getScheme()) && host != null && address.getUserInfo() == null
                    && TRUSTED.stream().anyMatch(trusted -> host.toLowerCase(Locale.ROOT).equals(trusted));
        } catch (IllegalArgumentException e) {
            return false;
        }
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
