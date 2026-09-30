package com.panita.enriquecraft.core.framework.config;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Owns the single config file. Each module binds one section; values are validated on load and
 * on reload, and the file is rewritten only to add missing keys.
 * <p>
 * Safety rules: a file that cannot be parsed is never overwritten, startup then uses defaults and
 * a reload keeps the previous values; values written by the administrator are kept as written even
 * when invalid; keys nobody declares are kept; writes are atomic.
 */
public final class ConfigManager {

    static final String FILE_NAME = "enriquecraft.json5";

    private static final Logger LOGGER = LoggerFactory.getLogger(ConfigManager.class);

    private final Path file;
    private final ConfigRenderer renderer = new ConfigRenderer();
    private final Map<String, List<ConfigValue<?>>> sections = new LinkedHashMap<>();
    private JsonObject root = new JsonObject();
    private boolean fileReadable = true;

    public ConfigManager(Path configDirectory) {
        this.file = configDirectory.resolve(FILE_NAME);
        ReadResult result = read();
        if (result.error() == null) {
            root = result.root();
        } else {
            fileReadable = false;
            log(List.of(new ConfigIssue(ConfigIssue.Kind.SYNTAX_ERROR, "", result.error())));
        }
    }

    /**
     * Creates the config object of a module and loads its section from the file.
     *
     * @param section the top-level key of the module in the file
     * @param type    the config class; it must have one public constructor taking a {@link ConfigSectionBuilder}
     */
    public <T extends ModConfig> T bind(String section, Class<T> type) {
        if (sections.containsKey(section)) {
            throw new IllegalStateException("The config section '" + section + "' is already bound");
        }
        ConfigSectionBuilder builder = new ConfigSectionBuilder();
        T config = construct(type, builder);
        List<ConfigValue<?>> values = builder.values();
        sections.put(section, values);

        List<ConfigIssue> issues = new ArrayList<>();
        boolean missing = apply(section, values, issues);
        if (missing) {
            save();
        }
        log(issues);
        return config;
    }

    /**
     * Re-reads the file and validates every value again. If the file cannot be parsed, the
     * current values are kept.
     */
    public ConfigReport reload() {
        ReadResult result = read();
        List<ConfigIssue> issues = new ArrayList<>();
        if (result.error() != null) {
            fileReadable = false;
            issues.add(new ConfigIssue(ConfigIssue.Kind.SYNTAX_ERROR, "", result.error()));
            log(issues);
            return new ConfigReport(issues);
        }
        fileReadable = true;
        root = result.root();
        boolean missing = false;
        for (Map.Entry<String, List<ConfigValue<?>>> section : sections.entrySet()) {
            missing |= apply(section.getKey(), section.getValue(), issues);
        }
        if (missing) {
            save();
        }
        log(issues);
        return new ConfigReport(issues);
    }

    /**
     * Applies the file's values to a section.
     *
     * @return whether any declared key is missing from the file
     */
    private boolean apply(String sectionName, List<ConfigValue<?>> values, List<ConfigIssue> issues) {
        JsonElement sectionElement = root.get(sectionName);
        JsonObject section = null;
        if (sectionElement != null) {
            if (sectionElement.isJsonObject()) {
                section = sectionElement.getAsJsonObject();
            } else {
                issues.add(new ConfigIssue(ConfigIssue.Kind.INVALID_VALUE, sectionName, "must be an object"));
            }
        }

        boolean missing = false;
        for (ConfigValue<?> value : values) {
            JsonElement raw = section == null ? null : find(section, value.path());
            if (raw == null) {
                value.reset();
                missing = true;
            } else {
                value.apply(raw).ifPresent(reason ->
                        issues.add(new ConfigIssue(ConfigIssue.Kind.INVALID_VALUE, sectionName + "." + value.path(), reason)));
            }
        }
        if (section != null) {
            findUnknownKeys(sectionName, section, values, issues);
        }
        return missing;
    }

    private void findUnknownKeys(String sectionName, JsonObject section, List<ConfigValue<?>> values,
                                 List<ConfigIssue> issues) {
        Set<String> valuePaths = new HashSet<>();
        Set<String> parentPaths = new HashSet<>();
        for (ConfigValue<?> value : values) {
            String path = value.path();
            valuePaths.add(path);
            for (int dot = path.indexOf('.'); dot >= 0; dot = path.indexOf('.', dot + 1)) {
                parentPaths.add(path.substring(0, dot));
            }
        }
        collectUnknownKeys(sectionName, section, "", valuePaths, parentPaths, issues);
    }

    private void collectUnknownKeys(String sectionName, JsonObject object, String prefix,
                                    Set<String> valuePaths, Set<String> parentPaths, List<ConfigIssue> issues) {
        for (Map.Entry<String, JsonElement> member : object.entrySet()) {
            String path = prefix.isEmpty() ? member.getKey() : prefix + "." + member.getKey();
            if (valuePaths.contains(path)) {
                continue;
            }
            if (parentPaths.contains(path)) {
                if (member.getValue().isJsonObject()) {
                    collectUnknownKeys(sectionName, member.getValue().getAsJsonObject(), path, valuePaths, parentPaths, issues);
                } else {
                    issues.add(new ConfigIssue(ConfigIssue.Kind.INVALID_VALUE, sectionName + "." + path, "must be an object"));
                }
            } else {
                issues.add(new ConfigIssue(ConfigIssue.Kind.UNKNOWN_KEY, sectionName + "." + path, "no module declares this key"));
            }
        }
    }

    private static JsonElement find(JsonObject section, String path) {
        JsonElement current = section;
        for (String segment : path.split("\\.")) {
            if (!current.isJsonObject()) {
                return null;
            }
            current = current.getAsJsonObject().get(segment);
            if (current == null) {
                return null;
            }
        }
        return current;
    }

    private static <T extends ModConfig> T construct(Class<T> type, ConfigSectionBuilder builder) {
        Constructor<?>[] constructors = type.getConstructors();
        if (constructors.length != 1 || !List.of(constructors[0].getParameterTypes()).equals(List.of(ConfigSectionBuilder.class))) {
            throw new IllegalStateException(type.getName() + " must declare exactly one public constructor taking a ConfigSectionBuilder");
        }
        try {
            return type.cast(constructors[0].newInstance(builder));
        } catch (InvocationTargetException e) {
            throw new IllegalStateException("Constructor of " + type.getName() + " failed", e.getCause());
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Could not instantiate " + type.getName(), e);
        }
    }

    private void save() {
        if (!fileReadable) {
            return;
        }
        String text = renderer.render(sections, root);
        Path temporary = file.resolveSibling(FILE_NAME + ".tmp");
        try {
            Files.createDirectories(file.getParent());
            Files.writeString(temporary, text, StandardCharsets.UTF_8);
            try {
                Files.move(temporary, file, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException e) {
                Files.move(temporary, file, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException e) {
            LOGGER.error("Could not write {}: {}", file, e.getMessage());
        }
    }

    private ReadResult read() {
        if (!Files.exists(file)) {
            return new ReadResult(new JsonObject(), null);
        }
        String text;
        try {
            text = Files.readString(file, StandardCharsets.UTF_8);
        } catch (IOException e) {
            return new ReadResult(null, "could not read the file: " + e.getMessage());
        }
        if (text.isBlank()) {
            return new ReadResult(new JsonObject(), null);
        }
        try {
            JsonElement parsed = JsonParser.parseString(text);
            if (!parsed.isJsonObject()) {
                return new ReadResult(null, "the file must contain a JSON object");
            }
            return new ReadResult(parsed.getAsJsonObject(), null);
        } catch (JsonParseException e) {
            return new ReadResult(null, e.getMessage());
        }
    }

    private void log(List<ConfigIssue> issues) {
        for (ConfigIssue issue : issues) {
            switch (issue.kind()) {
                case SYNTAX_ERROR -> LOGGER.error("Could not read {}: {}. The file was not changed; default or previous values stay in effect.",
                        file, issue.detail());
                case INVALID_VALUE -> LOGGER.warn("Invalid config value '{}': {}. Using the default.",
                        issue.path(), issue.detail());
                case UNKNOWN_KEY -> LOGGER.warn("Unknown config key '{}' ({}). It is kept but has no effect.",
                        issue.path(), issue.detail());
            }
        }
    }

    private record ReadResult(JsonObject root, String error) {
    }
}
