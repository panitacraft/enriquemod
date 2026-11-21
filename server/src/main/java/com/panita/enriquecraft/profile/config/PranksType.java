package com.panita.enriquecraft.profile.config;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.panita.enriquecraft.core.art.ArtLinks;
import com.panita.enriquecraft.core.framework.config.InvalidValueException;
import com.panita.enriquecraft.core.framework.config.ValueType;
import com.panita.enriquecraft.profile.data.EmblemLook;
import com.panita.enriquecraft.profile.data.PrankLevel;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * How the prank levels are written in the config file: a list, lowest level first, of objects with an {@code id}, a {@code title},
 * an optional {@code description}, an {@code icon} and a {@code color} for players without the client mod, and an optional
 * {@code image}. A problem in any entry makes the whole list fall back to its default, so every rule names the entry.
 */
final class PranksType implements ValueType<List<PrankLevel>> {

    private static final Pattern ID = Pattern.compile("[A-Za-z0-9_-]{1,64}");
    private static final String ID_KEY = "id";
    private static final String TITLE = "title";
    private static final String DESCRIPTION = "description";
    private static final String ICON = "icon";
    private static final String COLOR = "color";
    private static final String IMAGE = "image";
    private static final Set<String> SETTINGS = Set.of(ID_KEY, TITLE, DESCRIPTION, ICON, COLOR, IMAGE);

    @Override
    public List<PrankLevel> parse(JsonElement element) {
        if (!element.isJsonArray() || element.getAsJsonArray().isEmpty()) {
            throw new InvalidValueException("must be a list with at least one prank level");
        }
        List<PrankLevel> levels = new ArrayList<>();
        Set<String> ids = new HashSet<>();
        int position = 1;
        for (JsonElement entry : element.getAsJsonArray()) {
            PrankLevel level = level(position++, entry);
            if (!ids.add(level.id())) {
                throw new InvalidValueException("the id '" + level.id() + "' is used by more than one level");
            }
            levels.add(level);
        }
        return List.copyOf(levels);
    }

    @Override
    public JsonElement toJson(List<PrankLevel> value) {
        JsonArray levels = new JsonArray();
        for (PrankLevel level : value) {
            JsonObject json = new JsonObject();
            json.addProperty(ID_KEY, level.id());
            json.addProperty(TITLE, level.title());
            json.addProperty(DESCRIPTION, level.description());
            json.addProperty(ICON, level.look().icon());
            json.addProperty(COLOR, level.look().color());
            json.addProperty(IMAGE, level.image());
            levels.add(json);
        }
        return levels;
    }

    @Override
    public Optional<String> constraint() {
        return Optional.of("a list of levels, lowest first, each with an id, a title, a description (may be empty), an icon of one or "
                + "two characters, a color and an image (an https address on a trusted host, or empty)");
    }

    private PrankLevel level(int position, JsonElement element) {
        String where = "level " + position;
        if (!element.isJsonObject()) {
            throw new InvalidValueException(where + " must be an object");
        }
        JsonObject json = element.getAsJsonObject();
        for (String setting : json.keySet()) {
            if (!SETTINGS.contains(setting)) {
                throw new InvalidValueException(where + " has an unknown setting '" + setting + "'");
            }
        }
        String id = text(where, json, ID_KEY, false);
        where = "level " + position + " ('" + id + "')";
        String title = text(where, json, TITLE, false);
        String description = text(where, json, DESCRIPTION, true);
        String icon = text(where, json, ICON, false);
        String color = text(where, json, COLOR, false);
        String image = text(where, json, IMAGE, true);
        if (!ID.matcher(id).matches()) {
            throw new InvalidValueException(where + " must have an id of 1 to 64 letters, digits, - or _");
        }
        if (title.isBlank()) {
            throw new InvalidValueException("the title of " + where + " must not be blank");
        }
        if (!EmblemLook.isIcon(icon)) {
            throw new InvalidValueException("the icon of " + where + " must be one or two visible characters");
        }
        if (!EmblemLook.isColor(color)) {
            throw new InvalidValueException("the color of " + where + " must be a color name or a code such as #ffaa00");
        }
        if (!image.isEmpty() && !ArtLinks.isTrusted(image)) {
            throw new InvalidValueException("the image of " + where + " must be an https address on a trusted image host");
        }
        return new PrankLevel(id, title, description, new EmblemLook(icon, color), image);
    }

    /** A text setting; a missing one is empty when it may be and an error when it may not. */
    private static String text(String where, JsonObject json, String setting, boolean optional) {
        JsonElement element = json.get(setting);
        if (element == null && optional) {
            return "";
        }
        if (element == null || !element.isJsonPrimitive() || !((JsonPrimitive) element).isString()) {
            throw new InvalidValueException("the " + setting + " of " + where + " must be text");
        }
        return element.getAsString();
    }
}
