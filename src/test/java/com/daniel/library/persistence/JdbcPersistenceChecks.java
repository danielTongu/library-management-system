package com.daniel.library.persistence;

import com.daniel.library.model.Book;
import com.daniel.library.model.Loan;
import com.daniel.library.model.Member;
import com.daniel.library.service.Library;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.UUID;

/** Requires a dedicated library_management_test database and MySQL driver on the classpath. */
public class JdbcPersistenceChecks {
    public static void main(String[] args) throws Exception {
        JdbcConnectionSettings original = JdbcConnectionSettings.fromEnvironment();
        String testUrl = System.getenv("LIBRARY_TEST_DB_URL");
        if (!"jdbc:mysql://localhost:3306/library_management_test".equals(testUrl)) {
            throw new IllegalArgumentException("Set LIBRARY_TEST_DB_URL to jdbc:mysql://localhost:3306/library_management_test");
        }
        JdbcConnectionSettings settings = new JdbcConnectionSettings(testUrl, original.username(), original.password());
        JdbcLibraryRepository repository = new JdbcLibraryRepository(settings);
        repository.initializeSchema();
        String id = UUID.randomUUID().toString();
        try {
            Library first = new Library(repository);
            first.addBook(new Book(id, "Persistence ' Check", "Test Author"));
            first.addCopy(id, id);
            first.registerMember(new Member(id, "Test Member", "test@example.com"));
            Loan loan = first.checkoutBook(id, id);
            first.updateBook(id, "Edited Title", "Edited Author");
            first.updateMember(id, "Edited Member", "edited@example.com");

            Library restarted = new Library(new JdbcLibraryRepository(settings));
            check(restarted.findBookByIsbn(id).isPresent(), "Book survives restart");
            check(restarted.findMemberById(id).isPresent(), "Member survives restart");
            check(restarted.findBookByIsbn(id).orElseThrow().getTitle().equals("Edited Title"), "Book edit persists");
            check(restarted.findMemberById(id).orElseThrow().getEmail().equals("edited@example.com"), "Email edit persists");
            check(!restarted.isCopyAvailable(id), "Active loan survives restart");
            Loan restored = restarted.getActiveLoansForMember(id).getFirst();
            check(restored.getLoanId().equals(loan.getLoanId()), "Loan identity survives restart");
            check(restored.getDueDate().equals(loan.getDueDate()), "Due date survives restart");
            try {
                repository.addLoan(new Loan(restored.getCopy(), restored.getMember(),
                        restored.getCheckoutDate(), restored.getDueDate()));
                throw new AssertionError("Database must reject a second active loan");
            } catch (IllegalStateException expected) {
                // Database constraint enforces availability even outside Library.
            }
            restarted.returnBook(id);
            Library afterReturn = new Library(new JdbcLibraryRepository(settings));
            check(afterReturn.isCopyAvailable(id), "Return survives restart");
            afterReturn.archiveBook(id);
            Library archived = new Library(new JdbcLibraryRepository(settings));
            check(archived.findBookByIsbn(id).orElseThrow().isArchived(), "Archive survives restart");
            check(!archived.isCopyAvailable(id), "Archived copy unavailable after restart");
            archived.restoreBook(id);
            check(new Library(repository).isCopyAvailable(id), "Restore survives restart");
            afterReturn.checkoutBook(id, id);
            Library afterReborrow = new Library(new JdbcLibraryRepository(settings));
            long historySize = afterReborrow.getLoans().stream()
                    .filter(entry -> entry.getCopy().copyId().equals(id)).count();
            check(historySize == 2, "Returned and active loans both survive restart");
            Loan historical = new Loan(restored.getCopy(), restored.getMember(),
                    loan.getCheckoutDate(), loan.getDueDate());
            historical.markReturned(loan.getCheckoutDate());
            repository.addLoan(historical);
            Loan savedHistory = new Library(repository).getLoans().stream()
                    .filter(entry -> entry.getLoanId().equals(historical.getLoanId())).findFirst().orElseThrow();
            check(!savedHistory.isActive() && savedHistory.getReturnDate().equals(historical.getReturnDate()),
                    "Adding a historical sample loan preserves its return date");
            System.out.println("All MySQL persistence checks passed.");
        } finally {
            cleanup(settings, id);
        }
    }

    private static void cleanup(JdbcConnectionSettings settings, String id) throws Exception {
        try (Connection connection = settings.openConnection()) {
            for (String sql : new String[]{"DELETE FROM loans WHERE copy_id = ?",
                    "DELETE FROM book_copies WHERE copy_id = ?", "DELETE FROM members WHERE member_id = ?",
                    "DELETE FROM book_archive WHERE isbn = ?", "DELETE FROM books WHERE isbn = ?"}) {
                try (PreparedStatement statement = connection.prepareStatement(sql)) {
                    statement.setString(1, id);
                    statement.executeUpdate();
                }
            }
        }
    }

    private static void check(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }
}
