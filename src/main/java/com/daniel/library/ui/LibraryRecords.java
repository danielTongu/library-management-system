package com.daniel.library.ui;

import com.daniel.library.model.Book;
import com.daniel.library.model.BookCopy;
import com.daniel.library.model.Loan;
import com.daniel.library.model.Member;
import com.daniel.library.service.Library;
import com.daniel.library.service.LibrarySnapshot;
import com.daniel.library.service.LibrarySummary;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

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
        Map<String, Loan> loansByCopy = new HashMap<>();
        Map<String, List<Loan>> loansByMember = new HashMap<>();
        Map<String, List<Loan>> loansByBook = new HashMap<>();

        for (Loan loan : snapshot.records().loans()) {
            if (loan.isActive()) {
                loansByCopy.put(loan.getCopy().copyId(), loan);
                loansByMember.computeIfAbsent(loan.getMember().getMemberId(), key -> new ArrayList<>()).add(loan);
                loansByBook.computeIfAbsent(loan.getCopy().book().getIsbn(), key -> new ArrayList<>()).add(loan);
            }
        }

        List<Object[]> bookRows = new ArrayList<>();
        for (Book book : snapshot.records().books()) {
            String state = book.isArchived() ? "Archived" : "Active";
            String borrowers = loansByBook.getOrDefault(book.getIsbn(), List.of()).stream()
                    .map(loan -> loan.getMember().getName() + " (" + loan.getMember().getMemberId() + "), " + loan.getCopy().copyId() + ", due " + loan.getDueDate())
                    .collect(Collectors.joining("; "));
            long copies = snapshot.records().copies().stream().filter(copy -> copy.book().getIsbn().equals(book.getIsbn())).count();
            bookRows.add(new Object[]{book.getIsbn(), book.getTitle(), book.getAuthor(), state,
                    copies, borrowers.isEmpty() ? "None" : borrowers});
        }

        List<Object[]> copyRows = new ArrayList<>();
        for (BookCopy copy : snapshot.records().copies()) {
            Loan loan = loansByCopy.get(copy.copyId());
            String availability = copy.book().isArchived() ? "Archived" : loan == null ? "Available" : "Checked out";
            copyRows.add(new Object[]{copy.copyId(), copy.book().getIsbn(), copy.book().getTitle(), copy.book().getAuthor(), availability,
                    loan == null ? "" : loan.getMember().getMemberId(),
                    loan == null ? "" : loan.getMember().getName(), loan == null ? null : loan.getDueDate()});
        }

        List<Object[]> memberRows = new ArrayList<>();
        for (Member member : snapshot.records().members()) {
            List<Loan> current = loansByMember.getOrDefault(member.getMemberId(), List.of());
            memberRows.add(new Object[]{member.getMemberId(), member.getName(), member.getEmail(), current.size()});
        }

        List<Object[]> loanRows = new ArrayList<>();
        for (Loan loan : snapshot.records().loans()) {
            String state = !loan.isActive() ? "Returned" : snapshot.isOverdue(loan) ? "Overdue" : "Active";
            loanRows.add(new Object[]{
                    loan.getLoanId(), loan.getCopy().copyId(), loan.getCopy().book().getIsbn(),
                    loan.getCopy().book().getTitle(), loan.getCopy().book().getAuthor(),
                    loan.getMember().getMemberId(), loan.getMember().getName(), loan.getCheckoutDate(),
                    loan.getDueDate(), loan.getReturnDate(), state, snapshot.calculateFine(loan)
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
