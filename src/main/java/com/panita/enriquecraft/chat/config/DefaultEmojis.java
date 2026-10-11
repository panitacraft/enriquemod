package com.panita.enriquecraft.chat.config;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * The emojis a new config file starts with. Each is a symbol the game's own font draws, since a vanilla client has no
 * other: colored emoji would need a resource pack.
 */
final class DefaultEmojis {

    private DefaultEmojis() {
    }

    static Map<String, String> table() {
        Map<String, String> emojis = new LinkedHashMap<>();
        emojis.put(":heart:", "<red>❤");
        emojis.put("<3", "<red>❤");
        emojis.put(":star:", "<yellow>★");
        emojis.put(":check:", "<green>✔");
        emojis.put(":x:", "<red>✖");
        emojis.put(":warning:", "<yellow>⚠");
        emojis.put(":info:", "<aqua>ℹ");
        emojis.put(":skull:", "<gray>☠");
        emojis.put(":smile:", "<yellow>☺");
        emojis.put(":)", "<yellow>☺");
        emojis.put(":sad:", "<yellow>☹");
        emojis.put(":(", "<yellow>☹");
        emojis.put(":sun:", "<gold>☀");
        emojis.put(":cloud:", "<gray>☁");
        emojis.put(":snowflake:", "<aqua>❄");
        emojis.put(":bolt:", "<yellow>⚡");
        emojis.put(":music:", "<aqua>♪");
        emojis.put(":sword:", "<gray>⚔");
        emojis.put(":mail:", "<white>✉");
        emojis.put(":flag:", "<red>⚑");
        emojis.put(":flower:", "<light_purple>✿");
        emojis.put(":arrow:", "<gray>➜");
        emojis.put(":diamond:", "<aqua>♦");
        emojis.put(":coffee:", "<gold>☕");
        return Collections.unmodifiableMap(emojis);
    }
}
