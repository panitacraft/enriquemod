package com.panita.enriquecraft.client;

import net.minecraft.client.KeyMapping;
import net.minecraft.resources.Identifier;

/** The group of the mod's keys in the controls screen, shared by every key the client companion adds. */
public final class ModKeys {

    public static final KeyMapping.Category CATEGORY = KeyMapping.Category.register(Identifier.fromNamespaceAndPath("enriquecraft", "main"));

    private ModKeys() {
    }
}
