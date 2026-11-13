package com.panita.enriquecraft.profile.config;

import com.panita.enriquecraft.core.art.ArtLinks;
import com.panita.enriquecraft.core.framework.config.ConfigSectionBuilder;
import com.panita.enriquecraft.core.framework.config.ConfigValue;
import com.panita.enriquecraft.core.framework.config.ModConfig;
import com.panita.enriquecraft.profile.data.EmblemLook;
import com.panita.enriquecraft.profile.data.PrankLevel;
import com.panita.enriquecraft.profile.message.ProfileMessages;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
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
    private static final String PRANK_IMAGES = "https://res.cloudinary.com/panita/image/upload/v1794522697/panita-extra/pranks/";
    private static final List<String> PRANK_WORDS = List.of("zero", "one", "two", "three");
    private static final List<String> PRANK_COLORS = List.of("white", "yellow", "gold", "red");
    /** The ending asks the client companion to draw it 3 GUI pixels tall, a third of the 10 of the other art, as a small dot. */
    private static final String EMPTY_EMBLEM_IMAGE = "https://res.cloudinary.com/panita/image/upload/v1794522806/panita-extra/emblems/no_emblem.png#size=3";

    public final ConfigValue<String> editionId;
    public final ConfigValue<List<PrankLevel>> pranks;
    public final ConfigValue<Integer> prankCooldownSeconds;
    public final ConfigValue<String> emptyEmblemImage;
    public final ConfigValue<String> systemPrefixes;
    public final ConfigValue<Integer> loreMax;
    public final ConfigValue<Integer> refreshCooldownSeconds;
    public final ConfigValue<Integer> cacheSeconds;
    public final ConfigValue<Integer> tooltipWidth;
    public final ConfigValue<Map<String, EmblemLook>> looks;
    public final ConfigValue<Map<String, EmblemLook>> categoryLooks;

    public ProfileConfig(ConfigSectionBuilder builder) {
        editionId = builder.string("editionId", "enriquecraft6",
                "The edition whose emblems this server shows. The emblems of any other edition, and those of no edition, are ignored.",
                value -> ID.matcher(value).matches(), "must be 1 to 64 letters, digits, - or _");
        pranks = builder.custom("pranks", defaultPranks(),
                "The prank levels, lowest first. Every player shows one before their name. The first level is only where they start: it\n"
                        + "cannot be chosen, and a player who has left it cannot return. They can change to any other level, waiting\n"
                        + "prankCooldownSeconds between changes. The levels belong to the server, not to the Panita API: an emblem of the API with\n"
                        + "the id of a level is never offered as an ordinary emblem. A level has an id, a title,\n"
                        + "a description (may be empty), an icon and a color for players without the client mod, and an image (an https address on a\n"
                        + "trusted image host, or empty).",
                new PranksType());
        prankCooldownSeconds = builder.intRange("prankCooldownSeconds", 604_800, 0, 31_536_000,
                "Seconds a player must wait before changing their prank level again. The first choice has no wait. 0 turns the wait off.");
        emptyEmblemImage = builder.string("emptyEmblemImage", EMPTY_EMBLEM_IMAGE,
                "The image shown in place of an emblem the player has not chosen for one of their loreMax places: an https address on a\n"
                        + "trusted image host, or empty for text only. Ending it in #size=<n> (2 to 12) draws it n GUI pixels tall; ordinary\n"
                        + "emblems are 10 and the text is 8.",
                value -> value.isEmpty() || ArtLinks.isTrusted(value), "must be empty or an https address on a trusted image host");
        systemPrefixes = builder.string("systemPrefixes", "nationality_",
                "System emblems of the Panita API players can choose to show, by the start of their id, separated by commas. Empty offers\n"
                        + "none.",
                ProfileConfig::isIdList, "must be ids made of letters, digits, - or _, separated by commas");
        loreMax = builder.intRange("loreMax", 3, 0, 10, "The most emblems a player can choose to show next to their name, after it.");
        refreshCooldownSeconds = builder.intRange("refreshCooldownSeconds", 60, 0, 86_400,
                "Seconds a player must wait before refreshing their own profile again with /profile refresh. 0 turns the wait off.");
        cacheSeconds = builder.intRange("cacheSeconds", 1_800, 0, 604_800,
                "Seconds a player's profile is kept before joining or opening the menu reads it from the Panita API again. /profile refresh,\n"
                        + "/profile refresh all and /enriquecraft reload always read it. 0 reads it at every join and every menu opening.");
        tooltipWidth = builder.intRange("tooltipWidth", 40, 16, 120,
                "The most characters on a line of the description in an emblem's tooltip before it continues on the next line.");
        looks = builder.custom("looks", Map.of("enriquecraft6_staff", new EmblemLook("★", "#ff5555")),
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

    /** Four levels, each plain and plus, with the images and the colors (white, yellow, gold, red) the server was designed with. */
    private static List<PrankLevel> defaultPranks() {
        Iterator<String> titles = List.of(ProfileMessages.Prank.ZERO, ProfileMessages.Prank.ONE, ProfileMessages.Prank.ONE_PLUS,
                ProfileMessages.Prank.TWO, ProfileMessages.Prank.TWO_PLUS, ProfileMessages.Prank.THREE, ProfileMessages.Prank.THREE_PLUS)
                .iterator();
        List<PrankLevel> levels = new ArrayList<>();
        for (int level = 0; level < PRANK_WORDS.size(); level++) {
            String color = PRANK_COLORS.get(level);
            String image = PRANK_IMAGES + "prank_level_" + PRANK_WORDS.get(level);
            levels.add(new PrankLevel("enrique6_prank_" + level, titles.next(), "", new EmblemLook("☠", color), image + ".png"));
            // Level zero is only where players start, so it has no plus variant
            if (level > 0) {
                levels.add(new PrankLevel("enrique6_prank_" + level + "_plus", titles.next(), "", new EmblemLook("☠⁺", color),
                        image + "_plus.png"));
            }
        }
        return List.copyOf(levels);
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
