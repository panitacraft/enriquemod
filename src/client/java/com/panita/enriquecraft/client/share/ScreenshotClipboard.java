package com.panita.enriquecraft.client.share;

import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Util;

import javax.imageio.ImageIO;
import java.awt.GraphicsEnvironment;
import java.awt.Toolkit;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Path;

/**
 * Puts a saved screenshot on the system clipboard as a picture, so it can be pasted into a chat or an editor. The game's own
 * clipboard only holds text, so this uses the JDK's, off the game thread; where the JDK has no clipboard (a headless
 * environment) the player is told it could not be done.
 */
final class ScreenshotClipboard {

    private ScreenshotClipboard() {
    }

    /** Called on the client thread; says the result in the game chat. */
    static void copy(Path file) {
        Minecraft client = Minecraft.getInstance();
        Util.ioPool().execute(() -> {
            boolean copied = put(file);
            client.execute(() -> client.gui.hud.getChat().addClientSystemMessage(
                    Component.translatable(copied ? "enriquecraft.share.copied" : "enriquecraft.share.copy_failed")));
        });
    }

    private static boolean put(Path file) {
        if (GraphicsEnvironment.isHeadless()) {
            return false;
        }
        try {
            BufferedImage read = ImageIO.read(file.toFile());
            if (read == null) {
                return false;
            }
            // Windows pastes a picture with no transparency best
            BufferedImage picture = new BufferedImage(read.getWidth(), read.getHeight(), BufferedImage.TYPE_INT_RGB);
            picture.getGraphics().drawImage(read, 0, 0, null);
            Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new PictureSelection(picture), null);
            return true;
        } catch (IOException | RuntimeException e) {
            return false;
        }
    }

    private record PictureSelection(BufferedImage picture) implements Transferable {

        @Override
        public DataFlavor[] getTransferDataFlavors() {
            return new DataFlavor[]{DataFlavor.imageFlavor};
        }

        @Override
        public boolean isDataFlavorSupported(DataFlavor flavor) {
            return DataFlavor.imageFlavor.equals(flavor);
        }

        @Override
        public Object getTransferData(DataFlavor flavor) throws UnsupportedFlavorException {
            if (!isDataFlavorSupported(flavor)) {
                throw new UnsupportedFlavorException(flavor);
            }
            return picture;
        }
    }
}
