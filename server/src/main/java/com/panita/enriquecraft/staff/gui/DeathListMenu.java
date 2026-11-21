package com.panita.enriquecraft.staff.gui;

import com.panita.enriquecraft.core.message.Message;
import com.panita.enriquecraft.core.network.ButtonRole;
import com.panita.enriquecraft.core.network.UiElement;
import com.panita.enriquecraft.core.ui.ClickHints;
import com.panita.enriquecraft.core.ui.UiBuilder;
import com.panita.enriquecraft.core.ui.UiMenu;
import com.panita.enriquecraft.core.ui.UiPagedMenu;
import com.panita.enriquecraft.core.ui.UiService;
import com.panita.enriquecraft.staff.data.DeathRecord;
import com.panita.enriquecraft.staff.message.DeathInfo;
import com.panita.enriquecraft.staff.message.DeathInventoryView;
import com.panita.enriquecraft.staff.message.StaffMessages;
import com.panita.enriquecraft.staff.service.DeathInventoryService;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.time.Clock;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * The deaths of one player, newest first. Each shows the player's head while recent, then ages into a
 * skull and a bone, and carries a check once its items were returned. Left click opens the inventory.
 */
public final class DeathListMenu extends UiPagedMenu<DeathRecord> {

    private final DeathInventoryService service;
    private final DeathInventoryView view;
    private final UUID player;
    private final String playerName;
    private final Clock clock;

    public DeathListMenu(UiService ui, DeathInventoryService service, DeathInventoryView view, UUID player,
                         String playerName) {
        this(ui, service, view, player, playerName, null, Clock.systemDefaultZone());
    }

    /** A list that goes back to the menu it was opened from. */
    DeathListMenu(UiService ui, DeathInventoryService service, DeathInventoryView view, UUID player,
                  String playerName, UiMenu previous) {
        this(ui, service, view, player, playerName, previous, Clock.systemDefaultZone());
    }

    DeathListMenu(UiService ui, DeathInventoryService service, DeathInventoryView view, UUID player,
                  String playerName, Clock clock) {
        this(ui, service, view, player, playerName, null, clock);
    }

    private DeathListMenu(UiService ui, DeathInventoryService service, DeathInventoryView view, UUID player,
                          String playerName, UiMenu previous, Clock clock) {
        super(ui, previous);
        this.service = service;
        this.view = view;
        this.player = player;
        this.playerName = playerName;
        this.clock = clock;
    }

    @Override
    protected ItemStack icon() {
        return new ItemStack(Items.SKELETON_SKULL);
    }

    @Override
    protected Component title() {
        return factory().text(Message.plain(StaffMessages.Deaths.LIST_TITLE).with("player", playerName));
    }

    @Override
    protected List<DeathRecord> entries() {
        return service.records(player);
    }

    @Override
    protected UiElement render(UiBuilder builder, DeathRecord record) {
        List<Component> tooltip = new ArrayList<>(DeathInfo.summary(record, factory()));
        tooltip.add(factory().text(Message.plain("")));
        tooltip.add(ClickHints.left(factory(), "inspeccionar"));
        String badge = record.isRestored() ? "✔" : "";
        return builder.button(ButtonRole.NONE, DeathIcons.of(record, clock.instant()), DeathInfo.name(record, factory()), tooltip,
                badge, click -> {
            if (click.isLeft()) {
                new DeathInventoryMenu(ui(), service, view, record, this, clock).open(click.player());
            }
        });
    }
}
