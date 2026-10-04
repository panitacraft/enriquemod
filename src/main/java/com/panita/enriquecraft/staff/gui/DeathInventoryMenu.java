package com.panita.enriquecraft.staff.gui;

import com.panita.enriquecraft.core.message.Message;
import com.panita.enriquecraft.core.message.Timestamps;
import com.panita.enriquecraft.core.network.UiElement;
import com.panita.enriquecraft.core.ui.ChestStyle;
import com.panita.enriquecraft.core.ui.UiBuilder;
import com.panita.enriquecraft.core.ui.UiClick;
import com.panita.enriquecraft.core.ui.UiMenu;
import com.panita.enriquecraft.core.ui.UiService;
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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;

/**
 * The inventory a player had when they died, laid out like a player inventory, for staff to look
 * at. Nothing in it can be changed: clicking an item gives the staff member a copy of it, and the
 * bottom row offers the actions on the whole inventory.
 */
public final class DeathInventoryMenu extends UiMenu {

    private static final int GRID_COLUMNS = 9;
    private static final int GRID_ROWS = 5;

    private final DeathInventoryService service;
    private final DeathInventoryView view;
    private final DeathRecord record;

    public DeathInventoryMenu(UiService ui, DeathInventoryService service, DeathInventoryView view,
                              DeathRecord record, UiMenu previous) {
        super(ui, previous);
        this.service = service;
        this.view = view;
        this.record = record;
    }

    @Override
    protected ItemStack icon() {
        return new ItemStack(Items.SKELETON_SKULL);
    }

    @Override
    protected Component title() {
        return factory().text(Message.plain(StaffMessages.Deaths.INSPECT_TITLE).with("player", record.playerName()));
    }

    /** Every slot of the chest shows something, so the inventory reads as one block. */
    @Override
    protected ChestStyle chestStyle() {
        return ChestStyle.FILLED;
    }

    @Override
    protected UiElement describe(UiBuilder builder) {
        return new UiElement.Column(List.of(
                new UiElement.Grid(GRID_COLUMNS, GRID_ROWS, items(builder)),
                controls(builder)));
    }

    /** One cell per inventory slot, placed where a player inventory would have it. */
    private List<UiElement> items(UiBuilder builder) {
        List<UiElement> cells = new ArrayList<>(Collections.nCopies(GRID_COLUMNS * GRID_ROWS, new UiElement.Spacer()));
        List<Component> hint = List.of(factory().text(Message.plain("")), factory().text(StaffMessages.Deaths.ITEM_HINT));
        for (int slot = 0; slot < DeathRecord.SLOT_COUNT; slot++) {
            ItemStack stack = record.items().get(slot);
            if (stack.isEmpty()) {
                continue;
            }
            // No label: the button is the item itself, so it keeps its own name and lore.
            cells.set(DeathInventoryLayout.menuSlot(slot), builder.button(stack.copy(), Component.empty(), hint, click -> {
                if (click.isLeft()) {
                    view.giveItem(click.player(), stack);
                }
            }));
        }
        return cells;
    }

    private UiElement controls(UiBuilder builder) {
        UiElement.Spacer none = new UiElement.Spacer();
        UiElement back = builder.button(new ItemStack(Items.OAK_DOOR), factory().text(StaffMessages.Deaths.BACK), List.of(),
                click -> previous().open(click.player()));
        UiElement teleport = action(builder, Items.ENDER_PEARL, StaffMessages.Deaths.TELEPORT_NAME,
                StaffMessages.Deaths.TELEPORT_LORE, null, click -> {
                    ui().close(click.player());
                    view.teleport(click.player(), record);
                });
        UiElement chests = action(builder, Items.CHEST, StaffMessages.Deaths.CHESTS_NAME, StaffMessages.Deaths.CHESTS_LORE,
                null, click -> view.chestsGiven(click.player(), record,
                        service.giveChests(click.player(), record, this::chestName)));
        UiElement info = builder.button(new ItemStack(Items.PAPER), factory().text(StaffMessages.Deaths.INFO_NAME), infoLines(),
                click -> {
                });
        UiElement restore = action(builder, Items.EMERALD_BLOCK, StaffMessages.Deaths.RESTORE_NAME,
                StaffMessages.Deaths.RESTORE_LORE, StaffMessages.Deaths.RESTORE_WARNING, click -> restore(click.player()));
        UiElement delete = action(builder, Items.LAVA_BUCKET, StaffMessages.Deaths.DELETE_NAME,
                StaffMessages.Deaths.DELETE_LORE, StaffMessages.Deaths.DELETE_WARNING, click -> {
                    if (click.isShift()) {
                        service.delete(record.player(), record.id());
                        view.deleted(click.player());
                        previous().open(click.player());
                    } else {
                        view.deleteHint(click.player());
                    }
                });
        return new UiElement.Row(List.of(back, teleport, none, chests, info, restore, none, delete, none));
    }

    private List<Component> infoLines() {
        return List.of(
                factory().text(Message.plain(StaffMessages.Deaths.ENTRY_NAME).with("date", Timestamps.dateTime(record.diedAt()))),
                factory().text(Message.plain(StaffMessages.Deaths.ENTRY_CAUSE).with("cause", record.cause())),
                factory().text(Message.plain(StaffMessages.Deaths.ENTRY_DIMENSION)
                        .with("dimension", Dimensions.displayName(record.dimension()))),
                factory().text(Message.plain(StaffMessages.Deaths.ENTRY_POSITION)
                        .with("x", CoordinateView.number(record.x()))
                        .with("y", CoordinateView.number(record.y()))
                        .with("z", CoordinateView.number(record.z()))),
                factory().text(Message.plain(StaffMessages.Deaths.ENTRY_XP).with("level", record.xpLevel())));
    }

    /** A button that runs on a plain left click; a shift click counts, so a button can ask for it itself. */
    private UiElement action(UiBuilder builder, Item icon, String name, String lore, String warning,
                             Consumer<UiClick> onClick) {
        List<Component> tooltip = new ArrayList<>(List.of(factory().text(lore)));
        if (warning != null) {
            tooltip.add(factory().text(warning));
        }
        return builder.button(new ItemStack(icon), factory().text(name), tooltip, click -> {
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
