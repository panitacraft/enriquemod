package com.panita.enriquecraft.profile.gui;

import com.panita.enriquecraft.auth.data.AuthMode;
import com.panita.enriquecraft.core.message.Durations;
import com.panita.enriquecraft.core.message.Message;
import com.panita.enriquecraft.core.message.Messages;
import com.panita.enriquecraft.core.message.TextWrap;
import com.panita.enriquecraft.core.network.ButtonRole;
import com.panita.enriquecraft.core.network.UiElement;
import com.panita.enriquecraft.core.ui.ChestStyle;
import com.panita.enriquecraft.core.ui.ClickHints;
import com.panita.enriquecraft.core.ui.PlayerHeads;
import com.panita.enriquecraft.core.ui.PlayerModels;
import com.panita.enriquecraft.core.ui.UiBuilder;
import com.panita.enriquecraft.core.ui.UiMenu;
import com.panita.enriquecraft.profile.ProfilePermissions;
import com.panita.enriquecraft.profile.data.ProfileDetails;
import com.panita.enriquecraft.profile.message.BirthdayView;
import com.panita.enriquecraft.profile.message.ProfileMessages;
import com.panita.enriquecraft.profile.service.EmblemView;
import com.panita.enriquecraft.profile.service.PlayerEmblems;
import com.panita.enriquecraft.profile.service.ProfileSubject;
import com.panita.enriquecraft.staff.message.CoordinateView;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * A player's profile: who they are on this server and on Panitacraft, what they wrote about themselves, and every emblem they hold.
 * It is the profile of the viewer, who can edit it, or of any other player, who can only be looked at. The data is read again on every
 * redraw, so it follows a refresh that finishes while the menu is open.
 * <p>
 * The client companion's screen has two columns on top (the player and the facts about them), the bio below and the emblems as large
 * tiles; pressing one shows it in full. The edit view puts the settings in the right column and the bio in a field. Players without the
 * companion see the same things as dialogs ({@link ProfileDialogs}); a chest is only the last resort, for players who may not use one.
 */
final class ProfileMenu extends UiMenu {

    private static final int GRID_COLUMNS = 6;
    private static final int BIO_LINE = 56;
    private static final int BIO_SCROLL = 70;
    private static final int AREA_WIDTH = 300;
    private static final int AREA_LINES = 5;

    private final ProfileMenus menus;
    private final Optional<String> target;
    private boolean editing;

    /**
     * @param target  the name of the player to look at, or empty for the viewer's own profile
     * @param editing whether the viewer's own profile is shown for editing; ignored for another player's
     */
    ProfileMenu(ProfileMenus menus, Optional<String> target, boolean editing) {
        super(menus.ui(), null);
        this.menus = menus;
        this.target = target;
        this.editing = editing && target.isEmpty();
    }

    boolean editing() {
        return editing;
    }

    void edit(boolean editing) {
        this.editing = editing && isSelf();
    }

    boolean isSelf() {
        return target.isEmpty();
    }

    /** The player the profile is of; empty when nobody on the server has the name that was asked for. */
    Optional<ProfileSubject> subject() {
        ServerPlayer viewer = viewer();
        return isSelf() ? Optional.of(menus.subjects().of(viewer)) : menus.subjects().named(viewer.level().getServer(), target.get());
    }

    /** The name that was asked for, for a profile of another player. */
    String targetName() {
        return target.orElseThrow();
    }

    /** The title of a screen is only sent when it opens, so changing between viewing and editing opens the screen again. */
    private void showAs(ServerPlayer player, boolean editing) {
        new ProfileMenu(menus, target, editing).open(player);
    }

    void openEmblems(ServerPlayer player) {
        menus.emblemsMenu(this).open(player);
    }

    @Override
    protected Component title() {
        if (editing) {
            return factory().text(ProfileMessages.Menu.TITLE_EDIT);
        }
        if (isSelf()) {
            return factory().text(ProfileMessages.Menu.TITLE);
        }
        String name = subject().map(ProfileSubject::name).orElse(target.get());
        return factory().text(Message.plain(ProfileMessages.Menu.TITLE_OTHER).with("name", name));
    }

    @Override
    protected ItemStack icon() {
        return new ItemStack(Items.NAME_TAG);
    }

    @Override
    protected ChestStyle chestStyle() {
        return ChestStyle.FILLED;
    }

    @Override
    protected boolean openFallback(ServerPlayer player) {
        return menus.dialogs().show(player, this);
    }

    @Override
    protected void onClose() {
        menus.openMenus().closed(viewer().getUUID(), this);
    }

    @Override
    protected UiElement describe(UiBuilder builder) {
        ServerPlayer viewer = viewer();
        menus.openMenus().seen(viewer.getUUID(), this);
        Optional<ProfileSubject> found = subject();
        if (found.isEmpty()) {
            return new UiElement.Column(List.of(new UiElement.Label(factory().text(Message.plain(ProfileMessages.Menu.NOT_FOUND).with("name", target.get()))),
                    new UiElement.Row(List.of(close(builder)))));
        }
        ProfileSubject subject = found.get();
        PlayerEmblems emblems = subject.emblems();
        menus.tooltip().announce(viewer, Stream.concat(Stream.of(emblems.prank()), emblems.selectable().stream()).toList());

        List<UiElement> body = new ArrayList<>();
        body.add(new UiElement.Row(List.of(portrait(subject), new UiElement.Row(List.of(
                new UiElement.Column(editing ? editColumn(builder, subject) : viewColumn(subject), UiElement.Align.START))))));
        body.add(new UiElement.Divider());
        if (editing) {
            body.add(label(factory().text(ProfileMessages.Menu.BIO_HEADER)));
            UiElement.TextArea area = builder.textArea(factory().text(ProfileMessages.Edit.BIO), subject.details().bio(), ProfileDetails.BIO_MAX,
                    AREA_LINES, AREA_WIDTH, submit -> {
                        menus.edits().bio(submit.player(), submit.text());
                        refresh();
                    });
            body.add(area);
            body.add(controls(builder, Optional.of(area)));
        } else {
            body.addAll(bioSection(subject.details()));
            body.add(new UiElement.Divider());
            body.addAll(emblemSection(builder, emblems));
            body.add(controls(builder, Optional.empty()));
        }
        return new UiElement.Column(body);
    }

    /** The viewer sees themselves as the model of their own player; anyone else is shown by their head, with the skin the server knows. */
    private UiElement portrait(ProfileSubject subject) {
        ItemStack head = subject.skin().map(PlayerHeads::item).orElseGet(() -> PlayerHeads.item(subject.id()));
        return new UiElement.Detail(isSelf() ? PlayerModels.ofViewer(head) : head, Component.empty(), List.of());
    }

    private static UiElement.Label label(Component text) {
        return new UiElement.Label(text);
    }

    private static UiElement.Label name(ProfileSubject subject) {
        return label(Component.literal(subject.name()).withStyle(ChatFormatting.YELLOW, ChatFormatting.BOLD));
    }

    /** The facts about the player, one line each, which both the screen and the dialog show. */
    List<Component> factLines(ProfileSubject subject) {
        List<Component> lines = new ArrayList<>();
        subject.profile().ifPresentOrElse(
                profile -> {
                    lines.add(factory().text(Message.plain(ProfileMessages.Menu.LINE_DISCORD).with("discord", profile.discordName())));
                    profile.birthday().ifPresent(birthday -> lines.add(factory().text(Message.plain(ProfileMessages.Menu.LINE_BIRTHDAY)
                            .with("date", factory().text(BirthdayView.date(birthday))))));
                },
                () -> lines.add(factory().text(ProfileMessages.Menu.NO_DATA)));
        subject.playSeconds().ifPresent(seconds -> lines.add(factory().text(Message.plain(ProfileMessages.Menu.LINE_PLAY_TIME)
                .with("time", Durations.format(Duration.ofSeconds(seconds))))));
        lines.add(baseLine(subject.details()));
        if (isSelf()) {
            lines.add(factory().text(Message.plain(ProfileMessages.Menu.LINE_LOCATOR).with("state", factory().text(subject.details().shareLocation()
                    ? ProfileMessages.Menu.LOCATOR_SHARED : ProfileMessages.Menu.LOCATOR_HIDDEN))));
        }
        if (ProfilePermissions.canSeeStaffInfo(viewer())) {
            subject.profile().ifPresent(profile -> lines.add(factory().text(Message.plain(ProfileMessages.Menu.LINE_ACCOUNT)
                    .with("id", profile.userId())
                    .with("mode", factory().text(subject.mode() == AuthMode.PREMIUM ? ProfileMessages.Menu.MODE_PREMIUM : ProfileMessages.Menu.MODE_PIN)))));
        }
        return lines;
    }

    /** The base as the block it is on, which a click copies, and its dimension. */
    Component baseLine(ProfileDetails details) {
        return details.base().map(base -> factory().text(Message.plain(ProfileMessages.Menu.LINE_BASE)
                        .with("position", CoordinateView.position(base.x(), base.y(), base.z()))
                        .with("dimension", CoordinateView.dimension(base.dimension()))))
                .orElseGet(() -> factory().text(ProfileMessages.Menu.LINE_NO_BASE));
    }

    private List<UiElement> viewColumn(ProfileSubject subject) {
        List<UiElement> lines = new ArrayList<>();
        lines.add(name(subject));
        factLines(subject).forEach(line -> lines.add(label(line)));
        return lines;
    }

    /** The settings, in the right column where the facts are: the prank level, the base, the birthday and the locator bar. */
    private List<UiElement> editColumn(UiBuilder builder, ProfileSubject subject) {
        List<EmblemView> levels = menus.edits().levels();
        int selected = 0;
        for (int index = 0; index < levels.size(); index++) {
            if (levels.get(index).id().equals(subject.emblems().prank().id())) {
                selected = index;
            }
        }
        List<Component> options = levels.stream().map(level -> (Component) Component.literal(level.title())).toList();
        List<UiElement> lines = new ArrayList<>();
        lines.add(name(subject));
        lines.add(builder.dropdown(factory().text(ProfileMessages.Edit.PRANK), options, selected, select -> {
            menus.edits().prank(select.player(), levels.get(select.option()).id());
            refresh();
        }));
        lines.add(label(baseLine(subject.details())));
        lines.add(new UiElement.Row(List.of(
                builder.button(ButtonRole.NONE, new ItemStack(Items.COMPASS), factory().text(ProfileMessages.Edit.BASE_MARK),
                        List.of(factory().text(ProfileMessages.Edit.BASE_MARK_LORE)), click -> {
                            if (click.isLeft()) {
                                menus.edits().markBase(click.player());
                                refresh();
                            }
                        }),
                builder.button(ButtonRole.NONE, new ItemStack(Items.BARRIER), factory().text(ProfileMessages.Edit.BASE_CLEAR), List.of(), click -> {
                    if (click.isLeft()) {
                        menus.edits().clearBase(click.player());
                        refresh();
                    }
                }))));
        lines.add(label(birthdayLine(subject)));
        lines.add(label(factory().text(ProfileMessages.Edit.BIRTHDAY_NOTE)));
        lines.add(builder.toggle(factory().text(ProfileMessages.Edit.LOCATOR), subject.details().shareLocation(), toggle -> {
            menus.edits().locator(toggle.player(), toggle.checked());
            refresh();
        }));
        return lines;
    }

    /** The birthday, or that there is none, which the edit view shows although it can only be changed on the website. */
    Component birthdayLine(ProfileSubject subject) {
        Component date = subject.profile().flatMap(profile -> profile.birthday())
                .map(birthday -> factory().text(BirthdayView.date(birthday)))
                .orElseGet(() -> factory().text(ProfileMessages.Edit.BIRTHDAY_NONE));
        return factory().text(Message.plain(ProfileMessages.Menu.LINE_BIRTHDAY).with("date", date));
    }

    /** The bio on short lines in a scroll area, so a long one never makes the screen taller than the window. */
    private List<UiElement> bioSection(ProfileDetails details) {
        List<UiElement> section = new ArrayList<>();
        section.add(label(factory().text(ProfileMessages.Menu.BIO_HEADER)));
        if (details.bio().isEmpty()) {
            section.add(label(factory().text(ProfileMessages.Menu.NO_BIO)));
        } else {
            List<UiElement> lines = bioLines(details.bio()).stream().map(line -> (UiElement) label(line)).toList();
            section.add(new UiElement.Scroll(new UiElement.Column(lines, UiElement.Align.START), BIO_SCROLL));
        }
        return section;
    }

    /** The bio broken into lines short enough to read, which neither the screen nor a dialog does by itself. */
    static List<Component> bioLines(String bio) {
        return TextWrap.lines(bio, BIO_LINE).stream().map(line -> (Component) Component.literal(line)).toList();
    }

    /** The prank level on its own, then every emblem the player holds, as large tiles; the ones shown in chat are marked. */
    private List<UiElement> emblemSection(UiBuilder builder, PlayerEmblems emblems) {
        List<UiElement> section = new ArrayList<>();
        section.add(label(factory().text(ProfileMessages.Menu.EMBLEMS_HEADER)));
        section.add(new UiElement.Grid(1, 1, List.of(tile(builder, emblems.prank(), ""))).withLargeTiles());
        section.add(new UiElement.Rule());
        List<UiElement> tiles = emblems.selectable().stream()
                .map(emblem -> tile(builder, emblem, emblems.chosen().stream().anyMatch(shown -> shown.id().equals(emblem.id())) ? "✔" : ""))
                .toList();
        if (tiles.isEmpty()) {
            section.add(label(factory().text(ProfileMessages.Menu.NO_EMBLEMS)));
        } else {
            section.add(new UiElement.Grid(Math.min(GRID_COLUMNS, tiles.size()), (tiles.size() + GRID_COLUMNS - 1) / GRID_COLUMNS, tiles).withLargeTiles());
        }
        return section;
    }

    /** One emblem as a large tile with its image; pressing it shows the emblem in full. */
    private UiElement tile(UiBuilder builder, EmblemView emblem, String badge) {
        List<Component> lines = new ArrayList<>(menus.tooltip().description(emblem));
        lines.add(Component.empty());
        lines.add(ClickHints.left(factory(), ProfileMessages.Menu.EMBLEM_DETAIL));
        return builder.button(ButtonRole.NONE, menus.tooltip().icon(emblem), menus.tooltip().title(emblem), lines, badge, click -> {
            if (click.isLeft()) {
                menus.emblemDetail(this, emblem).open(click.player());
            }
        });
    }

    private UiElement.Button close(UiBuilder builder) {
        return builder.button(ButtonRole.CLOSE, new ItemStack(Items.BARRIER), factory().text(Messages.Gui.CLOSE), List.of(),
                click -> ui().close(click.player()));
    }

    /** The last band: refresh and edit in the header for the viewer's own profile, and what saves the bio while editing it. */
    private UiElement controls(UiBuilder builder, Optional<UiElement.TextArea> area) {
        List<UiElement> controls = new ArrayList<>();
        if (editing) {
            controls.add(builder.button(ButtonRole.BACK, new ItemStack(Items.OAK_DOOR), factory().text(ProfileMessages.Menu.VIEW), List.of(),
                    click -> showAs(click.player(), false)));
            controls.add(builder.submitButton(factory().text(ProfileMessages.Edit.BIO_SAVE), area.orElseThrow()));
            controls.add(builder.button(ButtonRole.NONE, new ItemStack(Items.NAME_TAG), factory().text(ProfileMessages.Edit.EMBLEMS),
                    List.of(factory().text(ProfileMessages.Edit.EMBLEMS_LORE)), click -> {
                        if (click.isLeft()) {
                            openEmblems(click.player());
                        }
                    }));
        } else if (isSelf()) {
            controls.add(builder.button(ButtonRole.REFRESH, new ItemStack(Items.CLOCK), factory().text(ProfileMessages.Menu.REFRESH),
                    List.of(factory().text(ProfileMessages.Menu.REFRESH_LORE)), click -> {
                        if (click.isLeft()) {
                            menus.refresher().refreshSelf(click.player()).thenAccept(result -> menus.view().selfRefresh(click.player(), result));
                        }
                    }));
            controls.add(builder.button(ButtonRole.EDIT, new ItemStack(Items.WRITABLE_BOOK), factory().text(ProfileMessages.Menu.EDIT),
                    List.of(factory().text(ProfileMessages.Menu.EDIT_LORE)), click -> {
                        if (click.isLeft()) {
                            showAs(click.player(), true);
                        }
                    }));
        }
        controls.add(close(builder));
        return new UiElement.Row(controls);
    }
}
