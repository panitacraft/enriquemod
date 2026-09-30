package com.panita.enriquecraft.staff.gui;

import com.panita.enriquecraft.core.gui.MenuFactory;
import com.panita.enriquecraft.core.gui.MenuItem;
import com.panita.enriquecraft.core.gui.PaginatedMenu;
import com.panita.enriquecraft.core.message.Message;
import com.panita.enriquecraft.core.message.Timestamps;
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
public final class DeathListMenu extends PaginatedMenu<DeathRecord> {

    private final DeathInventoryService service;
    private final DeathInventoryView view;
    private final UUID player;
    private final String playerName;

    public DeathListMenu(MenuFactory factory, DeathInventoryService service, DeathInventoryView view, UUID player,
                         String playerName) {
        super(factory, null);
        this.service = service;
        this.view = view;
        this.player = player;
        this.playerName = playerName;
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
    protected MenuItem render(DeathRecord record) {
        ItemStack stack = factory().item(Items.SKELETON_SKULL)
                .name(Message.plain(StaffMessages.Deaths.ENTRY_NAME).with("date", Timestamps.format(record.diedAt())))
                .lore(Message.plain(StaffMessages.Deaths.ENTRY_CAUSE).with("cause", record.cause()),
                        Message.plain(StaffMessages.Deaths.ENTRY_DIMENSION).with("dimension", Dimensions.displayName(record.dimension())),
                        Message.plain(StaffMessages.Deaths.ENTRY_POSITION)
                                .with("x", CoordinateView.number(record.x()))
                                .with("y", CoordinateView.number(record.y()))
                                .with("z", CoordinateView.number(record.z())),
                        Message.plain(StaffMessages.Deaths.ENTRY_ITEMS).with("count", record.nonEmptyItems().size()),
                        Message.plain(StaffMessages.Deaths.ENTRY_XP).with("level", record.xpLevel()),
                        Message.plain(StaffMessages.Deaths.ENTRY_CLICK_HINT))
                .build();
        return MenuItem.button(stack, click -> {
            if (click.isLeft()) {
                new DeathInventoryMenu(factory(), service, view, record, this).open(click.player());
            }
        });
    }
}
