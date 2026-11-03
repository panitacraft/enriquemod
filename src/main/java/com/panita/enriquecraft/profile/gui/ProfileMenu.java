package com.panita.enriquecraft.profile.gui;

import com.panita.enriquecraft.core.message.Durations;
import com.panita.enriquecraft.core.message.Message;
import com.panita.enriquecraft.core.message.Messages;
import com.panita.enriquecraft.core.network.UiElement;
import com.panita.enriquecraft.core.network.ButtonRole;
import com.panita.enriquecraft.core.ui.ChestStyle;
import com.panita.enriquecraft.core.ui.ClickHints;
import com.panita.enriquecraft.core.ui.PlayerHeads;
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
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.Stats;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;

/**
 * A player's own profile: who they are on this server and on Panitacraft, and the emblems that show next to their name. From it they
 * can read their emblems again from Panitacraft and go to the screen where they choose them. The data is read again on every redraw,
 * so it follows a refresh that finishes while the menu is open.
 */
final class ProfileMenu extends UiMenu {

    private static final int ROW_WIDTH = 9;

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

    /** The client companion draws the title of a card under its item, so its tooltip starts with the title as a line of its own. */
    @Override
    protected UiElement describe(UiBuilder builder) {
        return describe(builder, true);
    }

    /** A chest shows the title as the name of the item, so its lines do not repeat it. */
    @Override
    protected UiElement describeChest(UiBuilder builder) {
        return describe(builder, false);
    }

    private UiElement describe(UiBuilder builder, boolean titleInTooltip) {
        ServerPlayer player = viewer();
        openMenus.seen(player.getUUID(), this);
        UUID id = player.getUUID();
        Optional<ModProfile> profile = profiles.profileOf(id);
        PlayerEmblems emblems = profiles.emblemsOf(id);
        tooltip.announce(player, Stream.concat(emblems.system().stream(), emblems.chosen().stream()).toList());

        UiElement.Detail detail = new UiElement.Detail(PlayerHeads.item(player.getGameProfile()),
                Component.literal(player.getGameProfile().name()), lines(player, profile, emblems));
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
        UiElement controls = new UiElement.Row(List.of(none, none, openEmblems, none, close, none, refresh, none, none));
        return new UiElement.Column(List.of(detail, shown(builder, emblems, titleInTooltip), controls));
    }

    /** The emblems that show next to the player's name, as cards; the row keeps its place when there are none. */
    private UiElement shown(UiBuilder builder, PlayerEmblems emblems, boolean titleInTooltip) {
        List<EmblemView> shown = Stream.concat(emblems.system().stream(), emblems.chosen().stream()).limit(ROW_WIDTH).toList();
        if (shown.isEmpty()) {
            return new UiElement.Row(List.of());
        }
        List<UiElement> cards = new ArrayList<>();
        for (EmblemView emblem : shown) {
            List<Component> lines = new ArrayList<>();
            if (titleInTooltip) {
                lines.add(tooltip.title(emblem));
            }
            lines.addAll(tooltip.description(emblem));
            lines.add(Component.empty());
            lines.add(ClickHints.left(factory(), ProfileMessages.Menu.EMBLEMS_OPEN));
            cards.add(builder.detail(new ItemStack(Items.NAME_TAG), tooltip.title(emblem), List.of(), lines, click -> {
                if (click.isLeft()) {
                    menus.emblemsMenu(this).open(click.player());
                }
            }));
        }
        return new UiElement.Row(cards);
    }

    private List<Component> lines(ServerPlayer player, Optional<ModProfile> profile, PlayerEmblems emblems) {
        List<Component> lines = new ArrayList<>();
        profile.ifPresentOrElse(known -> lines.add(factory().text(Message.plain(ProfileMessages.Menu.LINE_DISCORD)
                .with("discord", known.discordName()))), () -> lines.add(factory().text(ProfileMessages.Menu.NO_DATA)));
        lines.add(factory().text(Message.plain(ProfileMessages.Menu.LINE_PLAY_TIME).with("time", playTime(player))));
        if (!emblems.titles().isEmpty()) {
            lines.add(factory().text(Message.plain(ProfileMessages.Menu.LINE_TITLES)
                    .with("titles", String.join(", ", emblems.titles().stream().map(EmblemView::title).toList()))));
        }
        if (profile.isPresent() && emblems.system().isEmpty() && emblems.selectable().isEmpty()) {
            lines.add(factory().text(ProfileMessages.Menu.NO_EMBLEMS));
        }
        return Collections.unmodifiableList(lines);
    }

    private static String playTime(ServerPlayer player) {
        long seconds = Math.max(0, player.getStats().getValue(Stats.CUSTOM.get(Stats.PLAY_TIME))) / 20L;
        return Durations.format(Duration.ofSeconds(seconds));
    }
}
