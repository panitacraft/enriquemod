package com.panita.enriquecraft.core.framework.config;

import java.util.List;

/**
 * The outcome of loading or reloading the config file.
 */
public record ConfigReport(List<ConfigIssue> issues) {

    public ConfigReport {
        issues = List.copyOf(issues);
    }

    public boolean isClean() {
        return issues.isEmpty();
    }

    /** Whether the file could not be read, so none of its values were applied. */
    public boolean hasSyntaxError() {
        return issues.stream().anyMatch(issue -> issue.kind() == ConfigIssue.Kind.SYNTAX_ERROR);
    }
}
