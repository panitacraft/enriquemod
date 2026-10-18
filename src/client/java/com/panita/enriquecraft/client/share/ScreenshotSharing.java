package com.panita.enriquecraft.client.share;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.panita.enriquecraft.discord.network.ShareBeginC2S;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.ClientCommands;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.contents.TranslatableContents;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.regex.Pattern;

/**
 * The client companion's screenshot sharing: a {@code [Compartir]} button added to the game's line about a saved screenshot, and
 * the client command that button runs, which opens the share screen. The command only ever opens a file the game itself saves in
 * the screenshots folder, by its plain name, and does nothing but show the screen.
 */
public final class ScreenshotSharing {

    static final String COMMAND = "enriquecraft_share";
    private static final String SUCCESS_KEY = "screenshot.success";
    private static final Pattern FILE_NAME = Pattern.compile("[A-Za-z0-9_.\\-]{1,100}\\.png");
    private static final long MAX_FILE_BYTES = 40L * 1024 * 1024;

    private ScreenshotSharing() {
    }

    public static void register() {
        ScreenshotSender.register();
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> dispatcher.register(
                ClientCommands.literal(COMMAND).then(ClientCommands.argument("file", StringArgumentType.string()).executes(context -> {
                    String name = StringArgumentType.getString(context, "file");
                    Minecraft client = context.getSource().getClient();
                    Optional<Path> file = screenshot(client.gameDirectory, name);
                    client.execute(() -> {
                        if (file.isPresent()) {
                            client.setScreenAndShow(new ShareScreen(file.get()));
                        } else {
                            client.gui.hud.getChat().addClientSystemMessage(Component.translatable("enriquecraft.share.missing"));
                        }
                    });
                    return 1;
                }))));
    }

    /** Wraps what the game does with its message about a saved screenshot, adding the share button when the server can take it. */
    public static Consumer<Component> withShareButton(Consumer<Component> original) {
        return message -> original.accept(sharable(message).orElse(message));
    }

    private static Optional<Component> sharable(Component message) {
        if (!ClientPlayNetworking.canSend(ShareBeginC2S.TYPE)
                || !(message.getContents() instanceof TranslatableContents contents) || !SUCCESS_KEY.equals(contents.getKey())
                || contents.getArgs().length == 0 || !(contents.getArgs()[0] instanceof Component name)
                || !FILE_NAME.matcher(name.getString()).matches()) {
            return Optional.empty();
        }
        MutableComponent button = Component.translatable("enriquecraft.share.button").withStyle(style -> style
                .withColor(0xF2B134)
                .withClickEvent(new ClickEvent.RunCommand("/" + COMMAND + " " + name.getString()))
                .withHoverEvent(new HoverEvent.ShowText(Component.translatable("enriquecraft.share.button.hover"))));
        return Optional.of(Component.empty().append(message).append(Component.literal(" ")).append(button));
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
