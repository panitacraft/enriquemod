package com.panita.enriquecraft.client.art;

import com.panita.enriquecraft.core.art.InlineArts;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;

import java.util.Optional;

/**
 * Draws an item the way the screens of the client companion draw every item, except one the server marked with an image
 * ({@link InlineArts#item}): that one is the image, at the size of an item and fitted into it with its proportions, as soon as it is
 * ready, and the item meanwhile.
 */
public final class ArtItems {

    private static final int SIZE = 16;

    private ArtItems() {
    }

    public static void draw(GuiGraphicsExtractor graphics, ItemStack stack, int x, int y) {
        Optional<Identifier> art = artOf(stack);
        if (art.isEmpty()) {
            graphics.item(stack, x, y);
            return;
        }
        // The pose scales the sixteen pixels of an item to the size of the icon; the texture is a square the picture is fitted into
        graphics.blit(RenderPipelines.GUI_TEXTURED, art.get(), x, y, 0.0F, 0.0F, SIZE, SIZE, InlineArtTextures.ICON_SIDE, InlineArtTextures.ICON_SIDE,
                InlineArtTextures.ICON_SIDE, InlineArtTextures.ICON_SIDE, ARGB.white(1.0F));
    }

    private static Optional<Identifier> artOf(ItemStack stack) {
        CustomData data = stack.get(DataComponents.CUSTOM_DATA);
        if (data == null) {
            return Optional.empty();
        }
        String id = data.copyTag().getStringOr(InlineArts.ITEM_KEY, "");
        return id.isEmpty() ? Optional.empty() : InlineArtTextures.iconTexture(id);
    }
}
