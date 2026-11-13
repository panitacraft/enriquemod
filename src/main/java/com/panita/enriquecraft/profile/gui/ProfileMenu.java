package com.panita.enriquecraft.profile.gui;

import com.panita.enriquecraft.auth.data.AuthMode;
import com.panita.enriquecraft.core.message.Durations;
import com.panita.enriquecraft.core.message.Message;
import com.panita.enriquecraft.core.message.Messages;
import com.panita.enriquecraft.core.network.ButtonRole;
import com.panita.enriquecraft.core.network.UiElement;
import com.panita.enriquecraft.core.ui.ChestStyle;
import com.panita.enriquecraft.core.ui.ClickHints;
import com.panita.enriquecraft.core.ui.PlayerHeads;
import com.panita.enriquecraft.core.ui.PlayerModels;
import com.panita.enriquecraft.core.ui.UiBuilder;
import com.panita.enriquecraft.core.ui.UiMenu;
import com.panita.enriquecraft.core.ui.UiService;
import com.panita.enriquecraft.profile.client.ModProfile;
import com.panita.enriquecraft.profile.message.EmblemTooltip;
import com.panita.enriquecraft.profile.message.ProfileMessages;
import com.panita.enriquecraft.profile.message.ProfileView;
import com.panita.enriquecraft.profile.service.EmblemView;
import com.panita.enriquecraft.profile.service.PlayerEmblems;
import com.panita.enriquecraft.profile.service.ProfileRefresher;
import com.panita.enriquecraft.profile.service.ProfileService;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.Stats;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;

/**
 * A player's own profile: who they are on this server and on Panitacraft, and the emblems that show next to their name. From it they
 * can read their emblems again from Panitacraft and go to the screen where they choose them. The data is read again on every redraw,
 * so it follows a refresh that finishes while the menu is open.
 * <p>
 * The client companion's screen has the player and their facts on top, then the prank level and below it the emblems the player can
 * choose, each as a small square with its image. A chest keeps to a row of cards, since it has no room for more.
 */
final class ProfileMenu extends UiMenu {

    private static final int ROW_WIDTH = 9;
    private static final int GRID_COLUMNS = 6;

    private final ProfileMenus menus;
    private final ProfileService profiles;
    private final ProfileView view;
    private final EmblemTooltip tooltip;
    private final ProfileRefresher refresher;
    private final OpenMenus openMenus;

    ProfileMenu(UiService ui, ProfileMenus menus, ProfileService profiles, ProfileView view, EmblemTooltip tooltip,
                ProfileRefresher refresher, OpenMenus openMenus) {
        super(ui, null);
        this.menus = menus;
        this.profiles = profiles;
        this.view = view;
        this.tooltip = tooltip;
        this.refresher = refresher;
        this.openMenus = openMenus;
    }

    @Override
    protected Component title() {
        return factory().text(ProfileMessages.Menu.TITLE);
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
    protected void onClose() {
        openMenus.closed(viewer().getUUID(), this);
    }

    /** The client companion: the player and their facts side by side, then the emblems as squares. */
    @Override
    protected UiElement describe(UiBuilder builder) {
        ServerPlayer player = viewer();
        UUID id = player.getUUID();
        openMenus.seen(id, this);
        PlayerEmblems emblems = profiles.emblemsOf(player);
        tooltip.announce(player, Stream.concat(Stream.of(emblems.prank()), emblems.selectable().stream()).toList());

        UiElement.Detail model = new UiElement.Detail(PlayerModels.ofViewer(PlayerHeads.item(player.getGameProfile())), Component.empty(), List.of());
        List<UiElement> lines = new ArrayList<>();
        lines.add(new UiElement.Label(Component.literal(player.getGameProfile().name()).withStyle(ChatFormatting.YELLOW, ChatFormatting.BOLD)));
        factLines(player, profiles.profileOf(id)).forEach(line -> lines.add(new UiElement.Label(line)));
        // A column beside the model would start at the top; inside a row of its own it sits in the middle of the model's height
        UiElement facts = new UiElement.Row(List.of(new UiElement.Column(lines, UiElement.Align.START)));
        UiElement identity = new UiElement.Row(List.of(model, facts));

        List<UiElement> body = new ArrayList<>();
        body.add(identity);
        body.add(new UiElement.Divider());
        body.add(new UiElement.Label(factory().text(ProfileMessages.Menu.EMBLEMS_HEADER)));
        body.add(new UiElement.Grid(1, 1, List.of(square(builder, emblems.prank(), ""))));
        body.add(new UiElement.Rule());
        List<EmblemView> chosen = emblems.chosen();
        List<UiElement> squares = emblems.selectable().stream()
                .map(emblem -> square(builder, emblem, chosen.stream().anyMatch(shown -> shown.id().equals(emblem.id())) ? "✔" : ""))
                .toList();
        if (squares.isEmpty()) {
            body.add(new UiElement.Label(factory().text(ProfileMessages.Menu.NO_EMBLEMS)));
        } else {
            body.add(new UiElement.Grid(Math.min(GRID_COLUMNS, squares.size()), (squares.size() + GRID_COLUMNS - 1) / GRID_COLUMNS, squares));
        }
        body.add(controls(builder));
        return new UiElement.Column(body);
    }

    /** A chest: the player, the emblems that show next to their name as cards, and the controls. */
    @Override
    protected UiElement describeChest(UiBuilder builder) {
        ServerPlayer player = viewer();
        openMenus.seen(player.getUUID(), this);
        PlayerEmblems emblems = profiles.emblemsOf(player);
        tooltip.announce(player, Stream.concat(Stream.of(emblems.prank()), emblems.chosen().stream()).toList());
        UiElement.Detail detail = new UiElement.Detail(PlayerHeads.item(player.getGameProfile()),
                Component.literal(player.getGameProfile().name()), factLines(player, profiles.profileOf(player.getUUID())));
        return new UiElement.Column(List.of(detail, cards(builder, emblems), controls(builder)));
    }

    private UiElement controls(UiBuilder builder) {
        UiElement none = new UiElement.Spacer();
        UiElement openEmblems = builder.button(ButtonRole.NONE, new ItemStack(Items.NAME_TAG), factory().text(ProfileMessages.Menu.EMBLEMS),
                List.of(factory().text(ProfileMessages.Menu.EMBLEMS_LORE), Component.empty(),
                        ClickHints.left(factory(), ProfileMessages.Menu.EMBLEMS_OPEN)), click -> {
                    if (click.isLeft()) {
                        menus.emblemsMenu(this).open(click.player());
                    }
                });
        UiElement refresh = builder.button(ButtonRole.NONE, new ItemStack(Items.CLOCK), factory().text(ProfileMessages.Menu.REFRESH),
                List.of(factory().text(ProfileMessages.Menu.REFRESH_LORE)), click -> {
                    if (click.isLeft()) {
                        refresher.refreshSelf(click.player()).thenAccept(result -> view.selfRefresh(click.player(), result));
                    }
                });
        UiElement close = builder.button(ButtonRole.CLOSE, new ItemStack(Items.BARRIER), factory().text(Messages.Gui.CLOSE), List.of(),
                click -> ui().close(click.player()));
        return new UiElement.Row(List.of(none, none, openEmblems, none, close, none, refresh, none, none));
    }

    /** One emblem as a square with its image; pressing it opens the screen where the emblems are chosen. */
    private UiElement square(UiBuilder builder, EmblemView emblem, String badge) {
        List<Component> lines = new ArrayList<>(tooltip.description(emblem));
        lines.add(Component.empty());
        lines.add(ClickHints.left(factory(), ProfileMessages.Menu.EMBLEMS_OPEN));
        return builder.button(ButtonRole.NONE, tooltip.icon(emblem), tooltip.title(emblem), lines, badge, click -> {
            if (click.isLeft()) {
                menus.emblemsMenu(this).open(click.player());
            }
        });
    }

    /** The emblems that show next to the player's name, as cards of a chest; the row keeps its place when there are none. */
    private UiElement cards(UiBuilder builder, PlayerEmblems emblems) {
        List<EmblemView> shown = Stream.concat(Stream.of(emblems.prank()), emblems.chosen().stream()).limit(ROW_WIDTH).toList();
        List<UiElement> cards = new ArrayList<>();
        for (EmblemView emblem : shown) {
            List<Component> lines = new ArrayList<>(tooltip.description(emblem));
            lines.add(Component.empty());
            lines.add(ClickHints.left(factory(), ProfileMessages.Menu.EMBLEMS_OPEN));
            cards.add(builder.detail(tooltip.icon(emblem), tooltip.title(emblem), List.of(), lines, click -> {
                if (click.isLeft()) {
                    menus.emblemsMenu(this).open(click.player());
                }
            }));
        }
        return new UiElement.Row(cards);
    }

    /** The facts about the player, one line each; the Discord name is grayed out. */
    private List<Component> factLines(ServerPlayer player, Optional<ModProfile> profile) {
        List<Component> lines = new ArrayList<>();
        profile.ifPresentOrElse(known -> lines.add(factory().text(Message.plain(ProfileMessages.Menu.LINE_DISCORD).with("discord", known.discordName()))),
                () -> lines.add(factory().text(ProfileMessages.Menu.NO_DATA)));
        boolean premium = AuthMode.of(player.getUUID(), player.getGameProfile().name()) == AuthMode.PREMIUM;
        lines.add(factory().text(Message.plain(ProfileMessages.Menu.LINE_STATUS)
                .with("status", premium ? ProfileMessages.Menu.PREMIUM : ProfileMessages.Menu.NO_PREMIUM)));
        lines.add(factory().text(Message.plain(ProfileMessages.Menu.LINE_PLAY_TIME).with("time", playTime(player))));
        lines.add(factory().text(Message.plain(ProfileMessages.Menu.LINE_DEATHS).with("count", deaths(player))));
        return lines;
    }

    private static String playTime(ServerPlayer player) {
        long seconds = Math.max(0, player.getStats().getValue(Stats.CUSTOM.get(Stats.PLAY_TIME))) / 20L;
        return Durations.format(Duration.ofSeconds(seconds));
    }

    private static int deaths(ServerPlayer player) {
        return Math.max(0, player.getStats().getValue(Stats.CUSTOM.get(Stats.DEATHS)));
    }
}
