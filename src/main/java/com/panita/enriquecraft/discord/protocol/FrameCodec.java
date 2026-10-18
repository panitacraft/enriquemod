package com.panita.enriquecraft.discord.protocol;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Optional;
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
        return new Decoded.Frame(new InFrame.Welcome(protocol, channels));
    }

    private static Decoded chat(JsonObject json) {
        String channel = json.get("channel").getAsString();
        String text = json.get("text").getAsString();
        JsonObject author = json.getAsJsonObject("author");
        String discordId = author.get("discordId").getAsString();
        String ign = author.get("ign").getAsString();
        if (!CHANNEL.matcher(channel).matches() || !DISCORD_ID.matcher(discordId).matches()
                || !PLAYER_NAME.matcher(ign).matches() || text.isEmpty() || text.length() > Protocol.MAX_CHAT_LENGTH) {
            return new Decoded.Invalid("bad chat");
        }
        return new Decoded.Frame(new InFrame.Chat(channel, discordId, ign, text));
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
