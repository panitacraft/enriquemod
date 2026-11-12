package com.panita.enriquecraft.profile.gui;

import com.panita.enriquecraft.core.message.Message;
import com.panita.enriquecraft.core.message.Messenger;
import com.panita.enriquecraft.core.network.UiElement;
import com.panita.enriquecraft.core.network.ButtonRole;
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
import java.util.Optional;
import java.util.stream.Stream;

/**
 * Where a player chooses what shows around their name. The prank levels come first: the one shown before the name is the default level
 * until the player presses another they hold, and pressing it again goes back. Every other emblem, the nationality and the staff
 * emblem included, can be shown or put away with a click after the name, and the shown ones are dragged into the order they want.
 * What a player chooses is the mod's own, so it applies at once.
 */
final class EmblemsMenu extends UiPagedMenu<EmblemsMenu.Slot> {

    /** What kind of place an emblem has on the screen. */
    enum Kind {
        /** The default prank level, shown before the name because the player chose no other; it is not a choice. */
        PRANK_DEFAULT,
        /** The prank level the player chose, shown before the name; pressing it goes back to the default. */
        PRANK_SHOWN,
        /** Another prank level the player holds, which they can show before the name instead. */
        PRANK_OTHER,
        /** One the player chose to show after the name. */
        CHOSEN,
        /** One the player holds and does not show. */
        OTHER;

        boolean isPrank() {
            return this == PRANK_DEFAULT || this == PRANK_SHOWN || this == PRANK_OTHER;
        }
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
        PlayerEmblems emblems = profiles.emblemsOf(viewer().getUUID());
        tooltip.announce(viewer(), Stream.of(Stream.of(emblems.prank()), emblems.pranks().stream(), emblems.selectable().stream())
                .flatMap(emblemStream -> emblemStream).toList());
        List<Slot> slots = new ArrayList<>();
        boolean prankChosen = emblems.pranks().stream().anyMatch(prank -> prank.id().equals(emblems.prank().id()));
        slots.add(new Slot(emblems.prank(), prankChosen ? Kind.PRANK_SHOWN : Kind.PRANK_DEFAULT));
        emblems.pranks().stream().filter(prank -> !prank.id().equals(emblems.prank().id()))
                .forEach(prank -> slots.add(new Slot(prank, Kind.PRANK_OTHER)));
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
        switch (slot.kind()) {
            case PRANK_DEFAULT -> lines.addAll(tooltip.note(ProfileMessages.Emblems.PRANK_DEFAULT));
            case PRANK_SHOWN -> {
                lines.add(factory().text(ProfileMessages.Emblems.PRANK_SHOWN));
                lines.add(ClickHints.left(factory(), ProfileMessages.Emblems.PRANK_RESET));
            }
            case PRANK_OTHER -> {
                lines.add(factory().text(ProfileMessages.Emblems.PRANK_HIDDEN));
                lines.add(ClickHints.left(factory(), ProfileMessages.Emblems.SHOW));
            }
            case CHOSEN -> {
                lines.add(factory().text(ProfileMessages.Emblems.SHOWN));
                lines.add(ClickHints.left(factory(), ProfileMessages.Emblems.HIDE));
            }
            case OTHER -> {
                lines.add(factory().text(ProfileMessages.Emblems.HIDDEN));
                lines.add(ClickHints.left(factory(), ProfileMessages.Emblems.SHOW));
            }
        }
        String badge = slot.kind() == Kind.CHOSEN || slot.kind() == Kind.PRANK_SHOWN ? CHOSEN_BADGE : "";
        ItemStack tag = tooltip.icon(emblem);
        ItemStack icon = isLifted(slot) ? factory().item(tag).glint().build() : tag;
        return builder.button(ButtonRole.NONE, icon, tooltip.title(emblem), lines, badge, click -> {
            if (!click.isLeft() || slot.kind() == Kind.PRANK_DEFAULT) {
                return;
            }
            if (slot.kind().isPrank()) {
                // Pressing the one that is shown goes back to the default level
                profiles.choosePrank(click.player(), slot.kind() == Kind.PRANK_SHOWN ? Optional.empty() : Optional.of(emblem.id()));
                refresh();
            } else if (arranging()) {
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
        List<String> ids = chosenIds(player);
        profiles.choose(player, EmblemSelection.move(ids, dragged.emblem().id(), onto.emblem().id()));
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
        return profiles.emblemsOf(player.getUUID()).chosen().stream().map(EmblemView::id).toList();
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
