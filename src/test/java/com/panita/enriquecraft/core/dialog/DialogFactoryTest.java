package com.panita.enriquecraft.core.dialog;

import com.panita.enriquecraft.MinecraftTestSupport;
import net.minecraft.core.Holder;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.protocol.common.ClientboundShowDialogPacket;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.server.dialog.ActionButton;
import net.minecraft.server.dialog.Dialog;
import net.minecraft.server.dialog.MultiActionDialog;
import net.minecraft.server.dialog.action.Action;
import net.minecraft.server.dialog.input.BooleanInput;
import net.minecraft.server.dialog.input.SingleOptionInput;
import net.minecraft.server.dialog.input.TextInput;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DialogFactoryTest {

    @BeforeAll
    static void bootstrap() {
        MinecraftTestSupport.bootstrap();
    }

    private static MultiActionDialog build(String token) {
        return assertInstanceOf(MultiActionDialog.class, DialogFactory.build(DialogTesting.spec(), token));
    }

    @Test
    void theDialogPassesTheGamesOwnCodec() {
        // The codec validates what a client would reject, such as a bad input key or an initial text over its limit.
        Dialog dialog = build("t");

        assertTrue(Dialog.DIRECT_CODEC.encodeStart(MinecraftTestSupport.ops(), dialog).result().isPresent());
    }

    @Test
    void theShowPacketSurvivesTheNetworkCodecAsAClientReadsIt() {
        MultiActionDialog dialog = build("secret");
        RegistryFriendlyByteBuf buffer = MinecraftTestSupport.buffer();

        ClientboundShowDialogPacket.STREAM_CODEC.encode(buffer, new ClientboundShowDialogPacket(Holder.direct(dialog)));
        ClientboundShowDialogPacket received = ClientboundShowDialogPacket.STREAM_CODEC.decode(buffer);

        MultiActionDialog read = assertInstanceOf(MultiActionDialog.class, received.dialog().value());
        assertEquals(dialog.common().inputs().stream().map(net.minecraft.server.dialog.Input::key).toList(),
                read.common().inputs().stream().map(net.minecraft.server.dialog.Input::key).toList());
        assertEquals("secret", clickOf(read.actions().getFirst()).getStringOr("token", ""));
        assertEquals("cancel", keyOf(read.exitAction().orElseThrow()));
    }

    @Test
    void theInputsFollowTheFieldsInOrder() {
        List<net.minecraft.server.dialog.Input> inputs = build("t").common().inputs();

        assertEquals(List.of("name", "bio", "shown", "size"), inputs.stream().map(net.minecraft.server.dialog.Input::key).toList());
        TextInput name = assertInstanceOf(TextInput.class, inputs.get(0).control());
        assertEquals(10, name.maxLength());
        assertTrue(name.multiline().isEmpty());
        TextInput bio = assertInstanceOf(TextInput.class, inputs.get(1).control());
        assertEquals(Optional.of(3), bio.multiline().orElseThrow().maxLines());
        assertTrue(assertInstanceOf(BooleanInput.class, inputs.get(2).control()).initial());
        SingleOptionInput size = assertInstanceOf(SingleOptionInput.class, inputs.get(3).control());
        assertEquals("small", size.initial().orElseThrow().id());
    }

    @Test
    void everyButtonSendsTheFixedIdWithTheTokenAndItsKey() {
        MultiActionDialog dialog = build("secret");

        assertEquals(List.of("save"), dialog.actions().stream().map(DialogFactoryTest::keyOf).toList());
        assertEquals("cancel", keyOf(dialog.exitAction().orElseThrow()));
        for (ActionButton button : List.of(dialog.actions().getFirst(), dialog.exitAction().orElseThrow())) {
            CompoundTag sent = clickOf(button);
            assertEquals("secret", sent.getStringOr("token", ""));
        }
        assertEquals(DialogFactory.CLICK_ID, customOf(dialog.actions().getFirst()).id());
    }

    @Test
    void escapePressesTheCancelButton() {
        MultiActionDialog dialog = build("secret");

        Action onCancel = dialog.onCancel().orElseThrow();
        ClickEvent.Custom click = assertInstanceOf(ClickEvent.Custom.class, onCancel.createAction(Map.of()).orElseThrow());
        assertEquals("cancel", ((CompoundTag) click.payload().orElseThrow()).getStringOr("button", ""));
    }

    @Test
    void theGameClosesTheDialogAfterAnswerAndDoesNotPause() {
        MultiActionDialog dialog = build("t");

        assertFalse(dialog.common().pause());
        assertEquals(net.minecraft.server.dialog.DialogAction.CLOSE, dialog.common().afterAction());
        assertTrue(dialog.common().canCloseWithEscape());
    }

    @Test
    void requireChoiceStopsEscape() {
        DialogSpec spec = DialogSpec.builder(Component.literal("t"))
                .button(new DialogButton("ok", Component.literal("Ok")))
                .requireChoice()
                .build();

        assertFalse(DialogFactory.build(spec, "t").common().canCloseWithEscape());
    }

    @Test
    void specsRefuseWhatTheProtocolCannotCarry() {
        Component label = Component.literal("x");
        DialogButton ok = new DialogButton("ok", label);
        assertThrows(IllegalArgumentException.class, () -> new DialogField.Toggle("token", label, false));
        assertThrows(IllegalArgumentException.class, () -> new DialogField.Toggle("bad name", label, false));
        assertThrows(IllegalArgumentException.class, () -> new DialogButton("button", label));
        assertThrows(IllegalArgumentException.class, () -> DialogSpec.builder(label).build());
        assertThrows(IllegalArgumentException.class, () -> DialogSpec.builder(label).button(ok).button(ok).build());
        assertThrows(IllegalArgumentException.class, () -> DialogSpec.builder(label).button(ok)
                .field(new DialogField.Toggle("a", label, false)).field(new DialogField.Toggle("a", label, true)).build());
        assertThrows(IllegalArgumentException.class, () -> new DialogField.Text("a", label, "toolong", 3, 0));
        assertThrows(IllegalArgumentException.class, () -> new DialogField.Choice("c", label,
                List.of(new DialogField.Option("a", label)), "b"));
    }

    private static String keyOf(ActionButton button) {
        return clickOf(button).getStringOr("button", "");
    }

    private static CompoundTag clickOf(ActionButton button) {
        Tag payload = customOf(button).additions().orElseThrow();
        return (CompoundTag) payload;
    }

    private static net.minecraft.server.dialog.action.CustomAll customOf(ActionButton button) {
        return assertInstanceOf(net.minecraft.server.dialog.action.CustomAll.class, button.action().orElseThrow());
    }
}
