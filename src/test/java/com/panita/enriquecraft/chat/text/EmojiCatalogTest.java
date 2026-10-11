package com.panita.enriquecraft.chat.text;

import com.panita.enriquecraft.MinecraftTestSupport;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

class EmojiCatalogTest {

    @BeforeAll
    static void startMinecraft() {
        MinecraftTestSupport.bootstrap();
    }

    private static EmojiCatalog catalog(AtomicReference<Map<String, String>> table, AtomicInteger parsed) {
        return new EmojiCatalog(table::get, text -> {
            parsed.incrementAndGet();
            return Component.literal(text.toUpperCase());
        });
    }

    @Test
    void longestTriggerWins() {
        Map<String, String> table = new LinkedHashMap<>();
        table.put(":)", "a");
        table.put(":))", "b");
        EmojiCatalog catalog = catalog(new AtomicReference<>(table), new AtomicInteger());

        assertEquals("B A", catalog.apply(Component.literal(":)) :)")).getString());
    }

    @Test
    void triggersAreMatchedLiterallyNotAsPatterns() {
        EmojiCatalog catalog = catalog(new AtomicReference<>(Map.of("a.c", "x", "(+)", "y")), new AtomicInteger());

        assertEquals("X abc Y", catalog.apply(Component.literal("a.c abc (+)")).getString());
    }

    @Test
    void emptyTableLeavesTheComponentAsItIs() {
        Component component = Component.literal(":heart:");
        EmojiCatalog catalog = catalog(new AtomicReference<>(Map.of()), new AtomicInteger());

        assertSame(component, catalog.apply(component));
    }

    @Test
    void replacementsAreParsedOnceUntilTheTableChanges() {
        Map<String, String> first = Map.of(":a:", "one");
        AtomicReference<Map<String, String>> table = new AtomicReference<>(first);
        AtomicInteger parsed = new AtomicInteger();
        EmojiCatalog catalog = catalog(table, parsed);

        catalog.apply(Component.literal(":a:"));
        catalog.apply(Component.literal(":a:"));
        assertEquals(1, parsed.get());

        table.set(Map.of(":a:", "two"));
        assertEquals("TWO", catalog.apply(Component.literal(":a:")).getString());
        assertEquals(2, parsed.get());
    }

    @Test
    void tableThatBecomesEmptyStopsReplacing() {
        AtomicReference<Map<String, String>> table = new AtomicReference<>(Map.of(":a:", "one"));
        EmojiCatalog catalog = catalog(table, new AtomicInteger());
        assertEquals("ONE", catalog.apply(Component.literal(":a:")).getString());

        table.set(Map.of());

        assertEquals(":a:", catalog.apply(Component.literal(":a:")).getString());
    }

    @Test
    void partsThatCanBeClickedAreLeftAlone() {
        Component link = Component.literal(":a:").setStyle(Style.EMPTY.withClickEvent(new ClickEvent.OpenUrl(URI.create("https://x.y"))));
        EmojiCatalog catalog = catalog(new AtomicReference<>(Map.of(":a:", "one")), new AtomicInteger());

        assertEquals(":a: ONE", catalog.apply(Component.empty().append(link).append(" :a:")).getString());
    }

    @Test
    void stylesAroundATriggerAreKept() {
        Component bold = Component.literal("x :a: y").withStyle(style -> style.withBold(true));
        EmojiCatalog catalog = catalog(new AtomicReference<>(Map.of(":a:", "one")), new AtomicInteger());

        Component result = catalog.apply(Component.empty().append(bold));

        assertEquals("x ONE y", result.getString());
        assertEquals(true, result.getSiblings().getFirst().getStyle().isBold());
    }
}
