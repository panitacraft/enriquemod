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
import net.minecraft.network.chat.MutableComponent;
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
 * and the model), then two columns of facts, each as a label with its value under it:
 * the personal ones on the left and the ones about the game on the right. The edit view is the same screen with the fields that can
 * change turned into controls: the emblems and the prank level open their lists, the pronouns and the description are fields.
 * Players without the companion see the facts as dialogs ({@link ProfileDialogs}); a chest is only the last resort.
 */
final class ProfileMenu extends UiMenu {

    private static final int COLUMN_BIO_LINE = 36;
    private static final int NOTE_LINES = 9;
    private static final int AREA_WIDTH = 190;
    private static final int AREA_LINES = 5;
    private static final int VALUE_COLOR = 0x8E8E8E;

    private final ProfileMenus menus;
    private final Optional<String> target;
    private boolean editing;
    // The description field of the edit view, kept while the screen is described so the save button can confirm the form
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

    private void openEmblems(ServerPlayer player, int slot) {
        menus.emblemsMenu(this, slot).open(player);
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
        menus.tooltip().announce(viewer, Stream.of(Stream.of(emblems.prank()), emblems.nationality().stream(), emblems.slots().stream())
                .flatMap(all -> all).toList());

        List<UiElement> body = new ArrayList<>();
        // A little room above, so a picture taller than a line is not cut by the top of a screen that scrolls
        body.add(new UiElement.Spacer());
        body.add(new UiElement.Row(List.of(
                // Each column is alone in a row of its own, which a row centers vertically; the columns of a row start at the top
                new UiElement.Row(List.of(portrait(subject))),
                new UiElement.Spacer(),
                new UiElement.Row(List.of(new UiElement.Column(identity(builder, subject, emblems), UiElement.Align.START))))));
        body.add(new UiElement.Divider());
        body.add(new UiElement.Row(List.of(
                new UiElement.Column(personalColumn(builder, subject), UiElement.Align.START),
                new UiElement.Divider(),
                new UiElement.Column(gameColumn(builder, subject, emblems), UiElement.Align.START))));
        body.add(controls(builder, subject));
        return new UiElement.Column(body);
    }

    /** The viewer sees themselves as the model of their own player; anyone else is shown by their head, with the skin the server knows. */
    private UiElement portrait(ProfileSubject subject) {
        ItemStack head = head(subject);
        return new UiElement.Detail(isSelf() ? PlayerModels.ofViewer(head) : PlayerModels.ofPlayer(head), Component.empty(), List.of());
    }

    /** The head of the player with the skin the server knows, or by their id when it knows none, which the client looks up. */
    static ItemStack head(ProfileSubject subject) {
        return subject.skin().map(PlayerHeads::item).orElseGet(() -> PlayerHeads.item(subject.id()));
    }

    /** Wrapped, since a screen only keeps the colors of the parts of a text, and draws the text itself in its own gray. */
    private static UiElement.Label label(Component text) {
        return new UiElement.Label(Component.empty().append(text));
    }

    /** The rank, the name and the nationality on one line, and the emblems the player shows under it. */
    private List<UiElement> identity(UiBuilder builder, ProfileSubject subject, PlayerEmblems emblems) {
        List<UiElement> lines = new ArrayList<>();
        // One line of text, so the space the rank brings is the only one between it and the name
        MutableComponent nameLine = Component.empty();
        rank(subject).ifPresent(nameLine::append);
        nameLine.append(Component.literal(subject.name()).withStyle(ChatFormatting.WHITE));
        emblems.nationality().ifPresent(nationality -> nameLine.append(Component.literal(" ")).append(menus.tooltip().picture(nationality)));
        lines.add(label(nameLine));
        if (editing) {
            lines.add(new UiElement.Row(List.of(label(white(ProfileMessages.Menu.FIELD_PRONOUNS)), pronounsInput(builder, subject))));
        } else if (!subject.details().pronouns().isEmpty()) {
            // Only the value: the field's name is for the form
            lines.add(label(Component.literal(subject.details().pronouns())));
        }
        List<UiElement> tiles = new ArrayList<>();
        for (int place = 0; place < emblems.slots().size(); place++) {
            tiles.add(emblemTile(builder, emblems.slots().get(place), place));
        }
        lines.add(new UiElement.Row(tiles));
        return lines;
    }

    /** The rank the player shows in chat, which needs the player to be online: their ranks live in LuckPerms, which only knows who is here. */
    Optional<Component> rank(ProfileSubject subject) {
        ServerPlayer player = viewer().level().getServer().getPlayerList().getPlayer(subject.id());
        if (player == null || !subject.online()) {
            return Optional.empty();
        }
        Component rank = menus.rankOf(player);
        return rank.getString().isBlank() ? Optional.empty() : Optional.of(rank);
    }

    /** One emblem the player shows, large, with nothing behind it. Looking at it opens it in full; editing, the list to change its place. */
    private UiElement emblemTile(UiBuilder builder, EmblemView emblem, int place) {
        List<Component> lines = new ArrayList<>(menus.tooltip().description(emblem));
        if (!lines.isEmpty()) {
            lines.add(Component.empty());
        }
        lines.add(ClickHints.left(factory(), editing ? ProfileMessages.Edit.EMBLEM_CHANGE : ProfileMessages.Menu.EMBLEM_DETAIL));
        return builder.button(ButtonRole.EMBLEM, menus.tooltip().icon(emblem), menus.tooltip().title(emblem), lines, click -> {
            if (!click.isLeft()) {
                return;
            }
            if (editing) {
                openEmblems(click.player(), place);
            } else {
                menus.emblemDetail(this, emblem).open(click.player());
            }
        });
    }

    /** A field as a label with its value under it. */
    private List<UiElement> field(String name, Component value) {
        return List.of(label(white(name)), label(value));
    }

    /** A title in the same white as the name, whatever color the administrator's text for it has. */
    private Component white(String name) {
        return Component.literal(factory().text(name).getString()).withStyle(ChatFormatting.WHITE);
    }

    private UiElement.TextInput pronounsInput(UiBuilder builder, ProfileSubject subject) {
        // No redraw: the form's other field is confirmed right after, and a redraw would change the ids it is sent to
        return builder.input(factory().text(ProfileMessages.Edit.PRONOUNS_HINT), subject.details().pronouns(), ProfileDetails.PRONOUNS_MAX,
                submit -> menus.edits().pronouns(submit.player(), submit.text()));
    }

    private Component value(String text) {
        return Component.literal(text);
    }

    Component none() {
        return factory().text(ProfileMessages.Menu.FIELD_NONE);
    }

    /** What the player says about themselves: pronouns, birthday and description. */
    private List<UiElement> personalColumn(UiBuilder builder, ProfileSubject subject) {
        List<UiElement> lines = new ArrayList<>();
        lines.addAll(field(ProfileMessages.Menu.FIELD_BIRTHDAY, subject.profile().flatMap(profile -> profile.birthday())
                .map(birthday -> factory().text(BirthdayView.date(birthday))).orElseGet(this::none)));
        lines.add(new UiElement.Spacer());
        lines.add(label(white(ProfileMessages.Menu.FIELD_BIO)));
        if (editing) {
            bioArea = builder.textArea(factory().text(ProfileMessages.Edit.BIO), subject.details().bio(), Math.max(ProfileDetails.BIO_MAX, subject.details().bio().length()),
                    AREA_LINES, AREA_WIDTH,
                    submit -> {
                        // The form's last field: saving is done, so the profile is shown again
                        menus.edits().bio(submit.player(), submit.text());
                        showAs(submit.player(), false);
                    });
            lines.add(bioArea);
        } else if (subject.details().bio().isEmpty()) {
            lines.add(label(none()));
        } else {
            lines.add(new UiElement.Note(bioNote(subject.details().bio())));
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
        lines.add(label(white(ProfileMessages.Menu.FIELD_BASE)));
        lines.add(label(baseValue(subject.details())));
        if (editing) {
            lines.add(baseControls(builder));
        }
        lines.add(new UiElement.Spacer());
        lines.add(label(white(ProfileMessages.Menu.FIELD_PRANK)));
        Optional<Duration> wait = editing ? menus.edits().prankWait(viewer()) : Optional.empty();
        lines.add(editing && wait.isEmpty() ? prankButton(builder, emblems.prank()) : label(menus.tooltip().title(emblems.prank())));
        wait.ifPresent(remaining -> lines.add(label(factory().text(Message.plain(ProfileMessages.Edit.PRANK_WAIT).with("time", Durations.format(remaining))))));
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
        if (!lines.isEmpty()) {
            lines.add(Component.empty());
        }
        lines.add(ClickHints.left(factory(), ProfileMessages.Edit.PRANK_CHANGE));
        return builder.button(ButtonRole.NONE, menus.tooltip().icon(prank), Component.literal(prank.title()).withColor(prank.look().textColor().getValue()),
                lines, click -> {
                    if (click.isLeft()) {
                        menus.pranksMenu(this).open(click.player());
                    }
                });
    }

    /** The base as the block it is on, which a click copies, and its dimension; or that there is none. */
    Component baseValue(ProfileDetails details) {
        // The position keeps its click that copies it, and takes the gray of the other values
        return details.base().map(base -> factory().text(Message.plain(ProfileMessages.Menu.BASE_VALUE)
                        .with("position", CoordinateView.position(base.x(), base.y(), base.z()).copy().withColor(VALUE_COLOR))
                        .with("dimension", CoordinateView.dimension(base.dimension()))))
                .orElseGet(this::none);
    }

    private static List<Component> bioLines(String bio, int width) {
        return TextWrap.lines(bio, width).stream().map(line -> (Component) Component.literal(line)).toList();
    }

    /** The description for the screen: short lines, and no more than fit, since a description saved before the limit may be longer. */
    private static List<Component> bioNote(String bio) {
        List<Component> lines = bioLines(bio, COLUMN_BIO_LINE);
        if (lines.size() <= NOTE_LINES) {
            return lines;
        }
        List<Component> cut = new ArrayList<>(lines.subList(0, NOTE_LINES - 1));
        cut.add(Component.literal(lines.get(NOTE_LINES - 1).getString() + "…"));
        return cut;
    }

    private UiElement.Button close(UiBuilder builder) {
        return builder.button(ButtonRole.CLOSE, new ItemStack(Items.BARRIER), factory().text(Messages.Gui.CLOSE), List.of(),
                click -> ui().close(click.player()));
    }

    /** The last band: refresh and edit in the header for the viewer's own profile, and the one button that saves the fields while editing. */
    private UiElement controls(UiBuilder builder, ProfileSubject subject) {
        List<UiElement> controls = new ArrayList<>();
        if (editing) {
            controls.add(builder.button(ButtonRole.BACK, new ItemStack(Items.OAK_DOOR), factory().text(ProfileMessages.Menu.VIEW), List.of(),
                    click -> showAs(click.player(), false)));
            // One button saves the whole form: the client sends every field of the screen
            controls.add(builder.submitButton(factory().text(ProfileMessages.Edit.SAVE), bioArea));
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
