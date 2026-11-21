package com.panita.enriquecraft.core.framework.config;

import java.util.List;

/**
 * The outcome of loading or reloading the config file.
 */
public record ConfigReport(List<ConfigIssue> issues) {

    public ConfigReport {
        issues = List.copyOf(issues);
    }

    /** This report followed by another, as when two files are reloaded together. */
    public ConfigReport and(ConfigReport other) {
        List<ConfigIssue> all = new java.util.ArrayList<>(issues);
        all.addAll(other.issues());
        return new ConfigReport(all);
    }

    public boolean isClean() {
        return issues.isEmpty();
    }

    /** Whether the file could not be read, so none of its values were applied. */
    public boolean hasSyntaxError() {
        return issues.stream().anyMatch(issue -> issue.kind() == ConfigIssue.Kind.SYNTAX_ERROR);
    }
}
