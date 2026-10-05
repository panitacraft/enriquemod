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
import java.util.Optional;

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
    private static final byte TEXT_INPUT = 6;
    private static final byte PAGE = 7;
    private static final byte DETAIL = 8;
    private static final byte SCROLL = 9;
    private static final byte DROPDOWN = 10;
    private static final byte DIVIDER = 11;

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
                buffer.writeEnum(button.role());
                ItemStack.OPTIONAL_STREAM_CODEC.encode(buffer, button.icon());
                ComponentSerialization.STREAM_CODEC.encode(buffer, button.label());
                TOOLTIP.encode(buffer, button.tooltip());
                buffer.writeUtf(button.badge(), UiElement.Button.MAX_BADGE_LENGTH * 4);
                buffer.writeVarInt(button.tint());
                buffer.writeBoolean(button.draggable());
            }
            case UiElement.TextInput input -> {
                buffer.writeByte(TEXT_INPUT);
                writeTextInput(buffer, input);
            }
            case UiElement.Page page -> {
                buffer.writeByte(PAGE);
                buffer.writeVarInt(page.page());
                buffer.writeVarInt(page.pages());
            }
            case UiElement.Detail detail -> {
                buffer.writeByte(DETAIL);
                ItemStack.OPTIONAL_STREAM_CODEC.encode(buffer, detail.icon());
                ComponentSerialization.STREAM_CODEC.encode(buffer, detail.title());
                TOOLTIP.encode(buffer, detail.lines());
                buffer.writeVarInt(detail.iconId() + 1);
                TOOLTIP.encode(buffer, detail.iconTooltip());
                buffer.writeBoolean(detail.editableTitle().isPresent());
                detail.editableTitle().ifPresent(input -> writeTextInput(buffer, input));
            }
            case UiElement.Scroll scroll -> {
                buffer.writeByte(SCROLL);
                buffer.writeVarInt(scroll.maxHeight());
                write(buffer, scroll.content(), depth + 1);
            }
            case UiElement.Dropdown dropdown -> {
                buffer.writeByte(DROPDOWN);
                buffer.writeVarInt(dropdown.id());
                ComponentSerialization.STREAM_CODEC.encode(buffer, dropdown.label());
                TOOLTIP.encode(buffer, dropdown.options());
                buffer.writeVarInt(dropdown.selected());
            }
            case UiElement.Divider ignored -> buffer.writeByte(DIVIDER);
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
            case BUTTON -> readButton(buffer);
            case TEXT_INPUT -> readTextInput(buffer);
            case PAGE -> readPage(buffer);
            case DETAIL -> readDetail(buffer);
            case SCROLL -> readScroll(buffer, depth);
            case DROPDOWN -> readDropdown(buffer);
            case DIVIDER -> new UiElement.Divider();
            case SPACER -> new UiElement.Spacer();
            default -> throw new DecoderException("Unknown screen element " + type);
        };
    }

    private static UiElement readButton(RegistryFriendlyByteBuf buffer) {
        int id = buffer.readVarInt();
        ButtonRole role = buffer.readEnum(ButtonRole.class);
        ItemStack icon = ItemStack.OPTIONAL_STREAM_CODEC.decode(buffer);
        Component label = ComponentSerialization.STREAM_CODEC.decode(buffer);
        List<Component> tooltip = TOOLTIP.decode(buffer);
        String badge = buffer.readUtf(UiElement.Button.MAX_BADGE_LENGTH * 4);
        int tint = buffer.readVarInt();
        boolean draggable = buffer.readBoolean();
        try {
            return new UiElement.Button(id, role, icon, label, tooltip, badge, tint, draggable);
        } catch (IllegalArgumentException e) {
            throw new DecoderException(e.getMessage());
        }
    }

    private static UiElement readScroll(RegistryFriendlyByteBuf buffer, int depth) {
        int maxHeight = buffer.readVarInt();
        UiElement content = read(buffer, depth + 1);
        try {
            return new UiElement.Scroll(content, maxHeight);
        } catch (IllegalArgumentException e) {
            throw new DecoderException(e.getMessage());
        }
    }

    private static UiElement readDropdown(RegistryFriendlyByteBuf buffer) {
        int id = buffer.readVarInt();
        Component label = ComponentSerialization.STREAM_CODEC.decode(buffer);
        List<Component> options = TOOLTIP.decode(buffer);
        int selected = buffer.readVarInt();
        try {
            return new UiElement.Dropdown(id, label, options, selected);
        } catch (IllegalArgumentException e) {
            throw new DecoderException(e.getMessage());
        }
    }

    private static UiElement readPage(RegistryFriendlyByteBuf buffer) {
        int page = buffer.readVarInt();
        int pages = buffer.readVarInt();
        try {
            return new UiElement.Page(page, pages);
        } catch (IllegalArgumentException e) {
            throw new DecoderException(e.getMessage());
        }
    }

    private static void writeTextInput(RegistryFriendlyByteBuf buffer, UiElement.TextInput input) {
        buffer.writeVarInt(input.id());
        ComponentSerialization.STREAM_CODEC.encode(buffer, input.hint());
        buffer.writeUtf(input.value(), UiElement.TextInput.MAX_LENGTH);
        buffer.writeVarInt(input.maxLength());
    }

    private static UiElement readDetail(RegistryFriendlyByteBuf buffer) {
        ItemStack icon = ItemStack.OPTIONAL_STREAM_CODEC.decode(buffer);
        Component title = ComponentSerialization.STREAM_CODEC.decode(buffer);
        List<Component> lines = TOOLTIP.decode(buffer);
        int iconId = buffer.readVarInt() - 1;
        List<Component> iconTooltip = TOOLTIP.decode(buffer);
        Optional<UiElement.TextInput> editableTitle = buffer.readBoolean()
                ? Optional.of(readTextInput(buffer)) : Optional.empty();
        return new UiElement.Detail(icon, title, lines, iconId, iconTooltip, editableTitle);
    }

    private static UiElement.TextInput readTextInput(RegistryFriendlyByteBuf buffer) {
        int id = buffer.readVarInt();
        Component hint = ComponentSerialization.STREAM_CODEC.decode(buffer);
        String value = buffer.readUtf(UiElement.TextInput.MAX_LENGTH);
        int maxLength = buffer.readVarInt();
        try {
            return new UiElement.TextInput(id, hint, value, maxLength);
        } catch (IllegalArgumentException e) {
            throw new DecoderException(e.getMessage());
        }
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
