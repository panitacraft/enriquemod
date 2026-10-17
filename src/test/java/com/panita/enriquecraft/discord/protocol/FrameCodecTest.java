package com.panita.enriquecraft.discord.protocol;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FrameCodecTest {

    private static final PlayerInfo JAVI = new PlayerInfo(UUID.fromString("dd12be42-52a9-4a91-a8a1-11c01849e498"),
            "Javithor360", Optional.empty(), true, Optional.empty());

    private static JsonObject json(OutFrame frame) {
        return JsonParser.parseString(FrameCodec.encode(frame)).getAsJsonObject();
    }

    private static InFrame frame(String text) {
        return assertInstanceOf(FrameCodec.Decoded.Frame.class, FrameCodec.decode(text)).frame();
    }

    private static void invalid(String text) {
        assertInstanceOf(FrameCodec.Decoded.Invalid.class, FrameCodec.decode(text), text);
    }

    @Test
    void encodesAHello() {
        JsonObject hello = json(new OutFrame.Hello(1, "0.0.1", "26.3", Instant.parse("2026-10-12T10:00:00Z"), 20, Optional.empty()));

        assertEquals("hello", hello.get("type").getAsString());
        assertEquals(1, hello.get("protocol").getAsInt());
        assertEquals("0.0.1", hello.get("modVersion").getAsString());
        assertEquals("26.3", hello.get("minecraftVersion").getAsString());
        assertEquals("2026-10-12T10:00:00Z", hello.get("startedAt").getAsString());
        assertEquals(20, hello.get("maxPlayers").getAsInt());
        assertFalse(hello.has("icon"));
    }

    @Test
    void encodesTheServerIconAsBase64() {
        byte[] icon = {(byte) 0x89, 'P', 'N', 'G', 1, 2, 3};

        JsonObject hello = json(new OutFrame.Hello(1, "0.0.1", "26.3", Instant.parse("2026-10-12T10:00:00Z"), 20, Optional.of(icon)));

        assertArrayEquals(icon, Base64.getDecoder().decode(hello.get("icon").getAsString()));
    }

    @Test
    void aHelloWithTheLargestIconStaysUnderTheFrameLimit() {
        byte[] icon = new byte[Protocol.MAX_ICON_BYTES];

        String text = FrameCodec.encode(new OutFrame.Hello(1, "0.0.1-beta.12+build.3", "26.3", Instant.parse("2026-10-12T10:00:00Z"), 200,
                Optional.of(icon)));

        assertTrue(text.getBytes(StandardCharsets.UTF_8).length < Protocol.MAX_FRAME_BYTES);
        assertFalse(text.contains("\n"));
    }

    @Test
    void encodesAChatLineWithAPlayerWhoHasNoNickname() {
        JsonObject chat = json(new OutFrame.Chat("global", JAVI, "hola", false));
        JsonObject player = chat.getAsJsonObject("player");

        assertEquals("chat", chat.get("type").getAsString());
        assertEquals("global", chat.get("channel").getAsString());
        assertEquals("hola", chat.get("text").getAsString());
        assertEquals(false, chat.get("emote").getAsBoolean());
        assertEquals("dd12be42-52a9-4a91-a8a1-11c01849e498", player.get("uuid").getAsString());
        assertEquals("Javithor360", player.get("name").getAsString());
        assertTrue(player.get("nickname").isJsonNull());
        assertEquals(true, player.get("premium").getAsBoolean());
    }

    @Test
    void encodesTheNicknameWhenThereIsOne() {
        PlayerInfo nicked = new PlayerInfo(JAVI.uuid(), JAVI.name(), Optional.of("Javi"), false, Optional.empty());

        JsonObject player = json(new OutFrame.Chat("global", nicked, "hola", true)).getAsJsonObject("player");

        assertEquals("Javi", player.get("nickname").getAsString());
        assertEquals(false, player.get("premium").getAsBoolean());
    }

    @Test
    void encodesTheSkinOfAPlayerOnlyWhenThereIsOne() {
        String hash = "292009a4925b58f02c77dadc3ecef07ea4c7472f64e0fdc32ce5522489362680";
        PlayerInfo skinned = new PlayerInfo(JAVI.uuid(), JAVI.name(), Optional.empty(), false, Optional.of(hash));

        assertEquals(hash, json(new OutFrame.Chat("global", skinned, "hola", false)).getAsJsonObject("player").get("skin").getAsString());
        assertFalse(json(new OutFrame.Chat("global", JAVI, "hola", false)).getAsJsonObject("player").has("skin"));
    }

    @Test
    void encodesEventsWithOnlyWhatTheyHave() {
        JsonObject death = json(new OutFrame.Event(EventKind.DEATH, Optional.of(JAVI), Optional.of("Javi was slain by Zombie"),
                Optional.of("death.attack.mob")));
        JsonObject started = json(new OutFrame.Event(EventKind.STARTED, Optional.empty(), Optional.empty(), Optional.empty()));

        assertEquals("death", death.get("kind").getAsString());
        assertEquals("Javi was slain by Zombie", death.get("text").getAsString());
        assertEquals("death.attack.mob", death.get("key").getAsString());
        assertTrue(death.has("player"));
        assertEquals("started", started.get("kind").getAsString());
        assertTrue(!started.has("player") && !started.has("text") && !started.has("key"));
    }

    @Test
    void everyKindOfEventHasTheNameTheProtocolUses() {
        assertEquals(List.of("join", "first_join", "leave", "death", "advancement", "broadcast", "started", "stopping"),
                java.util.Arrays.stream(EventKind.values()).map(EventKind::id).toList());
        assertEquals(List.of(true, true, true, true, true, false, false, false),
                java.util.Arrays.stream(EventKind.values()).map(EventKind::aboutPlayer).toList());
    }

    @Test
    void encodesTheTwoKindsOfReply() {
        JsonObject data = new JsonObject();
        data.addProperty("tps", 20.0);

        JsonObject ok = json(OutFrame.Reply.ok("q1", data));
        JsonObject failed = json(OutFrame.Reply.failed("q2", "unknown"));

        assertEquals(true, ok.get("ok").getAsBoolean());
        assertEquals(20.0, ok.getAsJsonObject("data").get("tps").getAsDouble());
        assertEquals(false, failed.get("ok").getAsBoolean());
        assertEquals("unknown", failed.get("error").getAsString());
        assertTrue(!failed.has("data"));
    }

    @Test
    void decodesAWelcome() {
        assertEquals(new InFrame.Welcome(1, List.of("global", "staff")),
                frame("{\"type\":\"welcome\",\"protocol\":1,\"channels\":[\"global\",\"staff\"]}"));
    }

    @Test
    void decodesAChatLineOfALinkedMember() {
        assertEquals(new InFrame.Chat("global", "123456789012345678", "Javithor360", "hola"),
                frame("{\"type\":\"chat\",\"channel\":\"global\",\"author\":{\"discordId\":\"123456789012345678\",\"ign\":\"Javithor360\"},\"text\":\"hola\"}"));
    }

    @Test
    void decodesAQuery() {
        assertEquals(new InFrame.Query("abc", QueryKind.PLAYERLIST), frame("{\"type\":\"query\",\"id\":\"abc\",\"what\":\"playerlist\"}"));
        assertEquals(new InFrame.Query("abc", QueryKind.TPS), frame("{\"type\":\"query\",\"id\":\"abc\",\"what\":\"tps\"}"));
        assertEquals(new InFrame.Query("abc", QueryKind.STATUS), frame("{\"type\":\"query\",\"id\":\"abc\",\"what\":\"status\"}"));
    }

    @Test
    void decodesAnError() {
        assertEquals(new InFrame.Error("rate", "too many frames"), frame("{\"type\":\"error\",\"code\":\"rate\",\"message\":\"too many frames\"}"));
    }

    @Test
    void skipsAFrameOfAnUnknownTypeAndIgnoresExtraFields() {
        assertInstanceOf(FrameCodec.Decoded.Ignored.class, FrameCodec.decode("{\"type\":\"telemetry\",\"x\":1}"));
        assertEquals(new InFrame.Welcome(1, List.of()), frame("{\"type\":\"welcome\",\"protocol\":1,\"channels\":[],\"later\":true}"));
    }

    @Test
    void rejectsWhatIsNotAValidFrame() {
        invalid("not json");
        invalid("[]");
        invalid("{}");
        invalid("{\"type\":1}");
        invalid("{\"type\":\"welcome\"}");
        invalid("{\"type\":\"welcome\",\"protocol\":1,\"channels\":[\"Bad Channel\"]}");
        invalid("{\"type\":\"query\",\"id\":\"a\",\"what\":\"everything\"}");
        invalid("{\"type\":\"query\",\"id\":\"\",\"what\":\"tps\"}");
    }

    @Test
    void rejectsAChatLineThatIsEmptyTooLongOrFromAnInvalidAuthor() {
        String valid = "{\"type\":\"chat\",\"channel\":\"global\",\"author\":{\"discordId\":\"123456789012345678\",\"ign\":\"Javithor360\"},\"text\":\"%s\"}";
        invalid(valid.formatted(""));
        invalid(valid.formatted("x".repeat(257)));
        invalid(valid.replace("Javithor360", "not a name"));
        invalid(valid.replace("123456789012345678", "123"));
        invalid(valid.replace("\"global\"", "\"Global\"").formatted("hola"));
    }

    @Test
    void rejectsAFrameOverTheSizeLimit() {
        invalid("{\"type\":\"error\",\"code\":\"x\",\"message\":\"" + "x".repeat(Protocol.MAX_FRAME_BYTES) + "\"}");
    }
}
