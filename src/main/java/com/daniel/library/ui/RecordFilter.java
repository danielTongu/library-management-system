package com.daniel.library.ui;

import java.util.Arrays;
import java.util.Locale;

/**
 * Literal text and exact status filtering used by desktop tables.
 */
public final class RecordFilter {
    private RecordFilter() {
    }

    /**
     * Matches a literal, case-insensitive substring and optional exact status.
     *
     * @param row          displayed values
     * @param query        search text; outer whitespace is ignored
     * @param statusColumn status position, or a negative value when there is no status column
     * @param status       required status; Current includes Active and Overdue, null/All includes every status
     * @return true if the row matches both criteria
     */
    public static boolean matches(Object[] row, String query, int statusColumn, String status) {
        if (statusColumn >= 0 && status != null && !status.equals("All")) {
            Object actual = row[statusColumn];
            boolean matches = status.equals("Current") ? "Active".equals(actual) || "Overdue".equals(actual) : status.equals(actual);
            if (!matches) {
                return false;
            }
        }
        String text = query.strip().toLowerCase(Locale.ROOT);
        return Arrays.stream(row).map(value -> value == null ? "" : value.toString())
                .anyMatch(value -> value.toLowerCase(Locale.ROOT).contains(text));
    }
}
