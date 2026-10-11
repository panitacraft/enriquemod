package com.panita.enriquecraft.chat.text;

import com.panita.enriquecraft.MinecraftTestSupport;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ChatTextParserTest {

    private static final Set<ChatCapability> NOTHING = EnumSet.noneOf(ChatCapability.class);
    private static final Set<ChatCapability> EVERYTHING = EnumSet.allOf(ChatCapability.class);

    private final ChatTextParser parser = new ChatTextParser(
            new EmojiCatalog(() -> Map.of(":heart:", "<red>❤", "<3", "<red>❤"), text -> Component.literal("[" + text + "]")),
            new LinkDetector(() -> Component.literal("tooltip")));

    @BeforeAll
    static void startMinecraft() {
        MinecraftTestSupport.bootstrap();
    }

    @Test
    void textWithoutCapabilitiesStaysLiteral() {
        Component component = parser.parse("<red>hi</red> &6gold <bold>b</bold> https://a.b :heart:", NOTHING);

        assertEquals("<red>hi</red> &6gold <bold>b</bold> https://a.b :heart:", component.getString());
        assertTrue(ComponentProbe.clicks(component).isEmpty());
    }

    @Test
    void colorCapabilityReadsColorTagsAndCodesOnly() {
        Component component = parser.parse("<red>r</red> &6g <bold>b</bold> &lx", Set.of(ChatCapability.COLOR));

        assertEquals("r g <bold>b</bold> &lx", component.getString());
        assertEquals("red", ComponentProbe.part(component, "r").getStyle().getColor().serialize());
        assertEquals("gold", ComponentProbe.part(component, "g <bold>b</bold> &lx").getStyle().getColor().serialize());
    }

    @Test
    void colorCapabilityReadsHexadecimalColors() {
        Component component = parser.parse("&#ff0000hot", Set.of(ChatCapability.COLOR));

        assertEquals("hot", component.getString());
        assertEquals("#FF0000", ComponentProbe.part(component, "hot").getStyle().getColor().serialize());
    }

    @Test
    void formatCapabilityReadsStylesOnly() {
        Component component = parser.parse("&lbold &6gold <red>r</red> <i>it</i>", Set.of(ChatCapability.FORMAT));

        assertEquals("bold &6gold <red>r</red> it", component.getString());
        assertTrue(ComponentProbe.part(component, "bold &6gold <red>r</red> ").getStyle().isBold());
        assertTrue(ComponentProbe.part(component, "it").getStyle().isItalic());
    }

    @Test
    void gradientCapabilityReadsGradientsOnly() {
        Component component = parser.parse("<gradient:#ff0000:#00ff00>ab</gradient> <red>r</red>", Set.of(ChatCapability.GRADIENT));

        assertEquals("ab <red>r</red>", component.getString());
    }

    @Test
    void hoverCapabilityShowsATooltip() {
        Component component = parser.parse("<hover:show_text:'tip'>h</hover>", Set.of(ChatCapability.HOVER));

        assertEquals("h", component.getString());
        assertInstanceOf(HoverEvent.ShowText.class, ComponentProbe.part(component, "h").getStyle().getHoverEvent());
    }

    @Test
    void clickCapabilityAllowsLinksSuggestionsAndCopyButNeverARunCommand() {
        Component component = parser.parse("<open_url:'https://x.y'>a</open_url> <suggest_command:'/msg x'>b</suggest_command> "
                + "<copy_to_clipboard:'text'>c</copy_to_clipboard> <run_command:'/op me'>d</run_command>", Set.of(ChatCapability.CLICK));

        assertEquals("a b c <run_command:'/op me'>d</run_command>", component.getString());
        assertEquals(3, ComponentProbe.clicks(component).size());
        assertTrue(ComponentProbe.clicks(component).stream().noneMatch(ClickEvent.RunCommand.class::isInstance));
    }

    @Test
    void noCapabilityEverRunsACommand() {
        Component component = parser.parse("<run_command:'/op me'>x</run_command> <click:run_command:'/op me'>y</click> "
                + "<run_cmd:'/op me'>z</run_cmd>", EVERYTHING);

        assertTrue(ComponentProbe.clicks(component).stream().noneMatch(ClickEvent.RunCommand.class::isInstance));
        assertTrue(component.getString().contains("/op me"));
    }

    @Test
    void tagsThatAreNotChatTagsAreNeverRead() {
        Component component = parser.parse("<lang:chat.type.text> <keybind:key.jump> <nbt:block:'0 0 0':x> <score:a:b> <selector:@a>", EVERYTHING);

        assertEquals("<lang:chat.type.text> <keybind:key.jump> <nbt:block:'0 0 0':x> <score:a:b> <selector:@a>", component.getString());
    }

    @Test
    void addressBecomesALinkWithoutTheEndOfTheSentence() {
        Component component = parser.parse("mira https://example.com/a?b=1, gracias", Set.of(ChatCapability.LINKS));

        assertEquals("mira https://example.com/a?b=1, gracias", component.getString());
        ClickEvent click = ComponentProbe.part(component, "https://example.com/a?b=1").getStyle().getClickEvent();
        assertEquals(URI.create("https://example.com/a?b=1"), assertInstanceOf(ClickEvent.OpenUrl.class, click).uri());
    }

    @Test
    void onlyWebAddressesAreLinks() {
        Component component = parser.parse("ftp://a.b file:///c javascript:alert(1) HTTP://A.B", Set.of(ChatCapability.LINKS));

        assertEquals(1, ComponentProbe.clicks(component).size());
        assertEquals("HTTP://A.B", assertInstanceOf(ClickEvent.OpenUrl.class, ComponentProbe.clicks(component).getFirst()).uri().toString());
    }

    @Test
    void emojiTriggersBecomeTheirReplacement() {
        Component component = parser.parse("amor :heart: y <3", Set.of(ChatCapability.EMOJI));

        assertEquals("amor [<red>❤] y [<red>❤]", component.getString());
    }

    @Test
    void emojiInsideATagKeepsTheStyleOfTheTag() {
        Component component = parser.parse("<red>amor :heart: fin</red>", Set.of(ChatCapability.COLOR, ChatCapability.EMOJI));

        assertEquals("amor [<red>❤] fin", component.getString());
        Component red = ComponentProbe.flatten(component).stream()
                .filter(part -> part.getStyle().getColor() != null && part.getStyle().getColor().serialize().equals("red"))
                .findFirst().orElseThrow();
        assertEquals("amor [<red>❤] fin", red.getString());
    }

    @Test
    void emojiInsideALinkLeavesTheLinkAlone() {
        Component component = parser.parse("https://a.b/:heart:", Set.of(ChatCapability.LINKS, ChatCapability.EMOJI));

        assertEquals("https://a.b/:heart:", component.getString());
    }

    @Test
    void controlCharactersAndSectionSignsAreRemoved() {
        Component component = parser.parse("  hola\u0000\n§cmundo\u007f ", NOTHING);

        assertEquals("holacmundo", component.getString());
    }

    @Test
    void colorThatIsNotAllowedLeavesNoStyleBehind() {
        Component component = parser.parse("&6dorado", NOTHING);

        assertNull(component.getStyle().getColor());
        assertEquals("&6dorado", component.getString());
    }
}
