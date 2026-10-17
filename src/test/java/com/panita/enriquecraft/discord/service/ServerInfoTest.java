package com.panita.enriquecraft.discord.service;

import com.panita.enriquecraft.discord.protocol.Protocol;
import org.junit.jupiter.api.Test;

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
