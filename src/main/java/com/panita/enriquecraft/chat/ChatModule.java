package com.panita.enriquecraft.chat;

import com.panita.enriquecraft.chat.config.ChatConfig;
import com.panita.enriquecraft.chat.message.ChatMessages;
import com.panita.enriquecraft.chat.text.ChatTextParser;
import com.panita.enriquecraft.chat.text.EmojiCatalog;
import com.panita.enriquecraft.chat.text.LinkDetector;
import com.panita.enriquecraft.core.framework.inject.ServiceRegistry;
import com.panita.enriquecraft.core.framework.module.EnriquecraftModule;
import com.panita.enriquecraft.core.message.Message;
import com.panita.enriquecraft.core.message.MessageFormatter;

import java.util.Optional;

/**
 * Everything the server says in chat: how lines look, channels, nicknames, private messages and the messages about
 * players coming and going. It decides how things are shown and never what players may do in the world.
 */
public final class ChatModule implements EnriquecraftModule {

    @Override
    public Optional<Class<?>> messages() {
        return Optional.of(ChatMessages.class);
    }

    @Override
    public void registerServices(ServiceRegistry services) {
        ChatConfig config = services.get(ChatConfig.class);
        MessageFormatter formatter = services.get(MessageFormatter.class);

        EmojiCatalog emojis = new EmojiCatalog(config.emojis::get, text -> formatter.format(Message.plain(text)));
        LinkDetector links = new LinkDetector(() -> formatter.format(Message.plain(ChatMessages.Format.LINK_TOOLTIP)));
        services.register(ChatTextParser.class, new ChatTextParser(emojis, links));
    }
}
