package com.daniel.library.service;

import com.daniel.library.model.Book;
import com.daniel.library.model.Member;

/** Run directly to check availability and member loan filtering. */
public class AvailabilityChecks {
    public static void main(String[] args) {
        Library library = new Library();
        library.addBook(new Book("123", "First Book", "Author"));
        library.addBook(new Book("456", "Second Book", "Author"));
        library.addBook(new Book("789", "No Copies", "Author"));
        library.addCopy("C001", "123");
        library.addCopy("C002", "123");
        library.addCopy("C003", "456");
        library.registerMember(new Member("M001", "Daniel", "daniel@example.com"));
        library.registerMember(new Member("M002", "Alex", "alex@example.com"));

        check(library.getAvailableCopies("123").size() == 2, "Both copies initially available");
        check(library.getAvailableCopies("789").isEmpty(), "Book without copies");
        library.checkoutBook("C001", "M001");
        library.checkoutBook("C003", "M002");
        check(!library.isCopyAvailable("C001"), "Checked-out copy unavailable");
        check(library.getAvailableCopies("123").getFirst().copyId().equals("C002"), "Filter by ISBN");
        check(library.getActiveLoansForMember("M001").size() == 1, "Only this member's loans");
        library.returnBook("C001");
        check(library.isCopyAvailable("C001"), "Returned copy available");
        check(library.getActiveLoansForMember("M001").isEmpty(), "Returned loans excluded");
        library.checkoutBook("C001", "M001");
        check(library.getActiveLoansForMember("M001").size() == 1, "Old loan excluded after reborrowing");

        expectMissing(() -> library.isCopyAvailable("missing"));
        expectMissing(() -> library.getAvailableCopies("missing"));
        expectMissing(() -> library.getActiveLoansForMember("missing"));
        System.out.println("All availability checks passed.");
    }

    private static void expectMissing(Runnable action) {
        try {
            action.run();
            throw new AssertionError("Unknown record must be rejected");
        } catch (IllegalArgumentException expected) {
            // Expected: an unknown ID differs from an existing record with no results.
        }
    }

    private static void check(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }
}
