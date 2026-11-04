package com.daniel.library.persistence;

import com.daniel.library.model.Book;
import com.daniel.library.model.BookCopy;
import com.daniel.library.model.Loan;
import com.daniel.library.model.Member;
import com.daniel.library.service.Library;

import java.time.LocalDate;

/** Checks that failed database writes cannot leave successful-looking changes in memory. */
public class PersistenceFailureChecks {
    public static void main(String[] args) {
        TestRepository repository = new TestRepository();
        Library library = new Library(repository);
        expectFailure(() -> library.addBook(new Book("456", "New Book", "Author")));
        check(library.getBooks().size() == 1, "Failed book insert leaves catalog unchanged");
        expectFailure(() -> library.archiveBook("123"));
        check(!library.getBooks().getFirst().isArchived(), "Failed archive leaves status unchanged");
        expectFailure(() -> library.updateBook("123", "Changed Title", "Changed Author"));
        check(library.getBooks().getFirst().getTitle().equals("Existing Book"), "Failed edit leaves book unchanged");
        expectFailure(() -> library.updateMember("M001", "Changed Name", "changed@example.com"));
        check(library.getMembers().getFirst().getName().equals("Daniel"), "Failed edit leaves member unchanged");
        expectFailure(() -> library.addCopy("C002", "123"));
        check(library.getCopies().size() == 1, "Failed copy insert leaves copies unchanged");
        expectFailure(() -> library.registerMember(new Member("M002", "Alex", "alex@example.com")));
        check(library.getMembers().size() == 1, "Failed member insert leaves members unchanged");
        expectFailure(() -> library.checkoutBook("C001", "M001"));
        check(library.getLoans().isEmpty(), "Failed checkout leaves no active loan");
        check(library.isCopyAvailable("C001"), "Failed checkout leaves copy available");

        repository.fail = false;
        Loan loan = library.checkoutBook("C001", "M001");
        repository.fail = true;
        expectFailure(() -> library.returnBook("C001"));
        check(loan.isActive(), "Failed return leaves loan active");
        check(!library.isCopyAvailable("C001"), "Failed return leaves copy unavailable");
        repository.fail = false;
        library.returnBook("C001");
        check(!loan.isActive(), "Successful return updates loan");
        Library other = new Library(repository);
        check(other.getLoans().size() == 1, "Records belong to the shared repository, not a Library instance");
        other.addBook(new Book("456", "New Book", "Author"));
        check(library.getBooks().size() == 2, "Library reads current repository records");
        Loan restored = new Loan(loan.getLoanId(), loan.getCopy(), loan.getMember(),
                loan.getCheckoutDate(), loan.getDueDate());
        check(library.calculateFine(restored).equals(library.calculateFine(loan)), "Fine lookup uses saved loan ID");
        library.archiveBook("123");
        repository.fail = true;
        expectFailure(() -> library.restoreBook("123"));
        check(library.getBooks().getFirst().isArchived(), "Failed restore leaves book archived");
        System.out.println("All persistence failure checks passed.");
    }

    private static void expectFailure(Runnable action) {
        try {
            action.run();
            throw new AssertionError("Write must fail");
        } catch (IllegalStateException expected) {
            // Expected: simulated storage failure.
        }
    }

    private static void check(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }

    private static class TestRepository extends InMemoryLibraryRepository {
        private boolean fail = true;

        private TestRepository() {
            Book book = new Book("123", "Existing Book", "Author");
            super.addBook(book);
            super.addCopy(new BookCopy("C001", book));
            super.addMember(new Member("M001", "Daniel", "daniel@example.com"));
        }

        @Override
        public void addBook(Book book) {
            write();
            super.addBook(book);
        }

        @Override
        public void setBookArchived(String isbn, boolean archived) {
            write();
            super.setBookArchived(isbn, archived);
        }

        @Override
        public void updateBook(Book book) {
            write();
            super.updateBook(book);
        }

        @Override
        public void updateMember(Member member) {
            write();
            super.updateMember(member);
        }

        @Override
        public void addCopy(BookCopy copy) {
            write();
            super.addCopy(copy);
        }

        @Override
        public void addMember(Member member) {
            write();
            super.addMember(member);
        }

        @Override
        public void addLoan(Loan loan) {
            write();
            super.addLoan(loan);
        }

        @Override
        public void returnLoan(Loan loan, LocalDate returnDate) {
            write();
            super.returnLoan(loan, returnDate);
        }

        private void write() {
            if (fail) {
                throw new IllegalStateException("Simulated database failure");
            }
        }
    }
}
