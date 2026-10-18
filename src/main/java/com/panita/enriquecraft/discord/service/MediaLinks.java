package com.panita.enriquecraft.discord.service;

import com.panita.enriquecraft.discord.protocol.MediaEntry;
import com.panita.enriquecraft.discord.protocol.Protocol;

import java.net.URI;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Which addresses of images the game may link, and how an image is described. The list of hosts is the administrator's
 * (a setting), but the bot's entries are untrusted, so nothing is linked that is not {@code https} on one of those hosts.
 */
public final class MediaLinks {

    private static final Pattern HOST = Pattern.compile("(\\*\\.)?[a-z0-9]([a-z0-9.-]*[a-z0-9])?");
    private static final long KIB = 1024;

    private MediaLinks() {
    }

    /** The hosts of a setting written as names separated by commas, in lower case, without blanks. */
    public static List<String> hostsOf(String setting) {
        return Arrays.stream(setting.split(",")).map(host -> host.strip().toLowerCase(Locale.ROOT)).filter(host -> !host.isEmpty()).toList();
    }

    /** Whether the setting names at least one host and every name is a plausible host name. */
    public static boolean isHostList(String setting) {
        List<String> hosts = hostsOf(setting);
        return !hosts.isEmpty() && hosts.stream().allMatch(host -> HOST.matcher(host).matches());
    }

    /**
     * Whether the image is on {@code https} (or a local host) and on one of the hosts: exactly, or for an entry written
     * {@code *.example.com}, on a subdomain of it (never on {@code example.com} itself, and never on a host that merely ends in the same
     * letters). Starting with a listed name is not enough either.
     */
    public static boolean allows(MediaEntry entry, List<String> hosts) {
        try {
            URI address = URI.create(entry.url());
            String host = address.getHost();
            return Protocol.isSecureOrLocal(address) && host != null && hosts.stream().anyMatch(allowed -> matches(allowed, host.toLowerCase(Locale.ROOT)));
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    /**
     * Hosts a client companion fetches images from on its own. Fixed in the code, not a setting: the player's address goes to
     * them, so neither the administrator nor the bot may widen the list, and a local host is never one of them.
     */
    private static final List<String> TRUSTED = List.of("cdn.discordapp.com", "media.discordapp.net", "*.klipy.com", "*.tenor.com", "*.giphy.com");

    /** Whether a client companion may fetch the image: {@code https} and on a fixed host. */
    public static boolean isTrusted(MediaEntry entry) {
        return isTrusted(entry.url());
    }

    /** The same check for an address, which the client makes again on what it was told. */
    public static boolean isTrusted(String url) {
        try {
            URI address = URI.create(url);
            String host = address.getHost();
            return "https".equalsIgnoreCase(address.getScheme()) && host != null
                    && TRUSTED.stream().anyMatch(allowed -> matches(allowed, host.toLowerCase(Locale.ROOT)));
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    private static boolean matches(String allowed, String host) {
        if (!allowed.startsWith("*.")) {
            return allowed.equals(host);
        }
        String suffix = allowed.substring(1);
        return host.length() > suffix.length() && host.endsWith(suffix);
    }

    /** The dimensions and size of an image for a tooltip, such as {@code 800x600, 245 KB}. */
    public static String details(MediaEntry entry) {
        String size = entry.size() < KIB ? entry.size() + " B" : Math.round(entry.size() / (double) KIB) + " KB";
        if (entry.width().isPresent() && entry.height().isPresent()) {
            return entry.width().getAsInt() + "x" + entry.height().getAsInt() + ", " + size;
        }
        return size;
    }
}
