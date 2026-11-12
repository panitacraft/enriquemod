package com.panita.enriquecraft.profile.gui;

import com.panita.enriquecraft.core.message.Durations;
import com.panita.enriquecraft.core.message.Message;
import com.panita.enriquecraft.profile.ProfilePermissions;
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
import com.panita.enriquecraft.profile.message.ProfileView;
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
import java.util.stream.Stream;

/**
 * Where a player chooses what shows around their name. The prank levels come first, in the config's order: the one shown before the
 * name is marked, any other can be pressed to take its place once the wait between changes is over, and there is always one shown.
 * The first level is only where players start: once they have left it, it is listed but can no longer be pressed.
 * Every other emblem, the nationality and the staff emblem included, can be shown or put away with a click after the name, and the
 * shown ones are dragged into the order they want. What a player chooses is the mod's own, so it applies at once.
 */
final class EmblemsMenu extends UiPagedMenu<EmblemsMenu.Slot> {

    /** What kind of place an emblem has on the screen. */
    enum Kind {
        /** The prank level shown before the name. */
        PRANK_SHOWN,
        /** Another prank level, which the player can show before the name instead. */
        PRANK_OTHER,
        /** The starting prank level once the player has left it: it can no longer be chosen. */
        PRANK_LOCKED,
        /** One the player chose to show after the name. */
        CHOSEN,
        /** One the player holds and does not show. */
        OTHER;

        boolean isPrank() {
            return this == PRANK_SHOWN || this == PRANK_OTHER || this == PRANK_LOCKED;
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
    private final ProfileView view;
    private final OpenMenus openMenus;

    EmblemsMenu(UiService ui, ProfileService profiles, EmblemTooltip tooltip, ProfileConfig config, Messenger messenger,
                ProfileView view, OpenMenus openMenus, UiMenu previous) {
        super(ui, previous);
        this.profiles = profiles;
        this.tooltip = tooltip;
        this.config = config;
        this.messenger = messenger;
        this.view = view;
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
        tooltip.announce(viewer(), Stream.concat(emblems.pranks().stream(), emblems.selectable().stream()).toList());
        List<Slot> slots = new ArrayList<>();
        emblems.pranks().forEach(prank -> slots.add(new Slot(prank, prankKind(emblems, prank))));
        emblems.chosen().forEach(emblem -> slots.add(new Slot(emblem, Kind.CHOSEN)));
        emblems.selectable().stream().filter(emblem -> emblems.chosen().stream().noneMatch(chosen -> chosen.id().equals(emblem.id())))
                .forEach(emblem -> slots.add(new Slot(emblem, Kind.OTHER)));
        return slots;
    }

    /** The levels are listed lowest first, and the first is where players start, which nobody can choose. */
    private static Kind prankKind(PlayerEmblems emblems, EmblemView prank) {
        if (prank.id().equals(emblems.prank().id())) {
            return Kind.PRANK_SHOWN;
        }
        return prank.id().equals(emblems.pranks().getFirst().id()) ? Kind.PRANK_LOCKED : Kind.PRANK_OTHER;
    }

    @Override
    protected UiElement render(UiBuilder builder, Slot slot) {
        EmblemView emblem = slot.emblem();
        List<Component> lines = new ArrayList<>(tooltip.description(emblem));
        lines.add(Component.empty());
        switch (slot.kind()) {
            case PRANK_SHOWN -> lines.add(factory().text(ProfileMessages.Emblems.PRANK_SHOWN));
            case PRANK_LOCKED -> lines.add(factory().text(ProfileMessages.Emblems.PRANK_LOCKED));
            case PRANK_OTHER -> {
                lines.add(factory().text(ProfileMessages.Emblems.PRANK_OTHER));
                lines.add(ClickHints.left(factory(), ProfileMessages.Emblems.PRANK_CHOOSE));
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
            if (!click.isLeft() || slot.kind() == Kind.PRANK_SHOWN || slot.kind() == Kind.PRANK_LOCKED) {
                return;
            }
            if (slot.kind().isPrank()) {
                choosePrank(click.player(), emblem);
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

    private void choosePrank(ServerPlayer player, EmblemView level) {
        ProfileService.PrankChange change = profiles.choosePrank(player, level.id(), ProfilePermissions.canBypassPrankCooldown(player));
        switch (change.status()) {
            case CHANGED -> {
                // Everyone is told, the player included
                view.prankChanged(player, level.title());
                refresh();
            }
            case COOLDOWN -> messenger.send(player, Message.warning(ProfileMessages.Prank.COOLDOWN).prefixed()
                    .with("time", Durations.format(change.remaining())));
            case NOT_LINKED -> messenger.send(player, Message.warning(ProfileMessages.Prank.NOT_LINKED).prefixed());
            case UNCHANGED -> { }
        }
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
