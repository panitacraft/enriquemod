package com.panita.enriquecraft.discord.protocol;

import java.util.OptionalInt;

/**
 * An image of a Discord message, as the bot describes it. Only its shape has been checked; the address still has to be on
 * a host the server allows before it is shown.
 *
 * @param url     an {@code https} address of the image
 * @param name    the file name, plain text
 * @param type    the media type, one of {@link Protocol#MEDIA_TYPES}
 * @param size    the size in bytes
 * @param spoiler whether the member marked it as a spoiler
 */
public record MediaEntry(String url, String name, String type, long size, OptionalInt width, OptionalInt height, boolean spoiler) {
}
