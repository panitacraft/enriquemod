package com.panita.enriquecraft.core.dialog;

import com.panita.enriquecraft.Enriquecraft;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import net.minecraft.server.dialog.ActionButton;
import net.minecraft.server.dialog.CommonButtonData;
import net.minecraft.server.dialog.CommonDialogData;
import net.minecraft.server.dialog.ConfirmationDialog;
import net.minecraft.server.dialog.Dialog;
import net.minecraft.server.dialog.DialogAction;
import net.minecraft.server.dialog.Input;
import net.minecraft.server.dialog.MultiActionDialog;
import net.minecraft.server.dialog.action.CustomAll;
import net.minecraft.server.dialog.body.DialogBody;
import net.minecraft.server.dialog.body.PlainMessage;
import net.minecraft.server.dialog.input.BooleanInput;
import net.minecraft.server.dialog.input.InputControl;
import net.minecraft.server.dialog.input.SingleOptionInput;
import net.minecraft.server.dialog.input.TextInput;

import java.util.List;
import java.util.Optional;

/**
 * Turns a {@link DialogSpec} into a game dialog. Every button runs the {@code custom_all} action with one fixed id:
 * the game then sends all the inputs back as they are, next to the token and the key of the button that is added here.
 * Nothing the player can edit decides what the server runs.
 */
final class DialogFactory {

    /** The id every button of every dialog answers with; the server tells dialogs apart by the token. */
    static final Identifier CLICK_ID = Identifier.fromNamespaceAndPath(Enriquecraft.MOD_ID, "dialog");

    /** The space the game leaves between the buttons of a row. */
    private static final int GAP = 4;

    private DialogFactory() {
    }

    static Dialog build(DialogSpec spec, String token) {
        List<DialogBody> body = spec.body().stream().map(block -> body(block, spec.width())).toList();
        List<Input> inputs = spec.fields().stream().map(field -> input(field, spec.width())).toList();
        // The game pauses a single-player world for a dialog by default; this one is a form, not a menu.
        CommonDialogData common = new CommonDialogData(spec.title(), Optional.empty(), spec.closeableWithEscape(), false,
                DialogAction.CLOSE, body, inputs);
        if (spec.pinned()) {
            int half = (spec.width() - GAP) / 2;
            return new ConfirmationDialog(common, action(spec.buttons().get(0), token, half), action(spec.buttons().get(1), token, half));
        }
        // Buttons of a row share the width of the dialog, so a row lines up with the inputs and the text above it
        int rowButtonWidth = (spec.width() - GAP * (spec.columns() - 1)) / spec.columns();
        List<ActionButton> actions = spec.buttons().stream().map(button -> action(button, token, rowButtonWidth)).toList();
        Optional<ActionButton> exit = spec.cancel().map(button -> action(button, token, spec.width()));
        return new MultiActionDialog(common, actions, exit, spec.columns());
    }

    private static DialogBody body(DialogBlock block, int width) {
        return switch (block) {
            case DialogBlock.Message message -> new PlainMessage(message.text(), width);
        };
    }

    private static ActionButton action(DialogButton button, String token, int width) {
        CompoundTag additions = new CompoundTag();
        additions.putString(DialogNames.TOKEN_KEY, token);
        additions.putString(DialogNames.BUTTON_KEY, button.key());
        CommonButtonData data = new CommonButtonData(button.label(), button.tooltip(), width);
        return new ActionButton(data, Optional.of(new CustomAll(CLICK_ID, Optional.of(additions))));
    }

    private static Input input(DialogField field, int width) {
        InputControl control = switch (field) {
            case DialogField.Text text -> new TextInput(width, text.label(), true, text.initial(), text.maxLength(),
                    text.multiline()
                            ? Optional.of(new TextInput.MultilineOptions(Optional.of(text.lines()), Optional.empty()))
                            : Optional.empty());
            case DialogField.Toggle toggle -> new BooleanInput(toggle.label(), toggle.initial(), "true", "false");
            case DialogField.Choice choice -> new SingleOptionInput(width, choice.options().stream()
                    .map(option -> new SingleOptionInput.Entry(option.id(), Optional.of(option.label()),
                            option.id().equals(choice.initial())))
                    .toList(), choice.label(), true);
        };
        return new Input(field.name(), control);
    }
}
