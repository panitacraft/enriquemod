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
 * A player's profile: who they are on this server and on Panitacraft, and what they chose to tell about themselves. It is the profile of
 * the viewer, who can edit it, or of any other player, who can only be looked at. The data is read again on every redraw, so it follows
 * a refresh that finishes while the menu is open.
 * <p>
 * The client companion's screen has the player and their identity on top (name and nationality, rank, the three emblems they show,
 * and a dot that says whether they are online in the header), then two columns of facts, each as a label with its value under it:
 * the personal ones on the left and the ones about the game on the right. The edit view is the same screen with the fields that can
 * change turned into controls: the emblems and the prank level open their lists, the pronouns and the description are fields.
 * Players without the companion see the facts as dialogs ({@link ProfileDialogs}); a chest is only the last resort.
 */
final class ProfileMenu extends UiMenu {

    private static final int BIO_LINE = 56;
    private static final int COLUMN_BIO_LINE = 30;
    private static final int BIO_SCROLL = 70;
    private static final int AREA_WIDTH = 190;
    private static final int AREA_LINES = 5;
    private static final int ONLINE_COLOR = 0x3FCF6B;
    private static final int OFFLINE_COLOR = 0x7A7A7A;

    private final ProfileMenus menus;
    private final Optional<String> target;
    private boolean editing;
    // The two fields of the edit view, kept while the screen is described so the controls can confirm them
    private UiElement.TextInput pronounsInput;
    private UiElement.TextArea bioArea;

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
        menus.tooltip().announce(viewer, Stream.of(Stream.of(emblems.prank()), emblems.nationality().stream(), emblems.chosen().stream(),
                emblems.empty().stream()).flatMap(all -> all).toList());

        List<UiElement> body = new ArrayList<>();
        body.add(new UiElement.Row(List.of(portrait(subject), new UiElement.Column(identity(builder, subject, emblems), UiElement.Align.START))));
        body.add(new UiElement.Divider());
        body.add(new UiElement.Row(List.of(
                new UiElement.Column(personalColumn(builder, subject), UiElement.Align.START),
                new UiElement.Column(gameColumn(builder, subject, emblems), UiElement.Align.START))));
        body.add(controls(builder, subject));
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

    /** The name with the nationality beside it, the rank under it, and the emblems the player shows under that. */
    private List<UiElement> identity(UiBuilder builder, ProfileSubject subject, PlayerEmblems emblems) {
        List<UiElement> lines = new ArrayList<>();
        List<UiElement> nameLine = new ArrayList<>();
        nameLine.add(label(Component.literal(subject.name()).withStyle(ChatFormatting.YELLOW, ChatFormatting.BOLD)));
        emblems.nationality().ifPresent(nationality -> nameLine.add(label(menus.tooltip().picture(nationality))));
        lines.add(new UiElement.Row(nameLine));
        rank(subject).ifPresent(rank -> lines.add(label(rank)));
        List<EmblemView> shown = Stream.concat(emblems.chosen().stream(), emblems.empty().stream()).toList();
        lines.add(new UiElement.Row(shown.stream().map(emblem -> emblemTile(builder, emblem)).toList()));
        return lines;
    }

    /** The rank the player shows in chat, which needs the player to be online: their ranks live in LuckPerms, which only knows who is here. */
    private Optional<Component> rank(ProfileSubject subject) {
        ServerPlayer player = viewer().level().getServer().getPlayerList().getPlayer(subject.id());
        if (player == null || !subject.online()) {
            return Optional.empty();
        }
        Component rank = menus.rankOf(player);
        return rank.getString().isBlank() ? Optional.empty() : Optional.of(rank);
    }

    /** One emblem the player shows, large, with nothing behind it. Looking at it opens it in full; editing, the list to change it. */
    private UiElement emblemTile(UiBuilder builder, EmblemView emblem) {
        List<Component> lines = new ArrayList<>(menus.tooltip().description(emblem));
        lines.add(Component.empty());
        lines.add(ClickHints.left(factory(), editing ? ProfileMessages.Edit.EMBLEM_CHANGE : ProfileMessages.Menu.EMBLEM_DETAIL));
        return builder.button(ButtonRole.EMBLEM, menus.tooltip().icon(emblem), menus.tooltip().title(emblem), lines, click -> {
            if (!click.isLeft()) {
                return;
            }
            if (editing) {
                openEmblems(click.player());
            } else {
                menus.emblemDetail(this, emblem).open(click.player());
            }
        });
    }

    /** A field as a label with its value under it. */
    private List<UiElement> field(String name, Component value) {
        return List.of(label(factory().text(name)), label(value));
    }

    private Component value(String text) {
        return Component.literal(text).withStyle(ChatFormatting.WHITE);
    }

    private Component none() {
        return factory().text(ProfileMessages.Menu.FIELD_NONE);
    }

    /** What the player says about themselves: pronouns, birthday and description. */
    private List<UiElement> personalColumn(UiBuilder builder, ProfileSubject subject) {
        List<UiElement> lines = new ArrayList<>();
        if (editing) {
            lines.add(label(factory().text(ProfileMessages.Menu.FIELD_PRONOUNS)));
            pronounsInput = builder.input(factory().text(ProfileMessages.Edit.PRONOUNS_HINT), subject.details().pronouns(), ProfileDetails.PRONOUNS_MAX,
                    submit -> {
                        menus.edits().pronouns(submit.player(), submit.text());
                        refresh();
                    });
            lines.add(pronounsInput);
        } else {
            String pronouns = subject.details().pronouns();
            lines.addAll(field(ProfileMessages.Menu.FIELD_PRONOUNS, pronouns.isEmpty() ? none() : value(pronouns)));
        }
        lines.add(new UiElement.Spacer());
        lines.addAll(field(ProfileMessages.Menu.FIELD_BIRTHDAY, subject.profile().flatMap(profile -> profile.birthday())
                .map(birthday -> factory().text(BirthdayView.date(birthday))).orElseGet(this::none)));
        lines.add(new UiElement.Spacer());
        lines.add(label(factory().text(ProfileMessages.Menu.FIELD_BIO)));
        if (editing) {
            bioArea = builder.textArea(factory().text(ProfileMessages.Edit.BIO), subject.details().bio(), ProfileDetails.BIO_MAX, AREA_LINES, AREA_WIDTH,
                    submit -> {
                        menus.edits().bio(submit.player(), submit.text());
                        refresh();
                    });
            lines.add(bioArea);
        } else if (subject.details().bio().isEmpty()) {
            lines.add(label(none()));
        } else {
            List<UiElement> text = bioLines(subject.details().bio(), COLUMN_BIO_LINE).stream().map(line -> (UiElement) label(line)).toList();
            lines.add(new UiElement.Scroll(new UiElement.Column(text, UiElement.Align.START), BIO_SCROLL));
        }
        return lines;
    }

    /** What the player did in the game: play time, deaths, base and prank level; the staff also see the account. */
    private List<UiElement> gameColumn(UiBuilder builder, ProfileSubject subject, PlayerEmblems emblems) {
        List<UiElement> lines = new ArrayList<>();
        lines.addAll(field(ProfileMessages.Menu.FIELD_PLAY_TIME, subject.playSeconds().stream()
                .mapToObj(seconds -> value(Durations.format(Duration.ofSeconds(seconds)))).findFirst().orElseGet(this::none)));
        lines.add(new UiElement.Spacer());
        lines.addAll(field(ProfileMessages.Menu.FIELD_DEATHS, subject.deaths().stream().mapToObj(deaths -> value(String.valueOf(deaths))).findFirst().orElseGet(this::none)));
        lines.add(new UiElement.Spacer());
        lines.add(label(factory().text(ProfileMessages.Menu.FIELD_BASE)));
        lines.add(label(baseValue(subject.details())));
        if (editing) {
            lines.add(baseControls(builder));
        }
        lines.add(new UiElement.Spacer());
        lines.add(label(factory().text(ProfileMessages.Menu.FIELD_PRANK)));
        lines.add(editing ? prankButton(builder, emblems.prank()) : label(menus.tooltip().title(emblems.prank())));
        if (editing) {
            lines.add(new UiElement.Spacer());
            lines.add(builder.toggle(factory().text(ProfileMessages.Edit.LOCATOR), subject.details().shareLocation(), toggle -> {
                menus.edits().locator(toggle.player(), toggle.checked());
                refresh();
            }));
        }
        if (ProfilePermissions.canSeeStaffInfo(viewer())) {
            subject.profile().ifPresent(profile -> {
                lines.add(new UiElement.Spacer());
                lines.add(label(factory().text(Message.plain(ProfileMessages.Menu.LINE_ACCOUNT).with("id", profile.userId())
                        .with("mode", factory().text(subject.mode() == AuthMode.PREMIUM ? ProfileMessages.Menu.MODE_PREMIUM : ProfileMessages.Menu.MODE_PIN)))));
            });
        }
        return lines;
    }

    /** Marking the base, and a small button that removes it. */
    private UiElement baseControls(UiBuilder builder) {
        return new UiElement.Row(List.of(
                builder.button(ButtonRole.NONE, new ItemStack(Items.COMPASS), factory().text(ProfileMessages.Edit.BASE_MARK),
                        List.of(factory().text(ProfileMessages.Edit.BASE_MARK_LORE)), click -> {
                            if (click.isLeft()) {
                                menus.edits().markBase(click.player());
                                refresh();
                            }
                        }),
                builder.button(ButtonRole.NONE, ItemStack.EMPTY, factory().text(ProfileMessages.Edit.BASE_CLEAR_ICON),
                        List.of(factory().text(ProfileMessages.Edit.BASE_CLEAR)), click -> {
                            if (click.isLeft()) {
                                menus.edits().clearBase(click.player());
                                refresh();
                            }
                        })));
    }

    /** The prank level as a button with its picture, which opens the list of levels to change it. */
    private UiElement prankButton(UiBuilder builder, EmblemView prank) {
        List<Component> lines = new ArrayList<>(menus.tooltip().description(prank));
        lines.add(Component.empty());
        lines.add(ClickHints.left(factory(), ProfileMessages.Edit.PRANK_CHANGE));
        return builder.button(ButtonRole.NONE, menus.tooltip().icon(prank), Component.literal(prank.title()).withColor(prank.look().textColor().getValue()),
                lines, click -> {
                    if (click.isLeft()) {
                        menus.pranksMenu(this).open(click.player());
                    }
                });
    }

    /** The facts about the player, one line each, which the dialogs show. */
    List<Component> factLines(ProfileSubject subject) {
        List<Component> lines = new ArrayList<>();
        subject.profile().ifPresentOrElse(
                profile -> {
                    lines.add(factory().text(Message.plain(ProfileMessages.Menu.LINE_DISCORD).with("discord", profile.discordName())));
                    profile.birthday().ifPresent(birthday -> lines.add(factory().text(Message.plain(ProfileMessages.Menu.LINE_BIRTHDAY)
                            .with("date", factory().text(BirthdayView.date(birthday))))));
                },
                () -> lines.add(factory().text(ProfileMessages.Menu.NO_DATA)));
        if (!subject.details().pronouns().isEmpty()) {
            lines.add(factory().text(Message.plain(ProfileMessages.Menu.LINE_PRONOUNS).with("pronouns", subject.details().pronouns())));
        }
        subject.playSeconds().ifPresent(seconds -> lines.add(factory().text(Message.plain(ProfileMessages.Menu.LINE_PLAY_TIME)
                .with("time", Durations.format(Duration.ofSeconds(seconds))))));
        subject.deaths().ifPresent(deaths -> lines.add(factory().text(Message.plain(ProfileMessages.Menu.LINE_DEATHS).with("deaths", String.valueOf(deaths)))));
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

    /** The base as the block it is on, which a click copies, and its dimension; or that there is none. */
    private Component baseValue(ProfileDetails details) {
        return details.base().map(base -> factory().text(Message.plain(ProfileMessages.Menu.BASE_VALUE)
                        .with("position", CoordinateView.position(base.x(), base.y(), base.z()))
                        .with("dimension", CoordinateView.dimension(base.dimension()))))
                .orElseGet(this::none);
    }

    /** The base as a line, for the dialogs. */
    Component baseLine(ProfileDetails details) {
        return details.base().map(base -> factory().text(Message.plain(ProfileMessages.Menu.LINE_BASE)
                        .with("position", CoordinateView.position(base.x(), base.y(), base.z()))
                        .with("dimension", CoordinateView.dimension(base.dimension()))))
                .orElseGet(() -> factory().text(ProfileMessages.Menu.LINE_NO_BASE));
    }

    /** The birthday, or that there is none, which the edit dialog shows although it can only be changed on the website. */
    Component birthdayLine(ProfileSubject subject) {
        Component date = subject.profile().flatMap(profile -> profile.birthday())
                .map(birthday -> factory().text(BirthdayView.date(birthday)))
                .orElseGet(() -> factory().text(ProfileMessages.Edit.BIRTHDAY_NONE));
        return factory().text(Message.plain(ProfileMessages.Menu.LINE_BIRTHDAY).with("date", date));
    }

    /** The bio broken into lines short enough to read, which neither the screen nor a dialog does by itself. */
    static List<Component> bioLines(String bio) {
        return bioLines(bio, BIO_LINE);
    }

    private static List<Component> bioLines(String bio, int width) {
        return TextWrap.lines(bio, width).stream().map(line -> (Component) Component.literal(line)).toList();
    }

    private UiElement.Button close(UiBuilder builder) {
        return builder.button(ButtonRole.CLOSE, new ItemStack(Items.BARRIER), factory().text(Messages.Gui.CLOSE), List.of(),
                click -> ui().close(click.player()));
    }

    /** The dot that says whether the player is online, which the client companion draws in the header. */
    private UiElement.Button status(UiBuilder builder, ProfileSubject subject) {
        return builder.button(ButtonRole.STATUS, ItemStack.EMPTY,
                factory().text(subject.online() ? ProfileMessages.Menu.STATUS_ONLINE : ProfileMessages.Menu.STATUS_OFFLINE), List.of(),
                subject.online() ? ONLINE_COLOR : OFFLINE_COLOR, click -> { });
    }

    /** The last band: the status, refresh and edit in the header for the viewer's own profile, and what saves the fields while editing. */
    private UiElement controls(UiBuilder builder, ProfileSubject subject) {
        List<UiElement> controls = new ArrayList<>();
        controls.add(status(builder, subject));
        if (editing) {
            controls.add(builder.button(ButtonRole.BACK, new ItemStack(Items.OAK_DOOR), factory().text(ProfileMessages.Menu.VIEW), List.of(),
                    click -> showAs(click.player(), false)));
            controls.add(builder.submitButton(factory().text(ProfileMessages.Edit.PRONOUNS_SAVE), pronounsInput));
            controls.add(builder.submitButton(factory().text(ProfileMessages.Edit.BIO_SAVE), bioArea));
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
