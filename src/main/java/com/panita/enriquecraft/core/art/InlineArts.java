package com.panita.enriquecraft.core.art;

import com.panita.enriquecraft.Enriquecraft;
import com.panita.enriquecraft.core.network.ClientCapabilities;
import com.panita.enriquecraft.core.network.ClientFeature;
import com.panita.enriquecraft.core.network.InlineArtS2C;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FontDescription;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;

import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Art drawn inside a line of text, the same line for every reader: a text component that a vanilla client reads as its fallback
 * and a client companion reads as an image.
 * <p>
 * The component is a translation. Its fallback, {@code %1$s}, is shown when the language has no such key, which is the case of every
 * vanilla client, and puts the first argument, the plain text, in its place. The companion's language does have the key and says
 * {@code %2$s}, which puts the second argument in its place: a glyph in a font named after the image, which the companion fills with
 * the picture. No line is built twice and no client is guessed at. The key carries a version, so a companion too old to know it shows
 * the text too.
 * <p>
 * A companion learns what a glyph is from an {@link InlineArtS2C}, sent once before the first line that holds it.
 */
public final class InlineArts {

    /** The key a companion translates to its glyph; the version changes when the way a glyph is drawn does. */
    public static final String KEY = "enriquecraft.art.v1";
    /** The start of the path of the font of a glyph, which ends with the id of its image. */
    public static final String FONT_PATH = "art/";
    /** The one character of a glyph. */
    public static final String GLYPH = "";

    /** The key in the custom data of an item that names the image a client companion draws in its place. */
    public static final String ITEM_KEY = "enriquecraft_art";

    private static final int MAX_KNOWN = 512;

    private final ClientCapabilities capabilities;
    private final Map<String, String> known = new LinkedHashMap<>(16, 0.75F, true) {
        @Override
        protected boolean removeEldestEntry(Map.Entry<String, String> eldest) {
            return size() > MAX_KNOWN;
        }
    };
    private final Map<UUID, Set<String>> sent = new ConcurrentHashMap<>();

    public InlineArts(ClientCapabilities capabilities) {
        this.capabilities = capabilities;
    }

    /**
     * The text with the image drawn in its place for a companion. An address that is not a trusted one leaves the text alone.
     *
     * @param text what everyone else sees
     */
    public Component wrap(Component text, String url) {
        if (!ArtLinks.isTrusted(url)) {
            return text;
        }
        String id = register(url).orElseThrow();
        Identifier font = Identifier.fromNamespaceAndPath(Enriquecraft.MOD_ID, FONT_PATH + id);
        MutableComponent glyph = Component.literal(GLYPH).withStyle(style -> style.withFont(new FontDescription.Resource(font)).withColor(0xFFFFFF));
        return Component.translatableWithFallback(KEY, "%1$s", text, glyph);
    }

    /**
     * The item with the image marked on it: a client companion draws the image in its place, every other client draws the item. The
     * mark is the id in the custom data of the item, so no packet changes; announce the id as for any other image.
     */
    public ItemStack item(ItemStack base, String url) {
        register(url).ifPresent(id -> {
            CompoundTag mark = new CompoundTag();
            mark.putString(ITEM_KEY, id);
            base.set(DataComponents.CUSTOM_DATA, CustomData.of(mark));
        });
        return base;
    }

    /** The id of the image at a trusted address, remembered so it can be announced; empty for any other address. */
    public Optional<String> register(String url) {
        if (!ArtLinks.isTrusted(url)) {
            return Optional.empty();
        }
        String id = ArtLinks.idOf(url);
        synchronized (known) {
            known.put(id, url);
        }
        return Optional.of(id);
    }

    /** Tells the readers who draw art what the images of the line are, once each. Call it before the line is sent. */
    public void announce(Collection<ServerPlayer> readers, Component line) {
        Set<String> ids = new HashSet<>();
        collect(line, ids);
        announceIds(readers, ids);
    }

    /** Tells the readers who draw art what these images (by id, from {@link #register}) are, once each. */
    public void announceIds(Collection<ServerPlayer> readers, Set<String> ids) {
        if (ids.isEmpty()) {
            return;
        }
        for (ServerPlayer reader : readers) {
            if (!capabilities.supports(reader.getUUID(), ClientFeature.INLINE_ART) || !ServerPlayNetworking.canSend(reader, InlineArtS2C.TYPE)) {
                continue;
            }
            Set<String> already = sent.computeIfAbsent(reader.getUUID(), player -> ConcurrentHashMap.newKeySet());
            for (String id : ids) {
                String url;
                synchronized (known) {
                    url = known.get(id);
                }
                if (url != null && already.add(id)) {
                    ServerPlayNetworking.send(reader, new InlineArtS2C(id, url));
                }
            }
        }
    }

    /** Forgets what a player was sent, when they leave. */
    public void forget(UUID player) {
        sent.remove(player);
    }

    /** The ids of the images in a text: the glyphs of every translation with this class's key, however deep. */
    static void collect(Component text, Set<String> ids) {
        if (text.getContents() instanceof TranslatableContents translation) {
            for (Object argument : translation.getArgs()) {
                if (argument instanceof Component component) {
                    if (KEY.equals(translation.getKey())) {
                        idOf(component).ifPresent(ids::add);
                    }
                    collect(component, ids);
                }
            }
        }
        if (text.getStyle().getHoverEvent() instanceof HoverEvent.ShowText hover) {
            collect(hover.value(), ids);
        }
        text.getSiblings().forEach(sibling -> collect(sibling, ids));
    }

    private static Optional<String> idOf(Component glyph) {
        if (glyph.getStyle().getFont() instanceof FontDescription.Resource resource && resource.id().getPath().startsWith(FONT_PATH)) {
            String id = resource.id().getPath().substring(FONT_PATH.length());
            if (ArtLinks.isId(id)) {
                return Optional.of(id);
            }
        }
        return Optional.empty();
    }
}
