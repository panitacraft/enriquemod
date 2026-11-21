package com.panita.enriquecraft.profile.gui;

import com.panita.enriquecraft.auth.data.AuthMode;
import com.panita.enriquecraft.core.dialog.DialogButton;
import com.panita.enriquecraft.core.dialog.DialogField;
import com.panita.enriquecraft.core.dialog.DialogService;
import com.panita.enriquecraft.core.dialog.DialogSpec;
import com.panita.enriquecraft.core.dialog.DialogValues;
import com.panita.enriquecraft.core.gui.MenuFactory;
import com.panita.enriquecraft.core.message.Durations;
import com.panita.enriquecraft.core.message.Message;
import com.panita.enriquecraft.core.ui.PlayerHeads;
import com.panita.enriquecraft.profile.ProfilePermissions;
import com.panita.enriquecraft.profile.config.ProfileConfig;
import com.panita.enriquecraft.profile.data.ProfileDetails;
import com.panita.enriquecraft.profile.message.BirthdayView;
import com.panita.enriquecraft.profile.message.EmblemTooltip;
import com.panita.enriquecraft.profile.message.ProfileMessages;
import com.panita.enriquecraft.profile.message.ProfileView;
import com.panita.enriquecraft.profile.service.EmblemSelection;
import com.panita.enriquecraft.profile.service.EmblemView;
import com.panita.enriquecraft.profile.service.PlayerEmblems;
import com.panita.enriquecraft.profile.service.ProfileDetailsService;
import com.panita.enriquecraft.profile.service.ProfileRefresher;
import com.panita.enriquecraft.profile.service.ProfileService;
import com.panita.enriquecraft.profile.service.ProfileSubject;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerPlayer;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * The profile for players without the client companion, as the game's own dialogs. A dialog is one column of text, inputs and buttons, so
 * the profile is split the way the game likes it: one calm dialog to look at a profile, and, to edit your own, a short list of what can
 * change, each opening a small dialog of its own (the texts, the base, the emblems, the prank level) that goes back to the list. Where a
 * button needs to say more, it says it in its tooltip. Every button of a dialog sends all of its fields, so what the player typed is
 * saved whichever button they press, and the dialog is shown again with what is now saved.
 */
final class ProfileDialogs {

    private static final int WIDTH = 260;
    /** The color of the title of a field, apart from its value, which stays white. */
    private static final int TITLE_COLOR = 0x8FD3FF;
    private static final int VALUE_COLOR = 0xFFFFFF;
    private static final int BIO_LINES = 6;
    private static final int TOOLTIP_BIO = 60;

    private static final String CLOSE = "close";
    private static final String EDIT = "edit";
    private static final String DONE = "done";
    private static final String BACK = "back";
    private static final String SAVE = "save";
    private static final String HUB_TEXTS = "hub_texts";
    private static final String HUB_BASE = "hub_base";
    private static final String HUB_EMBLEMS = "hub_emblems";
    private static final String HUB_PRANK = "hub_prank";
    private static final String MARK_BASE = "mark_base";
    private static final String CLEAR_BASE = "clear_base";
    private static final String SLOT = "slot_";
    private static final String EMBLEM = "emblem_";
    private static final String LEVEL = "level_";
    private static final String PRONOUNS = "pronouns";
    private static final String BIO = "bio";
    private static final String LOCATOR = "locator";

    private final DialogService dialogs;
    private final EmblemTooltip tooltip;
    private final ProfileEdits edits;
    private final ProfileDetailsService details;
    private final ProfileService profiles;
    private final ProfileConfig config;
    private final MenuFactory factory;

    ProfileDialogs(DialogService dialogs, EmblemTooltip tooltip, ProfileEdits edits, ProfileDetailsService details,
                   ProfileService profiles, ProfileConfig config, MenuFactory factory) {
        this.dialogs = dialogs;
        this.tooltip = tooltip;
        this.edits = edits;
        this.details = details;
        this.profiles = profiles;
        this.config = config;
        this.factory = factory;
    }

    /** Shows the menu's profile to the player in the form it is in now: to look at, or the list of what they can change. @return whether a dialog was shown */
    boolean show(ServerPlayer player, ProfileMenu menu) {
        Optional<ProfileSubject> subject = menu.subject();
        if (subject.isEmpty()) {
            return dialogs.show(player, DialogSpec.builder(menu.title()).width(WIDTH)
                    .body(factory.text(Message.plain(ProfileMessages.Menu.NOT_FOUND).with("name", menu.targetName())))
                    .button(new DialogButton(CLOSE, factory.text(ProfileMessages.Edit.DIALOG_CLOSE))).build(), (viewer, button, values) -> { });
        }
        return menu.editing() ? hub(player, menu, subject.get()) : look(player, menu, subject.get());
    }

    // ---- looking at a profile

    private boolean look(ServerPlayer player, ProfileMenu menu, ProfileSubject subject) {
        DialogSpec.Builder spec = DialogSpec.builder(menu.title()).width(WIDTH).columns(2);
        spec.body(identity(menu, subject));
        List<Component> about = new ArrayList<>(List.of(factory.text(ProfileMessages.Dialogs.HEADING_ABOUT),
                block(ProfileMessages.Menu.FIELD_BIRTHDAY, subject.profile().flatMap(profile -> profile.birthday())
                        .map(birthday -> factory.text(BirthdayView.date(birthday))).orElseGet(menu::none))));
        if (!subject.details().bio().isEmpty()) {
            // A description has several lines, so it starts under its title
            about.add(block(ProfileMessages.Menu.FIELD_BIO, Component.literal(subject.details().bio())));
        }
        spec.body(join(about));
        List<Component> game = new ArrayList<>(List.of(factory.text(ProfileMessages.Dialogs.HEADING_GAME),
                line(ProfileMessages.Menu.FIELD_PLAY_TIME, subject.playSeconds().stream()
                        .mapToObj(seconds -> value(Durations.format(Duration.ofSeconds(seconds)))).findFirst().orElseGet(menu::none)),
                line(ProfileMessages.Menu.FIELD_DEATHS, subject.deaths().stream().mapToObj(deaths -> value(String.valueOf(deaths))).findFirst().orElseGet(menu::none)),
                line(ProfileMessages.Menu.FIELD_BASE, menu.baseValue(subject.details(), VALUE_COLOR))));
        if (ProfilePermissions.canSeeStaffInfo(player)) {
            subject.profile().ifPresent(profile -> game.add(factory.text(Message.plain(ProfileMessages.Menu.LINE_ACCOUNT)
                    .with("id", profile.userId())
                    .with("mode", factory.text(subject.mode() == AuthMode.PREMIUM ? ProfileMessages.Menu.MODE_PREMIUM : ProfileMessages.Menu.MODE_PIN)))));
        }
        spec.body(join(game));
        DialogButton close = new DialogButton(CLOSE, factory.text(ProfileMessages.Edit.DIALOG_CLOSE));
        if (menu.isSelf()) {
            // Edit and close stay at the bottom, in view; Escape presses the second
            spec.pinned(new DialogButton(EDIT, factory.text(ProfileMessages.Edit.DIALOG_EDIT)), close);
        } else {
            spec.cancel(close);
        }
        return dialogs.show(player, spec.build(), (viewer, button, values) -> {
            if (EDIT.equals(button)) {
                menu.edit(true);
                show(viewer, menu);
            }
        });
    }

    /** Who the player is: their head, rank, name and the emblems they show, then their pronouns and their prank level. */
    private Component identity(ProfileMenu menu, ProfileSubject subject) {
        PlayerEmblems emblems = subject.emblems();
        MutableComponent name = Component.empty().append(PlayerHeads.glyph(subject.id(), subject.skin())).append(Component.literal(" "));
        menu.rank(subject).ifPresent(name::append);
        name.append(Component.literal(subject.name()).withStyle(ChatFormatting.WHITE, ChatFormatting.BOLD));
        // The emblems the player shows go right of the name, as they do in chat
        for (EmblemView emblem : emblems.slots()) {
            name.append(Component.literal(" ")).append(tooltip.glyph(emblem));
        }
        MutableComponent text = Component.empty().append(name);
        if (!subject.details().pronouns().isEmpty()) {
            text.append(Component.literal("\n" + subject.details().pronouns()).withStyle(ChatFormatting.GRAY));
        }
        return text.append(Component.literal("\n")).append(tooltip.title(emblems.prank()));
    }

    // ---- editing: the list of what can change

    private boolean hub(ServerPlayer player, ProfileMenu menu, ProfileSubject subject) {
        PlayerEmblems emblems = subject.emblems();
        DialogSpec.Builder spec = DialogSpec.builder(menu.title()).width(WIDTH).columns(2);
        spec.body(identity(menu, subject));
        spec.body(factory.text(ProfileMessages.Dialogs.HUB_HINT));
        String pronouns = subject.details().pronouns();
        String bio = subject.details().bio();
        spec.button(new DialogButton(HUB_TEXTS, factory.text(ProfileMessages.Dialogs.HUB_TEXTS), Optional.of(join(List.of(
                line(ProfileMessages.Menu.FIELD_PRONOUNS, pronouns.isEmpty() ? menu.none() : value(pronouns)),
                line(ProfileMessages.Menu.FIELD_BIO, bio.isEmpty() ? menu.none() : value(bio.length() > TOOLTIP_BIO ? bio.substring(0, TOOLTIP_BIO) + "…" : bio)))))));
        spec.button(new DialogButton(HUB_BASE, factory.text(ProfileMessages.Dialogs.HUB_BASE), Optional.of(join(List.of(
                line(ProfileMessages.Menu.FIELD_BASE, menu.baseValue(subject.details(), VALUE_COLOR)),
                factory.text(subject.details().shareLocation() ? ProfileMessages.Dialogs.LOCATOR_SHARED : ProfileMessages.Dialogs.LOCATOR_HIDDEN))))));
        spec.button(new DialogButton(HUB_EMBLEMS, factory.text(ProfileMessages.Dialogs.HUB_EMBLEMS), Optional.of(join(emblems.slots().stream()
                .map(tooltip::title).toList()))));
        spec.button(new DialogButton(HUB_PRANK, factory.text(ProfileMessages.Dialogs.HUB_PRANK), Optional.of(tooltip.title(emblems.prank()))));
        spec.cancel(new DialogButton(DONE, factory.text(ProfileMessages.Edit.DIALOG_DONE)));
        return dialogs.show(player, spec.build(), (viewer, button, values) -> {
            switch (button) {
                case HUB_TEXTS -> texts(viewer, menu);
                case HUB_BASE -> base(viewer, menu);
                case HUB_EMBLEMS -> emblems(viewer, menu);
                case HUB_PRANK -> prank(viewer, menu);
                default -> {
                    menu.edit(false);
                    show(viewer, menu);
                }
            }
        });
    }

    // ---- editing: the texts

    private boolean texts(ServerPlayer player, ProfileMenu menu) {
        ProfileDetails saved = details.detailsOf(player);
        DialogSpec spec = DialogSpec.builder(factory.text(ProfileMessages.Dialogs.HUB_TEXTS)).width(WIDTH).columns(1)
                .body(factory.text(ProfileMessages.Dialogs.TEXTS_NOTE))
                .field(new DialogField.Text(PRONOUNS, factory.text(Message.plain(ProfileMessages.Dialogs.TEXTS_PRONOUNS).with("max", ProfileDetails.PRONOUNS_MAX)),
                        saved.pronouns(), ProfileDetails.PRONOUNS_MAX, 0))
                .field(new DialogField.Text(BIO, factory.text(Message.plain(ProfileMessages.Dialogs.TEXTS_BIO).with("max", ProfileDetails.BIO_MAX)),
                        saved.bio(), Math.max(ProfileDetails.BIO_MAX, saved.bio().length()), BIO_LINES))
                .button(new DialogButton(SAVE, factory.text(ProfileMessages.Dialogs.SAVE)))
                .cancel(new DialogButton(BACK, factory.text(ProfileMessages.Dialogs.BACK)))
                .build();
        return dialogs.show(player, spec, (viewer, button, values) -> {
            if (SAVE.equals(button)) {
                edits.pronouns(viewer, values.text(PRONOUNS));
                edits.bio(viewer, values.text(BIO));
            }
            show(viewer, menu);
        });
    }

    // ---- editing: the base and the locator bar

    private boolean base(ServerPlayer player, ProfileMenu menu) {
        ProfileDetails saved = details.detailsOf(player);
        DialogSpec spec = DialogSpec.builder(factory.text(ProfileMessages.Dialogs.HUB_BASE)).width(WIDTH).columns(1)
                .body(line(ProfileMessages.Menu.FIELD_BASE, menu.baseValue(saved, VALUE_COLOR)))
                .body(factory.text(ProfileMessages.Dialogs.BASE_NOTE))
                .field(new DialogField.Toggle(LOCATOR, factory.text(ProfileMessages.Edit.LOCATOR), saved.shareLocation()))
                .button(new DialogButton(MARK_BASE, factory.text(ProfileMessages.Edit.BASE_MARK), Optional.of(factory.text(ProfileMessages.Edit.BASE_MARK_LORE))))
                .button(new DialogButton(CLEAR_BASE, factory.text(ProfileMessages.Edit.BASE_CLEAR)))
                .button(new DialogButton(SAVE, factory.text(ProfileMessages.Dialogs.SAVE)))
                .cancel(new DialogButton(BACK, factory.text(ProfileMessages.Dialogs.BACK)))
                .build();
        return dialogs.show(player, spec, (viewer, button, values) -> {
            if (BACK.equals(button)) {
                show(viewer, menu);
                return;
            }
            saveLocator(viewer, values);
            switch (button) {
                case MARK_BASE -> {
                    edits.markBase(viewer);
                    base(viewer, menu);
                }
                case CLEAR_BASE -> {
                    edits.clearBase(viewer);
                    base(viewer, menu);
                }
                default -> show(viewer, menu);
            }
        });
    }

    /** Saves the switch when the player changed it; what did not change is left alone, so it says nothing. */
    private void saveLocator(ServerPlayer player, DialogValues values) {
        if (values.toggle(LOCATOR) != details.detailsOf(player).shareLocation()) {
            edits.locator(player, values.toggle(LOCATOR));
        }
    }

    // ---- editing: the prank level

    private boolean prank(ServerPlayer player, ProfileMenu menu) {
        PlayerEmblems emblems = profiles.emblemsOf(player);
        Optional<Duration> wait = edits.prankWait(player);
        DialogSpec.Builder spec = DialogSpec.builder(factory.text(ProfileMessages.Dialogs.HUB_PRANK)).width(WIDTH).columns(2)
                .body(factory.text(Message.plain(ProfileMessages.Dialogs.PRANK_CURRENT).with("level", tooltip.title(emblems.prank()))));
        List<EmblemView> levels = edits.levels();
        if (wait.isPresent()) {
            spec.body(factory.text(Message.plain(ProfileMessages.Edit.PRANK_WAIT).with("time", Durations.format(wait.get()))));
            spec.button(new DialogButton(BACK, factory.text(ProfileMessages.Dialogs.BACK)));
        } else {
            spec.body(factory.text(ProfileMessages.Dialogs.PRANK_PICK));
            // The first level is only where players start, so it is not one to choose
            for (int index = 1; index < levels.size(); index++) {
                EmblemView level = levels.get(index);
                boolean current = level.id().equals(emblems.prank().id());
                Component mark = current ? Component.literal("✔ ").withStyle(ChatFormatting.GREEN) : Component.empty();
                Component hint = factory.text(current ? ProfileMessages.Edit.PRANK_CURRENT : ProfileMessages.Edit.PRANK_CHOOSE);
                spec.button(new DialogButton(LEVEL + index, Component.empty().append(mark).append(tooltip.title(level)),
                        Optional.of(join(withLast(tooltip.description(level), hint)))));
            }
            spec.cancel(new DialogButton(BACK, factory.text(ProfileMessages.Dialogs.BACK)));
        }
        return dialogs.show(player, spec.build(), (viewer, button, values) -> {
            if (button.startsWith(LEVEL)) {
                edits.prank(viewer, levels.get(Integer.parseInt(button.substring(LEVEL.length()))).id());
                prank(viewer, menu);
            } else {
                show(viewer, menu);
            }
        });
    }

    // ---- editing: the emblems

    private boolean emblems(ServerPlayer player, ProfileMenu menu) {
        List<EmblemView> slots = profiles.emblemsOf(player).slots();
        DialogSpec.Builder spec = DialogSpec.builder(factory.text(ProfileMessages.Dialogs.HUB_EMBLEMS)).width(WIDTH).columns(1)
                .body(factory.text(ProfileMessages.Dialogs.EMBLEMS_INTRO));
        for (int place = 0; place < slots.size(); place++) {
            EmblemView emblem = slots.get(place);
            spec.button(new DialogButton(SLOT + place, factory.text(Message.plain(ProfileMessages.Dialogs.SLOT).with("n", place + 1)
                    .with("emblem", tooltip.title(emblem))), Optional.of(join(withLast(tooltip.description(emblem), factory.text(ProfileMessages.Dialogs.SLOT_HINT))))));
        }
        spec.cancel(new DialogButton(BACK, factory.text(ProfileMessages.Dialogs.BACK)));
        return dialogs.show(player, spec.build(), (viewer, button, values) -> {
            if (button.startsWith(SLOT)) {
                pickEmblem(viewer, menu, Integer.parseInt(button.substring(SLOT.length())));
            } else {
                show(viewer, menu);
            }
        });
    }

    /** Every emblem the player holds, to choose the one for a place: it goes there, the one that is there is taken out, or two places trade. */
    private boolean pickEmblem(ServerPlayer player, ProfileMenu menu, int slot) {
        PlayerEmblems emblems = profiles.emblemsOf(player);
        List<String> placed = emblems.placedIds();
        List<EmblemView> held = emblems.selectable();
        DialogSpec.Builder spec = DialogSpec.builder(factory.text(Message.plain(ProfileMessages.Dialogs.PICK_TITLE).with("n", slot + 1))).width(WIDTH).columns(1);
        if (held.isEmpty()) {
            spec.body(factory.text(ProfileMessages.Dialogs.EMBLEMS_NONE));
            spec.button(new DialogButton(BACK, factory.text(ProfileMessages.Dialogs.BACK)));
        } else {
            spec.body(factory.text(Message.plain(ProfileMessages.Dialogs.PICK_INTRO).with("n", slot + 1)));
            for (int index = 0; index < held.size(); index++) {
                EmblemView emblem = held.get(index);
                int at = placed.indexOf(emblem.id());
                boolean here = at == slot;
                boolean elsewhere = at >= 0 && !here;
                Component mark = here ? Component.literal("✔ ").withStyle(ChatFormatting.GREEN)
                        : elsewhere ? Component.literal("↔ ").withStyle(ChatFormatting.AQUA) : Component.empty();
                Component hint = factory.text(here ? ProfileMessages.Emblems.REMOVE : elsewhere ? ProfileMessages.Emblems.SWAP : ProfileMessages.Emblems.PUT);
                spec.button(new DialogButton(EMBLEM + index, Component.empty().append(mark).append(tooltip.title(emblem)),
                        Optional.of(join(withLast(tooltip.description(emblem), hint)))));
            }
            spec.cancel(new DialogButton(BACK, factory.text(ProfileMessages.Dialogs.BACK)));
        }
        return dialogs.show(player, spec.build(), (viewer, button, values) -> {
            if (button.startsWith(EMBLEM)) {
                // What is held and where it is placed are read again, since they may have changed while the dialog was open
                List<EmblemView> now = profiles.emblemsOf(viewer).selectable();
                int index = Integer.parseInt(button.substring(EMBLEM.length()));
                if (index < now.size()) {
                    profiles.choose(viewer, EmblemSelection.place(profiles.emblemsOf(viewer).placedIds(), slot, now.get(index).id(), config.loreMax.get()));
                }
            }
            emblems(viewer, menu);
        });
    }

    // ---- text helpers

    /** The title of a field, in a color of its own so it stands apart from the value. */
    private Component title(String name) {
        return Component.literal(factory.text(name).getString()).withColor(TITLE_COLOR);
    }

    /** A field as one line: its title and its value as given. */
    private Component line(String name, Component value) {
        return Component.empty().append(title(name)).append(Component.literal(" ")).append(value);
    }

    /** A field whose value takes several lines: the value starts under the title. */
    private Component block(String name, Component value) {
        return Component.empty().append(title(name)).append(Component.literal("\n")).append(value);
    }

    private Component value(String text) {
        return Component.literal(text);
    }

    /** Pieces of text one under the other. */
    private static Component join(List<Component> parts) {
        MutableComponent all = Component.empty();
        for (int index = 0; index < parts.size(); index++) {
            if (index > 0) {
                all.append(Component.literal("\n"));
            }
            all.append(parts.get(index));
        }
        return all;
    }

    /** The lines of a description and then one more, such as what clicking does. */
    private static List<Component> withLast(List<Component> lines, Component last) {
        List<Component> all = new ArrayList<>(lines);
        all.add(last);
        return all;
    }
}
