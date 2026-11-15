package com.panita.enriquecraft.profile.gui;

import com.panita.enriquecraft.core.message.Message;
import com.panita.enriquecraft.core.message.Messenger;
import com.panita.enriquecraft.core.network.ButtonRole;
import com.panita.enriquecraft.core.network.UiElement;
import com.panita.enriquecraft.core.ui.ClickHints;
import com.panita.enriquecraft.core.ui.ConfirmMenu;
import com.panita.enriquecraft.core.ui.UiBuilder;
import com.panita.enriquecraft.core.ui.UiMenu;
import com.panita.enriquecraft.core.ui.UiPagedMenu;
import com.panita.enriquecraft.core.ui.UiService;
import com.panita.enriquecraft.profile.config.ProfileConfig;
import com.panita.enriquecraft.profile.message.EmblemTooltip;
import com.panita.enriquecraft.profile.message.ProfileMessages;
import com.panita.enriquecraft.profile.service.EmblemSelection;
import com.panita.enriquecraft.profile.service.EmblemView;
import com.panita.enriquecraft.profile.service.PlayerEmblems;
import com.panita.enriquecraft.profile.service.ProfileService;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.ArrayList;
import java.util.List;

/**
 * Where a player chooses which of their emblems show after their name. The ones they show come first, in the order they chose, and
 * can be dragged into another order; any other can be shown with a click, up to the most the config allows. What a player chooses is
 * the mod's own, so it applies at once. The prank level is not here: it is changed in the profile's edit view.
 */
final class EmblemsMenu extends UiPagedMenu<EmblemsMenu.Slot> {

    /** What kind of place an emblem has on the screen. */
    enum Kind {
        /** One the player chose to show after the name. */
        CHOSEN,
        /** One the player holds and does not show. */
        OTHER
    }

    /** One emblem on the screen and what the player can do with it. */
    record Slot(EmblemView emblem, Kind kind) {
    }

    private static final String CHOSEN_BADGE = "✔";

    private final ProfileService profiles;
    private final EmblemTooltip tooltip;
    private final ProfileConfig config;
    private final Messenger messenger;
    private final OpenMenus openMenus;

    EmblemsMenu(UiService ui, ProfileService profiles, EmblemTooltip tooltip, ProfileConfig config, Messenger messenger,
                OpenMenus openMenus, UiMenu previous) {
        super(ui, previous);
        this.profiles = profiles;
        this.tooltip = tooltip;
        this.config = config;
        this.messenger = messenger;
        this.openMenus = openMenus;
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
    protected List<Slot> entries() {
        openMenus.seen(viewer().getUUID(), this);
        PlayerEmblems emblems = profiles.emblemsOf(viewer());
        tooltip.announce(viewer(), emblems.selectable());
        List<Slot> slots = new ArrayList<>();
        emblems.chosen().forEach(emblem -> slots.add(new Slot(emblem, Kind.CHOSEN)));
        emblems.selectable().stream().filter(emblem -> emblems.chosen().stream().noneMatch(chosen -> chosen.id().equals(emblem.id())))
                .forEach(emblem -> slots.add(new Slot(emblem, Kind.OTHER)));
        return slots;
    }

    @Override
    protected UiElement render(UiBuilder builder, Slot slot) {
        EmblemView emblem = slot.emblem();
        List<Component> lines = new ArrayList<>(tooltip.description(emblem));
        lines.add(Component.empty());
        if (slot.kind() == Kind.CHOSEN) {
            lines.add(factory().text(ProfileMessages.Emblems.SHOWN));
            lines.add(ClickHints.left(factory(), ProfileMessages.Emblems.HIDE));
        } else {
            lines.add(factory().text(ProfileMessages.Emblems.HIDDEN));
            lines.add(ClickHints.left(factory(), ProfileMessages.Emblems.SHOW));
        }
        String badge = slot.kind() == Kind.CHOSEN ? CHOSEN_BADGE : "";
        ItemStack tag = tooltip.icon(emblem);
        ItemStack icon = isLifted(slot) ? factory().item(tag).glint().build() : tag;
        return builder.button(ButtonRole.NONE, icon, tooltip.title(emblem), lines, badge, click -> {
            if (!click.isLeft()) {
                return;
            }
            if (arranging()) {
                arrange(slot);
            } else {
                toggle(click.player(), slot);
            }
        });
    }

    @Override
    protected boolean reorderable() {
        return true;
    }

    /** Only the emblems the player shows have an order, so only they trade places. */
    @Override
    protected void move(Slot dragged, Slot onto) {
        if (dragged.kind() != Kind.CHOSEN || onto.kind() != Kind.CHOSEN) {
            return;
        }
        ServerPlayer player = viewer();
        profiles.choose(player, EmblemSelection.move(chosenIds(player), dragged.emblem().id(), onto.emblem().id()));
    }

    @Override
    protected UiElement extraControl(UiBuilder builder) {
        return builder.button(ButtonRole.DANGER, new ItemStack(Items.BARRIER), factory().text(ProfileMessages.Emblems.RESET),
                List.of(factory().text(ProfileMessages.Emblems.RESET_LORE)), click -> {
                    if (click.isLeft()) {
                        confirmReset().open(click.player());
                    }
                });
    }

    private void toggle(ServerPlayer player, Slot slot) {
        int max = config.loreMax.get();
        EmblemSelection.Toggled toggled = EmblemSelection.toggle(chosenIds(player), slot.emblem().id(), max);
        if (toggled.change() == EmblemSelection.Change.FULL) {
            messenger.send(player, Message.warning(ProfileMessages.Emblems.FULL).prefixed().with("max", max));
            return;
        }
        profiles.choose(player, toggled.emblems());
        refresh();
    }

    private List<String> chosenIds(ServerPlayer player) {
        return profiles.emblemsOf(player).chosen().stream().map(EmblemView::id).toList();
    }

    private ConfirmMenu confirmReset() {
        return new ConfirmMenu(ui(), this, factory().text(ProfileMessages.Emblems.RESET_TITLE), new ItemStack(Items.NAME_TAG),
                factory().text(ProfileMessages.Emblems.RESET_HEADLINE), List.of(factory().text(ProfileMessages.Emblems.RESET_WARNING)),
                player -> {
                    profiles.resetChoice(player);
                    previous().open(player);
                });
    }
}
