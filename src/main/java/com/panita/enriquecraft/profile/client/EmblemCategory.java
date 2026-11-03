package com.panita.enriquecraft.profile.client;

import java.util.Arrays;
import java.util.Optional;

/** The kinds of emblem the Panita API has. */
public enum EmblemCategory {

    /** Given by the system (nationality, veteran, awards); never tied to an edition. */
    SYSTEM("system"),
    /** A global emblem with an icon. */
    EMBLEM("emblem"),
    /** An emblem with an icon tied to an edition. */
    LOCAL_EMBLEM("local_emblem"),
    /** A title: text only, no icon. */
    TITLE("title");

    private final String id;

    EmblemCategory(String id) {
        this.id = id;
    }

    /** The name the API and the config use. */
    public String id() {
        return id;
    }

    public static Optional<EmblemCategory> byId(String id) {
        return Arrays.stream(values()).filter(category -> category.id.equals(id)).findFirst();
    }
}
