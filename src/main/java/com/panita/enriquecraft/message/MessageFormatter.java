package com.panita.enriquecraft.message;

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

    private static final String PREFIX = "<color #F2B134>[Enriquecraft]</color> ";

    private static final ParserContext.Key<Function<String, Component>> ARGUMENTS =
            ParserContext.Key.of("enriquecraft:arguments");

    private static final NodeParser PARSER = ParserBuilder.of()
            .quickText()
            .serverPlaceholders()
            .placeholders(TagLikeParser.Format.of('{', '}'), ARGUMENTS)
            .build();

    /**
     * Formats a message for a specific viewer.
     *
     * @param message the message to format
     * @param context the placeholder context (player, command source or server)
     * @return the finished component
     */
    public Component format(Message message, ServerPlaceholderContext context) {
        ParserContext parserContext = context.asParserContext().with(ARGUMENTS, message.arguments()::get);
        return PARSER.parseComponent(compose(message), parserContext);
    }

    private String compose(Message message) {
        String body = message.level().decorate(message.template());
        return message.hasPrefix() ? PREFIX + body : body;
    }
}
