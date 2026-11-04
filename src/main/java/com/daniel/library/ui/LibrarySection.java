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
            "Books is your catalog of titles and authors. Add a book first, then add its physical copies. " +
                    "Archive a title to stop lending its copies without deleting history; restore it to lend again.",
            List.of("ISBN", "Title", "Author", "Status"),
            3, List.of("All", "Active", "Archived")
    ),
    /**
     * Physical items and availability.
     */
    COPIES("Copies",
            "A copy is one physical book that someone can borrow. " +
                    "Each copy has its own Copy ID; several copies can share a book's ISBN. Check availability here.",
            List.of("Copy ID", "ISBN", "Title", "Status"),
            3, List.of("All", "Available", "Checked out", "Archived")
    ),
    /**
     * Registered borrower contact records.
     */
    MEMBERS("Members",
            "Members are the people registered to borrow books. Register each person with a unique "
                    + "Member ID. Use that ID when checking out a copy.",
            List.of("Member ID", "Name", "Email"),
            -1, List.of()
    ),
    /**
     * Borrowing, returns, and late charges.
     */
    LOANS("Loans",
            "A loan records which member borrowed a copy and when it is due back. " +
                    "Check out copy starts borrowing; Return copy records its return. Overdue means it is late. Fines show late charges.",
            List.of("Copy ID", "Member ID", "Member", "Due", "Returned", "Status", "Fine"),
            5, List.of("All", "Active", "Overdue", "Returned")
    ),
    /**
     * Read-only library totals.
     */
    OVERVIEW("Overview",
            "See the library at a glance. Current loans include overdue borrowing. " +
                    "Available copies exclude archived titles. Late charges are calculated fees; payments are not tracked.",
            List.of("Measure", "Value"),
            -1, List.of()
    );

    private final String title;
    private final String guidance;
    private final List<String> columns;
    private final int statusColumn;
    private final List<String> statuses;

    LibrarySection(String title, String guidance, List<String> columns, int statusColumn, List<String> statuses) {
        this.title = title;
        this.guidance = guidance;
        this.columns = columns;
        this.statusColumn = statusColumn;
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
