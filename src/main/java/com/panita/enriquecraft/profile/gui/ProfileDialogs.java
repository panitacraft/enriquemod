package com.panita.enriquecraft.profile.gui;

import com.panita.enriquecraft.core.dialog.DialogButton;
import com.panita.enriquecraft.core.dialog.DialogField;
import com.panita.enriquecraft.core.dialog.DialogService;
import com.panita.enriquecraft.core.dialog.DialogSpec;
import com.panita.enriquecraft.core.dialog.DialogValues;
import com.panita.enriquecraft.core.gui.MenuFactory;
import com.panita.enriquecraft.core.message.Message;
import com.panita.enriquecraft.profile.data.ProfileDetails;
import com.panita.enriquecraft.profile.message.EmblemTooltip;
import com.panita.enriquecraft.profile.message.ProfileMessages;
import com.panita.enriquecraft.profile.message.ProfileView;
import com.panita.enriquecraft.profile.service.EmblemView;
import com.panita.enriquecraft.profile.service.PlayerEmblems;
import com.panita.enriquecraft.profile.service.ProfileDetailsService;
import com.panita.enriquecraft.profile.service.ProfileRefresher;
import com.panita.enriquecraft.profile.service.ProfileSubject;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * The profile for players without the client companion, as the game's own dialogs: one to look at a profile and one to edit your own.
 * Every button of the edit dialog sends all of its fields, so what the player typed is saved whichever button they press, and the
 * dialog is shown again with what is now saved. A player's emblems are chosen in a chest, which every client can use.
 */
final class ProfileDialogs {

    private static final String CLOSE = "close";
    private static final String EDIT = "edit";
    private static final String REFRESH = "refresh";
    private static final String DONE = "done";
    private static final String CANCEL = "cancel";
    private static final String MARK_BASE = "mark_base";
    private static final String CLEAR_BASE = "clear_base";
    private static final String EMBLEMS = "emblems";
    private static final String PRANK = "prank";
    private static final String LOCATOR = "locator";
    private static final String BIO = "bio";
    private static final int BIO_LINES = 6;

    private final DialogService dialogs;
    private final EmblemTooltip tooltip;
    private final ProfileEdits edits;
    private final ProfileDetailsService details;
    private final ProfileRefresher refresher;
    private final ProfileView view;
    private final MenuFactory factory;

    ProfileDialogs(DialogService dialogs, EmblemTooltip tooltip, ProfileEdits edits, ProfileDetailsService details, ProfileRefresher refresher,
                   ProfileView view, MenuFactory factory) {
        this.dialogs = dialogs;
        this.tooltip = tooltip;
        this.edits = edits;
        this.details = details;
        this.refresher = refresher;
        this.view = view;
        this.factory = factory;
    }

    /** Shows the menu's profile to the player in the form it is in now. @return whether a dialog was shown */
    boolean show(ServerPlayer player, ProfileMenu menu) {
        Optional<ProfileSubject> subject = menu.subject();
        if (subject.isEmpty()) {
            return dialogs.show(player, DialogSpec.builder(menu.title())
                    .body(factory.text(Message.plain(ProfileMessages.Menu.NOT_FOUND).with("name", menu.targetName())))
                    .button(new DialogButton(CLOSE, factory.text(ProfileMessages.Edit.DIALOG_CLOSE))).build(), (viewer, button, values) -> { });
        }
        return menu.editing() ? edit(player, menu, subject.get()) : view(player, menu, subject.get());
    }

    private boolean view(ServerPlayer player, ProfileMenu menu, ProfileSubject subject) {
        PlayerEmblems emblems = subject.emblems();
        tooltip.announce(player, Stream.concat(Stream.of(emblems.prank()), emblems.selectable().stream()).toList());
        DialogSpec.Builder spec = DialogSpec.builder(menu.title());
        spec.body(Component.literal(subject.name()).withStyle(ChatFormatting.YELLOW, ChatFormatting.BOLD));
        menu.factLines(subject).forEach(spec::body);
        spec.body(Component.empty());
        spec.body(factory.text(ProfileMessages.Menu.BIO_HEADER));
        if (subject.details().bio().isEmpty()) {
            spec.body(factory.text(ProfileMessages.Menu.NO_BIO));
        } else {
            ProfileMenu.bioLines(subject.details().bio()).forEach(spec::body);
        }
        spec.body(Component.empty());
        spec.body(factory.text(ProfileMessages.Menu.EMBLEMS_HEADER));
        spec.body(emblemLine(emblems.prank()));
        emblems.selectable().forEach(emblem -> spec.body(emblemLine(emblem)));
        if (menu.isSelf()) {
            spec.button(new DialogButton(EDIT, factory.text(ProfileMessages.Edit.DIALOG_EDIT)));
            spec.button(new DialogButton(REFRESH, factory.text(ProfileMessages.Edit.DIALOG_REFRESH)));
            spec.cancel(new DialogButton(CLOSE, factory.text(ProfileMessages.Edit.DIALOG_CLOSE)));
        } else {
            spec.button(new DialogButton(CLOSE, factory.text(ProfileMessages.Edit.DIALOG_CLOSE)));
        }
        return dialogs.show(player, spec.build(), (viewer, button, values) -> {
            switch (button) {
                case EDIT -> {
                    menu.edit(true);
                    show(viewer, menu);
                }
                case REFRESH -> refresher.refreshSelf(viewer).thenAccept(result -> viewer.level().getServer().execute(() -> {
                    view.selfRefresh(viewer, result);
                    show(viewer, menu);
                }));
                default -> { }
            }
        });
    }

    /** An emblem as its title and, under it, its description, since a dialog has no tooltip to hold it. */
    private Component emblemLine(EmblemView emblem) {
        Component line = tooltip.title(emblem);
        List<Component> description = tooltip.description(emblem);
        return description.isEmpty() ? line : Component.empty().append(line).append(Component.literal(" ")).append(joined(description));
    }

    private static Component joined(List<Component> lines) {
        Component all = Component.empty();
        for (int index = 0; index < lines.size(); index++) {
            all = all.copy().append(index == 0 ? Component.empty() : Component.literal(" ")).append(lines.get(index));
        }
        return all;
    }

    private boolean edit(ServerPlayer player, ProfileMenu menu, ProfileSubject subject) {
        PlayerEmblems emblems = subject.emblems();
        List<DialogField.Option> levels = edits.levels().stream()
                .map(level -> new DialogField.Option(level.id(), Component.literal(level.title()))).toList();
        DialogSpec spec = DialogSpec.builder(menu.title())
                .body(menu.baseLine(subject.details()))
                .body(menu.birthdayLine(subject))
                .body(factory.text(ProfileMessages.Edit.BIRTHDAY_NOTE))
                .field(new DialogField.Choice(PRANK, factory.text(ProfileMessages.Edit.PRANK), levels, emblems.prank().id()))
                .field(new DialogField.Toggle(LOCATOR, factory.text(ProfileMessages.Edit.LOCATOR), subject.details().shareLocation()))
                .field(new DialogField.Text(BIO, factory.text(ProfileMessages.Edit.BIO), subject.details().bio(), ProfileDetails.BIO_MAX, BIO_LINES))
                .button(new DialogButton(MARK_BASE, factory.text(ProfileMessages.Edit.BASE_MARK), Optional.of(factory.text(ProfileMessages.Edit.BASE_MARK_LORE))))
                .button(new DialogButton(CLEAR_BASE, factory.text(ProfileMessages.Edit.BASE_CLEAR)))
                .button(new DialogButton(EMBLEMS, factory.text(ProfileMessages.Edit.EMBLEMS), Optional.of(factory.text(ProfileMessages.Edit.EMBLEMS_LORE))))
                .button(new DialogButton(DONE, factory.text(ProfileMessages.Edit.DIALOG_DONE)))
                .cancel(new DialogButton(CANCEL, factory.text(ProfileMessages.Edit.DIALOG_CANCEL)))
                .build();
        return dialogs.show(player, spec, (viewer, button, values) -> {
            if (CANCEL.equals(button)) {
                return;
            }
            save(viewer, values);
            switch (button) {
                case MARK_BASE -> {
                    edits.markBase(viewer);
                    show(viewer, menu);
                }
                case CLEAR_BASE -> {
                    edits.clearBase(viewer);
                    show(viewer, menu);
                }
                case EMBLEMS -> menu.openEmblems(viewer);
                case DONE -> {
                    menu.edit(false);
                    show(viewer, menu);
                }
                default -> { }
            }
        });
    }

    /** Saves what the player changed in the fields; what did not change is left alone, so it says nothing. */
    private void save(ServerPlayer player, DialogValues values) {
        ProfileDetails saved = details.detailsOf(player);
        if (!values.choice(PRANK).equals(edits.prankOf(player))) {
            edits.prank(player, values.choice(PRANK));
        }
        if (values.toggle(LOCATOR) != saved.shareLocation()) {
            edits.locator(player, values.toggle(LOCATOR));
        }
        if (!values.text(BIO).equals(saved.bio())) {
            edits.bio(player, values.text(BIO));
        }
    }
}
