package com.panita.enriquecraft.chat.message;

import com.panita.enriquecraft.MinecraftTestSupport;
import com.panita.enriquecraft.core.message.Message;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextColor;
import net.minecraft.ChatFormatting;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ShareCoordinatesMessagesTest {

    private static final int AQUA = TextColor.fromLegacyFormat(ChatFormatting.AQUA).getValue();
    private static final int GRAY = TextColor.fromLegacyFormat(ChatFormatting.GRAY).getValue();

    @TempDir
    Path directory;

    @BeforeAll
    static void bootstrap() {
        MinecraftTestSupport.bootstrap();
    }

    private Component shared(String template) {
        return MinecraftTestSupport.formatter(directory).format(Message.plain(template)
                .with("player", "Javi").with("x", 10).with("y", 64).with("z", -20).with("dimension", "Overworld"));
    }

    /** The color of each piece of text of the line, in order, with the text. */
    private static List<String> pieces(Component line) {
        List<String> pieces = new ArrayList<>();
        line.visit((style, text) -> {
            if (!text.isBlank()) {
                pieces.add(text.strip() + "@" + (style.getColor() == null ? "none" : Integer.toHexString(style.getColor().getValue())));
            }
            return java.util.Optional.empty();
        }, net.minecraft.network.chat.Style.EMPTY);
        return pieces;
    }

    @Test
    void thePublicLineReadsAsTheTezzlarOneDid() {
        Component line = shared(ChatMessages.ShareCoordinates.PUBLIC);

        assertEquals("Javi se encuentra en X: 10 Y: 64 Z: -20 (Overworld)", line.getString());
        assertEquals(List.of("Javi@" + Integer.toHexString(AQUA), "se encuentra en@" + Integer.toHexString(GRAY),
                        "X:@" + Integer.toHexString(AQUA), "10@" + Integer.toHexString(GRAY),
                        "Y:@" + Integer.toHexString(AQUA), "64@" + Integer.toHexString(GRAY),
                        "Z:@" + Integer.toHexString(AQUA), "-20@" + Integer.toHexString(GRAY),
                        "(@" + Integer.toHexString(GRAY), "Overworld@" + Integer.toHexString(AQUA), ")@" + Integer.toHexString(GRAY)),
                pieces(line));
    }

    @Test
    void theOtherLinesSayWhatTheyDid() {
        assertEquals("Javi te ha compartido su ubicación, X: 10 Y: 64 Z: -20 (Overworld)", shared(ChatMessages.ShareCoordinates.PRIVATE).getString());
        assertEquals("Tu ubicación es X: 10 Y: 64 Z: -20 (Overworld)", shared(ChatMessages.ShareCoordinates.SELF).getString());
        assertEquals("Has compartido tu ubicación con Javi.", shared(ChatMessages.ShareCoordinates.SENT_CONFIRMATION).getString());
        assertEquals("El jugador Javi no está en línea.", shared(ChatMessages.ShareCoordinates.NOT_ONLINE).getString());
    }
}
