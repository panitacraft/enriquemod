package com.panita.enriquecraft.staff.gui;

import com.panita.enriquecraft.core.gui.Menu;
import com.panita.enriquecraft.core.gui.MenuFactory;
import com.panita.enriquecraft.core.gui.MenuClick;
import com.panita.enriquecraft.core.gui.MenuItem;
import com.panita.enriquecraft.core.message.Message;
import com.panita.enriquecraft.core.message.Timestamps;
import com.panita.enriquecraft.staff.data.DeathRecord;
import com.panita.enriquecraft.staff.data.Dimensions;
import com.panita.enriquecraft.staff.message.CoordinateView;
import com.panita.enriquecraft.staff.message.DeathInventoryView;
import com.panita.enriquecraft.staff.message.StaffMessages;
import com.panita.enriquecraft.staff.service.DeathInventoryService;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.function.Consumer;

/**
 * The inventory a player had when they died, laid out like a player inventory, for staff to look
 * at. Nothing in it can be changed: clicking an item gives the staff member a copy of it, and the
 * bottom row offers the actions on the whole inventory.
 */
public final class DeathInventoryMenu extends Menu {

    private static final int BACK_SLOT = 45;
    private static final int TELEPORT_SLOT = 46;
    private static final int CHESTS_SLOT = 48;
    private static final int INFO_SLOT = 49;
    private static final int RESTORE_SLOT = 50;
    private static final int DELETE_SLOT = 52;

    private final DeathInventoryService service;
    private final DeathInventoryView view;
    private final DeathRecord record;

    public DeathInventoryMenu(MenuFactory factory, DeathInventoryService service, DeathInventoryView view,
                              DeathRecord record, Menu previous) {
        super(factory, previous);
        this.service = service;
        this.view = view;
        this.record = record;
    }

    @Override
    protected Component title() {
        return factory().text(Message.plain(StaffMessages.Deaths.INSPECT_TITLE).with("player", record.playerName()));
    }

    @Override
    protected int rows() {
        return 6;
    }

    @Override
    protected void draw() {
        drawItems();
        drawButtons();
        fillRest();
    }

    private void drawItems() {
        for (int slot = 0; slot < DeathRecord.SLOT_COUNT; slot++) {
            ItemStack stack = record.items().get(slot);
            if (stack.isEmpty()) {
                continue;
            }
            ItemStack shown = factory().item(stack)
                    .lore(Message.plain(""), Message.plain(StaffMessages.Deaths.ITEM_HINT))
                    .build();
            set(DeathInventoryLayout.menuSlot(slot), MenuItem.button(shown, click -> {
                if (click.isLeft()) {
                    view.giveItem(click.player(), stack);
                }
            }));
        }
    }

    private void drawButtons() {
        set(BACK_SLOT, MenuItem.button(factory().item(Items.OAK_DOOR).name(StaffMessages.Deaths.BACK).build(),
                click -> previous().open(click.player())));
        set(TELEPORT_SLOT, action(Items.ENDER_PEARL, StaffMessages.Deaths.TELEPORT_NAME, StaffMessages.Deaths.TELEPORT_LORE,
                null, click -> {
                    click.player().closeContainer();
                    view.teleport(click.player(), record);
                }));
        set(CHESTS_SLOT, action(Items.CHEST, StaffMessages.Deaths.CHESTS_NAME, StaffMessages.Deaths.CHESTS_LORE,
                null, click -> view.chestsGiven(click.player(), record,
                        service.giveChests(click.player(), record, this::chestName))));
        set(INFO_SLOT, MenuItem.display(factory().item(Items.PAPER).name(StaffMessages.Deaths.INFO_NAME)
                .lore(Message.plain(StaffMessages.Deaths.ENTRY_NAME).with("date", Timestamps.format(record.diedAt())),
                        Message.plain(StaffMessages.Deaths.ENTRY_CAUSE).with("cause", record.cause()),
                        Message.plain(StaffMessages.Deaths.ENTRY_DIMENSION).with("dimension", Dimensions.displayName(record.dimension())),
                        Message.plain(StaffMessages.Deaths.ENTRY_POSITION)
                                .with("x", CoordinateView.number(record.x()))
                                .with("y", CoordinateView.number(record.y()))
                                .with("z", CoordinateView.number(record.z())),
                        Message.plain(StaffMessages.Deaths.ENTRY_XP).with("level", record.xpLevel()))
                .build()));
        set(RESTORE_SLOT, action(Items.EMERALD_BLOCK, StaffMessages.Deaths.RESTORE_NAME, StaffMessages.Deaths.RESTORE_LORE,
                StaffMessages.Deaths.RESTORE_WARNING, click -> restore(click.player())));
        set(DELETE_SLOT, action(Items.LAVA_BUCKET, StaffMessages.Deaths.DELETE_NAME, StaffMessages.Deaths.DELETE_LORE,
                StaffMessages.Deaths.DELETE_WARNING, click -> {
                    if (click.isShift()) {
                        service.delete(record.player(), record.id());
                        view.deleted(click.player());
                        previous().open(click.player());
                    } else {
                        view.deleteHint(click.player());
                    }
                }));
    }

    /** A button that runs on a plain left click; a shift click counts, so a button can ask for it itself. */
    private MenuItem action(Item icon, String name, String lore, String warning,
                            Consumer<MenuClick> onClick) {
        var builder = factory().item(icon).name(name).lore(lore);
        if (warning != null) {
            builder.lore(warning);
        }
        return MenuItem.button(builder.build(), click -> {
            if (click.isLeft()) {
                onClick.accept(click);
            }
        });
    }

    private void restore(ServerPlayer staff) {
        ServerPlayer target = staff.level().getServer().getPlayerList().getPlayer(record.player());
        if (target == null) {
            view.targetOffline(staff, record.playerName());
            return;
        }
        view.restored(staff, target, service.restore(target, record));
    }

    private Component chestName(int number, int total) {
        String template = total == 1 ? StaffMessages.Deaths.CHEST_NAME : StaffMessages.Deaths.CHEST_NAME_PART;
        return factory().text(Message.plain(template)
                .with("player", record.playerName())
                .with("number", number)
                .with("total", total));
    }
}
