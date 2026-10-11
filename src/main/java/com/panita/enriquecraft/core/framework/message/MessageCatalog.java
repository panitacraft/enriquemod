package com.panita.enriquecraft.core.framework.message;

import com.panita.enriquecraft.core.framework.config.ConfigManager;
import com.panita.enriquecraft.core.framework.config.ConfigReport;
import com.panita.enriquecraft.core.framework.config.ConfigValue;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * The texts the mod shows to players, as the administrator wants them: one file, {@code enriquecraft-messages.json5},
 * with a section for each module.
 * <p>
 * A module hands over the class that holds its texts: nested classes of {@code public static final String}
 * constants, which are the default texts. Every constant becomes a setting named after its group and its name
 * ({@code Pin.ENTER_TITLE} is {@code pin.enterTitle}). Code keeps using the constants as it always did: a text is
 * found again by what it says by default, so the one place that turns a text into something to show,
 * {@link #resolve}, hands back what the file says instead. Constants that say exactly the same are one message to
 * the administrator, since they cannot be told apart: they share one setting.
 * <p>
 * The constants of a group called {@code Variables} are special: each one is a word that can be written in any
 * text between double braces, such as {@code {{PREFIX}}}, and is replaced by what the file says for it.
 */
public final class MessageCatalog {

    static final String FILE_NAME = "enriquecraft-messages.json5";

    private static final String VARIABLES = "Variables";
    private static final Pattern ARGUMENT = Pattern.compile("(?<!\\{)\\{([A-Za-z]+)}(?!})");
    private static final String HEADER = """
            // Enriquecraft messages: every text the mod shows to players can be changed here.
            // Change a value and run /enriquecraft reload, or restart the server.
            // Texts accept tags such as <red>, <bold> and <gradient:#FF0000:#0000FF>, and legacy codes such as &c.
            // Words in single braces, such as {name}, are filled in by the mod: keep them where they should show.
            // Words in double braces, such as {{PREFIX}}, stand for the texts of core.variables and work in any text.
            """;

    private final ConfigManager manager;
    private final Map<String, ConfigValue<String>> byDefault = new ConcurrentHashMap<>();
    private final Map<String, ConfigValue<String>> variables = new ConcurrentHashMap<>();

    public MessageCatalog(Path configDirectory) {
        this.manager = new ConfigManager(configDirectory, FILE_NAME, HEADER, "messages:");
    }

    /**
     * Makes the texts of a module configurable. Constants that have exactly the same text are one message to the
     * administrator, as the code could not tell them apart: they share one setting, named after the first of them
     * and said to be used by the others.
     *
     * @param section the module's section in the file
     * @param holder  the class whose nested classes hold the texts
     */
    public void register(String section, Class<?> holder) {
        Map<String, List<Constant>> byText = new LinkedHashMap<>();
        List<Constant> variableConstants = new ArrayList<>();
        for (Class<?> group : groupsOf(holder)) {
            for (Field field : constantsOf(group)) {
                Constant constant = new Constant(lowerFirst(group.getSimpleName()) + "." + lowerCamel(field.getName()),
                        field.getName(), textOf(field));
                byText.computeIfAbsent(constant.text(), text -> new ArrayList<>()).add(constant);
                if (group.getSimpleName().equals(VARIABLES)) {
                    variableConstants.add(constant);
                }
            }
        }
        manager.bindSection(section, builder -> {
            for (Map.Entry<String, List<Constant>> entry : byText.entrySet()) {
                // A text another module already has is the same setting, wherever it is used.
                if (!byDefault.containsKey(entry.getKey())) {
                    List<Constant> constants = entry.getValue();
                    byDefault.put(entry.getKey(), builder.text(constants.get(0).path(), entry.getKey(),
                            commentOf(constants, entry.getKey())));
                }
            }
        });
        for (Constant variable : variableConstants) {
            variables.put("{{" + variable.field() + "}}", byDefault.get(variable.text()));
        }
    }

    /** A text of the code: where it is in the file, the name of its constant and its default. */
    private record Constant(String path, String field, String text) {
    }

    /** What the file says for a text, given the text the code has for it by default; any other text is returned as it is. */
    public String resolve(String template) {
        ConfigValue<String> configured = byDefault.get(template);
        return configured == null ? template : configured.get();
    }

    /** The text with the words in double braces, such as {@code {{PREFIX}}}, replaced by what the file says for them. */
    public String expand(String text) {
        if (!text.contains("{{")) {
            return text;
        }
        String result = text;
        for (Map.Entry<String, ConfigValue<String>> variable : variables.entrySet()) {
            result = result.replace(variable.getKey(), variable.getValue().get());
        }
        return result;
    }

    /** Reads the file again; a file that cannot be read leaves the texts as they were. */
    public ConfigReport reload() {
        return manager.reload();
    }

    private static List<Class<?>> groupsOf(Class<?> holder) {
        List<Class<?>> groups = new ArrayList<>();
        for (Class<?> nested : holder.getDeclaredClasses()) {
            if (Modifier.isStatic(nested.getModifiers())) {
                groups.add(nested);
            }
        }
        // The variables come first, as they are what the rest refers to; the order of the others must not depend on the JVM.
        groups.sort(Comparator.comparing((Class<?> group) -> !group.getSimpleName().equals(VARIABLES))
                .thenComparing(Class::getSimpleName));
        return groups;
    }

    private static List<Field> constantsOf(Class<?> group) {
        return Arrays.stream(group.getDeclaredFields())
                .filter(field -> Modifier.isPublic(field.getModifiers()) && Modifier.isStatic(field.getModifiers())
                        && Modifier.isFinal(field.getModifiers()) && field.getType() == String.class)
                .toList();
    }

    private static String textOf(Field field) {
        try {
            return (String) field.get(null);
        } catch (IllegalAccessException e) {
            throw new IllegalStateException("Cannot read the text " + field, e);
        }
    }

    private static String commentOf(List<Constant> constants, String text) {
        List<String> arguments = new ArrayList<>();
        Matcher matcher = ARGUMENT.matcher(text);
        while (matcher.find()) {
            if (!arguments.contains(matcher.group(1))) {
                arguments.add(matcher.group(1));
            }
        }
        String comment = "The text " + constants.get(0).path() + ".";
        if (constants.size() > 1) {
            comment += "\nAlso used for: " + String.join(", ", constants.stream().skip(1).map(Constant::path).toList()) + ".";
        }
        if (!arguments.isEmpty()) {
            comment += "\nFilled in by the mod: " + String.join(", ", arguments.stream().map(name -> "{" + name + "}").toList()) + ".";
        }
        return comment;
    }

    /** {@code PinFlow} becomes {@code pinFlow}. */
    static String lowerFirst(String name) {
        return Character.toLowerCase(name.charAt(0)) + name.substring(1);
    }

    /** {@code ENTER_TITLE} becomes {@code enterTitle}. */
    static String lowerCamel(String name) {
        StringBuilder result = new StringBuilder();
        boolean upper = false;
        for (char character : name.toCharArray()) {
            if (character == '_') {
                upper = result.length() > 0;
            } else {
                result.append(upper ? Character.toUpperCase(character) : Character.toLowerCase(character));
                upper = false;
            }
        }
        return result.toString();
    }
}
