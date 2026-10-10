package com.panita.enriquecraft.auth.config;

import com.panita.enriquecraft.auth.message.AuthMessages;
import com.panita.enriquecraft.core.framework.config.ConfigSectionBuilder;
import com.panita.enriquecraft.core.framework.config.ConfigValue;
import com.panita.enriquecraft.core.framework.config.ModConfig;

/**
 * Settings of the auth module, stored in the {@code auth} section of the config file.
 */
public final class AuthConfig implements ModConfig {

    public final ConfigValue<String> screenTitle;
    public final ConfigValue<Integer> pinLength;
    public final ConfigValue<Integer> maxAttempts;
    public final ConfigValue<Integer> sessionHours;
    public final ConfigValue<Integer> loginTimeoutSeconds;
    public final ConfigValue<Integer> kicksBeforeBlock;
    public final ConfigValue<Integer> kickWindowMinutes;
    public final ConfigValue<Integer> blockMinutes;
    public final ConfigValue<Integer> lookupCacheMinutes;
    public final ConfigValue<Integer> lookupMaxRequestsPerTenMinutes;

    public AuthConfig(ConfigSectionBuilder builder) {
        screenTitle = builder.string("screen.title", AuthMessages.Pin.SCREEN_TITLE,
                "Title of the PIN screen. Supports text tags and legacy color codes.",
                text -> !text.isBlank(), "must not be blank");
        pinLength = builder.intRange("pin.length", 4, 4, 6,
                "How many digits the PIN of a new account has. Accounts keep the length they were created with.");
        maxAttempts = builder.intRange("pin.maxAttempts", 3, 1, 10,
                "How many wrong PINs a player may type before being kicked.");
        sessionHours = builder.intRange("session.hours", 24, 1, 720,
                "How long a correct PIN is remembered for the same address, counted from the moment it was typed.");
        loginTimeoutSeconds = builder.intRange("pin.timeoutSeconds", 60, 10, 600,
                "How long a player may take to enter their PIN before being kicked.");
        kicksBeforeBlock = builder.intRange("lockout.kicksBeforeBlock", 3, 1, 20,
                "How many lockouts within the window block an address. A lockout is as many wrong PINs as pin.maxAttempts allows, and every wrong PIN counts when it is typed, so leaving before the kick does not avoid it.");
        kickWindowMinutes = builder.intRange("lockout.windowMinutes", 10, 1, 1440,
                "How long a wrong PIN keeps counting toward a block.");
        blockMinutes = builder.intRange("lockout.blockMinutes", 15, 1, 10080,
                "How long a blocked address is refused.");
        lookupCacheMinutes = builder.intRange("premiumLookup.cacheMinutes", 30, 1, 1440,
                "How long the answer of Mojang about a name is remembered. Only used for names that are not in the whitelist or the op list.");
        lookupMaxRequestsPerTenMinutes = builder.intRange("premiumLookup.maxRequestsPerTenMinutes", 500, 1, 600,
                "How many questions about names the server asks Mojang every ten minutes; Mojang allows 600. Beyond it, players are asked to try again.");
    }
}
