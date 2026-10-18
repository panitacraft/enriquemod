package com.panita.enriquecraft.client.share;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.panita.enriquecraft.discord.network.ShareBeginC2S;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.ClientCommands;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.contents.TranslatableContents;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.regex.Pattern;

/**
 * The client companion's screenshot sharing: a {@code [Compartir]} button added to the game's line about a saved screenshot, and
 * the client command that button runs, which opens the share screen. The command only ever opens a file the game itself saves in
 * the screenshots folder, by its plain name, and does nothing but show the screen.
 */
public final class ScreenshotSharing {

    static final String COMMAND = "enriquecraft_share";
    static final String COPY_COMMAND = "enriquecraft_copy";
    private static final String SUCCESS_KEY = "screenshot.success";
    private static final Pattern FILE_NAME = Pattern.compile("[A-Za-z0-9_.\\-]{1,100}\\.png");
    private static final long MAX_FILE_BYTES = 40L * 1024 * 1024;

    private ScreenshotSharing() {
    }

    public static void register() {
        ScreenshotSender.register();
        ShareLastKey.register();
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {
            dispatcher.register(ClientCommands.literal(COMMAND).then(ClientCommands.argument("file", StringArgumentType.string())
                    .executes(context -> run(context.getSource().getClient(), StringArgumentType.getString(context, "file"), ShareScreen::new))));
            dispatcher.register(ClientCommands.literal(COPY_COMMAND).then(ClientCommands.argument("file", StringArgumentType.string())
                    .executes(context -> run(context.getSource().getClient(), StringArgumentType.getString(context, "file"), file -> {
                        ScreenshotClipboard.copy(file);
                        return null;
                    }))));
        });
    }

    /** Does something with the screenshot of that name on the client thread, or says it was not found. */
    private static int run(Minecraft client, String name, Function<Path, Screen> action) {
        Optional<Path> file = screenshot(client.gameDirectory, name);
        client.execute(() -> {
            if (file.isPresent()) {
                Screen screen = action.apply(file.get());
                if (screen != null) {
                    client.setScreenAndShow(screen);
                }
            } else {
                client.gui.hud.getChat().addClientSystemMessage(Component.translatable("enriquecraft.share.missing"));
            }
        });
        return 1;
    }

    /** Wraps what the game does with its message about a saved screenshot, adding the share button when the server can take it. */
    public static Consumer<Component> withShareButton(Consumer<Component> original) {
        return message -> original.accept(sharable(message).orElse(message));
    }

    private static Optional<Component> sharable(Component message) {
        if (!(message.getContents() instanceof TranslatableContents contents) || !SUCCESS_KEY.equals(contents.getKey())
                || contents.getArgs().length == 0 || !(contents.getArgs()[0] instanceof Component name)
                || !FILE_NAME.matcher(name.getString()).matches()) {
            return Optional.empty();
        }
        MutableComponent line = Component.empty().append(message).append(Component.literal(" "))
                .append(button("enriquecraft.share.copy.button", "enriquecraft.share.copy.hover", COPY_COMMAND, name.getString()));
        // Copying the picture needs nothing from the server; sharing it does
        if (ClientPlayNetworking.canSend(ShareBeginC2S.TYPE)) {
            line.append(Component.literal(" ")).append(button("enriquecraft.share.button", "enriquecraft.share.button.hover", COMMAND, name.getString()));
        }
        return Optional.of(line);
    }

    private static MutableComponent button(String label, String hover, String command, String file) {
        return Component.translatable(label).withStyle(style -> style
                .withColor(0xF2B134)
                .withClickEvent(new ClickEvent.RunCommand("/" + command + " " + file))
                .withHoverEvent(new HoverEvent.ShowText(Component.translatable(hover))));
    }

    /** The newest screenshot in the screenshots folder, whoever saved it. */
    static Optional<Path> latest(File gameDirectory) {
        File[] files = new File(gameDirectory, Screenshot.SCREENSHOT_DIR).listFiles((folder, name) -> FILE_NAME.matcher(name).matches());
        if (files == null) {
            return Optional.empty();
        }
        return Arrays.stream(files).max(Comparator.comparingLong(File::lastModified)).flatMap(file -> screenshot(gameDirectory, file.getName()));
    }

    /** The saved screenshot with this name, only when it is a plain file name of the screenshots folder and a reasonable file. */
    static Optional<Path> screenshot(File gameDirectory, String name) {
        if (!FILE_NAME.matcher(name).matches()) {
            return Optional.empty();
        }
        Path folder = new File(gameDirectory, Screenshot.SCREENSHOT_DIR).toPath().toAbsolutePath().normalize();
        Path file = folder.resolve(name).normalize();
        try {
            return file.getParent().equals(folder) && Files.isRegularFile(file) && Files.size(file) <= MAX_FILE_BYTES
                    ? Optional.of(file) : Optional.empty();
        } catch (java.io.IOException e) {
            return Optional.empty();
        }
    }
}
