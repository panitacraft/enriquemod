package com.panita.enriquecraft.core.message;

import com.panita.enriquecraft.core.config.CoreConfig;
import eu.pb4.placeholders.api.ParserContext;
import eu.pb4.placeholders.api.ServerPlaceholderContext;
import eu.pb4.placeholders.api.parsers.NodeParser;
import eu.pb4.placeholders.api.parsers.ParserBuilder;
import eu.pb4.placeholders.api.parsers.TagLikeParser;
import net.minecraft.network.chat.Component;

import java.util.function.Function;

/**
 * Turns a {@link Message} into a vanilla {@link Component}: applies the prefix and level styling,
 * then resolves text tags, server placeholders and named arguments.
 */
public final class MessageFormatter {

    private static final ParserContext.Key<Function<String, Component>> ARGUMENTS =
            ParserContext.Key.of("enriquecraft:arguments");

    private static final NodeParser TEMPLATE_PARSER = ParserBuilder.of()
            .quickText()
            .serverPlaceholders()
            .placeholders(TagLikeParser.Format.of('{', '}'), ARGUMENTS)
            .build();

    /** Parser for trusted markup arguments: text tags plus legacy color and style codes. */
    private static final NodeParser MARKUP_PARSER = ParserBuilder.of()
            .quickText()
            .legacyAll()
            .build();

    private final CoreConfig config;

    public MessageFormatter(CoreConfig config) {
        this.config = config;
    }

    /**
     * Formats a message for a specific viewer.
     *
     * @param message the message to format
     * @param context the placeholder context (player, command source or server)
     * @return the finished component
     */
    public Component format(Message message, ServerPlaceholderContext context) {
        ParserContext parserContext = context.asParserContext().with(ARGUMENTS, name -> resolve(message, name));
        return TEMPLATE_PARSER.parseComponent(compose(message), parserContext);
    }

    private Component resolve(Message message, String name) {
        MessageArgument argument = message.arguments().get(name);
        return switch (argument) {
            case null -> null;
            case MessageArgument.Text text -> text.component();
            case MessageArgument.Markup markup -> MARKUP_PARSER.parseComponent(markup.raw(), ParserContext.of());
        };
    }

    private String compose(Message message) {
        String body = message.level().decorate(message.template());
        return message.hasPrefix() ? config.prefix.get() + " " + body : body;
    }
}
