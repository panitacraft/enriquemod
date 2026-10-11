package com.panita.enriquecraft.chat;

import com.panita.enriquecraft.auth.service.HeldPlayers;
import com.panita.enriquecraft.chat.channel.ChannelRegistry;
import com.panita.enriquecraft.chat.channel.ChannelSelection;
import com.panita.enriquecraft.chat.config.ChatConfig;
import com.panita.enriquecraft.chat.message.ChatMessages;
import com.panita.enriquecraft.chat.message.ChatView;
import com.panita.enriquecraft.chat.message.NicknameView;
import com.panita.enriquecraft.chat.message.PrivateMessageView;
import com.panita.enriquecraft.chat.message.SenderNameView;
import com.panita.enriquecraft.chat.message.SystemMessageView;
import com.panita.enriquecraft.chat.message.WelcomeView;
import com.panita.enriquecraft.chat.service.ChatPlaceholders;
import com.panita.enriquecraft.chat.service.ChatService;
import com.panita.enriquecraft.chat.service.NicknameService;
import com.panita.enriquecraft.chat.service.PrivateMessages;
import com.panita.enriquecraft.chat.service.ReplyTracker;
import com.panita.enriquecraft.chat.service.SystemMessageClassifier;
import com.panita.enriquecraft.chat.service.SystemMessages;
import com.panita.enriquecraft.chat.service.TabListNames;
import com.panita.enriquecraft.chat.text.ChatTextParser;
import com.panita.enriquecraft.chat.text.EmojiCatalog;
import com.panita.enriquecraft.chat.text.LinkDetector;
import com.panita.enriquecraft.core.framework.data.WorldData;
import com.panita.enriquecraft.core.framework.inject.ServiceRegistry;
import com.panita.enriquecraft.core.framework.module.EnriquecraftModule;
import com.panita.enriquecraft.core.message.Message;
import com.panita.enriquecraft.core.message.MessageFormatter;
import com.panita.enriquecraft.core.message.Messenger;

import java.time.Instant;
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

        ChannelSelection selection = new ChannelSelection(channels);
        NicknameService nicknames = new NicknameService(services.get(WorldData.class), parser, config, Instant::now);
        Messenger messenger = services.get(Messenger.class);
        SenderNameView names = new SenderNameView(messenger, nicknames);
        ChatView view = new ChatView(messenger, names);
        HeldPlayers held = services.get(HeldPlayers.class);
        ChatService chat = new ChatService(parser, view, held, selection);
        ReplyTracker replies = new ReplyTracker();
        PrivateMessages privateMessages = new PrivateMessages(parser, new PrivateMessageView(messenger, names), replies, held);

        services.register(ChatTextParser.class, parser);
        services.register(ChannelRegistry.class, channels);
        services.register(ChannelSelection.class, selection);
        services.register(ChatView.class, view);
        services.register(ChatService.class, chat);
        services.register(ReplyTracker.class, replies);
        services.register(PrivateMessages.class, privateMessages);
        services.register(NicknameService.class, nicknames);
        services.register(NicknameView.class, new NicknameView(messenger, config));
        services.register(TabListNames.class, new TabListNames());
        new ChatPlaceholders(nicknames, selection).register();
        SystemMessages systemMessages = new SystemMessages(new SystemMessageClassifier(), new SystemMessageView(messenger), config, held);
        services.register(WelcomeView.class, new WelcomeView(messenger));
        ChatHooks.install(nicknames, systemMessages, privateMessages, chat);
    }
}
