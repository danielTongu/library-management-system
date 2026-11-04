package com.daniel.library.service;

import com.daniel.library.demo.SampleData;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

/** Verifies totals after borrowing, returning, archiving, and restoring. */
public class SummaryChecks {
    public static void main(String[] args) {
        Clock clock = Clock.fixed(Instant.parse("2026-11-03T12:00:00Z"), ZoneOffset.UTC);
        LibrarySummary empty = new Library(new BigDecimal("0.50"), clock).getSummary();
        check(empty.bookTitles() == 0 && empty.availableCopies() == 0 && empty.currentLoans() == 0, "Empty totals");
        check(empty.currentLateCharges().equals(new BigDecimal("0.00")), "Empty charges");
        var repository = SampleData.repository();
        Library library = new Library(new BigDecimal("0.50"), clock, repository);
        LibrarySummary summary = library.getSummary();
        check(summary.bookTitles() == 3 && summary.archivedTitles() == 0, "Catalog totals");
        check(summary.physicalCopies() == 4 && summary.availableCopies() == 2, "Copy totals");
        check(summary.members() == 3, "Member total");
        check(summary.currentLoans() == 2 && summary.overdueLoans() == 1, "Overdue loans included in current loans");
        check(summary.returnedLoans() == 1, "Returned loan count");
        check(summary.currentLateCharges().equals(new BigDecimal("5.00")), "Current late charges");
        check(summary.returnedLateCharges().equals(new BigDecimal("1.50")), "Returned late charges");

        library.archiveBook("9780132350884");
        summary = library.getSummary();
        check(summary.archivedTitles() == 1 && summary.archivedCopies() == 1, "Archive totals");
        check(summary.availableCopies() == 1 && summary.bookTitles() == 3, "Archived records retained but unavailable");
        library.returnBook("S-C003");
        summary = library.getSummary();
        check(summary.currentLoans() == 1 && summary.overdueLoans() == 0, "Return updates current borrowing");
        check(summary.returnedLoans() == 2 && summary.availableCopies() == 2, "Return frees a copy");
        check(summary.currentLateCharges().equals(new BigDecimal("0.00")), "Returned charges excluded from current");
        check(summary.returnedLateCharges().equals(new BigDecimal("6.50")), "Returned fees retained");
        library.restoreBook("9780132350884");
        check(library.getSummary().availableCopies() == 3, "Restore updates availability");
        library.checkoutBook("S-C004", "S-M001");
        check(library.getSummary().availableCopies() == 2, "Checkout reduces availability");
        Library later = new Library(new BigDecimal("0.50"),
                Clock.fixed(Instant.parse("2026-11-10T12:00:00Z"), ZoneOffset.UTC), repository);
        check(later.getSummary().returnedLateCharges().equals(new BigDecimal("6.50")), "Returned fees stop growing");
        System.out.println("All summary checks passed.");
    }

    private static void check(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }
}
