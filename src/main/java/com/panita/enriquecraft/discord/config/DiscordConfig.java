package com.panita.enriquecraft.discord.config;

import com.panita.enriquecraft.core.framework.config.ConfigSectionBuilder;
import com.panita.enriquecraft.core.framework.config.ConfigValue;
import com.panita.enriquecraft.core.framework.config.ModConfig;
import com.panita.enriquecraft.discord.service.MediaLinks;

import java.net.URI;
import java.util.Set;

/**
 * Settings of the discord module, stored in the {@code discord} section of the config file. The key of the bridge is
 * not here: it is a secret and lives in its own file.
 */
public final class DiscordConfig implements ModConfig {

    private static final Set<String> LOCAL_HOSTS = Set.of("localhost", "127.0.0.1", "[::1]");

    public final ConfigValue<Boolean> enabled;
    public final ConfigValue<String> url;
    public final ConfigValue<String> deliveryPrefix;
    public final ConfigValue<Boolean> showReplies;
    public final ConfigValue<Boolean> mediaEnabled;
    public final ConfigValue<String> mediaHosts;
    public final ConfigValue<Integer> mediaMapSeconds;
    public final ConfigValue<Integer> mediaCooldownSeconds;
    public final ConfigValue<Boolean> mediaDither;
    public final ConfigValue<Boolean> mediaAnimate;
    public final ConfigValue<Boolean> shareEnabled;
    public final ConfigValue<Integer> sharePerMinute;

    public DiscordConfig(ConfigSectionBuilder builder) {
        enabled = builder.bool("enabled", true,
                "Whether the server connects to the Panita Bot to share chat and events with Discord.");
        url = builder.string("url", "",
                "The address of the bridge of the Panita Bot, such as \"wss://bot.example.com/bridge\". Empty keeps the connection off.",
                DiscordConfig::isBridgeUrl, "must be empty, or a wss address (ws is only allowed for localhost)");
        deliveryPrefix = builder.text("deliveryPrefix", "<blue>[Discord]",
                "What is shown before the name of a Discord member in the game chat, written exactly as it should look: text tags, colors and fonts are allowed, and nothing is added around it, so a custom glyph needs no brackets.");
        showReplies = builder.bool("showReplies", true,
                "Whether a Discord reply shows, before it, a line saying which message it answers. Applies at the next connection to the bot.");
        mediaEnabled = builder.bool("mediaEnabled", true,
                "Whether the images that Discord members send are shown in the game chat as links. Off, they are written as [adjunto]. Applies at the next connection to the bot.");
        mediaHosts = builder.string("mediaHosts", "cdn.discordapp.com,media.discordapp.net,*.klipy.com,*.tenor.com,*.giphy.com",
                "The hosts a link to an image may point to, separated by commas; *.example.com allows the subdomains of example.com. Anything else is not shown.",
                MediaLinks::isHostList, "must be one or more host names separated by commas");
        mediaMapSeconds = builder.intRange("mediaMapSeconds", 60, 5, 300,
                "The most seconds an image stays on a map in the hand of a player who clicked it. It also ends as soon as they do anything but move or chat.");
        mediaCooldownSeconds = builder.intRange("mediaCooldownSeconds", 5, 0, 60,
                "The seconds a player waits between two requests to see an image.");
        mediaAnimate = builder.bool("mediaAnimate", true,
                "Whether an animated GIF plays on the map. Off, only its first frame is shown.");
        mediaDither = builder.bool("mediaDither", true,
                "Whether the colors of a picture on a map are blended to look closer to the original, at the cost of some noise.");
        shareEnabled = builder.bool("shareEnabled", true,
                "Whether players with the client companion may share screenshots to the game chat and to Discord. Needs the bot to offer it.");
        sharePerMinute = builder.intRange("sharePerMinute", 3, 1, 20,
                "The most screenshots a player may share in a minute.");
    }

    /** Whether the address may be the bridge's: secure, or a local one for development. */
    static boolean isBridgeUrl(String value) {
        if (value.isEmpty()) {
            return true;
        }
        try {
            URI uri = URI.create(value);
            if (uri.getHost() == null) {
                return false;
            }
            return "wss".equals(uri.getScheme()) || ("ws".equals(uri.getScheme()) && LOCAL_HOSTS.contains(uri.getHost()));
        } catch (IllegalArgumentException e) {
            return false;
        }
    }
}
