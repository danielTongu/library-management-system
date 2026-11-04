package com.daniel.library.service;

import com.daniel.library.model.Book;
import com.daniel.library.model.Loan;
import com.daniel.library.model.Member;

/** Checks validation and preservation of copy/member links when details change. */
public class EditingChecks {
    public static void main(String[] args) {
        Library library = new Library();
        library.addBook(new Book("123", "Old Title", "Old Author"));
        library.addCopy("C001", "123");
        library.registerMember(new Member("M001", "Old Name", "old@example.com"));
        Loan loan = library.checkoutBook("C001", "M001");
        String loanId = loan.getLoanId();

        library.updateBook("123", "New Title", "New Author");
        library.updateMember("M001", "New Name", "new@example.com");
        check(loan.getCopy().book().getTitle().equals("New Title"), "Loan keeps updated book reference");
        check(loan.getMember().getName().equals("New Name"), "Loan keeps updated member reference");
        check(library.findCopyById("C001").orElseThrow().book().getAuthor().equals("New Author"), "Copy updated");
        check(loan.getMember().getEmail().equals("new@example.com"), "Email updated");
        check(loan.getCopy().book().getIsbn().equals("123"), "ISBN unchanged");
        check(loan.getMember().getMemberId().equals("M001"), "Member ID unchanged");
        check(loan.getLoanId().equals(loanId) && loan.isActive(), "Loan identity and status unchanged");

        expectInvalid(() -> library.updateBook("123", "Another Title", " "));
        expectInvalid(() -> library.updateMember("M001", "Another Name", "invalid"));
        check(loan.getCopy().book().getTitle().equals("New Title"), "Invalid edit leaves book unchanged");
        check(loan.getMember().getName().equals("New Name"), "Invalid edit leaves member unchanged");
        expectInvalid(() -> loan.getCopy().book().updateDetails("Bad Update", ""));
        expectInvalid(() -> loan.getMember().updateDetails("Bad Update", "invalid"));
        check(loan.getCopy().book().getTitle().equals("New Title"), "Model validation is atomic");
        check(loan.getMember().getName().equals("New Name"), "Contact validation is atomic");
        expectInvalid(() -> library.updateBook("missing", "Title", "Author"));
        expectInvalid(() -> library.updateMember("missing", "Name", "name@example.com"));
        library.returnBook("C001");
        check(library.getLoans().size() == 1 && !loan.isActive(), "Return still works after editing");
        System.out.println("All editing checks passed.");
    }

    private static void expectInvalid(Runnable action) {
        try {
            action.run();
            throw new AssertionError("Invalid edit must fail");
        } catch (IllegalArgumentException expected) {
            // Expected: invalid fields or an unknown identifier.
        }
    }

    private static void check(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }
}
