package com.panita.enriquecraft.core.framework.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Writes the config file text: every declared value with its comment, using the value found in
 * the original file when there is one and the default otherwise. Anything in the original that no
 * module declares is kept after the declared values.
 */
final class ConfigRenderer {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().serializeNulls().create();
    private static final String INDENT = "  ";
    private static final String HEADER = """
            // Enriquecraft configuration.
            // Change a value and run /enriquecraft reload, or restart the server.
            """;

    String render(Map<String, List<ConfigValue<?>>> sections, JsonObject original) {
        List<String> members = new ArrayList<>();
        for (Map.Entry<String, List<ConfigValue<?>>> section : sections.entrySet()) {
            JsonObject originalSection = objectOrNull(original.get(section.getKey()));
            String body = objectText(treeOf(section.getValue()), originalSection, 1);
            members.add(INDENT + quote(section.getKey()) + ": " + body);
        }
        for (Map.Entry<String, JsonElement> extra : original.entrySet()) {
            if (!sections.containsKey(extra.getKey())) {
                members.add(INDENT + quote(extra.getKey()) + ": " + pretty(extra.getValue(), 1));
            }
        }
        return HEADER + "{\n" + String.join(",\n", members) + "\n}\n";
    }

    private String objectText(Node node, JsonObject original, int depth) {
        return "{\n" + String.join(",\n", members(node, original, depth + 1)) + "\n" + indent(depth) + "}";
    }

    private List<String> members(Node node, JsonObject original, int depth) {
        List<String> members = new ArrayList<>();
        for (Map.Entry<String, Node> entry : node.children.entrySet()) {
            String key = entry.getKey();
            Node child = entry.getValue();
            JsonElement originalChild = original == null ? null : original.get(key);
            StringBuilder member = new StringBuilder();
            if (child.value != null) {
                appendComments(member, child.value, depth);
                JsonElement json = originalChild != null ? originalChild : child.value.defaultJson();
                member.append(indent(depth)).append(quote(key)).append(": ").append(pretty(json, depth));
            } else {
                member.append(indent(depth)).append(quote(key)).append(": ")
                        .append(objectText(child, objectOrNull(originalChild), depth));
            }
            members.add(member.toString());
        }
        if (original != null) {
            for (Map.Entry<String, JsonElement> extra : original.entrySet()) {
                if (!node.children.containsKey(extra.getKey())) {
                    members.add(indent(depth) + quote(extra.getKey()) + ": " + pretty(extra.getValue(), depth));
                }
            }
        }
        return members;
    }

    private void appendComments(StringBuilder out, ConfigValue<?> value, int depth) {
        String prefix = indent(depth) + "// ";
        for (String line : value.comment().split("\n")) {
            out.append(prefix).append(line).append('\n');
        }
        value.constraint().ifPresent(constraint -> out.append(prefix).append("Allowed: ").append(constraint).append('\n'));
        out.append(prefix).append("Default: ").append(value.defaultJson()).append('\n');
    }

    private Node treeOf(List<ConfigValue<?>> values) {
        Node root = new Node();
        for (ConfigValue<?> value : values) {
            Node current = root;
            for (String segment : value.path().split("\\.")) {
                current = current.children.computeIfAbsent(segment, key -> new Node());
            }
            current.value = value;
        }
        return root;
    }

    private static JsonObject objectOrNull(JsonElement element) {
        return element != null && element.isJsonObject() ? element.getAsJsonObject() : null;
    }

    private static String pretty(JsonElement element, int depth) {
        return GSON.toJson(element).replace("\n", "\n" + indent(depth));
    }

    private static String quote(String key) {
        return GSON.toJson(new JsonPrimitive(key));
    }

    private static String indent(int depth) {
        return INDENT.repeat(depth);
    }

    private static final class Node {
        private final Map<String, Node> children = new LinkedHashMap<>();
        private ConfigValue<?> value;
    }
}
