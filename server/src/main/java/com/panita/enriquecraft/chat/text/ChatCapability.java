package com.panita.enriquecraft.chat.text;

import com.panita.enriquecraft.Enriquecraft;
import net.minecraft.server.permissions.PermissionLevel;

/**
 * Something a player may do with the text they write in chat. Each capability is one permission node, so an
 * administrator grants them to groups one by one. Nothing a player writes is ever read with more than their
 * capabilities allow.
 */
public enum ChatCapability {

    /** Color tags and {@code &} color codes. */
    COLOR("color", PermissionLevel.ADMINS),
    /** Bold, italic, underline, strikethrough and obfuscated text. */
    FORMAT("format", PermissionLevel.ADMINS),
    /** Gradient and rainbow tags. */
    GRADIENT("gradient", PermissionLevel.ADMINS),
    /** Tooltips written with the hover tag. */
    HOVER("hover", PermissionLevel.ADMINS),
    /** Click tags that open a link, suggest a command or copy text; never one that runs a command. */
    CLICK("click", PermissionLevel.ADMINS),
    /** Web addresses that become links. */
    LINKS("links", PermissionLevel.ALL),
    /** Emoji triggers that become symbols. */
    EMOJI("emoji", PermissionLevel.ALL);

    private final String node;
    private final PermissionLevel fallback;

    ChatCapability(String id, PermissionLevel fallback) {
        this.node = Enriquecraft.MOD_ID + ".chat." + id;
        this.fallback = fallback;
    }

    /** The permission node that grants it. */
    public String node() {
        return node;
    }

    /** The vanilla level that grants it when no permission manager decides. */
    public PermissionLevel fallback() {
        return fallback;
    }
}
