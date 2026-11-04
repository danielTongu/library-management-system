package com.daniel.library.service;

import com.daniel.library.model.Book;
import com.daniel.library.model.BookCopy;
import com.daniel.library.model.Loan;
import com.daniel.library.model.Member;
import com.daniel.library.persistence.InMemoryLibraryRepository;
import com.daniel.library.persistence.LibraryRepository;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Application API for catalog, membership, and lending workflows.
 * Repositories own records; this service validates operations before requesting writes.
 * Instances are intended for one serialized desktop workflow, not concurrent writers.
 */
public class Library {
    private static final int LOAN_PERIOD_DAYS = 14;
    private static final BigDecimal DEFAULT_DAILY_FINE_RATE = new BigDecimal("0.50");

    private final Clock clock;
    private final BigDecimal dailyFineRate;
    private final LibraryRepository repository;

    /**
     * Creates an empty temporary library using the default clock and a 0.50 daily fine rate.
     */
    public Library() {
        this(DEFAULT_DAILY_FINE_RATE, Clock.systemDefaultZone());
    }

    /**
     * Creates a temporary library with explicit time and fine policy.
     *
     * @param dailyFineRate nonnegative charge per overdue day
     * @param clock         clock used for checkout, return, and reporting dates
     * @throws IllegalArgumentException if the rate is invalid
     * @throws NullPointerException     if the clock is missing
     */
    public Library(BigDecimal dailyFineRate, Clock clock) {
        this(dailyFineRate, clock, new InMemoryLibraryRepository());
    }

    /**
     * Uses the supplied repository with the default clock and fine rate.
     *
     * @param repository storage implementation
     * @throws NullPointerException if the repository is missing
     */
    public Library(LibraryRepository repository) {
        this(DEFAULT_DAILY_FINE_RATE, Clock.systemDefaultZone(), Objects.requireNonNull(repository));
    }

    /**
     * Creates a library with explicit policy and storage.
     *
     * @param dailyFineRate nonnegative charge per overdue day
     * @param clock         source of application dates
     * @param repository    storage implementation
     * @throws IllegalArgumentException if the rate is invalid
     * @throws NullPointerException     if the clock or repository is missing
     */
    public Library(BigDecimal dailyFineRate, Clock clock, LibraryRepository repository) {
        if (dailyFineRate == null || dailyFineRate.signum() < 0) {
            throw new IllegalArgumentException("Daily fine rate cannot be missing or negative.");
        }
        this.dailyFineRate = dailyFineRate;
        this.clock = Objects.requireNonNull(clock, "Clock is required.");
        this.repository = Objects.requireNonNull(repository, "Repository is required.");
    }

    /**
     * Adds a catalog title with a unique ISBN.
     *
     * @param book validated catalog title
     * @throws IllegalArgumentException if missing or its ISBN already exists
     */
    public void addBook(Book book) {
        if (book == null) {
            throw new IllegalArgumentException("Book cannot be null.");
        }

        if (findBookByIsbn(book.getIsbn()).isPresent()) {
            throw new IllegalArgumentException("A book with this ISBN already exists.");
        }

        repository.addBook(book);
    }

    /**
     * {@return all catalog titles, including archived titles, in a read-only list}
     */
    public List<Book> getBooks() {
        return repository.load().books();
    }

    /**
     * Replaces title and author while retaining the ISBN and archive state.
     *
     * @param isbn   existing catalog identifier
     * @param title  nonblank replacement title
     * @param author nonblank replacement author
     * @throws IllegalArgumentException if the book or replacement fields are invalid
     */
    public void updateBook(String isbn, String title, String author) {
        if (findBookByIsbn(isbn).isEmpty()) {
            throw new IllegalArgumentException("Book not found.");
        }
        repository.updateBook(new Book(isbn, title, author));
    }

    /**
     * Stops lending a title while retaining its copies and loan history.
     *
     * @param isbn existing catalog identifier
     * @throws IllegalArgumentException if the title is unknown
     * @throws IllegalStateException    if any copy is still borrowed
     */
    public void archiveBook(String isbn) {
        Book book = findBookByIsbn(isbn).orElseThrow(() -> new IllegalArgumentException("Book not found."));
        if (getLoans().stream().anyMatch(loan -> loan.isActive() && loan.getCopy().book().getIsbn().equals(isbn))) {
            throw new IllegalStateException("Return all borrowed copies of this book before archiving it.");
        }
        if (!book.isArchived()) {
            repository.setBookArchived(isbn, true);
        }
    }

    /**
     * Re-enables lending for a previously archived title.
     *
     * @param isbn existing catalog identifier
     * @throws IllegalArgumentException if the title is unknown
     */
    public void restoreBook(String isbn) {
        Book book = findBookByIsbn(isbn).orElseThrow(() -> new IllegalArgumentException("Book not found."));
        if (book.isArchived()) {
            repository.setBookArchived(isbn, false);
        }
    }

    /**
     * Looks up an exact, case-sensitive ISBN.
     *
     * @param isbn identifier to find
     * @return the title, or an empty result if absent
     */
    public Optional<Book> findBookByIsbn(String isbn) {
        for (Book book : getBooks()) {
            if (book.getIsbn().equals(isbn)) {
                return Optional.of(book);
            }
        }

        return Optional.empty();
    }

    /**
     * Adds a uniquely identified physical item to an active catalog title.
     *
     * @param copyId nonblank, unused physical-item identifier
     * @param isbn   existing catalog identifier
     * @throws IllegalArgumentException if an identifier is invalid, duplicated, or unknown
     * @throws IllegalStateException    if the title is archived
     */
    public void addCopy(String copyId, String isbn) {
        if (findCopyById(copyId).isPresent()) {
            throw new IllegalArgumentException("A copy with this ID already exists.");
        }

        Book book = findBookByIsbn(isbn).orElseThrow(() -> new IllegalArgumentException(
                "Add the book to the catalog first."));
        if (book.isArchived()) {
            throw new IllegalStateException("Restore this book before adding copies.");
        }

        BookCopy copy = new BookCopy(copyId, book);
        repository.addCopy(copy);
    }

    /**
     * {@return all physical items, including copies of archived titles, in a read-only list}
     */
    public List<BookCopy> getCopies() {
        return repository.load().copies();
    }

    /**
     * Checks whether a physical item belongs to an active title and is not borrowed.
     *
     * @param copyId existing physical-item identifier
     * @return true when the item can be checked out
     * @throws IllegalArgumentException if the item is unknown
     */
    public boolean isCopyAvailable(String copyId) {
        LibrarySnapshot snapshot = getSnapshot();
        BookCopy copy = snapshot.records().copies().stream().filter(entry -> entry.copyId().equals(copyId))
                .findFirst().orElseThrow(() -> new IllegalArgumentException("Copy not found."));
        return snapshot.isAvailable(copy);
    }

    /**
     * Finds borrowable copies of one title using a single reporting snapshot.
     *
     * @param isbn existing catalog identifier
     * @return available copies; empty for an archived title or no available items
     * @throws IllegalArgumentException if the title is unknown
     */
    public List<BookCopy> getAvailableCopies(String isbn) {
        LibrarySnapshot snapshot = getSnapshot();
        if (snapshot.records().books().stream().noneMatch(book -> book.getIsbn().equals(isbn))) {
            throw new IllegalArgumentException("Book not found.");
        }
        return snapshot.records().copies().stream().filter(copy -> copy.book().getIsbn().equals(isbn))
                .filter(snapshot::isAvailable).toList();
    }

    /**
     * Looks up an exact, case-sensitive copy ID.
     *
     * @param copyId identifier to find
     * @return the physical item, or an empty result if absent
     */
    public Optional<BookCopy> findCopyById(String copyId) {
        for (BookCopy copy : getCopies()) {
            if (copy.copyId().equals(copyId)) {
                return Optional.of(copy);
            }
        }

        return Optional.empty();
    }

    /**
     * Registers a borrower with a unique member ID.
     *
     * @param member validated borrower
     * @throws IllegalArgumentException if missing or the member ID is already registered
     */
    public void registerMember(Member member) {
        if (member == null) {
            throw new IllegalArgumentException("Member cannot be null.");
        }

        if (findMemberById(member.getMemberId()).isPresent()) {
            throw new IllegalArgumentException("A member with this ID already exists.");
        }

        repository.addMember(member);
    }

    /**
     * {@return registered borrowers in a read-only list}
     */
    public List<Member> getMembers() {
        return repository.load().members();
    }

    /**
     * Replaces a member name and email without changing identity or loan history.
     *
     * @param memberId existing borrower identifier
     * @param name     nonblank replacement name
     * @param email    replacement address in the supported basic format
     * @throws IllegalArgumentException if the member or contact fields are invalid
     */
    public void updateMember(String memberId, String name, String email) {
        if (findMemberById(memberId).isEmpty()) {
            throw new IllegalArgumentException("Member not found.");
        }
        repository.updateMember(new Member(memberId, name, email));
    }

    /**
     * Looks up an exact, case-sensitive member ID.
     *
     * @param memberId identifier to find
     * @return the borrower, or an empty result if absent
     */
    public Optional<Member> findMemberById(String memberId) {
        for (Member member : getMembers()) {
            if (member.getMemberId().equals(memberId)) {
                return Optional.of(member);
            }
        }

        return Optional.empty();
    }

    /**
     * Creates an active loan due fourteen calendar days after today.
     *
     * @param copyId   existing, available physical-item identifier
     * @param memberId registered borrower identifier
     * @return the newly saved loan
     * @throws IllegalArgumentException if either participant is unknown
     * @throws IllegalStateException    if the item is borrowed, archived, or storage rejects the write
     */
    public Loan checkoutBook(String copyId, String memberId) {
        BookCopy copy = findCopyById(copyId).orElseThrow(() -> new IllegalArgumentException("Copy not found."));
        if (copy.book().isArchived()) {
            throw new IllegalStateException("Restore this book before checking out its copies.");
        }

        Member member = findMemberById(memberId).orElseThrow(() -> new IllegalArgumentException("Member not found."));

        if (findActiveLoan(copyId).isPresent()) {
            throw new IllegalStateException("This copy is already checked out.");
        }

        LocalDate today = LocalDate.now(clock);
        Loan loan = new Loan(copy, member, today, today.plusDays(LOAN_PERIOD_DAYS));
        repository.addLoan(loan);

        return loan;
    }

    /**
     * Saves a return before updating the returned loan object.
     *
     * @param copyId physical item with an active loan
     * @return the completed loan
     * @throws IllegalStateException    if no active loan exists or storage rejects the write
     * @throws IllegalArgumentException if the clock would produce a return before checkout
     */
    public Loan returnBook(String copyId) {
        Loan loan = findActiveLoan(copyId)
                .orElseThrow(() -> new IllegalStateException("No active loan found for this copy."));
        LocalDate today = LocalDate.now(clock);
        if (today.isBefore(loan.getCheckoutDate())) {
            throw new IllegalArgumentException("Return date cannot be before checkout.");
        }
        repository.returnLoan(loan, today);
        return loan;
    }

    /**
     * {@return active and returned loan history in a read-only list}
     */
    public List<Loan> getLoans() {
        return repository.load().loans();
    }

    /**
     * Lists current borrowing, including overdue items, for a registered member.
     *
     * @param memberId existing borrower identifier
     * @return active loans for that member
     * @throws IllegalArgumentException if the member is unknown
     */
    public List<Loan> getActiveLoansForMember(String memberId) {
        if (findMemberById(memberId).isEmpty()) {
            throw new IllegalArgumentException("Member not found.");
        }
        return getLoans().stream()
                .filter(loan -> loan.isActive() && loan.getMember().getMemberId().equals(memberId))
                .toList();
    }

    private Optional<Loan> findActiveLoan(String copyId) {
        return getLoans().stream()
                .filter(loan -> loan.isActive() && loan.getCopy().copyId().equals(copyId))
                .findFirst();
    }

    /**
     * {@return the configured daily charge per overdue day}
     */
    public BigDecimal getDailyFineRate() {
        return dailyFineRate;
    }

    /**
     * Resolves a loan by stable ID before calculating its current late charge.
     *
     * @param loan loan whose identifier belongs to this library
     * @return current charge, capped at the stored return date
     * @throws IllegalArgumentException if the loan is missing or unknown
     */
    public BigDecimal calculateFine(Loan loan) {
        if (loan == null) {
            throw new IllegalArgumentException("Loan is required.");
        }
        LibrarySnapshot snapshot = getSnapshot();
        Loan stored = snapshot.records().loans().stream().filter(entry -> entry.getLoanId().equals(loan.getLoanId())).findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Loan does not belong to this library."));
        return snapshot.calculateFine(stored);
    }

    /**
     * {@return currently borrowed loans that are late on the library clock date}
     */
    public List<Loan> getOverdueLoans() {
        LibrarySnapshot snapshot = getSnapshot();
        return snapshot.records().loans().stream().filter(snapshot::isOverdue).toList();
    }

    /**
     * {@return records and reporting policy captured with one repository read and one clock date}
     */
    public LibrarySnapshot getSnapshot() {
        return new LibrarySnapshot(repository.load(), LocalDate.now(clock), dailyFineRate);
    }

    /**
     * {@return overview totals calculated from one snapshot}
     */
    public LibrarySummary getSummary() {
        return getSnapshot().summary();
    }

}
