package com.panita.enriquecraft.discord.service;

import com.panita.enriquecraft.discord.protocol.MediaEntry;

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

    private static final Pattern HOST = Pattern.compile("[a-z0-9]([a-z0-9.-]*[a-z0-9])?");
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

    /** Whether the image is on {@code https} and exactly on one of the hosts, which is not the same as the host starting with one. */
    public static boolean allows(MediaEntry entry, List<String> hosts) {
        try {
            URI address = URI.create(entry.url());
            String host = address.getHost();
            return "https".equals(address.getScheme()) && host != null && hosts.contains(host.toLowerCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            return false;
        }
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
