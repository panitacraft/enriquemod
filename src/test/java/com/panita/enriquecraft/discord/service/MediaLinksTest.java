package com.panita.enriquecraft.discord.service;

import com.panita.enriquecraft.discord.protocol.MediaEntry;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.OptionalInt;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MediaLinksTest {

    private static final List<String> HOSTS = List.of("cdn.discordapp.com", "media.discordapp.net");

    private static MediaEntry at(String url) {
        return new MediaEntry(url, "foto.png", "image/png", 1000, OptionalInt.empty(), OptionalInt.empty(), false);
    }

    @Test
    void readsTheHostsOfASettingInLowerCaseWithoutBlanks() {
        assertEquals(List.of("cdn.discordapp.com", "media.discordapp.net"), MediaLinks.hostsOf(" CDN.discordapp.com , media.discordapp.net,, "));
        assertEquals(List.of(), MediaLinks.hostsOf(" , "));
    }

    @Test
    void aSettingIsAValidListOfHostsOrItIsNot() {
        assertTrue(MediaLinks.isHostList("cdn.discordapp.com,media.discordapp.net"));
        assertTrue(MediaLinks.isHostList("localhost"));
        assertFalse(MediaLinks.isHostList(""));
        assertFalse(MediaLinks.isHostList(" , "));
        assertFalse(MediaLinks.isHostList("https://cdn.discordapp.com"));
        assertFalse(MediaLinks.isHostList("cdn.discordapp.com/path"));
        assertFalse(MediaLinks.isHostList("cdn discordapp com"));
        assertFalse(MediaLinks.isHostList("cdn.discordapp.com,*.example.com"));
    }

    @Test
    void allowsAnHttpsAddressOnAListedHost() {
        assertTrue(MediaLinks.allows(at("https://cdn.discordapp.com/attachments/1/2/foto.png?ex=1"), HOSTS));
        assertTrue(MediaLinks.allows(at("https://MEDIA.discordapp.net/a.png"), HOSTS));
    }

    @Test
    void refusesEveryOtherAddress() {
        for (String url : List.of(
                "http://cdn.discordapp.com/a.png",
                "https://evil.example/a.png",
                "https://cdn.discordapp.com.evil.example/a.png",
                "https://evilcdn.discordapp.com/a.png",
                "https://cdn.discordapp.com@evil.example/a.png",
                "https://evil.example/https://cdn.discordapp.com/a.png",
                "ftp://cdn.discordapp.com/a.png",
                "file:///etc/passwd",
                "/relative.png",
                "not a url")) {
            assertFalse(MediaLinks.allows(at(url), HOSTS), url);
        }
    }

    @Test
    void anEmptyListAllowsNothing() {
        assertFalse(MediaLinks.allows(at("https://cdn.discordapp.com/a.png"), List.of()));
    }

    @Test
    void describesAnImageWithItsDimensionsAndSize() {
        assertEquals("800x600, 245 KB", MediaLinks.details(new MediaEntry("https://cdn.discordapp.com/a.png", "a.png", "image/png", 250_880, OptionalInt.of(800), OptionalInt.of(600), false)));
        assertEquals("512 B", MediaLinks.details(new MediaEntry("https://cdn.discordapp.com/a.png", "a.png", "image/png", 512, OptionalInt.empty(), OptionalInt.empty(), false)));
        assertEquals("8192 KB", MediaLinks.details(new MediaEntry("https://cdn.discordapp.com/a.png", "a.png", "image/png", 8L * 1024 * 1024, OptionalInt.of(10), OptionalInt.empty(), false)));
    }
}
