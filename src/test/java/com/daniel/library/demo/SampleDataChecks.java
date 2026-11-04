package com.daniel.library.demo;

import static com.daniel.library.ui.LibrarySection.LOANS;

import com.daniel.library.model.Book;
import com.daniel.library.model.Loan;
import com.daniel.library.persistence.InMemoryLibraryRepository;
import com.daniel.library.service.Library;
import com.daniel.library.ui.LibraryRecords;
import com.daniel.library.ui.LibrarySection;
import com.daniel.library.ui.RecordFilter;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

/** Checks sample records and repeatable population through the repository API. */
public class SampleDataChecks {
    public static void main(String[] args) throws Exception {
        Clock clock = Clock.fixed(Instant.parse("2026-11-03T12:00:00Z"), ZoneOffset.UTC);
        Library library = new Library(new BigDecimal("0.50"), clock, SampleData.repository());
        LibraryRecords records = LibraryRecords.load(library, "Samples");
        check(records.books().size() == 3 && records.copies().size() == 4, "Sample catalog and copies");
        check(records.members().size() == 3 && records.loans().size() == 3, "Sample members and loans");
        check(records.loans().stream().anyMatch(row -> row[LOANS.statusColumn()].equals("Active")), "Active example");
        check(records.loans().stream().anyMatch(row -> row[LOANS.statusColumn()].equals("Overdue")), "Overdue example");
        check(records.loans().stream().anyMatch(row -> row[LOANS.statusColumn()].equals("Returned")),
                "Returned example");
        for (LibrarySection section : LibrarySection.values()) {
            check(!section.guidance().isBlank(), "Every desktop section has guidance");
            for (Object[] row : records.rows(section)) {
                check(row.length == section.columns().size(), "Sample rows match desktop columns");
            }
        }
        var filtered = records.loans().stream()
                .filter(row -> RecordFilter.matches(row, "Alex", LOANS.statusColumn(), "Overdue")).toList();
        check(filtered.size() == 1 && filtered.getFirst()[LOANS.columnIndex("Member")].equals("Alex Morgan"),
                "Sample loan filtering");
        InMemoryLibraryRepository target = new InMemoryLibraryRepository();
        SampleData.populate(target);
        check(target.load().books().size() == 3 && target.load().loans().size() == 3, "Population adds sample records");
        check(!target.load().loans().getLast().isActive(), "Returned example remains returned");
        Library populated = new Library(new BigDecimal("0.50"), clock, target);
        populated.updateBook("9780134685991", "User's title", "User's author");
        populated.returnBook("S-C001");
        SampleData.populate(target);
        check(target.load().books().getFirst().getTitle().equals("User's title"), "Population preserves edits");
        check(populated.isCopyAvailable("S-C001"), "Population does not undo returns");
        check(target.load().copies().size() == 4 && target.load().loans().size() == 3, "Population does not duplicate");
        populated.archiveBook("9780134685991");
        SampleData.populate(target);
        check(populated.findBookByIsbn("9780134685991").orElseThrow().isArchived(), "Archives preserved");
        InMemoryLibraryRepository archived = new InMemoryLibraryRepository();
        Book book = new Book("9780134685991", "Archived title", "Author");
        book.setArchived(true);
        archived.addBook(book);
        SampleData.populate(archived);
        check(archived.load().copies().size() == 2, "No copies added to an archived title");
        InMemoryLibraryRepository conflict = new InMemoryLibraryRepository();
        var fixture = SampleData.repository().load();
        fixture.books().forEach(conflict::addBook);
        fixture.members().forEach(conflict::addMember);
        fixture.copies().forEach(conflict::addCopy);
        new Library(new BigDecimal("0.50"), clock, conflict).checkoutBook("S-C001", "S-M003");
        SampleData.populate(conflict);
        check(conflict.load().loans().stream().filter(Loan::isActive)
                .filter(loan -> loan.getCopy().copyId().equals("S-C001")).count() == 1, "Borrowing conflict preserved");
        FailOnceRepository retry = new FailOnceRepository();
        try {
            SampleData.populate(retry);
            throw new AssertionError("Simulated write failure must propagate");
        } catch (IllegalStateException expected) {
            // Existing successful inserts remain; a retry should finish without duplicates.
        }
        SampleData.populate(retry);
        check(retry.load().books().size() == 3 && retry.load().copies().size() == 4
                && retry.load().loans().size() == 3, "Population can resume after a partial failure");
        System.out.println("All shared sample data checks passed.");
    }

    private static void check(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }

    private static class FailOnceRepository extends InMemoryLibraryRepository {
        private boolean fail = true;

        @Override
        public void addLoan(Loan loan) {
            if (fail) {
                fail = false;
                throw new IllegalStateException("Simulated write failure");
            }
            super.addLoan(loan);
        }
    }
}
