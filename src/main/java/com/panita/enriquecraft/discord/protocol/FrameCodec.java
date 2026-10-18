package com.panita.enriquecraft.discord.protocol;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;

import java.math.BigDecimal;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.regex.Pattern;

/**
 * Turns frames into JSON text and back. What comes from the bot is untrusted: it is checked for shape and size here,
 * so nothing past this class has to wonder whether a field is there.
 */
public final class FrameCodec {

    /** What reading a text gave. */
    public sealed interface Decoded {
        /** A frame of a type this version knows, well formed. */
        record Frame(InFrame frame) implements Decoded {
        }

        /** A frame of a type this version does not know, which is skipped so a later version can add types. */
        record Ignored() implements Decoded {
        }

        /** Something that is not a valid frame. */
        record Invalid(String reason) implements Decoded {
        }
    }

    private static final Pattern CHANNEL = Pattern.compile("[a-z0-9_-]{1,32}");
    private static final Pattern PLAYER_NAME = Pattern.compile("[A-Za-z0-9_]{1,16}");
    private static final Pattern DISCORD_ID = Pattern.compile("[0-9]{17,20}");
    private static final Pattern FEATURE = Pattern.compile("[a-z0-9_]{1,32}");
    private static final Pattern CONTROL = Pattern.compile("\\p{Cntrl}");
    private static final int MAX_FEATURES = 16;
    private static final long MAX_DIMENSION = 100_000;
    private static final int MAX_ID = 64;
    private static final int MAX_MESSAGE = 200;

    private FrameCodec() {
    }

    public static String encode(OutFrame frame) {
        JsonObject json = new JsonObject();
        switch (frame) {
            case OutFrame.Hello hello -> {
                json.addProperty("type", "hello");
                json.addProperty("protocol", hello.protocol());
                json.addProperty("modVersion", hello.modVersion());
                json.addProperty("minecraftVersion", hello.minecraftVersion());
                json.addProperty("startedAt", hello.startedAt().toString());
                json.addProperty("maxPlayers", hello.maxPlayers());
                // The basic encoder writes no line breaks
                hello.icon().ifPresent(icon -> json.addProperty("icon", Base64.getEncoder().encodeToString(icon)));
                JsonArray features = new JsonArray();
                hello.features().forEach(features::add);
                json.add("features", features);
            }
            case OutFrame.Chat chat -> {
                json.addProperty("type", "chat");
                json.addProperty("channel", chat.channel());
                json.add("player", player(chat.player()));
                json.addProperty("text", chat.text());
                json.addProperty("emote", chat.emote());
            }
            case OutFrame.Event event -> {
                json.addProperty("type", "event");
                json.addProperty("kind", event.kind().id());
                event.player().ifPresent(player -> json.add("player", player(player)));
                event.text().ifPresent(text -> json.addProperty("text", text));
                event.key().ifPresent(key -> json.addProperty("key", key));
            }
            case OutFrame.Reply reply -> {
                json.addProperty("type", "reply");
                json.addProperty("id", reply.id());
                json.addProperty("ok", reply.data().isPresent());
                reply.data().ifPresent(data -> json.add("data", data));
                reply.error().ifPresent(error -> json.addProperty("error", error));
            }
        }
        return json.toString();
    }

    private static JsonObject player(PlayerInfo player) {
        JsonObject json = new JsonObject();
        json.addProperty("uuid", player.uuid().toString());
        json.addProperty("name", player.name());
        json.addProperty("nickname", player.nickname().orElse(null));
        json.addProperty("premium", player.premium());
        player.skin().ifPresent(skin -> json.addProperty("skin", skin));
        return json;
    }

    public static Decoded decode(String text) {
        if (text.getBytes(StandardCharsets.UTF_8).length > Protocol.MAX_FRAME_BYTES) {
            return new Decoded.Invalid("too large");
        }
        try {
            JsonObject json = JsonParser.parseString(text).getAsJsonObject();
            JsonPrimitive typeField = json.getAsJsonPrimitive("type");
            if (!typeField.isString()) {
                return new Decoded.Invalid("bad type");
            }
            String type = typeField.getAsString();
            return switch (type) {
                case "welcome" -> welcome(json);
                case "chat" -> chat(json);
                case "query" -> query(json);
                case "error" -> new Decoded.Frame(new InFrame.Error(string(json, "code", 32), string(json, "message", MAX_MESSAGE)));
                default -> new Decoded.Ignored();
            };
        } catch (RuntimeException e) {
            return new Decoded.Invalid("not a valid frame");
        }
    }

    private static Decoded welcome(JsonObject json) {
        int protocol = json.get("protocol").getAsInt();
        JsonArray array = json.getAsJsonArray("channels");
        List<String> channels = new ArrayList<>();
        for (JsonElement element : array) {
            String channel = element.getAsString();
            if (!CHANNEL.matcher(channel).matches()) {
                return new Decoded.Invalid("bad channel");
            }
            channels.add(channel);
        }
        return new Decoded.Frame(new InFrame.Welcome(protocol, channels, features(json.get("features"))));
    }

    /** The features of a welcome. They are optional, so whatever is not a short feature name is left out and never fails the frame. */
    private static List<String> features(JsonElement element) {
        List<String> features = new ArrayList<>();
        if (element != null && element.isJsonArray()) {
            for (JsonElement entry : element.getAsJsonArray()) {
                if (features.size() < MAX_FEATURES && entry.isJsonPrimitive() && entry.getAsJsonPrimitive().isString()
                        && FEATURE.matcher(entry.getAsString()).matches()) {
                    features.add(entry.getAsString());
                }
            }
        }
        return features;
    }

    private static Decoded chat(JsonObject json) {
        String channel = json.get("channel").getAsString();
        String text = json.get("text").getAsString();
        JsonObject author = json.getAsJsonObject("author");
        String discordId = author.get("discordId").getAsString();
        String ign = author.get("ign").getAsString();
        List<MediaEntry> media = new ArrayList<>();
        int rejected = media(json.get("media"), media);
        if (!CHANNEL.matcher(channel).matches() || !DISCORD_ID.matcher(discordId).matches()
                || !PLAYER_NAME.matcher(ign).matches() || (text.isEmpty() && media.isEmpty()) || text.length() > Protocol.MAX_CHAT_LENGTH) {
            return new Decoded.Invalid("bad chat");
        }
        return new Decoded.Frame(new InFrame.Chat(channel, discordId, ign, text, media, rejected));
    }

    /**
     * Reads the images of a chat frame into {@code into}, each one on its own: an invalid entry, or one past the limit, does
     * not spoil the others. Returns how many were left out. Where an address may point is decided later, from the config.
     */
    private static int media(JsonElement element, List<MediaEntry> into) {
        if (element == null || !element.isJsonArray()) {
            return 0;
        }
        int rejected = 0;
        for (JsonElement entry : element.getAsJsonArray()) {
            Optional<MediaEntry> read = into.size() < Protocol.MAX_MEDIA ? mediaEntry(entry) : Optional.empty();
            if (read.isPresent()) {
                into.add(read.get());
            } else {
                rejected++;
            }
        }
        return rejected;
    }

    private static Optional<MediaEntry> mediaEntry(JsonElement element) {
        try {
            JsonObject json = element.getAsJsonObject();
            String url = strictString(json.get("url"));
            String name = withoutControlCharacters(strictString(json.get("name"))).strip();
            String type = strictString(json.get("type"));
            long size = strictWholeNumber(json.get("size"));
            OptionalInt width = dimension(json.get("width"));
            OptionalInt height = dimension(json.get("height"));
            JsonElement spoilerField = json.get("spoiler");
            if (spoilerField == null || !spoilerField.isJsonPrimitive() || !spoilerField.getAsJsonPrimitive().isBoolean()) {
                return Optional.empty();
            }
            boolean spoiler = spoilerField.getAsBoolean();
            URI address = URI.create(url);
            boolean valid = url.length() <= Protocol.MAX_MEDIA_URL && "https".equals(address.getScheme()) && address.getHost() != null
                    && !name.isEmpty() && name.length() <= Protocol.MAX_MEDIA_NAME && Protocol.MEDIA_TYPES.contains(type)
                    && size >= 0 && size <= Protocol.MAX_MEDIA_BYTES;
            return valid ? Optional.of(new MediaEntry(url, name, type, size, width, height, spoiler)) : Optional.empty();
        } catch (RuntimeException e) {
            return Optional.empty();
        }
    }

    /** A whole number between 1 and {@value #MAX_DIMENSION}, or nothing when Discord did not say or it makes no sense. */
    private static OptionalInt dimension(JsonElement element) {
        if (element == null || element.isJsonNull()) {
            return OptionalInt.empty();
        }
        long value = strictWholeNumber(element);
        return value >= 1 && value <= MAX_DIMENSION ? OptionalInt.of((int) value) : OptionalInt.empty();
    }

    /** Gson would read the string "yes" as a boolean and "12" as a number; a field of the wrong type is invalid here. */
    private static String strictString(JsonElement element) {
        if (element == null || !element.isJsonPrimitive() || !element.getAsJsonPrimitive().isString()) {
            throw new IllegalArgumentException("not a string");
        }
        return element.getAsString();
    }

    private static long strictWholeNumber(JsonElement element) {
        if (element == null || !element.isJsonPrimitive() || !element.getAsJsonPrimitive().isNumber()) {
            throw new IllegalArgumentException("not a number");
        }
        BigDecimal number = element.getAsBigDecimal();
        return number.stripTrailingZeros().scale() <= 0 ? number.longValueExact() : -1;
    }

    private static String withoutControlCharacters(String value) {
        return CONTROL.matcher(value).replaceAll("");
    }

    private static Decoded query(JsonObject json) {
        String id = string(json, "id", MAX_ID);
        Optional<QueryKind> what = QueryKind.of(json.get("what").getAsString());
        if (id.isEmpty() || what.isEmpty()) {
            return new Decoded.Invalid("bad query");
        }
        return new Decoded.Frame(new InFrame.Query(id, what.get()));
    }

    private static String string(JsonObject json, String field, int max) {
        String value = json.get(field).getAsString();
        if (value.length() > max) {
            throw new IllegalArgumentException(field + " is too long");
        }
        return value;
    }
}
