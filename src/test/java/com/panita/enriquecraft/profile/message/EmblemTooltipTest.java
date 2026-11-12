package com.panita.enriquecraft.profile.message;

import com.panita.enriquecraft.core.art.InlineArts;
import com.panita.enriquecraft.core.network.ClientCapabilities;
import com.panita.enriquecraft.profile.client.EmblemCategory;
import com.panita.enriquecraft.profile.data.EmblemLook;
import com.panita.enriquecraft.profile.service.EmblemView;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EmblemTooltipTest {

    private final EmblemTooltip tooltip = new EmblemTooltip(() -> 20, new InlineArts(new ClientCapabilities()));

    private static EmblemView emblem(Optional<String> description) {
        return new EmblemView("staff", "Staff", description, Optional.empty(), EmblemCategory.LOCAL_EMBLEM, new EmblemLook("★", "#ff5555"));
    }

    @Test
    void theTitleLineHasTheCharacterAndTheTitleInTheColorOfTheEmblem() {
        Component title = tooltip.title(emblem(Optional.empty()));

        assertEquals("★ Staff", title.getString());
        assertEquals(0xFF5555, title.getSiblings().get(0).getStyle().getColor().getValue());
        assertEquals(0xFF5555, title.getSiblings().get(1).getStyle().getColor().getValue());
    }

    @Test
    void anEmblemWithAnImageHasItInPlaceOfTheCharacterAndTheCharacterIsTheFallback() {
        EmblemView drawn = new EmblemView("staff", "Staff", Optional.empty(), Optional.of("https://res.cloudinary.com/x/staff.png"),
                EmblemCategory.LOCAL_EMBLEM, new EmblemLook("★", "#ff5555"));

        Component title = tooltip.title(drawn);

        assertEquals("★ Staff", title.getString());
        assertInstanceOf(net.minecraft.network.chat.contents.TranslatableContents.class, title.getSiblings().get(0).getContents());
    }

    @Test
    void theDescriptionIsBrokenIntoShortLines() {
        List<Component> lines = tooltip.description(emblem(Optional.of("Del equipo que cuida el servidor todos los dias")));

        assertEquals(List.of("Del equipo que cuida", "el servidor todos", "los dias"), lines.stream().map(Component::getString).toList());
    }

    @Test
    void anEmblemWithoutDescriptionHasOnlyItsTitle() {
        assertEquals(List.of(), tooltip.description(emblem(Optional.empty())));
        assertEquals("★ Staff", tooltip.hover(emblem(Optional.empty())).getString());
    }

    @Test
    void theHoverIsTheTitleThenTheDescriptionOnItsOwnLines() {
        assertEquals("★ Staff\nDel equipo que cuida\nel servidor", tooltip.hover(emblem(Optional.of("Del equipo que cuida el servidor"))).getString());
    }

    @Test
    void theGlyphIsTheCharacterAndShowsTheTooltipWhenPointedAt() {
        Component glyph = tooltip.glyph(emblem(Optional.of("Del equipo")));

        assertEquals("★", glyph.getString());
        assertEquals(0xFF5555, glyph.getStyle().getColor().getValue());
        HoverEvent.ShowText hover = assertInstanceOf(HoverEvent.ShowText.class, glyph.getStyle().getHoverEvent());
        assertTrue(hover.value().getString().startsWith("★ Staff\nDel equipo"));
    }

    @Test
    void theWidthIsReadAtEachUse() {
        int[] width = {40};
        EmblemTooltip adjustable = new EmblemTooltip(() -> width[0], new InlineArts(new ClientCapabilities()));
        EmblemView view = emblem(Optional.of("uno dos tres cuatro"));

        assertEquals(1, adjustable.description(view).size());
        width[0] = 8;
        assertEquals(3, adjustable.description(view).size());
    }
}
