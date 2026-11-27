package com.panita.enriquecraft.moderation.service;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * The LuckPerms commands that silence and free the voice chat. The voice chat mod lets a player speak only with the permission
 * {@code speakNode}, so muting everyone denies it to the group everybody is in and grants it back to the staff, and freeing everyone
 * removes both. Muting one player denies it to that player alone. Pure text, so the commands are checked without a server.
 */
public final class VoiceChatCommands {

    private static final Pattern PLAYER_NAME = Pattern.compile("[A-Za-z0-9_]{1,16}");

    private final String group;
    private final String speakNode;

    public VoiceChatCommands(String group, String speakNode) {
        this.group = group;
        this.speakNode = speakNode;
    }

    /** Names go into a command, so only what a Minecraft name can be is accepted. */
    public static boolean isPlayerName(String name) {
        return PLAYER_NAME.matcher(name).matches();
    }

    public List<String> muteAll(List<String> staff) {
        List<String> commands = new ArrayList<>();
        commands.add("lp group " + group + " permission set " + speakNode + " false");
        staff.forEach(name -> commands.add("lp user " + name + " permission set " + speakNode + " true"));
        return commands;
    }

    public List<String> unmuteAll(List<String> staff) {
        List<String> commands = new ArrayList<>();
        commands.add("lp group " + group + " permission unset " + speakNode);
        staff.forEach(name -> commands.add("lp user " + name + " permission unset " + speakNode));
        return commands;
    }

    public List<String> mute(String player) {
        return List.of("lp user " + player + " permission set " + speakNode + " false");
    }

    public List<String> unmute(String player) {
        return List.of("lp user " + player + " permission unset " + speakNode);
    }
}
