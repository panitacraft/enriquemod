package com.panita.enriquecraft.chat.text;

import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.PlainTextContents;

import java.util.ArrayList;
import java.util.List;

/** Looks inside a component, which tests need to tell what a player's text became. */
final class ComponentProbe {

    private ComponentProbe() {
    }

    /** The component and everything below it, in reading order. */
    static List<Component> flatten(Component component) {
        List<Component> parts = new ArrayList<>();
        collect(component, parts);
        return parts;
    }

    static List<ClickEvent> clicks(Component component) {
        return flatten(component).stream().map(part -> part.getStyle().getClickEvent()).filter(click -> click != null).toList();
    }

    /** The first part whose own text is exactly the given one. */
    static Component part(Component component, String text) {
        return flatten(component).stream()
                .filter(part -> part.getContents() instanceof PlainTextContents.LiteralContents literal && literal.text().equals(text))
                .findFirst()
                .orElseThrow(() -> new AssertionError("No part says '" + text + "' in " + component));
    }

    private static void collect(Component component, List<Component> into) {
        into.add(component);
        component.getSiblings().forEach(sibling -> collect(sibling, into));
    }
}
