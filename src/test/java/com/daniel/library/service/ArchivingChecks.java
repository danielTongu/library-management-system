package com.daniel.library.service;

import com.daniel.library.model.Book;
import com.daniel.library.model.Member;
import com.daniel.library.ui.LibraryRecords;

/** Checks that archiving changes lending eligibility without deleting records or history. */
public class ArchivingChecks {
    public static void main(String[] args) {
        Library library = new Library();
        library.addBook(new Book("123", "Test Book", "Author"));
        library.addCopy("C001", "123");
        library.addCopy("C002", "123");
        library.registerMember(new Member("M001", "Daniel", "daniel@example.com"));
        library.checkoutBook("C001", "M001");
        expectRejected(() -> library.archiveBook("123"));
        check(!library.getBooks().getFirst().isArchived(), "Borrowed title cannot be archived");
        library.returnBook("C001");
        library.archiveBook("123");
        library.archiveBook("123");
        check(library.getBooks().size() == 1 && library.getCopies().size() == 2, "Records retained");
        check(library.getLoans().size() == 1, "History retained");
        check(!library.isCopyAvailable("C001") && !library.isCopyAvailable("C002"), "All copies unavailable");
        check(library.getAvailableCopies("123").isEmpty(), "Archived copies excluded from available results");
        expectRejected(() -> library.checkoutBook("C001", "M001"));
        expectRejected(() -> library.addCopy("C003", "123"));
        library.updateBook("123", "Edited Archived Book", "New Author");
        check(library.getBooks().getFirst().isArchived(), "Editing does not restore an archived book");
        LibraryRecords records = LibraryRecords.load(library, "");
        check(records.books().getFirst()[3].equals("Archived"), "Shared book status");
        check(records.copies().getFirst()[3].equals("Archived"), "Shared copy status");
        library.restoreBook("123");
        library.restoreBook("123");
        check(library.isCopyAvailable("C001"), "Restored copy available");
        library.checkoutBook("C001", "M001");
        check(library.getLoans().size() == 2, "Borrowing works after restoring");
        expectRejected(() -> library.archiveBook("missing"));
        expectRejected(() -> library.restoreBook("missing"));
        System.out.println("All archiving checks passed.");
    }

    private static void expectRejected(Runnable action) {
        try {
            action.run();
            throw new AssertionError("Operation should be rejected");
        } catch (IllegalArgumentException | IllegalStateException expected) {
            // Expected: unknown record, archived title, or active borrowing.
        }
    }

    private static void check(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }
}
