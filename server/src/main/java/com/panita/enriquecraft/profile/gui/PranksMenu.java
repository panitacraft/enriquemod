package com.panita.enriquecraft.profile.gui;

import com.panita.enriquecraft.core.network.ButtonRole;
import com.panita.enriquecraft.core.network.UiElement;
import com.panita.enriquecraft.core.ui.ClickHints;
import com.panita.enriquecraft.core.ui.UiBuilder;
import com.panita.enriquecraft.core.ui.UiMenu;
import com.panita.enriquecraft.core.ui.UiPagedMenu;
import com.panita.enriquecraft.core.ui.UiService;
import com.panita.enriquecraft.profile.message.EmblemTooltip;
import com.panita.enriquecraft.profile.message.ProfileMessages;
import com.panita.enriquecraft.profile.service.EmblemView;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.ArrayList;
import java.util.List;

/**
 * Where a player picks their prank level, from the levels they may choose: all but the first, which is only where everyone starts.
 * The level they are at has a check. A change is subject to the wait the config sets, which the player is told about when it applies.
 */
final class PranksMenu extends UiPagedMenu<EmblemView> {

    private static final String CURRENT_BADGE = "✔";

    private final ProfileEdits edits;
    private final EmblemTooltip tooltip;

    PranksMenu(UiService ui, ProfileEdits edits, EmblemTooltip tooltip, UiMenu previous) {
        super(ui, previous);
        this.edits = edits;
        this.tooltip = tooltip;
    }

    @Override
    protected Component title() {
        return factory().text(ProfileMessages.Edit.PRANKS_TITLE);
    }

    @Override
    protected ItemStack icon() {
        return new ItemStack(Items.SKELETON_SKULL);
    }

    @Override
    protected List<EmblemView> entries() {
        List<EmblemView> levels = edits.choosableLevels(viewer());
        tooltip.announce(viewer(), levels);
        return levels;
    }

    @Override
    protected UiElement render(UiBuilder builder, EmblemView level) {
        boolean current = level.id().equals(edits.prankOf(viewer()));
        List<Component> lines = new ArrayList<>(tooltip.description(level));
        if (!lines.isEmpty()) {
            lines.add(Component.empty());
        }
        lines.add(current ? factory().text(ProfileMessages.Edit.PRANK_CURRENT) : ClickHints.left(factory(), ProfileMessages.Edit.PRANK_CHOOSE));
        return builder.button(ButtonRole.NONE, tooltip.icon(level), tooltip.title(level), lines, current ? CURRENT_BADGE : "", click -> {
            if (click.isLeft()) {
                edits.prank(click.player(), level.id());
                refresh();
            }
        });
    }
}
