package com.panita.enriquecraft.core.network;

import io.netty.handler.codec.DecoderException;
import io.netty.handler.codec.EncoderException;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

/**
 * Reads and writes a {@link UiElement} tree. Written by hand because the tree is recursive and the
 * decoder must bound its depth and size: the receiver should not be at the mercy of what arrives.
 */
public final class UiElementCodec {

    private static final int MAX_DEPTH = 8;
    private static final int MAX_CHILDREN = 256;
    private static final int MAX_TOOLTIP_LINES = 32;

    private static final byte COLUMN = 0;
    private static final byte ROW = 1;
    private static final byte GRID = 2;
    private static final byte LABEL = 3;
    private static final byte BUTTON = 4;
    private static final byte SPACER = 5;

    private static final StreamCodec<RegistryFriendlyByteBuf, List<Component>> TOOLTIP =
            ComponentSerialization.STREAM_CODEC.apply(ByteBufCodecs.list(MAX_TOOLTIP_LINES));

    public static final StreamCodec<RegistryFriendlyByteBuf, UiElement> STREAM_CODEC = new StreamCodec<>() {
        @Override
        public UiElement decode(RegistryFriendlyByteBuf buffer) {
            return read(buffer, 0);
        }

        @Override
        public void encode(RegistryFriendlyByteBuf buffer, UiElement element) {
            write(buffer, element, 0);
        }
    };

    private UiElementCodec() {
    }

    private static void write(RegistryFriendlyByteBuf buffer, UiElement element, int depth) {
        if (depth > MAX_DEPTH) {
            throw new EncoderException("A screen may nest at most " + MAX_DEPTH + " levels");
        }
        switch (element) {
            case UiElement.Column column -> {
                buffer.writeByte(COLUMN);
                writeChildren(buffer, column.children(), depth);
            }
            case UiElement.Row row -> {
                buffer.writeByte(ROW);
                writeChildren(buffer, row.children(), depth);
            }
            case UiElement.Grid grid -> {
                buffer.writeByte(GRID);
                buffer.writeVarInt(grid.columns());
                buffer.writeVarInt(grid.rows());
                writeChildren(buffer, grid.children(), depth);
            }
            case UiElement.Label label -> {
                buffer.writeByte(LABEL);
                ComponentSerialization.STREAM_CODEC.encode(buffer, label.text());
            }
            case UiElement.Button button -> {
                buffer.writeByte(BUTTON);
                buffer.writeVarInt(button.id());
                ItemStack.OPTIONAL_STREAM_CODEC.encode(buffer, button.icon());
                ComponentSerialization.STREAM_CODEC.encode(buffer, button.label());
                TOOLTIP.encode(buffer, button.tooltip());
            }
            case UiElement.Spacer ignored -> buffer.writeByte(SPACER);
        }
    }

    private static void writeChildren(RegistryFriendlyByteBuf buffer, List<UiElement> children, int depth) {
        if (children.size() > MAX_CHILDREN) {
            throw new EncoderException("A container may hold at most " + MAX_CHILDREN + " elements");
        }
        buffer.writeVarInt(children.size());
        children.forEach(child -> write(buffer, child, depth + 1));
    }

    private static UiElement read(RegistryFriendlyByteBuf buffer, int depth) {
        if (depth > MAX_DEPTH) {
            throw new DecoderException("A screen may nest at most " + MAX_DEPTH + " levels");
        }
        byte type = buffer.readByte();
        return switch (type) {
            case COLUMN -> new UiElement.Column(readChildren(buffer, depth));
            case ROW -> new UiElement.Row(readChildren(buffer, depth));
            case GRID -> {
                int columns = buffer.readVarInt();
                int rows = buffer.readVarInt();
                List<UiElement> children = readChildren(buffer, depth);
                try {
                    yield new UiElement.Grid(columns, rows, children);
                } catch (IllegalArgumentException e) {
                    throw new DecoderException(e.getMessage());
                }
            }
            case LABEL -> new UiElement.Label(ComponentSerialization.STREAM_CODEC.decode(buffer));
            case BUTTON -> new UiElement.Button(
                    buffer.readVarInt(),
                    ItemStack.OPTIONAL_STREAM_CODEC.decode(buffer),
                    ComponentSerialization.STREAM_CODEC.decode(buffer),
                    TOOLTIP.decode(buffer));
            case SPACER -> new UiElement.Spacer();
            default -> throw new DecoderException("Unknown screen element " + type);
        };
    }

    private static List<UiElement> readChildren(RegistryFriendlyByteBuf buffer, int depth) {
        int count = buffer.readVarInt();
        if (count < 0 || count > MAX_CHILDREN) {
            throw new DecoderException("A container may hold at most " + MAX_CHILDREN + " elements, got " + count);
        }
        List<UiElement> children = new ArrayList<>(count);
        for (int index = 0; index < count; index++) {
            children.add(read(buffer, depth + 1));
        }
        return children;
    }
}
