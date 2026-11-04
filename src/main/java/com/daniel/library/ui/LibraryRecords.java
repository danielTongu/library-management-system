package com.daniel.library.ui;

import com.daniel.library.model.Book;
import com.daniel.library.model.BookCopy;
import com.daniel.library.model.Loan;
import com.daniel.library.model.Member;
import com.daniel.library.service.Library;
import com.daniel.library.service.LibrarySnapshot;
import com.daniel.library.service.LibrarySummary;

import java.util.ArrayList;
import java.util.List;

/**
 * Presentation rows for a single screen refresh, prepared outside the Swing event thread.
 * Rows follow LibrarySection column definitions and are not intended for mutation.
 *
 * @param books    catalog rows
 * @param copies   physical-item rows
 * @param members  borrower rows
 * @param loans    borrowing history rows
 * @param overview calculated summary rows
 * @param message  operation result displayed after refresh
 */
public record LibraryRecords(List<Object[]> books, List<Object[]> copies, List<Object[]> members, List<Object[]> loans, List<Object[]> overview, String message) {

    /**
     * Builds every section from one repository snapshot and one calculation date.
     *
     * @param library service providing records and policy
     * @param message operation result to carry to the workspace
     * @return complete presentation rows for the refresh
     */
    public static LibraryRecords load(Library library, String message) {
        LibrarySnapshot snapshot = library.getSnapshot();
        List<Object[]> bookRows = new ArrayList<>();
        for (Book book : snapshot.records().books()) {
            String state = book.isArchived() ? "Archived" : "Active";
            bookRows.add(new Object[]{book.getIsbn(), book.getTitle(), book.getAuthor(), state});
        }

        List<Object[]> copyRows = new ArrayList<>();
        for (BookCopy copy : snapshot.records().copies()) {
            String availability = copy.book().isArchived() ? "Archived"
                    : snapshot.isAvailable(copy) ? "Available" : "Checked out";
            copyRows.add(new Object[]{copy.copyId(), copy.book().getIsbn(), copy.book().getTitle(), availability});
        }

        List<Object[]> memberRows = new ArrayList<>();
        for (Member member : snapshot.records().members()) {
            memberRows.add(new Object[]{member.getMemberId(), member.getName(), member.getEmail()});
        }

        List<Object[]> loanRows = new ArrayList<>();
        for (Loan loan : snapshot.records().loans()) {
            String state = !loan.isActive() ? "Returned" : snapshot.isOverdue(loan) ? "Overdue" : "Active";
            loanRows.add(new Object[]{
                    loan.getCopy().copyId(),
                    loan.getMember().getMemberId(),
                    loan.getMember().getName(),
                    loan.getDueDate(),
                    loan.getReturnDate(),
                    state,
                    snapshot.calculateFine(loan)
            });
        }

        LibrarySummary summary = snapshot.summary();

        List<Object[]> overviewRows = List.of(
                new Object[]{"As of", summary.asOfDate()},
                new Object[]{"Book titles (including archived)", summary.bookTitles()},
                new Object[]{"Archived titles", summary.archivedTitles()},
                new Object[]{"Physical copies (including archived)", summary.physicalCopies()},
                new Object[]{"Available copies", summary.availableCopies()},
                new Object[]{"Archived copies", summary.archivedCopies()},
                new Object[]{"Registered members", summary.members()},
                new Object[]{"Current loans (including overdue)", summary.currentLoans()},
                new Object[]{"Overdue loans", summary.overdueLoans()},
                new Object[]{"Returned loans", summary.returnedLoans()},
                new Object[]{"Late charges on current loans", summary.currentLateCharges()},
                new Object[]{"Late charges recorded on returned loans", summary.returnedLateCharges()}
        );

        return new LibraryRecords(bookRows, copyRows, memberRows, loanRows, overviewRows, message);
    }

    /**
     * Selects rows whose positions match the requested section columns.
     *
     * @param section desktop section
     * @return rows for that section
     */
    public List<Object[]> rows(LibrarySection section) {
        return switch (section) {
            case BOOKS -> books;
            case COPIES -> copies;
            case MEMBERS -> members;
            case LOANS -> loans;
            case OVERVIEW -> overview;
        };
    }
}
