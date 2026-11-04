package com.daniel.library.model;

import java.time.LocalDate;

/** Run directly to verify that invalid records fail before they enter the library. */
public class ValidationChecks {
    public static void main(String[] args) {
        for (String missing : new String[]{null, "", " \t"}) {
            expectInvalid(() -> new Book(missing, "Title", "Author"));
            expectInvalid(() -> new Book("123", missing, "Author"));
            expectInvalid(() -> new Book("123", "Title", missing));
            expectInvalid(() -> new Member(missing, "Daniel", "daniel@example.com"));
            expectInvalid(() -> new Member("M001", missing, "daniel@example.com"));
            expectInvalid(() -> new Member("M001", "Daniel", missing));
        }
        for (String email : new String[]{"invalid", "name@", "@example.com", "a@@example.com", "a b@example.com"}) {
            expectInvalid(() -> new Member("M001", "Daniel", email));
        }
        Book book = new Book("123", "Test Book", "Author");
        BookCopy copy = new BookCopy("C001", book);
        Member member = new Member("M001", "Daniel", "daniel+library@example.com");
        LocalDate date = LocalDate.of(2026, 10, 1);
        expectInvalid(() -> new BookCopy(null, book));
        expectInvalid(() -> new BookCopy(" ", book));
        expectInvalid(() -> new BookCopy("C002", null));
        expectInvalid(() -> new Loan(null, member, date, date));
        expectInvalid(() -> new Loan(copy, null, date, date));
        expectInvalid(() -> new Loan(copy, member, null, date));
        expectInvalid(() -> new Loan(copy, member, date, null));
        expectInvalid(() -> new Loan(copy, member, date, date.minusDays(1)));
        Loan loan = new Loan(copy, member, date, date.plusDays(14));
        expectInvalid(() -> loan.markReturned(date.minusDays(1)));
        expectInvalid(() -> loan.markReturned(null));
        if (!loan.isActive()) {
            throw new AssertionError("Invalid return must leave loan active");
        }
        new Loan(copy, member, date, date);
        System.out.println("All model validation checks passed.");
    }

    private static void expectInvalid(Runnable action) {
        try {
            action.run();
            throw new AssertionError("Invalid record must be rejected");
        } catch (IllegalArgumentException expected) {
            // Expected: validation rejects the record before construction or mutation.
        }
    }
}
