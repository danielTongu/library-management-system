package com.daniel.library.service;

import com.daniel.library.model.Book;
import com.daniel.library.model.BookCopy;
import com.daniel.library.model.Loan;
import com.daniel.library.persistence.LibraryData;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

/**
 * A reporting view from one repository read and one calculation date.
 * Lists are read-only, but domain objects are shared in memory and must not be mutated while rendering.
 *
 * @param records       related catalog, member, and loan records
 * @param asOfDate      date used consistently throughout the report
 * @param dailyFineRate nonnegative monetary charge per overdue day
 */
public record LibrarySnapshot(LibraryData records, LocalDate asOfDate, BigDecimal dailyFineRate) {
    /**
     * Validates the reporting policy without reading storage.
     *
     * @param records       related records from one repository read
     * @param asOfDate      reporting date
     * @param dailyFineRate nonnegative monetary charge per overdue day
     * @throws NullPointerException     if records or date are missing
     * @throws IllegalArgumentException if the fine rate is missing or negative
     */
    public LibrarySnapshot {
        Objects.requireNonNull(records, "Records are required.");
        Objects.requireNonNull(asOfDate, "Reporting date is required.");
        if (dailyFineRate == null || dailyFineRate.signum() < 0) {
            throw new IllegalArgumentException("Daily fine rate cannot be missing or negative.");
        }
    }

    /**
     * Tests lending availability within these records.
     *
     * @param copy physical copy belonging to these records
     * @return true when its title is active and no active loan refers to the copy
     */
    public boolean isAvailable(BookCopy copy) {
        return !copy.book().isArchived() && records.loans().stream()
                .noneMatch(loan -> loan.isActive() && loan.getCopy().copyId().equals(copy.copyId()));
    }

    /**
     * Tests whether a loan is still borrowed and late on the report date.
     *
     * @param loan loan from these records
     * @return true for an unreturned overdue loan
     */
    public boolean isOverdue(Loan loan) {
        return loan.isOverdue(asOfDate);
    }

    /**
     * Calculates a loan's late charge using the report date and rate.
     *
     * @param loan loan from these records
     * @return monetary charge rounded to two decimal places
     */
    public BigDecimal calculateFine(Loan loan) {
        return loan.calculateFine(asOfDate, dailyFineRate);
    }

    /**
     * {@return totals computed from the same records, date, and fine rate}
     */
    public LibrarySummary summary() {
        Set<String> borrowedCopies = new HashSet<>();

        int current = 0;
        int overdue = 0;

        BigDecimal currentCharges = new BigDecimal("0.00");
        BigDecimal returnedCharges = new BigDecimal("0.00");

        for (Loan loan : records.loans()) {
            BigDecimal charge = loan.calculateFine(asOfDate, dailyFineRate);
            if (loan.isActive()) {
                current++;
                borrowedCopies.add(loan.getCopy().copyId());
                currentCharges = currentCharges.add(charge);
                if (loan.isOverdue(asOfDate)) {
                    overdue++;
                }
            } else {
                returnedCharges = returnedCharges.add(charge);
            }
        }

        int archivedTitles = (int) records.books().stream().filter(Book::isArchived).count();
        int archivedCopies = (int) records.copies().stream().filter(copy -> copy.book().isArchived()).count();
        int available = (int) records.copies().stream().filter(copy -> !copy.book().isArchived() && !borrowedCopies.contains(copy.copyId())).count();

        return new LibrarySummary(
                asOfDate,
                records.books().size(),
                archivedTitles,
                records.copies().size(),
                available,
                archivedCopies,
                records.members().size(),
                current,
                overdue,
                records.loans().size() - current,
                currentCharges,
                returnedCharges
        );
    }
}
