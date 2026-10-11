package com.panita.enriquecraft.chat;

import com.panita.enriquecraft.auth.service.HeldPlayers;
import com.panita.enriquecraft.chat.channel.ChannelRegistry;
import com.panita.enriquecraft.chat.channel.ChannelSelection;
import com.panita.enriquecraft.chat.config.ChatConfig;
import com.panita.enriquecraft.chat.message.ChatMessages;
import com.panita.enriquecraft.chat.message.ChatView;
import com.panita.enriquecraft.chat.service.ChatService;
import com.panita.enriquecraft.chat.text.ChatTextParser;
import com.panita.enriquecraft.chat.text.EmojiCatalog;
import com.panita.enriquecraft.chat.text.LinkDetector;
import com.panita.enriquecraft.core.framework.inject.ServiceRegistry;
import com.panita.enriquecraft.core.framework.module.EnriquecraftModule;
import com.panita.enriquecraft.core.message.Message;
import com.panita.enriquecraft.core.message.MessageFormatter;
import com.panita.enriquecraft.core.message.Messenger;

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
        ChatTextParser parser = new ChatTextParser(emojis, links);
        ChannelRegistry channels = new ChannelRegistry(config.channels::get);
        ChatView view = new ChatView(services.get(Messenger.class));

        services.register(ChatTextParser.class, parser);
        services.register(ChannelRegistry.class, channels);
        services.register(ChannelSelection.class, new ChannelSelection(channels));
        services.register(ChatView.class, view);
        services.register(ChatService.class, new ChatService(parser, view, services.get(HeldPlayers.class)));
    }
}
