package com.panita.enriquecraft.staff.gui;

import com.panita.enriquecraft.core.message.Message;
import com.panita.enriquecraft.core.message.Timestamps;
import com.panita.enriquecraft.core.network.UiElement;
import com.panita.enriquecraft.core.ui.ClickHints;
import com.panita.enriquecraft.core.ui.PlayerHeads;
import com.panita.enriquecraft.core.ui.UiBuilder;
import com.panita.enriquecraft.core.ui.UiMenu;
import com.panita.enriquecraft.core.ui.UiPagedMenu;
import com.panita.enriquecraft.core.ui.UiService;
import com.panita.enriquecraft.staff.data.DeathPlayer;
import com.panita.enriquecraft.staff.message.DeathInventoryView;
import com.panita.enriquecraft.staff.message.StaffMessages;
import com.panita.enriquecraft.staff.service.DeathInventoryService;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.List;
import java.util.Optional;
import java.util.function.Function;

/**
 * Every player who has died, the most recent death first, searchable by name. Pressing one opens their
 * deaths. This is how staff reach a death inventory without typing a name.
 */
final class DeathPlayersMenu extends UiPagedMenu<DeathPlayer> {

    private final DeathInventoryService service;
    private final DeathInventoryView view;

    DeathPlayersMenu(UiService ui, DeathInventoryService service, DeathInventoryView view, UiMenu previous) {
        super(ui, previous);
        this.service = service;
        this.view = view;
    }

    @Override
    protected ItemStack icon() {
        return new ItemStack(Items.SKELETON_SKULL);
    }

    @Override
    protected Component title() {
        return factory().text(StaffMessages.Deaths.PLAYERS_TITLE);
    }

    @Override
    protected List<DeathPlayer> entries() {
        return service.playersWithDeaths();
    }

    @Override
    protected Optional<Function<DeathPlayer, String>> searchText() {
        return Optional.of(DeathPlayer::name);
    }

    @Override
    protected UiElement render(UiBuilder builder, DeathPlayer player) {
        Component name = factory().text(Message.plain(StaffMessages.Deaths.PLAYER_NAME).with("player", player.name()));
        List<Component> tooltip = List.of(
                factory().text(Message.plain(StaffMessages.Deaths.PLAYER_DEATHS).with("count", player.deaths())),
                factory().text(Message.plain(StaffMessages.Deaths.PLAYER_LAST).with("date", Timestamps.dateTime(player.lastDeath()))),
                Component.empty(),
                ClickHints.left(factory(), "ver muertes"));
        return builder.button(PlayerHeads.item(player.id()), name, tooltip, click -> {
            if (click.isLeft()) {
                new DeathListMenu(ui(), service, view, player.id(), player.name(), this).open(click.player());
            }
        });
    }
}
