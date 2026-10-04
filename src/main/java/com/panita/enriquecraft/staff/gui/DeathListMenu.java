package com.panita.enriquecraft.staff.gui;

import com.panita.enriquecraft.core.message.Message;
import com.panita.enriquecraft.core.message.Timestamps;
import com.panita.enriquecraft.core.network.UiElement;
import com.panita.enriquecraft.core.ui.UiBuilder;
import com.panita.enriquecraft.core.ui.UiPagedMenu;
import com.panita.enriquecraft.core.ui.UiService;
import com.panita.enriquecraft.staff.data.DeathRecord;
import com.panita.enriquecraft.staff.data.Dimensions;
import com.panita.enriquecraft.staff.message.CoordinateView;
import com.panita.enriquecraft.staff.message.DeathInventoryView;
import com.panita.enriquecraft.staff.message.StaffMessages;
import com.panita.enriquecraft.staff.service.DeathInventoryService;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.List;
import java.util.UUID;

/**
 * The deaths of one player, newest first. Left click opens the inventory the player had.
 */
public final class DeathListMenu extends UiPagedMenu<DeathRecord> {

    private final DeathInventoryService service;
    private final DeathInventoryView view;
    private final UUID player;
    private final String playerName;

    public DeathListMenu(UiService ui, DeathInventoryService service, DeathInventoryView view, UUID player,
                         String playerName) {
        super(ui, null);
        this.service = service;
        this.view = view;
        this.player = player;
        this.playerName = playerName;
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
        Component name = factory().text(
                Message.plain(StaffMessages.Deaths.ENTRY_NAME).with("date", Timestamps.dateTime(record.diedAt())));
        List<Component> details = List.of(
                factory().text(Message.plain(StaffMessages.Deaths.ENTRY_CAUSE).with("cause", record.cause())),
                factory().text(Message.plain(StaffMessages.Deaths.ENTRY_DIMENSION)
                        .with("dimension", Dimensions.displayName(record.dimension()))),
                factory().text(Message.plain(StaffMessages.Deaths.ENTRY_POSITION)
                        .with("x", CoordinateView.number(record.x()))
                        .with("y", CoordinateView.number(record.y()))
                        .with("z", CoordinateView.number(record.z()))),
                factory().text(Message.plain(StaffMessages.Deaths.ENTRY_ITEMS).with("count", record.nonEmptyItems().size())),
                factory().text(Message.plain(StaffMessages.Deaths.ENTRY_XP).with("level", record.xpLevel())),
                factory().text(StaffMessages.Deaths.ENTRY_CLICK_HINT));
        return builder.button(new ItemStack(Items.SKELETON_SKULL), name, details, click -> {
            if (click.isLeft()) {
                new DeathInventoryMenu(ui(), service, view, record, this).open(click.player());
            }
        });
    }
}
