package com.panita.enriquecraft.moderation.service;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class VoiceChatCommandsTest {

    private final VoiceChatCommands commands = new VoiceChatCommands("default", "voicechat.speak");

    @Test
    void mutingEveryoneDeniesTheGroupAndGrantsTheStaff() {
        assertEquals(List.of(
                "lp group default permission set voicechat.speak false",
                "lp user Fiendrox permission set voicechat.speak true",
                "lp user Javithor360 permission set voicechat.speak true"),
                commands.muteAll(List.of("Fiendrox", "Javithor360")));
    }

    @Test
    void freeingEveryoneRemovesBothForTheGroupAndTheStaff() {
        assertEquals(List.of(
                "lp group default permission unset voicechat.speak",
                "lp user Fiendrox permission unset voicechat.speak"),
                commands.unmuteAll(List.of("Fiendrox")));
    }

    @Test
    void withoutStaffOnlyTheGroupChanges() {
        assertEquals(List.of("lp group default permission set voicechat.speak false"), commands.muteAll(List.of()));
    }

    @Test
    void onePlayerIsDeniedAndFreedAlone() {
        assertEquals(List.of("lp user Tezzlar permission set voicechat.speak false"), commands.mute("Tezzlar"));
        assertEquals(List.of("lp user Tezzlar permission unset voicechat.speak"), commands.unmute("Tezzlar"));
    }

    @Test
    void onlyMinecraftNamesGoIntoACommand() {
        assertTrue(VoiceChatCommands.isPlayerName("Tezzlar_3"));
        assertFalse(VoiceChatCommands.isPlayerName("x permission set * true"));
        assertFalse(VoiceChatCommands.isPlayerName(""));
        assertFalse(VoiceChatCommands.isPlayerName("ThisNameIsFarTooLong"));
    }
}
