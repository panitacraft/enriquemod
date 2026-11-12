package com.panita.enriquecraft.profile.client;

import java.util.Optional;

/**
 * An emblem as the Panita API describes it.
 *
 * @param id          the id the administrator chose, such as {@code staff} or {@code nationality_co}
 * @param name        its title
 * @param description what it is for, if the API has one
 * @param iconUrl     the address of its image, if it has one (a title has none)
 * @param position    where it stands among all emblems; the API orders by it
 * @param editionId   the edition it is tied to, if any
 */
public record ApiEmblem(String id, String name, Optional<String> description, Optional<String> iconUrl, EmblemCategory category,
                        int position, Optional<String> editionId) {
}
