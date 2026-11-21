package com.panita.enriquecraft.core.message;

/**
 * Visual category of a message. Each level defines the color and icon applied to the whole message.
 * Icons are plain Unicode characters so vanilla clients can render them with the default font.
 */
public enum MessageLevel {
    PLAIN("", ""),
    INFO("#5BC0EB", "ℹ"),
    SUCCESS("#9BC53D", "✔"),
    WARNING("#FDE74C", "⚠"),
    ERROR("#E55934", "✖");

    private final String color;
    private final String icon;

    MessageLevel(String color, String icon) {
        this.color = color;
        this.icon = icon;
    }

    /**
     * Wraps a template with this level's color and icon.
     *
     * @param template the raw template to decorate
     * @return the decorated template, or the template unchanged for {@link #PLAIN}
     */
    String decorate(String template) {
        if (this == PLAIN) {
            return template;
        }
        return "<color " + color + ">" + icon + " " + template + "</color>";
    }
}
