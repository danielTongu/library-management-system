package com.daniel.library.ui;

import java.util.List;

/**
 * Section names, guidance, columns, and status choices used across desktop sections.
 */
public enum LibrarySection {
    /**
     * Catalog titles and archive status.
     */
    BOOKS("Books",
            "Books lists titles and authors. Active titles are shown first; choose Archived to restore a title. "
                    + "Select an active book to add a copy. Archive is available only after all copies are returned.",
            List.of("ISBN", "Title", "Author", "Status", "Copies", "Current borrowers"),
            List.of("All", "Active", "Archived")
    ),
    /**
     * Physical items and availability.
     */
    COPIES("Copies",
            "A copy is one physical book that someone can borrow. " +
                    "Each copy has its own Copy ID; several copies can share a book's ISBN. " +
                    "See who has each checked-out copy and when it is due. Select an available row to check it out. " +
                    "Record returns in Loans.",
            List.of("Copy ID", "ISBN", "Title", "Author", "Status", "Member ID", "Borrower", "Due"),
            List.of("All", "Available", "Checked out", "Archived")
    ),
    /**
     * Registered borrower contact records.
     */
    MEMBERS("Members",
            "Members are the people registered to borrow books. Register each person with a unique Member ID. " +
                    "Current loans counts the copies they still have. Select a member and choose View current loans to review or return them.",
            List.of("Member ID", "Name", "Email", "Current loans"),
            List.of()
    ),
    /**
     * Borrowing, returns, and late charges.
     */
    LOANS("Loans",
            "Loans records who borrowed each copy and when it is due. Current shows active and overdue borrowing; "
                    + "choose Returned or All for history. Start borrowing in Copies. Select a current loan to return it.",
            List.of("Loan ID", "Copy ID", "ISBN", "Title", "Author", "Member ID", "Member",
                    "Checked out", "Due", "Returned", "Status", "Fine"),
            List.of("All", "Current", "Active", "Overdue", "Returned")
    ),
    /**
     * Read-only library totals.
     */
    OVERVIEW("Overview",
            "See the library at a glance. Current loans include overdue borrowing. " +
                    "Available copies exclude archived titles. Late charges are calculated fees; payments are not tracked.",
            List.of("Measure", "Value"),
            List.of()
    );

    private final String title;
    private final String guidance;
    private final List<String> columns;
    private final int statusColumn;
    private final List<String> statuses;

    LibrarySection(String title, String guidance, List<String> columns, List<String> statuses) {
        this.title = title;
        this.guidance = guidance;
        this.columns = columns;
        this.statusColumn = columns.indexOf("Status");
        this.statuses = statuses;
    }

    /**
     * {@return the navigation title}
     */
    public String title() {
        return title;
    }

    /**
     * {@return the section explanation}
     */
    public String guidance() {
        return guidance;
    }

    /**
     * {@return ordered table and export headers}
     */
    public List<String> columns() {
        return columns;
    }

    /**
     * Finds a field by its stable header rather than a display position.
     * @param column column header defined for this section
     * @return the model column index
     * @throws IllegalArgumentException if the section does not define that column
     */
    public int columnIndex(String column) {
        int index = columns.indexOf(column);
        if (index < 0) {
            throw new IllegalArgumentException("Unknown " + title + " column: " + column);
        }
        return index;
    }

    /**
     * {@return the status column position, or -1 when absent}
     */
    public int statusColumn() {
        return statusColumn;
    }

    /**
     * {@return status choices with All first, or an empty list}
     */
    public List<String> statuses() {
        return statuses;
    }
}
