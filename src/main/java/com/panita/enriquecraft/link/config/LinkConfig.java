package com.panita.enriquecraft.link.config;

import com.panita.enriquecraft.core.framework.config.ConfigSectionBuilder;
import com.panita.enriquecraft.core.framework.config.ConfigValue;
import com.panita.enriquecraft.core.framework.config.ModConfig;

import java.net.URI;

/**
 * Settings of the link module, stored in the {@code link} section of the config file. The service key is
 * not here: it is a secret and lives in its own file.
 */
public final class LinkConfig implements ModConfig {

    public final ConfigValue<String> apiUrl;
    public final ConfigValue<Integer> requestTimeoutSeconds;
    public final ConfigValue<Integer> maxRemovalPercent;

    public LinkConfig(ConfigSectionBuilder builder) {
        apiUrl = builder.string("apiUrl", "https://api.panitacraft.com",
                "Address of the Panita API, without a trailing slash.",
                LinkConfig::isHttpUrl, "must be an http or https address without a trailing slash");
        requestTimeoutSeconds = builder.intRange("requestTimeoutSeconds", 10, 1, 120,
                "How long to wait for the Panita API before giving up on a sync.");
        maxRemovalPercent = builder.intRange("maxRemovalPercent", 50, 0, 100,
                "The largest share of the linked players that one sync may remove. A list that would remove more is refused, as it is more likely a mistake than a real change.");
    }

    private static boolean isHttpUrl(String value) {
        try {
            URI uri = URI.create(value);
            return uri.getHost() != null && !value.endsWith("/")
                    && (uri.getScheme().equals("https") || uri.getScheme().equals("http"));
        } catch (IllegalArgumentException | NullPointerException e) {
            return false;
        }
    }
}
