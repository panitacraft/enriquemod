package com.panita.enriquecraft.discord.service;

import com.panita.enriquecraft.discord.protocol.Protocol;
import org.junit.jupiter.api.Test;

import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ServerInfoTest {

    private static final byte[] PNG_SIGNATURE = {(byte) 0x89, 'P', 'N', 'G', '\r', '\n', 0x1A, '\n'};

    private static byte[] png(int length) {
        byte[] bytes = new byte[length];
        System.arraycopy(PNG_SIGNATURE, 0, bytes, 0, PNG_SIGNATURE.length);
        return bytes;
    }

    @Test
    void readsTheIconOfTheServerFolderBeforeTheOneOfTheWorld(@TempDir Path folder) throws IOException {
        Path server = Files.write(folder.resolve("server-icon.png"), png(20));
        Path world = Files.write(folder.resolve("icon.png"), png(30));

        assertArrayEquals(png(20), ServerInfo.readIcon(server, Optional.of(world)).orElseThrow());
    }

    @Test
    void fallsBackToTheIconOfTheWorld(@TempDir Path folder) throws IOException {
        Path world = Files.write(folder.resolve("icon.png"), png(30));

        assertArrayEquals(png(30), ServerInfo.readIcon(folder.resolve("server-icon.png"), Optional.of(world)).orElseThrow());
    }

    @Test
    void hasNoIconWhenNeitherFileExists(@TempDir Path folder) {
        assertTrue(ServerInfo.readIcon(folder.resolve("server-icon.png"), Optional.of(folder.resolve("icon.png"))).isEmpty());
        assertTrue(ServerInfo.readIcon(folder.resolve("server-icon.png"), Optional.empty()).isEmpty());
    }

    @Test
    void keepsAnIconThatFits() {
        byte[] icon = png(Protocol.MAX_ICON_BYTES);

        assertArrayEquals(icon, ServerInfo.fitIcon(Optional.of(icon)).orElseThrow());
    }

    @Test
    void hasNoIconWhenTheServerHasNone() {
        assertTrue(ServerInfo.fitIcon(Optional.empty()).isEmpty());
    }

    @Test
    void dropsAnIconThatIsTooLarge() {
        assertTrue(ServerInfo.fitIcon(Optional.of(png(Protocol.MAX_ICON_BYTES + 1))).isEmpty());
    }

    @Test
    void dropsWhatIsNotAPng() {
        byte[] notPng = new byte[100];
        Arrays.fill(notPng, (byte) 1);

        assertTrue(ServerInfo.fitIcon(Optional.of(notPng)).isEmpty());
        assertTrue(ServerInfo.fitIcon(Optional.of(new byte[0])).isEmpty());
    }
}
