package com.panita.enriquecraft.core.dialog;

import net.minecraft.nbt.ByteTag;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;

import java.util.List;
import java.util.Optional;

/** A dialog with one field of each kind and a reply a client would send for it. */
final class DialogTesting {

    static final String TOKEN = "tokenvalue";

    private DialogTesting() {
    }

    static DialogSpec spec() {
        return DialogSpec.builder(Component.literal("Form"))
                .body(Component.literal("Fill this in"))
                .field(new DialogField.Text("name", Component.literal("Name"), "", 10, 0))
                .field(new DialogField.Text("bio", Component.literal("Bio"), "", 100, 3))
                .field(new DialogField.Toggle("shown", Component.literal("Shown"), true))
                .field(new DialogField.Choice("size", Component.literal("Size"),
                        List.of(new DialogField.Option("small", Component.literal("Small")),
                                new DialogField.Option("large", Component.literal("Large"))), "small"))
                .button(new DialogButton("save", Component.literal("Save")))
                .cancel(new DialogButton("cancel", Component.literal("Cancel")))
                .build();
    }

    /** What a real client sends for {@link #spec()} after the player typed "Ana" and pressed Save. */
    static CompoundTag reply() {
        CompoundTag tag = new CompoundTag();
        tag.putString("token", TOKEN);
        tag.putString("button", "save");
        tag.putString("name", "Ana");
        tag.putString("bio", "one\ntwo");
        tag.put("shown", ByteTag.valueOf(true));
        tag.putString("size", "large");
        return tag;
    }

    static Optional<Tag> payload(CompoundTag tag) {
        return Optional.of(tag);
    }
}
