package com.daniel.library.service;

import com.daniel.library.model.Book;
import com.daniel.library.model.Loan;
import com.daniel.library.model.Member;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZoneOffset;

/** Run this class directly in IntelliJ to check fine and return behavior. */
public class FineChecks {
    public static void main(String[] args) {
        TestClock clock = new TestClock();
        Library library = new Library(new BigDecimal("0.50"), clock);
        library.addBook(new Book("123", "Test Book", "Test Author"));
        library.addCopy("C001", "123");
        library.registerMember(new Member("M001", "Daniel", "daniel@example.com"));
        Loan loan = library.checkoutBook("C001", "M001");

        check(library.calculateFine(loan).equals(new BigDecimal("0.00")), "No fine at checkout");
        clock.date = loan.getDueDate();
        check(library.getOverdueLoans().isEmpty(), "Due date is not overdue");
        check(library.calculateFine(loan).equals(new BigDecimal("0.00")), "No fine on due date");

        clock.date = loan.getDueDate().plusDays(1);
        check(library.calculateFine(loan).equals(new BigDecimal("0.50")), "One day late");
        check(library.getOverdueLoans().size() == 1, "Active overdue loan listed");

        clock.date = loan.getDueDate().plusDays(3);
        check(library.calculateFine(loan).equals(new BigDecimal("1.50")), "Three days late");
        library.returnBook("C001");
        clock.date = clock.date.plusDays(10);
        check(library.calculateFine(loan).equals(new BigDecimal("1.50")), "Fine stops at return");
        check(library.getOverdueLoans().isEmpty(), "Returned loan excluded from overdue list");
        check(loan.calculateFine(clock.date, BigDecimal.ZERO).equals(new BigDecimal("0.00")), "Zero rate supported");

        try {
            library.returnBook("C001");
            throw new AssertionError("Second return must fail");
        } catch (IllegalStateException expected) {
            // Expected: this copy has already been returned.
        }

        library.checkoutBook("C001", "M001");
        check(library.getLoans().size() == 2, "Reborrowing preserves history");
        try {
            library.checkoutBook("C001", "M001");
            throw new AssertionError("Duplicate checkout must fail");
        } catch (IllegalStateException expected) {
            // Expected: the second loan is active.
        }

        try {
            new Library(new BigDecimal("-0.50"), clock);
            throw new AssertionError("Negative fine rate must fail");
        } catch (IllegalArgumentException expected) {
            // Expected: fine rates cannot be negative.
        }
        System.out.println("All fine and return checks passed.");
    }

    private static void check(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }

    private static class TestClock extends Clock {
        private LocalDate date = LocalDate.of(2026, 10, 1);

        @Override
        public ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return Clock.fixed(instant(), zone);
        }

        @Override
        public Instant instant() {
            return date.atStartOfDay(ZoneOffset.UTC).toInstant();
        }
    }
}
