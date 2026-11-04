package com.daniel.library.persistence;

import com.daniel.library.model.Book;
import com.daniel.library.model.BookCopy;
import com.daniel.library.model.Loan;
import com.daniel.library.model.Member;

import java.time.LocalDate;

/**
 * Low-level record storage shared by temporary and MySQL implementations.
 * Application callers should use Library for validation and lending rules.
 * A failed write must not mutate domain objects to suggest success.
 * Storage failures are reported as IllegalStateException; there is no silent memory fallback.
 */
public interface LibraryRepository {
    /**
     * Loads related records with copies and loans linked to the loaded catalog objects.
     *
     * @return read-only record lists; contained domain objects can still be mutable
     * @throws IllegalStateException if storage cannot be read
     */
    LibraryData load();

    /**
     * Stores one new title after service validation.
     *
     * @param book title to insert
     */
    void addBook(Book book);

    /**
     * Replaces title and author while preserving stored identity and archive state.
     *
     * @param book validated replacement details for an existing ISBN
     */
    void updateBook(Book book);

    /**
     * Stores archive state without deleting catalog or history.
     *
     * @param isbn     existing catalog identifier
     * @param archived desired archive flag
     */
    void setBookArchived(String isbn, boolean archived);

    /**
     * Stores one physical copy referencing an existing title.
     *
     * @param copy physical item to insert
     */
    void addCopy(BookCopy copy);

    /**
     * Stores one registered borrower.
     *
     * @param member borrower to insert
     */
    void addMember(Member member);

    /**
     * Replaces contact fields while preserving the member ID.
     *
     * @param member validated replacement details
     */
    void updateMember(Member member);

    /**
     * Stores a borrowing record, including its return date for historical/sample records.
     *
     * @param loan new active or returned record
     */
    void addLoan(Loan loan);

    /**
     * Saves the return before marking the supplied loan returned; failure leaves it unchanged.
     *
     * @param loan       active loan to complete
     * @param returnDate date on or after checkout
     */
    void returnLoan(Loan loan, LocalDate returnDate);
}
