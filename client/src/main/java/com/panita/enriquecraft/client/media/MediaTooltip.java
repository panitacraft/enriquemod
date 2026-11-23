package com.panita.enriquecraft.client.media;

import com.panita.enriquecraft.discord.service.MediaLinks;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.world.inventory.tooltip.TooltipComponent;

import java.util.Optional;

/**
 * The preview that shows under the text when the label of an image in the chat is pointed at. The hover of the label is a translation
 * the server sends to every reader ({@link MediaLinks#HOVER_KEY}: the text for vanilla clients, the text for this client and the
 * address of the image), and this is where the client turns it into the text and the picture.
 */
public final class MediaTooltip {

    private static final int LOADING_WIDTH = 90;
    private static final int PADDING = 2;

    private MediaTooltip() {
    }

    /** What an image label's hover says: the text to show and the image it stands for. */
    public record Hover(Component text, String url) {
    }

    /** The data the game hands back to {@link #componentFor} when it builds the tooltip. */
    public record Data(String url) implements TooltipComponent {
    }

    /** The hover of the style when it is the one of an image label. */
    public static Optional<Hover> hoverOf(Style style) {
        if (style.getHoverEvent() instanceof HoverEvent.ShowText show && show.value().getContents() instanceof TranslatableContents translation
                && translation.getKey().equals(MediaLinks.HOVER_KEY) && translation.getArgs().length == 3
                && translation.getArgs()[1] instanceof Component text && translation.getArgs()[2] instanceof String url) {
            return Optional.of(new Hover(text, url));
        }
        return Optional.empty();
    }

    /** Registered with Fabric: the picture part of the tooltip, for the data of {@link Data}; any other data is not ours. */
    public static ClientTooltipComponent componentFor(TooltipComponent data) {
        return data instanceof Data media ? new Picture(media.url()) : null;
    }

    private record Picture(String url) implements ClientTooltipComponent {

        @Override
        public int getWidth(Font font) {
            return MediaImages.preview(url).map(image -> image.width() / MediaImages.SCALE).orElse(LOADING_WIDTH);
        }

        @Override
        public int getHeight(Font font) {
            return MediaImages.preview(url).map(image -> image.height() / MediaImages.SCALE).orElse(font.lineHeight) + PADDING;
        }

        @Override
        public void extractText(GuiGraphicsExtractor graphics, Font font, int x, int y) {
            if (MediaImages.preview(url).isEmpty()) {
                graphics.text(font, Component.translatable("enriquecraft.media.loading"), x, y + PADDING, 0xFFAAAAAA);
            }
        }

        @Override
        public void extractImage(Font font, int x, int y, int width, int height, GuiGraphicsExtractor graphics) {
            MediaImages.preview(url).ifPresent(image -> {
                image.advance(System.currentTimeMillis());
                graphics.blit(RenderPipelines.GUI_TEXTURED, image.id(), x, y + PADDING, 0.0F, 0.0F, image.width() / MediaImages.SCALE,
                        image.height() / MediaImages.SCALE, image.width(), image.height(), image.width(), image.height());
            });
        }
    }
}
