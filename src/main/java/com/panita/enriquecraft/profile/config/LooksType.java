package com.panita.enriquecraft.profile.config;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.panita.enriquecraft.core.framework.config.InvalidValueException;
import com.panita.enriquecraft.core.framework.config.ValueType;
import com.panita.enriquecraft.profile.data.EmblemLook;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;

/**
 * How a table of looks is written in the config file: an object with one entry per key, each with an {@code icon} and a
 * {@code color}. A problem in any entry makes the whole table fall back to its default, so every rule names the entry.
 */
final class LooksType implements ValueType<Map<String, EmblemLook>> {

    private static final String ICON = "icon";
    private static final String COLOR = "color";
    private static final Set<String> SETTINGS = Set.of(ICON, COLOR);

    private final Predicate<String> keyRule;
    private final String keyConstraint;

    /**
     * @param keyRule       accepts the keys that may appear
     * @param keyConstraint what a key may be, such as "emblem id"
     */
    LooksType(Predicate<String> keyRule, String keyConstraint) {
        this.keyRule = keyRule;
        this.keyConstraint = keyConstraint;
    }

    @Override
    public Map<String, EmblemLook> parse(JsonElement element) {
        if (!element.isJsonObject()) {
            throw new InvalidValueException("must be an object with one entry per " + keyConstraint);
        }
        Map<String, EmblemLook> looks = new LinkedHashMap<>();
        for (Map.Entry<String, JsonElement> entry : element.getAsJsonObject().entrySet()) {
            if (!keyRule.test(entry.getKey())) {
                throw new InvalidValueException("the key '" + entry.getKey() + "' is not a valid " + keyConstraint);
            }
            looks.put(entry.getKey(), look(entry.getKey(), entry.getValue()));
        }
        return Collections.unmodifiableMap(looks);
    }

    @Override
    public JsonElement toJson(Map<String, EmblemLook> value) {
        JsonObject looks = new JsonObject();
        value.forEach((key, look) -> {
            JsonObject json = new JsonObject();
            json.addProperty(ICON, look.icon());
            json.addProperty(COLOR, look.color());
            looks.add(key, json);
        });
        return looks;
    }

    @Override
    public Optional<String> constraint() {
        return Optional.of("an object with one entry per " + keyConstraint + ", each with an icon of one or two characters and a color "
                + "(a name such as gold or a code such as #ffaa00)");
    }

    private static EmblemLook look(String key, JsonElement element) {
        if (!element.isJsonObject()) {
            throw new InvalidValueException("the look of '" + key + "' must be an object with an icon and a color");
        }
        JsonObject json = element.getAsJsonObject();
        for (String setting : json.keySet()) {
            if (!SETTINGS.contains(setting)) {
                throw new InvalidValueException("the look of '" + key + "' has an unknown setting '" + setting + "'");
            }
        }
        String icon = text(key, json, ICON);
        String color = text(key, json, COLOR);
        if (!EmblemLook.isIcon(icon)) {
            throw new InvalidValueException("the icon of '" + key + "' must be one or two visible characters");
        }
        if (!EmblemLook.isColor(color)) {
            throw new InvalidValueException("the color of '" + key + "' must be a color name or a code such as #ffaa00");
        }
        return new EmblemLook(icon, color);
    }

    private static String text(String key, JsonObject json, String setting) {
        JsonElement element = json.get(setting);
        if (element == null || !element.isJsonPrimitive() || !((JsonPrimitive) element).isString()) {
            throw new InvalidValueException("the " + setting + " of '" + key + "' must be text");
        }
        return element.getAsString();
    }
}
