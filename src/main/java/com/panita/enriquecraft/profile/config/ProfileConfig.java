package com.panita.enriquecraft.profile.config;

import com.panita.enriquecraft.core.framework.config.ConfigSectionBuilder;
import com.panita.enriquecraft.core.framework.config.ConfigValue;
import com.panita.enriquecraft.core.framework.config.ModConfig;
import com.panita.enriquecraft.profile.data.EmblemLook;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Settings of the profile module, stored in the {@code profile} section of the config file. The address of the Panita API and
 * its key are the link module's.
 */
public final class ProfileConfig implements ModConfig {

    private static final Pattern ID = Pattern.compile("[A-Za-z0-9_-]{1,64}");
    private static final List<String> CATEGORIES = List.of("system", "emblem", "local_emblem");

    public final ConfigValue<String> editionId;
    public final ConfigValue<String> systemPrefixes;
    public final ConfigValue<String> systemIds;
    public final ConfigValue<Integer> systemMax;
    public final ConfigValue<Integer> loreMax;
    public final ConfigValue<Integer> refreshCooldownSeconds;
    public final ConfigValue<Integer> tooltipWidth;
    public final ConfigValue<Map<String, EmblemLook>> looks;
    public final ConfigValue<Map<String, EmblemLook>> categoryLooks;

    public ProfileConfig(ConfigSectionBuilder builder) {
        editionId = builder.string("editionId", "enriquecraft6",
                "The edition whose emblems this server shows. The emblems of any other edition, and those of no edition, are ignored.",
                value -> ID.matcher(value).matches(), "must be 1 to 64 letters, digits, - or _");
        systemPrefixes = builder.string("systemPrefixes", "nationality_",
                "System emblems of the Panita API this server shows, by the start of their id, separated by commas. Empty shows none.",
                ProfileConfig::isIdList, "must be ids made of letters, digits, - or _, separated by commas");
        systemIds = builder.string("systemIds", "staff",
                "Emblems of the edition that are shown with the system emblems, before the nationality: they are always shown, cannot be\n"
                        + "chosen or put away, and count toward systemMax. Ids separated by commas. Empty promotes none.",
                ProfileConfig::isIdList, "must be ids made of letters, digits, - or _, separated by commas");
        systemMax = builder.intRange("systemMax", 2, 0, 10, "The most system emblems shown next to a name.");
        loreMax = builder.intRange("loreMax", 3, 0, 10, "The most emblems a player can choose to show next to their name.");
        refreshCooldownSeconds = builder.intRange("refreshCooldownSeconds", 60, 0, 86_400,
                "Seconds a player must wait before refreshing their own profile again with /profile refresh. 0 turns the wait off.");
        tooltipWidth = builder.intRange("tooltipWidth", 40, 16, 120,
                "The most characters on a line of the description in an emblem's tooltip before it continues on the next line.");
        looks = builder.custom("looks", Map.of("staff", new EmblemLook("★", "#ff5555")),
                "How an emblem looks to a player without the client mod, by its id: icon (one or two characters the game's font draws)\n"
                        + "and color (a name such as gold or a code such as #ffaa00). The icon and the color also draw its tooltip.\n"
                        + "An emblem with no entry looks as its category does (categoryLooks).",
                new LooksType(value -> ID.matcher(value).matches(), "emblem id"));
        categoryLooks = builder.custom("categoryLooks", defaultCategoryLooks(),
                "How an emblem with no entry in looks is drawn, by its category: system, emblem or local_emblem.",
                new LooksType(CATEGORIES::contains, "category (system, emblem or local_emblem)"));
    }

    /** The ids of a setting written as names separated by commas, without blanks. */
    public static List<String> idsOf(String setting) {
        return Arrays.stream(setting.split(",")).map(String::strip).filter(id -> !id.isEmpty()).toList();
    }

    private static Map<String, EmblemLook> defaultCategoryLooks() {
        Map<String, EmblemLook> defaults = new LinkedHashMap<>();
        defaults.put("system", new EmblemLook("●", "gold"));
        defaults.put("emblem", new EmblemLook("○", "gray"));
        defaults.put("local_emblem", new EmblemLook("○", "gray"));
        return Collections.unmodifiableMap(defaults);
    }

    private static boolean isIdList(String value) {
        return value.isBlank() || idsOf(value).stream().allMatch(id -> ID.matcher(id).matches());
    }
}
