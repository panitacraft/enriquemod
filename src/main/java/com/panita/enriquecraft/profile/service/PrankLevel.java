package com.panita.enriquecraft.profile.service;

import com.panita.enriquecraft.profile.data.EmblemLook;

import java.util.Comparator;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A prank level, read from the id of its emblem: the configured prefix, the level and, for the plus variant, {@code _plus}
 * ({@code enrique6_prank_2} and {@code enrique6_prank_2_plus}). A higher level outranks a lower one, and at the same level the plus
 * variant outranks the plain one.
 */
record PrankLevel(int level, boolean plus) implements Comparable<PrankLevel> {

    /** What a player with no prank emblem is at. */
    static final PrankLevel DEFAULT = new PrankLevel(0, false);

    private static final Pattern SUFFIX = Pattern.compile("(\\d{1,2})(_plus)?");
    private static final Comparator<PrankLevel> ORDER = Comparator.comparingInt(PrankLevel::level).thenComparing(PrankLevel::plus);

    /** The level an emblem id stands for, when it is a prank id under this prefix. */
    static Optional<PrankLevel> parse(String id, String prefix) {
        if (!id.startsWith(prefix)) {
            return Optional.empty();
        }
        Matcher suffix = SUFFIX.matcher(id.substring(prefix.length()));
        return suffix.matches() ? Optional.of(new PrankLevel(Integer.parseInt(suffix.group(1)), suffix.group(2) != null)) : Optional.empty();
    }

    String id(String prefix) {
        return prefix + level + (plus ? "_plus" : "");
    }

    /** The look of a prank emblem that has none of its own: its level, with a plus sign when it has one and there is room for it. */
    EmblemLook look() {
        String icon = plus && level < 10 ? level + "+" : String.valueOf(level);
        return new EmblemLook(icon, plus ? "gold" : "aqua");
    }

    @Override
    public int compareTo(PrankLevel other) {
        return ORDER.compare(this, other);
    }
}
