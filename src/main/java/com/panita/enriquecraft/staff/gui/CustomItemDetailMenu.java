package com.panita.enriquecraft.staff.gui;

import com.panita.enriquecraft.core.message.Message;
import com.panita.enriquecraft.core.message.Messages;
import com.panita.enriquecraft.core.message.Timestamps;
import com.panita.enriquecraft.core.network.ButtonRole;
import com.panita.enriquecraft.core.network.UiElement;
import com.panita.enriquecraft.core.ui.ChestStyle;
import com.panita.enriquecraft.core.ui.ClickHints;
import com.panita.enriquecraft.core.ui.ConfirmMenu;
import com.panita.enriquecraft.core.ui.CopyText;
import com.panita.enriquecraft.core.ui.PlayerHeads;
import com.panita.enriquecraft.core.ui.UiBuilder;
import com.panita.enriquecraft.core.ui.UiMenu;
import com.panita.enriquecraft.core.ui.UiService;
import com.panita.enriquecraft.staff.data.SavedItem;
import com.panita.enriquecraft.staff.message.CustomItemView;
import com.panita.enriquecraft.staff.message.ItemMetadata;
import com.panita.enriquecraft.staff.message.StaffMessages;
import com.panita.enriquecraft.staff.service.CustomItemService;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * One saved custom item in full: the item large with what the mod knows about it (name, id, who saved
 * it and when), then, below a separator, everything else the item carries, in an area that scrolls when
 * it is long and that an arrow hides. Getting a copy and deleting the item, after confirming, are the actions. As a chest the
 * item shows all of it as its tooltip. The item is read again on every redraw.
 */
public final class CustomItemDetailMenu extends UiMenu {

    /** How tall the extended data may grow, in GUI pixels, before it scrolls. */
    private static final int SCROLL_HEIGHT = 130;

    private final CustomItemService service;
    private final CustomItemView view;
    private final String name;
    /** Whether the extra data shows; the arrow flips it. */
    private boolean expanded = true;

    public CustomItemDetailMenu(UiService ui, CustomItemService service, CustomItemView view, String name, UiMenu previous) {
        super(ui, previous);
        this.service = service;
        this.view = view;
        this.name = name;
    }

    @Override
    protected Component title() {
        return factory().text(StaffMessages.Items.DETAIL_TITLE);
    }

    @Override
    protected ItemStack icon() {
        return new ItemStack(Items.ENCHANTED_BOOK);
    }

    @Override
    protected ChestStyle chestStyle() {
        return ChestStyle.FILLED;
    }

    @Override
    protected UiElement describe(UiBuilder builder) {
        Optional<SavedItem> found = service.find(name);
        if (found.isEmpty()) {
            return missing(builder);
        }
        SavedItem item = found.get();
        List<UiElement> body = new ArrayList<>(List.of(
                itemDetail(builder, item, ownLines(item)),
                new UiElement.Divider(),
                toggle(builder)));
        if (expanded) {
            body.add(new UiElement.Scroll(metadataBody(item), SCROLL_HEIGHT));
        }
        body.add(controls(builder, item));
        return new UiElement.Column(body);
    }

    /** The arrow that shows or hides the extra data. */
    private UiElement toggle(UiBuilder builder) {
        return builder.button(ButtonRole.NONE, ItemStack.EMPTY,
                factory().text(expanded ? StaffMessages.Items.DETAIL_HIDE : StaffMessages.Items.DETAIL_SHOW), List.of(), click -> {
                    if (click.isLeft()) {
                        expanded = !expanded;
                        refresh();
                    }
                });
    }

    /**
     * The extra data in up to two columns, each group kept whole and under the one before it, with space between
     * groups. A group goes to the shorter column, so the two stay about even.
     */
    private UiElement metadataBody(SavedItem item) {
        List<List<UiElement>> columns = List.of(new ArrayList<>(), new ArrayList<>());
        int[] heights = new int[2];
        for (List<Component> group : ItemMetadata.groups(item.stack(), factory())) {
            int target = heights[0] <= heights[1] ? 0 : 1;
            List<UiElement> column = columns.get(target);
            if (!column.isEmpty()) {
                column.add(new UiElement.Spacer());
                column.add(new UiElement.Spacer());
            }
            group.forEach(line -> column.add(new UiElement.Label(line)));
            heights[target] += group.size() + 1;
        }
        if (columns.get(1).isEmpty()) {
            return new UiElement.Column(columns.get(0), UiElement.Align.START);
        }
        return new UiElement.Row(List.of(
                new UiElement.Column(columns.get(0), UiElement.Align.START),
                new UiElement.Spacer(),
                new UiElement.Column(columns.get(1), UiElement.Align.START)));
    }

    /** A chest has no scrolling or arrow, so the item carries every line as its tooltip. */
    @Override
    protected UiElement describeChest(UiBuilder builder) {
        Optional<SavedItem> found = service.find(name);
        if (found.isEmpty()) {
            return missing(builder);
        }
        SavedItem item = found.get();
        List<Component> lines = new ArrayList<>(ownLines(item));
        lines.add(Component.empty());
        lines.addAll(ItemMetadata.lines(item.stack(), factory()));
        return new UiElement.Column(List.of(
                itemDetail(builder, item, lines),
                controls(builder, item)));
    }

    private UiElement missing(UiBuilder builder) {
        UiElement back = builder.button(ButtonRole.BACK, new ItemStack(Items.OAK_DOOR), factory().text(Messages.Gui.BACK),
                List.of(), click -> previous().open(click.player()));
        UiElement gone = new UiElement.Detail(new ItemStack(Items.BARRIER),
                factory().text(Message.plain(StaffMessages.Items.NOT_FOUND).with("name", name)), List.of());
        return new UiElement.Column(List.of(gone, new UiElement.Row(List.of(back))));
    }

    private UiElement controls(UiBuilder builder, SavedItem item) {
        UiElement back = builder.button(ButtonRole.BACK, new ItemStack(Items.OAK_DOOR), factory().text(Messages.Gui.BACK),
                List.of(), click -> previous().open(click.player()));
        UiElement delete = builder.button(ButtonRole.DANGER, new ItemStack(Items.LAVA_BUCKET),
                factory().text(StaffMessages.Items.DETAIL_DELETE),
                List.of(factory().text(StaffMessages.Items.DETAIL_DELETE_LORE)), click -> {
                    if (click.isLeft()) {
                        confirmDelete(item).open(click.player());
                    }
                });
        UiElement none = new UiElement.Spacer();
        return new UiElement.Row(List.of(back, none, none, none, delete, none, none, none, none));
    }

    /** The item large; pressing it gives a copy (a full stack with shift), as it does in the list. */
    private UiElement itemDetail(UiBuilder builder, SavedItem item, List<Component> lines) {
        return builder.detail(display(item), item.stack().getHoverName(), lines,
                List.of(ClickHints.left(factory(), "obtener copia")), click -> {
                    if (click.isLeft()) {
                        view.give(click.player(), item, click.isShift());
                    }
                });
    }

    private ConfirmMenu confirmDelete(SavedItem item) {
        return new ConfirmMenu(ui(), this, factory().text(StaffMessages.Items.DELETE_TITLE), item.stack().copy(),
                factory().text(Message.plain(StaffMessages.Items.DELETE_HEADLINE).with("name", item.name())),
                List.of(factory().text(StaffMessages.Items.DELETE_WARNING)), player -> {
                    service.remove(item.name());
                    previous().open(player);
                });
    }

    /** A copy without its lore: the tooltip here is ours, and the item's lore is deliberately not part of it. */
    private static ItemStack display(SavedItem item) {
        ItemStack copy = item.stack().copy();
        copy.remove(DataComponents.LORE);
        return copy;
    }

    /** What the mod itself knows: the id, who saved the item and when. */
    private List<Component> ownLines(SavedItem item) {
        return List.of(
                factory().text(Message.plain(StaffMessages.Items.ENTRY_ID).with("id", CopyText.of("enriquecraft:" + item.name()))),
                factory().text(Message.plain(StaffMessages.Items.ENTRY_SAVED_BY)
                        .with("player", PlayerHeads.inline(item.savedBy(), Component.literal(item.savedByName())))),
                factory().text(Message.plain(StaffMessages.Items.ENTRY_DATE)
                        .with("date", Timestamps.dateTime(item.savedAt()))));
    }
}
