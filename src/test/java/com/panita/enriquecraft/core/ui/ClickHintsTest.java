package com.panita.enriquecraft.core.ui;

import com.panita.enriquecraft.MinecraftTestSupport;
import com.panita.enriquecraft.core.gui.MenuFactory;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextColor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ClickHintsTest {

    @TempDir
    Path directory;

    private MenuFactory factory;

    @BeforeEach
    void createFactory() {
        factory = MinecraftTestSupport.menuFactory(directory);
    }

    private static TextColor firstColor(Component component) {
        if (component.getStyle().getColor() != null) {
            return component.getStyle().getColor();
        }
        for (Component sibling : component.getSiblings()) {
            TextColor color = firstColor(sibling);
            if (color != null) {
                return color;
            }
        }
        return null;
    }

    @Test
    void theLeftClickIsAbbreviatedAndNamesTheAction() {
        assertEquals("◀ Clic Izq. para ir", ClickHints.left(factory, "ir").getString());
    }

    @Test
    void theRightClickIsAbbreviatedAndNamesTheAction() {
        assertEquals("▶ Clic Der. para info", ClickHints.right(factory, "info").getString());
    }

    @Test
    void eachClickHasItsOwnPastelColor() {
        assertEquals(TextColor.fromRgb(0xA8E6CF), firstColor(ClickHints.left(factory, "ir")));
        assertEquals(TextColor.fromRgb(0xA0D2F0), firstColor(ClickHints.right(factory, "info")));
    }
}
