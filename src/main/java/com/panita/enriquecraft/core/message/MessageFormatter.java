package com.panita.enriquecraft.core.message;

import com.panita.enriquecraft.core.framework.message.MessageCatalog;
import eu.pb4.placeholders.api.ParserContext;
import eu.pb4.placeholders.api.ServerPlaceholderContext;
import eu.pb4.placeholders.api.parsers.NodeParser;
import eu.pb4.placeholders.api.parsers.ParserBuilder;
import eu.pb4.placeholders.api.parsers.TagLikeParser;
import net.minecraft.network.chat.Component;

import java.util.function.Function;

/**
 * Turns a {@link Message} into a vanilla {@link Component}: takes the text the administrator configured for it,
 * applies the prefix and level styling, replaces the words in double braces ({@code {{PREFIX}}}), then resolves
 * text tags (including gradients and shadows), legacy color codes, named arguments and, when a context is given,
 * server placeholders.
 */
public final class MessageFormatter {

    private static final ParserContext.Key<Function<String, Component>> ARGUMENTS =
            ParserContext.Key.of("enriquecraft:arguments");

    private static final TagLikeParser.Format ARGUMENT_FORMAT = TagLikeParser.Format.of('{', '}');

    /** What goes before a message that wants the prefix: the prefix, the separator and a space. */
    private static final String PREFIX_LEAD = "{{PREFIX}} {{SEPARATOR}} ";

    private static final NodeParser TEXT_PARSER = ParserBuilder.of()
            .quickText()
            .simplifiedTextFormat()
            .legacyAll()
            .placeholders(ARGUMENT_FORMAT, ARGUMENTS)
            .build();

    private final MessageCatalog catalog;

    public MessageFormatter(MessageCatalog catalog) {
        this.catalog = catalog;
    }

    /**
     * Formats a message without resolving server placeholders.
     */
    public Component format(Message message) {
        return TEXT_PARSER.parseComponent(compose(message), withArguments(ParserContext.of(), message));
    }

    /**
     * Formats a message and resolves server placeholders, such as {@code %player:name%}, for the
     * given context.
     */
    public Component format(Message message, ServerPlaceholderContext context) {
        return PlaceholderParser.INSTANCE.parseComponent(compose(message), withArguments(context.asParserContext(), message));
    }

    private static ParserContext withArguments(ParserContext base, Message message) {
        return base.with(ARGUMENTS, name -> message.arguments().get(name));
    }

    private String compose(Message message) {
        String body = message.level().decorate(catalog.resolve(message.template()));
        return catalog.expand(message.hasPrefix() ? PREFIX_LEAD + body : body);
    }

    /**
     * Built on first use: Placeholder API registers its placeholders when this parser is created,
     * which needs a running Fabric Loader. Formatting without placeholders never pays for it.
     */
    private static final class PlaceholderParser {
        private static final NodeParser INSTANCE = ParserBuilder.of()
                .quickText()
                .simplifiedTextFormat()
                .legacyAll()
                .serverPlaceholders()
                .placeholders(ARGUMENT_FORMAT, ARGUMENTS)
                .build();
    }
}
