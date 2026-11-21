package com.panita.enriquecraft.core.ui;

import com.panita.enriquecraft.core.gui.Menu;
import net.minecraft.network.chat.Component;

/**
 * Shows a {@link UiMenu} as a vanilla chest. Its size is fixed by the first description, so the
 * shape of a screen must not change while it is open.
 */
final class UiChestMenu extends Menu {

    private final UiMenu owner;
    private final int rows;

    UiChestMenu(UiMenu owner) {
        super(owner.factory(), null);
        this.owner = owner;
        this.rows = describe().rows();
    }

    @Override
    protected Component title() {
        return owner.title();
    }

    @Override
    protected int rows() {
        return rows;
    }

    @Override
    protected void draw() {
        ChestLayout.Plan plan = describe();
        if (plan.rows() != rows) {
            throw new IllegalStateException("A screen changed from " + rows + " to " + plan.rows() + " rows while open");
        }
        ChestStyle style = owner.chestStyle();
        if (style == ChestStyle.FRAMED) {
            frame();
        }
        plan.items().forEach(this::set);
        if (style == ChestStyle.FILLED) {
            fillRest();
        } else if (style == ChestStyle.SLOTS) {
            fillRest(plan.gridSlots());
        }
    }

    @Override
    protected void onClose() {
        owner.closed();
    }

    private ChestLayout.Plan describe() {
        return ChestLayout.plan(owner.chestLayout(0), owner.chestStyle(), owner.factory(),
                (player, field, answer) -> owner.ui().prompt(player, owner, field, answer));
    }
}
