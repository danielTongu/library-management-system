package com.daniel.library.service;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Overview totals for one reporting date; charges do not represent payments or balances.
 *
 * @param asOfDate            calculation date
 * @param bookTitles          total titles, including archived titles
 * @param archivedTitles      archived title count
 * @param physicalCopies      all physical items
 * @param availableCopies     unborrowed copies of active titles
 * @param archivedCopies      items belonging to archived titles
 * @param members             registered borrower count
 * @param currentLoans        active borrowing, including overdue loans
 * @param overdueLoans        active loans past their due date
 * @param returnedLoans       completed borrowing records
 * @param currentLateCharges  late charges on active borrowing
 * @param returnedLateCharges charges capped at each recorded return date
 */
public record LibrarySummary(
        LocalDate asOfDate,
        int bookTitles,
        int archivedTitles,
        int physicalCopies,
        int availableCopies,
        int archivedCopies,
        int members,
        int currentLoans,
        int overdueLoans,
        int returnedLoans,
        BigDecimal currentLateCharges,
        BigDecimal returnedLateCharges
) {
}
