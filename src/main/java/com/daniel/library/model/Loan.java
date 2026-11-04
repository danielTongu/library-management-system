package com.daniel.library.model;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

/**
 * A borrowing record linking one member to one physical copy.
 * Checkout and due dates are fixed; a null return date means the loan is active.
 * Business workflows and persistence belong to the Library service and repositories.
 */
public class Loan {
    private final String loanId;
    private final BookCopy copy;
    private final Member member;
    private final LocalDate checkoutDate;
    private final LocalDate dueDate;
    private LocalDate returnDate;

    /**
     * Creates a loan with a new UUID identifier.
     * @param copy physical item being borrowed
     * @param member registered borrower
     * @param checkoutDate first day of borrowing
     * @param dueDate final day before late charges start
     * @throws IllegalArgumentException if participants or dates are missing, or dates are reversed
     */
    public Loan(BookCopy copy, Member member, LocalDate checkoutDate, LocalDate dueDate) {
        this(UUID.randomUUID().toString(), copy, member, checkoutDate, dueDate);
    }

    /**
     * Reconstructs a loan with an existing identifier, including records loaded from storage.
     * @param loanId stable, nonblank borrowing-record identifier
     * @param copy physical item being borrowed
     * @param member registered borrower
     * @param checkoutDate first day of borrowing
     * @param dueDate final day before late charges start
     * @throws IllegalArgumentException if the identifier, participants, or dates are invalid
     */
    public Loan(String loanId, BookCopy copy, Member member, LocalDate checkoutDate, LocalDate dueDate) {
        Validation.requireText(loanId, "Loan ID");
        if (copy == null || member == null) {
            throw new IllegalArgumentException("Copy and member are required.");
        }
        if (checkoutDate == null || dueDate == null) {
            throw new IllegalArgumentException("Checkout date and due date are required.");
        }
        if (dueDate.isBefore(checkoutDate)) {
            throw new IllegalArgumentException("Due date cannot be before checkout.");
        }
        this.loanId = loanId;
        this.copy = copy;
        this.member = member;
        this.checkoutDate = checkoutDate;
        this.dueDate = dueDate;
    }

    /** {@return the borrowed physical copy} */
    public BookCopy getCopy() {
        return copy;
    }

    /** {@return the stable loan identifier} */
    public String getLoanId() {
        return loanId;
    }

    /** {@return the borrower} */
    public Member getMember() {
        return member;
    }

    /** {@return the checkout date} */
    public LocalDate getCheckoutDate() {
        return checkoutDate;
    }

    /** {@return the due date} */
    public LocalDate getDueDate() {
        return dueDate;
    }

    /** {@return the return date, or null while still borrowed} */
    public LocalDate getReturnDate() {
        return returnDate;
    }

    /** {@return true while no return has been recorded} */
    public boolean isActive() {
        return returnDate == null;
    }

    /**
     * Counts calendar days after the due date, stopping at the return date when applicable.
     * @param asOfDate reporting date on or after checkout
     * @return a nonnegative number of overdue days
     * @throws IllegalArgumentException if the reporting date is missing or precedes checkout
     */
    public long getOverdueDays(LocalDate asOfDate) {
        if (asOfDate == null || asOfDate.isBefore(checkoutDate)) {
            throw new IllegalArgumentException("Date must be on or after checkout.");
        }

        LocalDate endDate = returnDate != null && !returnDate.isAfter(asOfDate) ? returnDate : asOfDate;
        return Math.max(0, ChronoUnit.DAYS.between(dueDate, endDate));
    }

    /**
     * Tests the current active state against the requested reporting date.
     * @param asOfDate reporting date on or after checkout
     * @return true when the loan is currently active and late on that date
     * @throws IllegalArgumentException if an active loan receives an invalid reporting date
     */
    public boolean isOverdue(LocalDate asOfDate) {
        return isActive() && getOverdueDays(asOfDate) > 0;
    }

    /**
     * Calculates the late charge; this does not record a payment or balance.
     * @param asOfDate reporting date on or after checkout
     * @param dailyRate nonnegative monetary charge for each overdue day
     * @return charge rounded to two decimal places using HALF_UP
     * @throws IllegalArgumentException if the date or rate is invalid
     */
    public BigDecimal calculateFine(LocalDate asOfDate, BigDecimal dailyRate) {
        if (dailyRate == null || dailyRate.signum() < 0) {
            throw new IllegalArgumentException("Daily fine rate cannot be missing or negative.");
        }

        return dailyRate.multiply(BigDecimal.valueOf(getOverdueDays(asOfDate))).setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * Records one return without persisting it; repositories save before calling this method.
     * @param date return date on or after checkout
     * @throws IllegalStateException if already returned
     * @throws IllegalArgumentException if the date is missing or precedes checkout
     */
    public void markReturned(LocalDate date) {
        if (!isActive()) {
            throw new IllegalStateException("This loan is already returned.");
        }

        if (date == null || date.isBefore(checkoutDate)) {
            throw new IllegalArgumentException("Return date cannot be missing or before checkout.");
        }

        returnDate = date;
    }

    @Override
    public String toString() {
        return copy.copyId() + " borrowed by " + member.getName() + " | Due: " + dueDate
                + " | " + (isActive() ? "Active" : "Returned: " + returnDate);
    }
}
