package com.panita.enriquecraft.staff.gui;

import com.panita.enriquecraft.core.message.Messages;
import com.panita.enriquecraft.core.network.ButtonRole;
import com.panita.enriquecraft.core.network.UiElement;
import com.panita.enriquecraft.core.ui.ChestStyle;
import com.panita.enriquecraft.core.ui.PlayerHeads;
import com.panita.enriquecraft.core.ui.UiBuilder;
import com.panita.enriquecraft.core.ui.UiMenu;
import com.panita.enriquecraft.core.ui.UiService;
import com.panita.enriquecraft.core.ui.UiSubmit;
import com.panita.enriquecraft.staff.data.PlayerRef;
import com.panita.enriquecraft.staff.data.SavedCoordinate;
import com.panita.enriquecraft.staff.message.StaffMessages;
import com.panita.enriquecraft.staff.service.CoordinateService;
import com.panita.enriquecraft.staff.service.PlayerLookup;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

/**
 * Asks for the name of the player whose head a coordinate should use as its icon. A name the server knows sets
 * the icon and goes to the coordinate; one it cannot find, or that cannot be a name, stays here and says why.
 * <p>
 * The client companion shows a label with the field right below it and a cancel and a confirm button in the
 * footer; a chest shows the head with an explanation and the field, which applies a name as soon as it is typed.
 */
final class CoordinateHeadMenu extends UiMenu {

    /** What stopped a name from being used, if anything. */
    enum Problem { NONE, INVALID, UNKNOWN }

    private static final int CANCEL_SLOT = 0;
    private static final int CONFIRM_SLOT = 5;
    private static final int ROW_WIDTH = 9;

    private final CoordinateService service;
    private final String name;
    private final UiMenu coordinateMenu;
    private final PlayerLookup lookup;
    private Problem problem = Problem.NONE;

    /**
     * @param previous       the icon list this was opened from, which back returns to
     * @param coordinateMenu the coordinate's own screen, where choosing an icon ends
     */
    CoordinateHeadMenu(UiService ui, CoordinateService service, String name, UiMenu previous, UiMenu coordinateMenu) {
        this(ui, service, name, previous, coordinateMenu, PlayerLookup.ofServer());
    }

    CoordinateHeadMenu(UiService ui, CoordinateService service, String name, UiMenu previous, UiMenu coordinateMenu,
                       PlayerLookup lookup) {
        super(ui, previous);
        this.service = service;
        this.name = name;
        this.coordinateMenu = coordinateMenu;
        this.lookup = lookup;
    }

    /** Uses the text as the player whose head the coordinate shows, or says why it cannot. */
    Problem choose(String text, ServerPlayer asker) {
        String trimmed = text.trim();
        if (!SavedCoordinate.isValidPlayerName(trimmed)) {
            return Problem.INVALID;
        }
        Optional<PlayerRef> found = lookup.find(asker, trimmed);
        return found.isPresent() && service.updateIconHead(name, found.get()) ? Problem.NONE : Problem.UNKNOWN;
    }

    @Override
    protected Component title() {
        return factory().text(StaffMessages.Coordinates.HEAD_TITLE);
    }

    @Override
    protected ItemStack icon() {
        return new ItemStack(Items.PLAYER_HEAD);
    }

    @Override
    protected ChestStyle chestStyle() {
        return ChestStyle.FILLED;
    }

    private Optional<PlayerRef> owner() {
        return service.find(name).flatMap(SavedCoordinate::iconPlayer);
    }

    /** What went wrong with the last name, as a line of text. */
    private Optional<Component> problemLine() {
        return switch (problem) {
            case INVALID -> Optional.of(factory().text(StaffMessages.Coordinates.HEAD_INVALID));
            case UNKNOWN -> Optional.of(factory().text(StaffMessages.Coordinates.HEAD_UNKNOWN));
            case NONE -> Optional.empty();
        };
    }

    /** Confirming, by Enter or by the confirm button, uses the name or shows the problem. */
    private Consumer<UiSubmit> onSubmit() {
        return submit -> {
            if (submit.text().isBlank()) {
                return;
            }
            problem = choose(submit.text(), submit.player());
            if (problem == Problem.NONE) {
                coordinateMenu.open(submit.player());
            } else {
                refresh();
            }
        };
    }

    /** The label, the field right below it, and the buttons that cancel and confirm. */
    @Override
    protected UiElement describe(UiBuilder builder) {
        UiElement.TextInput input = builder.input(factory().text(StaffMessages.Coordinates.HEAD_PLACEHOLDER),
                owner().map(PlayerRef::name).orElse(""), SavedCoordinate.MAX_PLAYER_NAME, onSubmit());
        List<UiElement> form = new ArrayList<>(List.of(new UiElement.Label(factory().text(StaffMessages.Coordinates.HEAD_LABEL)), input));
        problemLine().ifPresent(line -> form.add(new UiElement.Label(line)));

        UiElement cancel = builder.button(ButtonRole.CANCEL, new ItemStack(Items.OAK_DOOR), factory().text(Messages.Gui.CANCEL),
                List.of(), click -> previous().open(click.player()));
        UiElement confirm = builder.submitButton(factory().text(Messages.Gui.CONFIRM), input);
        return new UiElement.Column(List.of(new UiElement.Column(form), controls(cancel, confirm)));
    }

    private static UiElement controls(UiElement cancel, UiElement confirm) {
        List<UiElement> slots = new ArrayList<>(Collections.nCopies(ROW_WIDTH, new UiElement.Spacer()));
        slots.set(CANCEL_SLOT, cancel);
        slots.set(CONFIRM_SLOT, confirm);
        return new UiElement.Row(slots);
    }

    /** A chest shows the head and what to do, with the field in the bottom row beside the back door. */
    @Override
    protected UiElement describeChest(UiBuilder builder) {
        List<Component> lines = new ArrayList<>(List.of(factory().text(StaffMessages.Coordinates.HEAD_HELP)));
        problemLine().ifPresent(lines::add);
        // No title of its own: the screen's title says what this is.
        UiElement detail = new UiElement.Detail(
                owner().map(player -> PlayerHeads.item(player.id())).orElseGet(() -> new ItemStack(Items.PLAYER_HEAD)),
                Component.empty(), lines);
        UiElement back = builder.button(ButtonRole.BACK, new ItemStack(Items.OAK_DOOR), factory().text(Messages.Gui.BACK),
                List.of(), click -> previous().open(click.player()));
        UiElement input = builder.input(factory().text(StaffMessages.Coordinates.HEAD_HINT),
                owner().map(PlayerRef::name).orElse(""), SavedCoordinate.MAX_PLAYER_NAME, onSubmit());
        UiElement none = new UiElement.Spacer();
        return new UiElement.Column(List.of(detail, new UiElement.Row(List.of(back, input, none, none, none, none, none, none, none))));
    }
}
