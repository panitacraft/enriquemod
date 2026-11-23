package com.panita.enriquecraft.core.message;

import com.panita.enriquecraft.MinecraftTestSupport;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.TextColor;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TpsViewTest {

    @BeforeAll
    static void bootstrap() {
        MinecraftTestSupport.bootstrap();
    }

    private static String text(double tps) {
        return TpsView.value(tps).getString();
    }

    private static Integer color(double tps) {
        return TpsView.value(tps).getStyle().getColor().getValue();
    }

    private static Integer of(ChatFormatting formatting) {
        return TextColor.fromLegacyFormat(formatting).getValue();
    }

    @Test
    void anAverageIsRoundedToTwoDecimals() {
        assertEquals("19.57", text(19.567));
        assertEquals("20.00", text(20.0));
    }

    @Test
    void anAverageIsNeverShownAboveTwentyAndAClearlyFasterOneGetsAnAsterisk() {
        assertEquals("20.00", text(20.4));
        assertEquals("*20.00", text(21.5));
    }

    @Test
    void theColorFollowsTheHealthOfTheServer() {
        assertEquals(of(ChatFormatting.GREEN), color(19.0));
        assertEquals(of(ChatFormatting.YELLOW), color(17.0));
        assertEquals(of(ChatFormatting.RED), color(12.0));
        assertEquals(of(ChatFormatting.RED), color(16.0));
    }
}
