package com.panita.enriquecraft.chat.config;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.panita.enriquecraft.chat.channel.ChannelDefinition;
import com.panita.enriquecraft.core.framework.config.InvalidValueException;
import com.panita.enriquecraft.core.framework.config.ValueType;
import net.minecraft.server.permissions.PermissionLevel;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * How the set of chat channels is written in the config file: an object with one entry per channel. A problem in any
 * channel makes the whole set fall back to its default, so every rule names the channel it is about.
 */
final class ChannelsType implements ValueType<Map<String, ChannelDefinition>> {

    private static final Pattern NAME = Pattern.compile("[a-z0-9_]{1,16}");
    private static final String NAME_RULE = "must be 1 to 16 lowercase letters, digits or _";
    private static final String ALIASES = "aliases";
    private static final String FORMAT = "format";
    private static final String PERMISSION = "permission";
    private static final String FALLBACK_LEVEL = "fallbackLevel";
    private static final String DEFAULT = "default";
    private static final Set<String> SETTINGS = Set.of(ALIASES, FORMAT, PERMISSION, FALLBACK_LEVEL, DEFAULT);

    @Override
    public Map<String, ChannelDefinition> parse(JsonElement element) {
        if (!element.isJsonObject()) {
            throw new InvalidValueException("must be an object with one entry per channel");
        }
        Map<String, ChannelDefinition> channels = new LinkedHashMap<>();
        for (Map.Entry<String, JsonElement> entry : element.getAsJsonObject().entrySet()) {
            if (!NAME.matcher(entry.getKey()).matches()) {
                throw new InvalidValueException("the channel name '" + entry.getKey() + "' " + NAME_RULE);
            }
            channels.put(entry.getKey(), channel(entry.getKey(), entry.getValue()));
        }
        checkTogether(channels);
        return Collections.unmodifiableMap(channels);
    }

    @Override
    public JsonElement toJson(Map<String, ChannelDefinition> value) {
        JsonObject channels = new JsonObject();
        value.forEach((id, channel) -> {
            JsonObject json = new JsonObject();
            JsonArray aliases = new JsonArray();
            channel.aliases().forEach(aliases::add);
            json.add(ALIASES, aliases);
            json.addProperty(FORMAT, channel.format());
            json.addProperty(PERMISSION, channel.permission());
            json.addProperty(FALLBACK_LEVEL, channel.fallbackLevel().id());
            json.addProperty(DEFAULT, channel.isDefault());
            channels.add(id, json);
        });
        return channels;
    }

    @Override
    public Optional<String> constraint() {
        return Optional.of("an object of channels named with 1 to 16 lowercase letters, digits or _; "
                + "global and staff are required and exactly one channel is the default, which needs no permission");
    }

    private static ChannelDefinition channel(String id, JsonElement element) {
        if (!element.isJsonObject()) {
            throw new InvalidValueException("the channel '" + id + "' must be an object");
        }
        JsonObject json = element.getAsJsonObject();
        for (String setting : json.keySet()) {
            if (!SETTINGS.contains(setting)) {
                throw new InvalidValueException("the channel '" + id + "' has an unknown setting '" + setting + "'");
            }
        }
        String format = text(id, json, FORMAT, null);
        if (format.isBlank()) {
            throw new InvalidValueException("the channel '" + id + "' needs a " + FORMAT + " that is not blank");
        }
        return new ChannelDefinition(id, aliases(id, json), format, text(id, json, PERMISSION, "").strip(),
                level(id, json), flag(id, json));
    }

    private static List<String> aliases(String id, JsonObject json) {
        JsonElement element = json.get(ALIASES);
        if (element == null) {
            return List.of();
        }
        if (!element.isJsonArray()) {
            throw new InvalidValueException("the " + ALIASES + " of the channel '" + id + "' must be a list of names");
        }
        List<String> aliases = new ArrayList<>();
        for (JsonElement alias : element.getAsJsonArray()) {
            if (!isText(alias) || !NAME.matcher(alias.getAsString()).matches()) {
                throw new InvalidValueException("each alias of the channel '" + id + "' " + NAME_RULE);
            }
            aliases.add(alias.getAsString());
        }
        return aliases;
    }

    private static String text(String id, JsonObject json, String setting, String missing) {
        JsonElement element = json.get(setting);
        if (element == null && missing != null) {
            return missing;
        }
        if (element == null || !isText(element)) {
            throw new InvalidValueException("the " + setting + " of the channel '" + id + "' must be text");
        }
        return element.getAsString();
    }

    private static PermissionLevel level(String id, JsonObject json) {
        JsonElement element = json.get(FALLBACK_LEVEL);
        if (element == null) {
            return PermissionLevel.ALL;
        }
        if (!element.isJsonPrimitive() || !element.getAsJsonPrimitive().isNumber()) {
            throw new InvalidValueException("the " + FALLBACK_LEVEL + " of the channel '" + id + "' must be a whole number from 0 to 4");
        }
        double number = element.getAsDouble();
        if (number != Math.rint(number) || number < 0 || number > 4) {
            throw new InvalidValueException("the " + FALLBACK_LEVEL + " of the channel '" + id + "' must be a whole number from 0 to 4");
        }
        return PermissionLevel.byId((int) number);
    }

    private static boolean flag(String id, JsonObject json) {
        JsonElement element = json.get(DEFAULT);
        if (element == null) {
            return false;
        }
        if (!element.isJsonPrimitive() || !element.getAsJsonPrimitive().isBoolean()) {
            throw new InvalidValueException("the " + DEFAULT + " of the channel '" + id + "' must be true or false");
        }
        return element.getAsBoolean();
    }

    private static boolean isText(JsonElement element) {
        return element.isJsonPrimitive() && ((JsonPrimitive) element).isString();
    }

    private static void checkTogether(Map<String, ChannelDefinition> channels) {
        for (String required : List.of(ChannelDefinition.GLOBAL, ChannelDefinition.STAFF)) {
            if (!channels.containsKey(required)) {
                throw new InvalidValueException("the channel '" + required + "' is required");
            }
        }
        List<ChannelDefinition> defaults = channels.values().stream().filter(ChannelDefinition::isDefault).toList();
        if (defaults.size() != 1) {
            throw new InvalidValueException("exactly one channel must have " + DEFAULT + " true, but " + defaults.size() + " do");
        }
        if (!defaults.getFirst().permission().isEmpty()) {
            throw new InvalidValueException("the default channel '" + defaults.getFirst().id() + "' must not need a permission");
        }
        Set<String> taken = new HashSet<>();
        for (ChannelDefinition channel : channels.values()) {
            for (String name : channel.names()) {
                if (!taken.add(name)) {
                    throw new InvalidValueException("the name '" + name + "' is used by more than one channel");
                }
            }
        }
    }
}
