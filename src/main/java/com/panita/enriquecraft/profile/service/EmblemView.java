package com.panita.enriquecraft.profile.service;

import com.panita.enriquecraft.profile.client.EmblemCategory;
import com.panita.enriquecraft.profile.data.EmblemLook;

import java.util.Optional;

/**
 * An emblem ready to be shown: what the API says of it and how it looks to a player who sees only text. Everything that draws an
 * emblem (chat, a chest, the client's screens, a tooltip) reads this and nothing else.
 *
 * @param id          the id the administrator chose
 * @param title       its title
 * @param description what it is for, if the API has one
 * @param iconUrl     the address of its image, if it has one
 * @param category    its kind
 * @param look        the character and the color it has for players without the client mod
 */
public record EmblemView(String id, String title, Optional<String> description, Optional<String> iconUrl, EmblemCategory category,
                         EmblemLook look) {
}
