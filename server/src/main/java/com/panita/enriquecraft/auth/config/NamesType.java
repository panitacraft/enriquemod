package com.panita.enriquecraft.auth.config;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;
import com.panita.enriquecraft.core.framework.config.InvalidValueException;
import com.panita.enriquecraft.core.framework.config.ValueType;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;

/**
 * A list of Minecraft names written in the config file. A name that is not valid makes the whole list fall back to its default, so
 * the rule names the entry.
 */
final class NamesType implements ValueType<List<String>> {

    private static final Pattern NAME = Pattern.compile("[A-Za-z0-9_]{1,16}");

    @Override
    public List<String> parse(JsonElement element) {
        if (!element.isJsonArray()) {
            throw new InvalidValueException("must be a list of names");
        }
        List<String> names = new ArrayList<>();
        int position = 1;
        for (JsonElement entry : element.getAsJsonArray()) {
            if (!entry.isJsonPrimitive() || !entry.getAsJsonPrimitive().isString() || !NAME.matcher(entry.getAsString()).matches()) {
                throw new InvalidValueException("entry " + position + " must be a Minecraft name of 1 to 16 letters, digits or _");
            }
            names.add(entry.getAsString());
            position++;
        }
        return List.copyOf(names);
    }

    @Override
    public JsonElement toJson(List<String> value) {
        JsonArray names = new JsonArray();
        value.forEach(name -> names.add(new JsonPrimitive(name)));
        return names;
    }

    @Override
    public Optional<String> constraint() {
        return Optional.of("a list of Minecraft names, each 1 to 16 letters, digits or _");
    }
}
