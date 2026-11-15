package com.panita.enriquecraft.profile.gui;

import com.panita.enriquecraft.core.network.ButtonRole;
import com.panita.enriquecraft.core.network.UiElement;
import com.panita.enriquecraft.core.ui.ClickHints;
import com.panita.enriquecraft.core.ui.UiBuilder;
import com.panita.enriquecraft.core.ui.UiMenu;
import com.panita.enriquecraft.core.ui.UiPagedMenu;
import com.panita.enriquecraft.core.ui.UiService;
import com.panita.enriquecraft.profile.config.ProfileConfig;
import com.panita.enriquecraft.profile.message.EmblemTooltip;
import com.panita.enriquecraft.profile.message.ProfileMessages;
import com.panita.enriquecraft.profile.service.EmblemSelection;
import com.panita.enriquecraft.profile.service.EmblemView;
import com.panita.enriquecraft.profile.service.ProfileService;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.ArrayList;
import java.util.List;

/**
 * Where a player changes one of the places of the emblems they show after their name. It lists every emblem they hold: choosing one
 * puts it in the place, choosing the one the place already has takes it out, and choosing one that another place shows trades the two
 * (see {@link EmblemSelection#place}). One choice is one change, and the menu goes back to the profile. What a player chooses is the
 * mod's own, so it applies at once. The prank level is not here: it has its own menu.
 */
final class EmblemsMenu extends UiPagedMenu<EmblemView> {

    private static final String HERE_BADGE = "✔";
    private static final String ELSEWHERE_BADGE = "↔";

    private final ProfileService profiles;
    private final EmblemTooltip tooltip;
    private final ProfileConfig config;
    private final OpenMenus openMenus;
    private final int slot;

    /** @param slot the place being changed, counted from 0 */
    EmblemsMenu(UiService ui, ProfileService profiles, EmblemTooltip tooltip, ProfileConfig config, OpenMenus openMenus, UiMenu previous,
                int slot) {
        super(ui, previous);
        this.profiles = profiles;
        this.tooltip = tooltip;
        this.config = config;
        this.openMenus = openMenus;
        this.slot = slot;
    }

    @Override
    protected Component title() {
        return factory().text(ProfileMessages.Emblems.TITLE);
    }

    @Override
    protected ItemStack icon() {
        return new ItemStack(Items.NAME_TAG);
    }

    @Override
    protected void onClose() {
        openMenus.closed(viewer().getUUID(), this);
    }

    @Override
    protected List<EmblemView> entries() {
        openMenus.seen(viewer().getUUID(), this);
        List<EmblemView> held = profiles.emblemsOf(viewer()).selectable();
        tooltip.announce(viewer(), held);
        return held;
    }

    @Override
    protected UiElement render(UiBuilder builder, EmblemView emblem) {
        int at = chosenIds().indexOf(emblem.id());
        boolean here = at >= 0 && at == slot;
        boolean elsewhere = at >= 0 && !here;
        List<Component> lines = new ArrayList<>(tooltip.description(emblem));
        if (!lines.isEmpty()) {
            lines.add(Component.empty());
        }
        if (here) {
            lines.add(factory().text(ProfileMessages.Emblems.HERE));
            lines.add(ClickHints.left(factory(), ProfileMessages.Emblems.REMOVE));
        } else if (elsewhere) {
            lines.add(factory().text(ProfileMessages.Emblems.ELSEWHERE));
            lines.add(ClickHints.left(factory(), ProfileMessages.Emblems.SWAP));
        } else {
            lines.add(ClickHints.left(factory(), ProfileMessages.Emblems.PUT));
        }
        String badge = here ? HERE_BADGE : elsewhere ? ELSEWHERE_BADGE : "";
        return builder.button(ButtonRole.NONE, tooltip.icon(emblem), tooltip.title(emblem), lines, badge, click -> {
            if (click.isLeft()) {
                profiles.choose(click.player(), EmblemSelection.place(chosenIds(), slot, emblem.id(), config.loreMax.get()));
                previous().open(click.player());
            }
        });
    }

    private List<String> chosenIds() {
        return profiles.emblemsOf(viewer()).chosen().stream().map(EmblemView::id).toList();
    }
}
