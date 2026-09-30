package com.panita.enriquecraft.core.gui;

import java.util.List;

/**
 * Page arithmetic for lists shown a page at a time. Pages are counted from zero; an empty list
 * still has one (empty) page.
 */
public final class Paginator {

    private final int pageSize;

    public Paginator(int pageSize) {
        if (pageSize < 1) {
            throw new IllegalArgumentException("The page size must be at least 1");
        }
        this.pageSize = pageSize;
    }

    public int pageCount(int total) {
        return Math.max(1, (total + pageSize - 1) / pageSize);
    }

    /** Brings a page number into the valid range, for example after entries were removed. */
    public int clamp(int page, int total) {
        return Math.min(Math.max(page, 0), pageCount(total) - 1);
    }

    public boolean hasPrevious(int page) {
        return page > 0;
    }

    public boolean hasNext(int page, int total) {
        return page < pageCount(total) - 1;
    }

    /** The entries of a page; the page number is clamped first. */
    public <T> List<T> slice(List<T> entries, int page) {
        int start = clamp(page, entries.size()) * pageSize;
        int end = Math.min(start + pageSize, entries.size());
        return entries.subList(start, end);
    }
}
