package com.panita.enriquecraft.core.dialog;

import net.minecraft.nbt.ByteTag;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.FloatTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static com.panita.enriquecraft.core.dialog.DialogTesting.TOKEN;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DialogPayloadReaderTest {

    private final DialogSpec spec = DialogTesting.spec();

    private DialogPayloadReader.Result read(CompoundTag reply) {
        return DialogPayloadReader.read(spec, TOKEN, Optional.of(reply));
    }

    private void assertRejected(CompoundTag reply) {
        assertInstanceOf(DialogPayloadReader.Rejected.class, read(reply));
    }

    @Test
    void acceptsWhatARealClientSends() {
        DialogPayloadReader.Accepted accepted = assertInstanceOf(DialogPayloadReader.Accepted.class, read(DialogTesting.reply()));

        assertEquals("save", accepted.button());
        assertEquals("Ana", accepted.values().text("name"));
        assertEquals("one\ntwo", accepted.values().text("bio"));
        assertTrue(accepted.values().toggle("shown"));
        assertEquals("large", accepted.values().choice("size"));
    }

    @Test
    void acceptsTheCancelButton() {
        CompoundTag reply = DialogTesting.reply();
        reply.putString("button", "cancel");

        assertEquals("cancel", assertInstanceOf(DialogPayloadReader.Accepted.class, read(reply)).button());
    }

    @Test
    void refusesAMissingPayloadOrOneThatIsNotACompound() {
        assertInstanceOf(DialogPayloadReader.Rejected.class, DialogPayloadReader.read(spec, TOKEN, Optional.empty()));
        assertInstanceOf(DialogPayloadReader.Rejected.class, DialogPayloadReader.read(spec, TOKEN, Optional.of(new ListTag())));
        assertInstanceOf(DialogPayloadReader.Rejected.class, DialogPayloadReader.read(spec, TOKEN, Optional.of(StringTag.valueOf(TOKEN))));
    }

    @Test
    void aWrongOrMissingTokenIsStaleNotHostile() {
        CompoundTag wrong = DialogTesting.reply();
        wrong.putString("token", "other");
        CompoundTag missing = DialogTesting.reply();
        missing.remove("token");
        CompoundTag notAString = DialogTesting.reply();
        notAString.putInt("token", 1);

        assertInstanceOf(DialogPayloadReader.Stale.class, read(wrong));
        assertInstanceOf(DialogPayloadReader.Stale.class, read(missing));
        assertInstanceOf(DialogPayloadReader.Stale.class, read(notAString));
    }

    @Test
    void aVeryLongTokenIsNotCompared() {
        CompoundTag reply = DialogTesting.reply();
        reply.putString("token", "x".repeat(10_000));

        assertInstanceOf(DialogPayloadReader.Stale.class, read(reply));
    }

    @Test
    void refusesEntriesTheDialogDidNotDeclare() {
        CompoundTag extra = DialogTesting.reply();
        extra.putString("admin", "true");
        CompoundTag swapped = DialogTesting.reply();
        swapped.remove("size");
        swapped.putString("other", "large");

        assertRejected(extra);
        assertRejected(swapped);
    }

    @Test
    void refusesAMissingField() {
        CompoundTag reply = DialogTesting.reply();
        reply.remove("shown");

        assertRejected(reply);
    }

    @Test
    void refusesAButtonTheDialogDoesNotHave() {
        CompoundTag reply = DialogTesting.reply();
        reply.putString("button", "delete");
        CompoundTag notAString = DialogTesting.reply();
        notAString.put("button", ByteTag.valueOf(true));

        assertRejected(reply);
        assertRejected(notAString);
    }

    @Test
    void refusesATextOverItsLimit() {
        CompoundTag reply = DialogTesting.reply();
        reply.putString("name", "12345678901");

        assertRejected(reply);
        reply.putString("name", "1234567890");
        assertInstanceOf(DialogPayloadReader.Accepted.class, read(reply));
    }

    @Test
    void refusesAWrongKindOfValue() {
        CompoundTag number = DialogTesting.reply();
        number.put("name", FloatTag.valueOf(1f));
        CompoundTag text = DialogTesting.reply();
        text.putString("shown", "true");
        CompoundTag outOfRange = DialogTesting.reply();
        outOfRange.put("shown", ByteTag.valueOf((byte) 2));
        CompoundTag option = DialogTesting.reply();
        option.put("size", ByteTag.valueOf(true));

        assertRejected(number);
        assertRejected(text);
        assertRejected(outOfRange);
        assertRejected(option);
    }

    @Test
    void refusesAnOptionThatWasNotOffered() {
        CompoundTag reply = DialogTesting.reply();
        reply.putString("size", "huge");

        assertRejected(reply);
    }

    @Test
    void cleansControlCharactersAndTheSectionSign() {
        CompoundTag reply = DialogTesting.reply();
        reply.putString("name", "§cA\u0000n\ra​");

        DialogPayloadReader.Accepted accepted = assertInstanceOf(DialogPayloadReader.Accepted.class, read(reply));

        assertEquals("cAna", accepted.values().text("name"));
    }

    @Test
    void newlinesStayOnlyInATextArea() {
        CompoundTag reply = DialogTesting.reply();
        reply.putString("name", "a\nb");
        reply.putString("bio", "a\n\u0007b\nc");

        DialogPayloadReader.Accepted accepted = assertInstanceOf(DialogPayloadReader.Accepted.class, read(reply));

        assertEquals("ab", accepted.values().text("name"));
        assertEquals("a\nb\nc", accepted.values().text("bio"));
    }

    @Test
    void refusesMoreLinesThanATextAreaHas() {
        CompoundTag reply = DialogTesting.reply();
        reply.putString("bio", "a\nb\nc\nd");

        assertRejected(reply);
    }

    @Test
    void anEmptyTextIsAValidValue() {
        CompoundTag reply = DialogTesting.reply();
        reply.putString("name", "");

        assertEquals("", assertInstanceOf(DialogPayloadReader.Accepted.class, read(reply)).values().text("name"));
    }

    @Test
    void valuesRefuseAskingForTheWrongField() {
        DialogValues values = assertInstanceOf(DialogPayloadReader.Accepted.class, read(DialogTesting.reply())).values();

        assertThrows(IllegalArgumentException.class, () -> values.text("shown"));
        assertThrows(IllegalArgumentException.class, () -> values.toggle("missing"));
        assertFalse(values.text("name").isEmpty());
    }
}
