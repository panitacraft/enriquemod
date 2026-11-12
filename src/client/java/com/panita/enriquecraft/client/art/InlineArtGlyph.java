package com.panita.enriquecraft.client.art;

import com.mojang.blaze3d.font.GlyphInfo;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.renderpearl.api.pipeline.RenderPipeline;
import com.mojang.renderpearl.api.textures.GpuTextureView;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GlyphSource;
import net.minecraft.client.gui.font.GlyphRenderTypes;
import net.minecraft.client.gui.font.PlainTextRenderable;
import net.minecraft.client.gui.font.TextRenderable;
import net.minecraft.client.gui.font.glyphs.BakedGlyph;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.Identifier;
import net.minecraft.util.RandomSource;
import org.jspecify.annotations.Nullable;
import org.joml.Matrix4fc;

/**
 * The one glyph of the font of an image: it draws that image, at the height of the text, and has the width the image has at that
 * height. While the image is not ready it takes the room of a letter and draws nothing, so a line that arrives before its image only
 * shifts a little when the image is there. Used on the client thread only.
 */
final class InlineArtGlyph implements GlyphSource, BakedGlyph {

    /** The width of a glyph whose image is not ready. */
    private static final float PLACEHOLDER_ADVANCE = 8.0F;
    /** Taller than the text (8) and raised by RAISE, so it reaches a little above and below the line without moving the text. */
    private static final float HEIGHT = 10.0F;
    private static final float RAISE = 1.0F;

    private @Nullable Identifier texture;
    private float advance = PLACEHOLDER_ADVANCE;
    private @Nullable GlyphRenderTypes renderTypes;

    /** Makes the glyph draw the texture, whose width at the height of the text is {@code width}. */
    void show(Identifier texture, float width) {
        this.texture = texture;
        this.advance = width;
        this.renderTypes = GlyphRenderTypes.createForColorTexture(texture);
    }

    /** Makes the glyph draw nothing again. */
    void hide() {
        texture = null;
        advance = PLACEHOLDER_ADVANCE;
        renderTypes = null;
    }

    @Override
    public BakedGlyph getGlyph(int codepoint) {
        return this;
    }

    @Override
    public BakedGlyph getRandomGlyph(RandomSource random, int width) {
        return this;
    }

    @Override
    public GlyphInfo info() {
        return GlyphInfo.simple(advance);
    }

    @Override
    public TextRenderable.@Nullable Styled createGlyph(float x, float y, int color, int shadowColor, Style style, float boldOffset,
                                                       float shadowOffset) {
        if (texture == null || renderTypes == null) {
            return null;
        }
        return new Instance(texture, renderTypes, advance, x, y, color, shadowColor, shadowOffset, style);
    }

    private record Instance(Identifier texture, GlyphRenderTypes renderTypes, float width, float x, float y, int color, int shadowColor,
                            float shadowOffset, Style style) implements PlainTextRenderable {

        @Override
        public void renderSprite(Matrix4fc pose, VertexConsumer buffer, int packedLightCoords, float offsetX, float offsetY, float z, int color) {
            float x0 = offsetX + left();
            float x1 = offsetX + right();
            float y0 = offsetY + top() - RAISE;
            float y1 = y0 + HEIGHT;
            buffer.addVertex(pose, x0, y0, z).setUv(0, 0).setColor(color).setLight(packedLightCoords);
            buffer.addVertex(pose, x0, y1, z).setUv(0, 1).setColor(color).setLight(packedLightCoords);
            buffer.addVertex(pose, x1, y1, z).setUv(1, 1).setColor(color).setLight(packedLightCoords);
            buffer.addVertex(pose, x1, y0, z).setUv(1, 0).setColor(color).setLight(packedLightCoords);
        }

        @Override
        public float height() {
            return HEIGHT;
        }

        @Override
        public RenderType renderType(Font.DisplayMode displayMode) {
            return renderTypes.select(displayMode);
        }

        @Override
        public RenderPipeline guiPipeline() {
            return renderTypes.guiPipeline();
        }

        @Override
        public GpuTextureView textureView() {
            return Minecraft.getInstance().getTextureManager().getTexture(texture).getTextureView();
        }
    }
}
