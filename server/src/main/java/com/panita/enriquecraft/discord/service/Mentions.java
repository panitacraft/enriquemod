package com.panita.enriquecraft.discord.service;

import com.panita.enriquecraft.discord.protocol.OutFrame;
import com.panita.enriquecraft.discord.protocol.Protocol;
import com.panita.enriquecraft.link.client.LinkedPlayer;
import com.panita.enriquecraft.link.service.LinkedPlayers;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Finds the Discord members a line of chat mentions. A mention is {@code @} and the Minecraft name of a linked member, and only
 * the link list decides who that is: the text never names a Discord account, so nobody outside the list, no role and no
 * {@code @everyone} can be mentioned from the game.
 */
public final class Mentions {

    // Not preceded by a name character or another @, so an address such as me@host or @@name is not a mention
    private static final Pattern MENTION = Pattern.compile("(?<![A-Za-z0-9_@])@([A-Za-z0-9_]{1,16})(?![A-Za-z0-9_])");

    private final LinkedPlayers linked;

    public Mentions(LinkedPlayers linked) {
        this.linked = linked;
    }

    /**
     * @return the members mentioned, in the order of the text, each once and at most {@link Protocol#MAX_MENTIONS}; {@code name} is
     *         spelled as the text spells it, so the bot can find it there
     */
    public List<OutFrame.Mention> in(String text) {
        List<OutFrame.Mention> found = new ArrayList<>();
        Set<String> accounts = new HashSet<>();
        Matcher matcher = MENTION.matcher(text);
        while (found.size() < Protocol.MAX_MENTIONS && matcher.find()) {
            String name = matcher.group(1);
            Optional<LinkedPlayer> member = linked.byName(name);
            if (member.isPresent() && accounts.add(member.get().discordId())) {
                found.add(new OutFrame.Mention(name, member.get().discordId()));
            }
        }
        return found;
    }
}
