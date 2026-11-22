package com.panita.enriquecraft.moderation.message;

import com.panita.enriquecraft.core.message.Message;
import com.panita.enriquecraft.core.message.Messenger;
import net.minecraft.network.chat.Component;

/**
 * The disconnect screen of a player whose client has a mod that is not allowed. Presentation only.
 */
public final class ClientModView {

    private final Messenger messenger;

    public ClientModView(Messenger messenger) {
        this.messenger = messenger;
    }

    public Component screen(String mod) {
        return messenger.format(Message.plain(ModerationMessages.ClientMods.SCREEN).with("mod", Component.literal(mod)));
    }
}
