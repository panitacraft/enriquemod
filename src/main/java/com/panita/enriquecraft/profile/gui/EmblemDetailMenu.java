package com.panita.enriquecraft.profile.gui;

import com.panita.enriquecraft.core.message.Messages;
import com.panita.enriquecraft.core.network.ButtonRole;
import com.panita.enriquecraft.core.network.UiElement;
import com.panita.enriquecraft.core.ui.UiBuilder;
import com.panita.enriquecraft.core.ui.UiMenu;
import com.panita.enriquecraft.core.ui.UiService;
import com.panita.enriquecraft.profile.message.EmblemTooltip;
import com.panita.enriquecraft.profile.service.EmblemView;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.List;

/** One emblem in full: its picture large, its title in the header and its description beside the picture. */
final class EmblemDetailMenu extends UiMenu {

    private final EmblemTooltip tooltip;
    private final EmblemView emblem;

    EmblemDetailMenu(UiService ui, EmblemTooltip tooltip, UiMenu previous, EmblemView emblem) {
        super(ui, previous);
        this.tooltip = tooltip;
        this.emblem = emblem;
    }

    /** Only the words: the header already shows the emblem's picture next to the title. */
    @Override
    protected Component title() {
        return Component.literal(emblem.title()).withColor(emblem.look().textColor().getValue());
    }

    @Override
    protected ItemStack icon() {
        return tooltip.icon(emblem);
    }

    @Override
    protected UiElement describe(UiBuilder builder) {
        tooltip.announce(viewer(), List.of(emblem));
        UiElement detail = new UiElement.Detail(tooltip.icon(emblem), Component.empty(), tooltip.description(emblem));
        UiElement back = builder.button(ButtonRole.BACK, new ItemStack(Items.OAK_DOOR), factory().text(Messages.Gui.BACK), List.of(),
                click -> previous().open(click.player()));
        UiElement close = builder.button(ButtonRole.CLOSE, new ItemStack(Items.BARRIER), factory().text(Messages.Gui.CLOSE), List.of(),
                click -> ui().close(click.player()));
        return new UiElement.Column(List.of(detail, new UiElement.Row(List.of(back, close))));
    }
}
