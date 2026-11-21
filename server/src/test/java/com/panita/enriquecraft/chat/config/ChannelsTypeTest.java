package com.panita.enriquecraft.chat.config;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.panita.enriquecraft.chat.channel.ChannelDefinition;
import com.panita.enriquecraft.core.framework.config.InvalidValueException;
import net.minecraft.server.permissions.PermissionLevel;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ChannelsTypeTest {

    private final ChannelsType type = new ChannelsType();

    private static final String GLOBAL = "\"global\": {\"format\": \"{message}\", \"default\": true}";
    private static final String STAFF = "\"staff\": {\"format\": \"[s] {message}\", \"permission\": \"x.staff\", \"fallbackLevel\": 2}";

    private static JsonElement json(String channels) {
        return JsonParser.parseString("{" + channels + "}");
    }

    private String rejection(String channels) {
        return assertThrows(InvalidValueException.class, () -> type.parse(json(channels))).getMessage();
    }

    @Test
    void defaultsSurviveAWriteAndARead() {
        Map<String, ChannelDefinition> defaults = DefaultChannels.table();

        assertEquals(defaults, type.parse(type.toJson(defaults)));
    }

    @Test
    void defaultsAreTheGlobalAndTheStaffChannel() {
        Map<String, ChannelDefinition> defaults = DefaultChannels.table();

        assertEquals(List.of("global", "staff"), List.copyOf(defaults.keySet()));
        assertTrue(defaults.get("global").isDefault());
        assertEquals("", defaults.get("global").permission());
        assertEquals(List.of("g"), defaults.get("global").aliases());
        assertEquals(PermissionLevel.GAMEMASTERS, defaults.get("staff").fallbackLevel());
    }

    @Test
    void settingsThatAreLeftOutTakeTheirUsualValue() {
        ChannelDefinition global = type.parse(json(GLOBAL + "," + STAFF)).get("global");

        assertEquals(List.of(), global.aliases());
        assertEquals("", global.permission());
        assertEquals(PermissionLevel.ALL, global.fallbackLevel());
        assertEquals("{message}", global.format());
    }

    @Test
    void anotherChannelCanBeAdded() {
        Map<String, ChannelDefinition> channels = type.parse(json(GLOBAL + "," + STAFF
                + ",\"trade\": {\"aliases\": [\"t\", \"comercio\"], \"format\": \"[t] {message}\"}"));

        assertEquals(List.of("global", "staff", "trade"), List.copyOf(channels.keySet()));
        assertEquals(List.of("trade", "t", "comercio"), channels.get("trade").names());
    }

    @Test
    void globalAndStaffAreRequired() {
        assertEquals("the channel 'staff' is required", rejection(GLOBAL));
        assertEquals("the channel 'global' is required", rejection(STAFF));
    }

    @Test
    void exactlyOneChannelIsTheDefault() {
        assertEquals("exactly one channel must have default true, but 0 do",
                rejection("\"global\": {\"format\": \"a\"}," + STAFF));
        assertEquals("exactly one channel must have default true, but 2 do",
                rejection(GLOBAL + ",\"staff\": {\"format\": \"a\", \"default\": true}"));
    }

    @Test
    void theDefaultChannelIsOpenToEveryone() {
        assertEquals("the default channel 'global' must not need a permission",
                rejection("\"global\": {\"format\": \"a\", \"default\": true, \"permission\": \"x\"}," + STAFF));
    }

    @Test
    void aNameBelongsToOneChannel() {
        assertEquals("the name 'g' is used by more than one channel", rejection(
                "\"global\": {\"format\": \"a\", \"default\": true, \"aliases\": [\"g\"]},"
                        + "\"staff\": {\"format\": \"a\", \"aliases\": [\"g\"]}"));
        assertEquals("the name 'staff' is used by more than one channel", rejection(
                "\"global\": {\"format\": \"a\", \"default\": true, \"aliases\": [\"staff\"]}," + STAFF));
    }

    @Test
    void channelNamesAreSmallWords() {
        assertEquals("the channel name 'Global' must be 1 to 16 lowercase letters, digits or _", rejection("\"Global\": {\"format\": \"a\"}"));
        assertEquals("each alias of the channel 'global' must be 1 to 16 lowercase letters, digits or _",
                rejection("\"global\": {\"format\": \"a\", \"aliases\": [\"A B\"]}"));
    }

    @Test
    void unknownSettingIsNamed() {
        assertEquals("the channel 'global' has an unknown setting 'range'",
                rejection("\"global\": {\"format\": \"a\", \"range\": 10}"));
    }

    @Test
    void formatIsRequiredAndMustBeText() {
        assertEquals("the format of the channel 'global' must be text", rejection("\"global\": {}"));
        assertEquals("the format of the channel 'global' must be text", rejection("\"global\": {\"format\": 3}"));
        assertEquals("the channel 'global' needs a format that is not blank", rejection("\"global\": {\"format\": \"  \"}"));
    }

    @Test
    void fallbackLevelIsAWholeNumberFromZeroToFour() {
        for (String level : List.of("5", "-1", "1.5", "\"two\"")) {
            assertEquals("the fallbackLevel of the channel 'global' must be a whole number from 0 to 4",
                    rejection("\"global\": {\"format\": \"a\", \"fallbackLevel\": " + level + "}"), level);
        }
        assertEquals(PermissionLevel.OWNERS, type.parse(json(GLOBAL.replace("\"default\": true", "\"default\": true, \"fallbackLevel\": 4") + "," + STAFF))
                .get("global").fallbackLevel());
    }

    @Test
    void settingsOfTheWrongTypeAreNamed() {
        assertEquals("the channel 'global' must be an object", rejection("\"global\": 3"));
        assertEquals("the aliases of the channel 'global' must be a list of names",
                rejection("\"global\": {\"format\": \"a\", \"aliases\": \"g\"}"));
        assertEquals("the default of the channel 'global' must be true or false",
                rejection("\"global\": {\"format\": \"a\", \"default\": \"yes\"}"));
        assertEquals("the permission of the channel 'global' must be text",
                rejection("\"global\": {\"format\": \"a\", \"permission\": 1}"));
    }

    @Test
    void valueMustBeAnObject() {
        assertEquals("must be an object with one entry per channel",
                assertThrows(InvalidValueException.class, () -> type.parse(JsonParser.parseString("[]"))).getMessage());
    }

    @Test
    void permissionIsStripped() {
        assertFalse(type.parse(json(GLOBAL + ",\"staff\": {\"format\": \"a\", \"permission\": \"  x.staff \"}"))
                .get("staff").permission().contains(" "));
    }
}
