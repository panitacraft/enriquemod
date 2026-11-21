package com.panita.enriquecraft.client.mixin;

import com.panita.enriquecraft.client.art.InlineArtTextures;
import net.minecraft.client.gui.GlyphSource;
import net.minecraft.network.chat.FontDescription;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Gives the font code the glyphs of the images the server announced. The game finds the glyphs of a font in its own fonts, and a
 * font that is the name of an image is not one of them, so the question is answered here first. Every other font is left to the game.
 */
@Mixin(targets = "net.minecraft.client.gui.font.FontManager$CachedFontProvider")
public abstract class FontManagerGlyphMixin {

    @Inject(method = "getGlyphSource", at = @At("HEAD"), cancellable = true)
    private void enriquecraft$inlineArt(FontDescription description, CallbackInfoReturnable<GlyphSource> callback) {
        InlineArtTextures.sourceFor(description).ifPresent(callback::setReturnValue);
    }
}
