package com.panita.enriquecraft.staff.gui;

import com.panita.enriquecraft.core.message.Messages;
import com.panita.enriquecraft.core.network.ButtonRole;
import com.panita.enriquecraft.core.network.UiElement;
import com.panita.enriquecraft.core.ui.ChestStyle;
import com.panita.enriquecraft.core.ui.ClickHints;
import com.panita.enriquecraft.core.ui.UiBuilder;
import com.panita.enriquecraft.core.ui.UiMenu;
import com.panita.enriquecraft.core.ui.UiService;
import com.panita.enriquecraft.staff.message.StaffMessages;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 * The way in to every staff tool: one large icon per tool the player may use. Pressing one opens its menu,
 * which can go back here.
 */
final class StaffMenu extends UiMenu {

    private static final int ROW_WIDTH = 9;

    private final StaffMenus menus;
    /** Fixed when the menu is created: a chest cannot change while it is open. */
    private final List<StaffSection> sections;

    StaffMenu(UiService ui, StaffMenus menus, Set<StaffSection> allowed) {
        super(ui, null);
        this.menus = menus;
        this.sections = List.of(StaffSection.values()).stream().filter(allowed::contains).toList();
    }

    @Override
    protected Component title() {
        return factory().text(StaffMessages.Staff.MENU_TITLE);
    }

    @Override
    protected ItemStack icon() {
        return new ItemStack(Items.NETHER_STAR);
    }

    @Override
    protected ChestStyle chestStyle() {
        return ChestStyle.FILLED;
    }

    @Override
    protected UiElement describe(UiBuilder builder) {
        UiElement close = builder.button(ButtonRole.CLOSE, new ItemStack(Items.BARRIER), factory().text(Messages.Gui.CLOSE),
                List.of(), click -> ui().close(click.player()));
        UiElement none = new UiElement.Spacer();
        return new UiElement.Column(List.of(
                tools(builder),
                new UiElement.Row(List.of(none, none, none, none, close, none, none, none, none))));
    }

    /** The tools spread across one row, centered, with the same gap between each. */
    private UiElement tools(UiBuilder builder) {
        if (sections.isEmpty()) {
            return new UiElement.Row(List.of(new UiElement.Label(factory().text(StaffMessages.Staff.NO_ACCESS))));
        }
        List<UiElement> cells = new ArrayList<>(Collections.nCopies(ROW_WIDTH, new UiElement.Spacer()));
        int[] slots = slotsFor(sections.size());
        for (int index = 0; index < sections.size(); index++) {
            cells.set(slots[index], card(builder, sections.get(index)));
        }
        return new UiElement.Row(cells);
    }

    private static int[] slotsFor(int count) {
        return switch (count) {
            case 1 -> new int[]{4};
            case 2 -> new int[]{2, 6};
            default -> new int[]{1, 4, 7};
        };
    }

    private UiElement card(UiBuilder builder, StaffSection section) {
        List<Component> tooltip = List.of(factory().text(section.lore()), Component.empty(),
                ClickHints.left(factory(), StaffMessages.Staff.OPEN));
        return builder.detail(new ItemStack(section.icon()), factory().text(section.title()), List.of(), tooltip, click -> {
            if (click.isLeft()) {
                menus.menuOf(section, this).open(click.player());
            }
        });
    }
}
