package com.panita.enriquecraft.client.ui;

import com.panita.enriquecraft.core.network.UiElement;

import java.util.ArrayList;
import java.util.List;

/**
 * A screen description split into the regions the client companion lays out itself. When the last
 * band of the screen is a row, it is the controls: navigation buttons go to the header and the page
 * buttons, search fields and actions to the footer, whatever order the row had in a chest. Anything
 * else is the body.
 *
 * @param body     what fills the middle of the screen
 * @param back     the button that goes back, or null
 * @param page     where a list is, or null when the screen is not a list
 * @param previous the button that shows the previous page, or null when there is none
 * @param next     the button that shows the next page, or null when there is none
 * @param inputs   the text fields of the controls
 * @param actions  the other buttons of the controls
 */
record ScreenParts(UiElement body, UiElement.Button back, UiElement.Page page, UiElement.Button previous,
                   UiElement.Button next, List<UiElement.TextInput> inputs, List<UiElement> actions) {

    static ScreenParts split(UiElement root) {
        if (!(root instanceof UiElement.Column column) || column.children().size() < 2
                || !(column.children().getLast() instanceof UiElement.Row controls)) {
            return new ScreenParts(root, null, null, null, null, List.of(), List.of());
        }

        List<UiElement> bands = column.children().subList(0, column.children().size() - 1);
        UiElement body = bands.size() == 1 ? bands.getFirst() : new UiElement.Column(bands);

        UiElement.Button back = null;
        UiElement.Page page = null;
        UiElement.Button previous = null;
        UiElement.Button next = null;
        List<UiElement.TextInput> inputs = new ArrayList<>();
        List<UiElement> actions = new ArrayList<>();
        for (UiElement element : controls.children()) {
            switch (element) {
                case UiElement.Button button -> {
                    switch (button.role()) {
                        // The client draws its own close button, always.
                        case CLOSE -> { }
                        case BACK -> back = button;
                        case PREVIOUS -> previous = button;
                        case NEXT -> next = button;
                        case NONE -> actions.add(button);
                    }
                }
                case UiElement.Page indicator -> page = indicator;
                case UiElement.TextInput input -> inputs.add(input);
                case UiElement.Spacer ignored -> { }
                default -> actions.add(element);
            }
        }
        return new ScreenParts(body, back, page, previous, next, inputs, actions);
    }
}
