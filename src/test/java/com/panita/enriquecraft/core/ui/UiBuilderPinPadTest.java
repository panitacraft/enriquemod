package com.panita.enriquecraft.core.ui;

import com.panita.enriquecraft.core.network.UiElement;
import net.minecraft.network.chat.Component;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UiBuilderPinPadTest {

    private final List<String> pressed = new ArrayList<>();
    private final PinKeys keys = new PinKeys() {
        @Override
        public void digit(int digit) {
            pressed.add("digit " + digit);
        }

        @Override
        public void backspace() {
            pressed.add("backspace");
        }

        @Override
        public void clear() {
            pressed.add("clear");
        }
    };

    private UiLayout layout(UiBuilder builder, UiElement.PinPad pad) {
        return builder.build(pad);
    }

    @Test
    void theKeysGetConsecutiveIdsFromTheBuildersCounter() {
        UiBuilder builder = new UiBuilder(7);

        UiElement.PinPad pad = builder.pinPad(Component.empty(), Component.empty(), 4, 0, 3, 3, 0, false, keys);

        assertEquals(7, pad.firstKeyId());
        assertEquals(7 + UiElement.PinPad.KEYS, layout(builder, pad).nextId());
    }

    @Test
    void everyKeyReachesItsAction() {
        UiBuilder builder = new UiBuilder(0);
        UiElement.PinPad pad = builder.pinPad(Component.empty(), Component.empty(), 4, 0, 3, 3, 0, false, keys);
        UiLayout layout = layout(builder, pad);

        for (int digit = 0; digit <= 9; digit++) {
            layout.handlers().get(pad.firstKeyId() + digit).accept(new UiClick(null, 0, false));
        }
        layout.handlers().get(pad.firstKeyId() + UiElement.PinPad.BACKSPACE).accept(new UiClick(null, 0, false));
        layout.handlers().get(pad.firstKeyId() + UiElement.PinPad.CLEAR).accept(new UiClick(null, 0, false));

        assertEquals(List.of("digit 0", "digit 1", "digit 2", "digit 3", "digit 4", "digit 5", "digit 6", "digit 7",
                "digit 8", "digit 9", "backspace", "clear"), pressed);
    }

    @Test
    void onlyLeftPressesCount() {
        UiBuilder builder = new UiBuilder(0);
        UiElement.PinPad pad = builder.pinPad(Component.empty(), Component.empty(), 4, 0, 3, 3, 0, false, keys);

        builder.build(pad).handlers().get(pad.firstKeyId() + 5).accept(new UiClick(null, 1, false));

        assertTrue(pressed.isEmpty());
    }
}
