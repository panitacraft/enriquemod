package com.panita.enriquecraft.staff.gui;

import com.panita.enriquecraft.core.message.Message;
import com.panita.enriquecraft.core.message.Timestamps;
import com.panita.enriquecraft.core.network.ButtonRole;
import com.panita.enriquecraft.core.network.UiElement;
import com.panita.enriquecraft.core.ui.ChestStyle;
import com.panita.enriquecraft.core.ui.ClickHints;
import com.panita.enriquecraft.core.ui.ConfirmMenu;
import com.panita.enriquecraft.core.ui.PlayerHeads;
import com.panita.enriquecraft.core.ui.UiBuilder;
import com.panita.enriquecraft.core.ui.UiClick;
import com.panita.enriquecraft.core.ui.UiMenu;
import com.panita.enriquecraft.core.ui.UiService;
import com.panita.enriquecraft.staff.data.DeathRecord;
import com.panita.enriquecraft.staff.message.DeathInfo;
import com.panita.enriquecraft.staff.message.DeathInventoryView;
import com.panita.enriquecraft.staff.message.StaffMessages;
import com.panita.enriquecraft.staff.service.DeathInventoryService;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.time.Clock;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;

/**
 * The inventory a player had when they died, for staff to look at. Nothing in it can be changed:
 * clicking an item gives the staff member a copy of it, and the actions work on the whole inventory.
 * <p>
 * The client companion shows the player's head with what is known about the death, a separator, and
 * the carried items in a grid that scrolls when there are many. A chest has no scrolling, so it lays the
 * slots out like a player inventory instead, with the same actions in its bottom row.
 */
public final class DeathInventoryMenu extends UiMenu {

    private static final int GRID_COLUMNS = 9;
    private static final int CHEST_GRID_ROWS = 5;

    /** How tall the items may grow, in GUI pixels, before they scroll: four rows of cells. */
    private static final int SCROLL_HEIGHT = 100;

    private final DeathInventoryService service;
    private final DeathInventoryView view;
    private final Clock clock;
    /** Replaced when the items are returned, so the screen shows the new state. */
    private DeathRecord record;

    public DeathInventoryMenu(UiService ui, DeathInventoryService service, DeathInventoryView view,
                              DeathRecord record, UiMenu previous) {
        this(ui, service, view, record, previous, Clock.systemDefaultZone());
    }

    DeathInventoryMenu(UiService ui, DeathInventoryService service, DeathInventoryView view,
                       DeathRecord record, UiMenu previous, Clock clock) {
        super(ui, previous);
        this.service = service;
        this.view = view;
        this.record = record;
        this.clock = clock;
    }

    @Override
    protected ItemStack icon() {
        return DeathIcons.of(record, clock.instant());
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
        List<UiElement> items = new ArrayList<>();
        for (ItemStack stack : record.nonEmptyItems()) {
            items.add(itemButton(builder, stack));
        }
        int rows = Math.max(1, (items.size() + GRID_COLUMNS - 1) / GRID_COLUMNS);
        return new UiElement.Column(List.of(
                // No title: the date leads the lines, and the screen's own title already says whose inventory it is.
                new UiElement.Detail(PlayerHeads.item(record.player()), Component.empty(), DeathInfo.full(record, factory())),
                new UiElement.Divider(),
                new UiElement.Scroll(new UiElement.Grid(GRID_COLUMNS, rows, items), SCROLL_HEIGHT),
                controls(builder, false)));
    }

    /** One cell per inventory slot, placed where a player inventory would have it. */
    @Override
    protected UiElement describeChest(UiBuilder builder) {
        List<UiElement> cells = new ArrayList<>(Collections.nCopies(GRID_COLUMNS * CHEST_GRID_ROWS, new UiElement.Spacer()));
        for (int slot = 0; slot < DeathRecord.SLOT_COUNT; slot++) {
            ItemStack stack = record.items().get(slot);
            if (!stack.isEmpty()) {
                cells.set(DeathInventoryLayout.menuSlot(slot), itemButton(builder, stack));
            }
        }
        return new UiElement.Column(List.of(
                new UiElement.Grid(GRID_COLUMNS, CHEST_GRID_ROWS, cells),
                controls(builder, true)));
    }

    /** No label: the button is the item itself, so it keeps its own name and lore. */
    private UiElement itemButton(UiBuilder builder, ItemStack stack) {
        List<Component> hint = List.of(factory().text(Message.plain("")), ClickHints.left(factory(), "obtener copia"));
        return builder.button(stack.copy(), Component.empty(), hint, click -> {
            if (click.isLeft()) {
                view.giveItem(click.player(), stack);
            }
        });
    }

    /**
     * @param withInfo whether to include the paper with the death's data, which only a chest needs: the client
     *                 companion shows that data on the screen itself
     */
    private UiElement controls(UiBuilder builder, boolean withInfo) {
        UiElement.Spacer none = new UiElement.Spacer();
        UiElement back = builder.button(ButtonRole.BACK, new ItemStack(Items.OAK_DOOR), factory().text(StaffMessages.Deaths.BACK),
                List.of(), click -> previous().open(click.player()));
        UiElement teleport = action(builder, ButtonRole.NONE, Items.ENDER_PEARL, StaffMessages.Deaths.TELEPORT_NAME,
                StaffMessages.Deaths.TELEPORT_LORE, List.of(), click -> {
                    ui().close(click.player());
                    view.teleport(click.player(), record);
                });
        UiElement chests = action(builder, ButtonRole.NONE, Items.CHEST, StaffMessages.Deaths.CHESTS_NAME, StaffMessages.Deaths.CHESTS_LORE,
                List.of(), click -> view.chestsGiven(click.player(), record,
                        service.giveChests(click.player(), record, this::chestName)));
        UiElement restore = action(builder, ButtonRole.SUCCESS, Items.EMERALD_BLOCK, StaffMessages.Deaths.RESTORE_NAME,
                StaffMessages.Deaths.RESTORE_LORE, restoreWarnings(), click -> restore(click.player()));
        UiElement delete = action(builder, ButtonRole.DANGER, Items.LAVA_BUCKET, StaffMessages.Deaths.DELETE_NAME,
                StaffMessages.Deaths.DELETE_LORE, List.of(), click -> confirmDelete().open(click.player()));
        List<UiElement> row = new ArrayList<>(List.of(back, teleport, none, chests));
        row.add(withInfo ? info(builder) : none);
        row.addAll(List.of(restore, none, delete, none));
        return new UiElement.Row(row);
    }

    private UiElement info(UiBuilder builder) {
        return builder.button(new ItemStack(Items.PAPER), factory().text(StaffMessages.Deaths.INFO_NAME),
                DeathInfo.full(record, factory()), click -> {
                });
    }

    /** That the player must be online, and that returning the items twice duplicates them. */
    private List<String> restoreWarnings() {
        return record.isRestored()
                ? List.of(StaffMessages.Deaths.RESTORE_WARNING, StaffMessages.Deaths.RESTORE_AGAIN)
                : List.of(StaffMessages.Deaths.RESTORE_WARNING);
    }

    /** A button that runs on a plain left click. */
    private UiElement action(UiBuilder builder, ButtonRole role, Item icon, String name, String lore, List<String> warnings,
                             Consumer<UiClick> onClick) {
        List<Component> tooltip = new ArrayList<>(List.of(factory().text(lore)));
        warnings.forEach(warning -> tooltip.add(factory().text(warning)));
        return builder.button(role, new ItemStack(icon), factory().text(name), tooltip, click -> {
            if (click.isLeft()) {
                onClick.accept(click);
            }
        });
    }

    private ConfirmMenu confirmDelete() {
        return new ConfirmMenu(ui(), this, factory().text(StaffMessages.Deaths.DELETE_TITLE), PlayerHeads.item(record.player()),
                factory().text(Message.plain(StaffMessages.Deaths.DELETE_HEADLINE)
                        .with("date", Timestamps.dateTime(record.diedAt()))),
                List.of(factory().text(StaffMessages.Deaths.DELETE_WARNING)), staff -> {
                    service.delete(record.player(), record.id());
                    view.deleted(staff);
                    previous().open(staff);
                });
    }

    private void restore(ServerPlayer staff) {
        ServerPlayer target = staff.level().getServer().getPlayerList().getPlayer(record.player());
        if (target == null) {
            view.targetOffline(staff, record.playerName());
            return;
        }
        view.restored(staff, target, service.restore(target, record, clock.instant()));
        record = service.find(record.player(), record.id()).orElse(record);
        refresh();
    }

    private Component chestName(int number, int total) {
        String template = total == 1 ? StaffMessages.Deaths.CHEST_NAME : StaffMessages.Deaths.CHEST_NAME_PART;
        return factory().text(Message.plain(template)
                .with("player", record.playerName())
                .with("number", number)
                .with("total", total));
    }
}
