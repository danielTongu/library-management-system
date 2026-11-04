package com.daniel.library.demo;

import com.daniel.library.model.Book;
import com.daniel.library.model.BookCopy;
import com.daniel.library.model.Loan;
import com.daniel.library.model.Member;
import com.daniel.library.persistence.InMemoryLibraryRepository;
import com.daniel.library.persistence.LibraryData;
import com.daniel.library.persistence.LibraryRepository;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * One sample dataset for temporary and MySQL-backed libraries.
 */
public final class SampleData {
    private SampleData() {
    }

    /**
     * {@return fresh, independent example records in temporary storage}
     */
    public static InMemoryLibraryRepository repository() {
        InMemoryLibraryRepository repository = new InMemoryLibraryRepository();

        repository.addBook(new Book("9780134685991", "Effective Java", "Joshua Bloch"));
        repository.addBook(new Book("9780596009205", "Head First Java", "Kathy Sierra and Bert Bates"));
        repository.addBook(new Book("9780132350884", "Clean Code", "Robert C. Martin"));
        repository.addMember(new Member("S-M001", "Daniel", "daniel@example.com"));
        repository.addMember(new Member("S-M002", "Alex Morgan", "alex@example.com"));
        repository.addMember(new Member("S-M003", "Sam Rivera", "sam@example.com"));

        var books = repository.load().books();

        repository.addCopy(new BookCopy("S-C001", books.get(0)));
        repository.addCopy(new BookCopy("S-C002", books.get(0)));
        repository.addCopy(new BookCopy("S-C003", books.get(1)));
        repository.addCopy(new BookCopy("S-C004", books.get(2)));

        LibraryData data = repository.load();

        Loan active = new Loan("sample-loan-active", data.copies().get(0), data.members().get(0),
                LocalDate.of(2026, 11, 1), LocalDate.of(2026, 11, 15));
        Loan overdue = new Loan("sample-loan-overdue", data.copies().get(2), data.members().get(1),
                LocalDate.of(2026, 10, 10), LocalDate.of(2026, 10, 24));
        Loan returned = new Loan("sample-loan-returned", data.copies().get(3), data.members().get(2),
                LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 15));
        returned.markReturned(LocalDate.of(2026, 10, 18));
        repository.addLoan(active);
        repository.addLoan(overdue);
        repository.addLoan(returned);

        return repository;
    }

    /**
     * Adds missing examples without overwriting edits, archive flags, or borrowing history.
     * Conflicting copy identities and active borrowing are skipped. Writes are individual;
     * a retry can resume after partial failure.
     *
     * @param target repository to populate
     * @throws IllegalStateException if a storage operation fails
     */
    public static void populate(LibraryRepository target) {
        LibraryData samples = repository().load();
        LibraryData existing = target.load();

        Map<String, Book> books = new LinkedHashMap<>();
        Map<String, Member> members = new LinkedHashMap<>();
        Map<String, BookCopy> copies = new LinkedHashMap<>();

        Set<String> loanIds = new HashSet<>();
        Set<String> borrowedCopies = new HashSet<>();

        existing.books().forEach(book -> books.put(book.getIsbn(), book));
        existing.members().forEach(member -> members.put(member.getMemberId(), member));
        existing.copies().forEach(copy -> copies.put(copy.copyId(), copy));

        existing.loans().forEach(loan -> {
            loanIds.add(loan.getLoanId());
            if (loan.isActive()) {
                borrowedCopies.add(loan.getCopy().copyId());
            }
        });

        for (Book book : samples.books()) {
            if (!books.containsKey(book.getIsbn())) {
                target.addBook(book);
                books.put(book.getIsbn(), book);
            }
        }
        for (Member member : samples.members()) {
            if (!members.containsKey(member.getMemberId())) {
                target.addMember(member);
                members.put(member.getMemberId(), member);
            }
        }
        for (BookCopy copy : samples.copies()) {
            Book book = books.get(copy.book().getIsbn());
            if (!copies.containsKey(copy.copyId()) && !book.isArchived()) {
                BookCopy added = new BookCopy(copy.copyId(), book);
                target.addCopy(added);
                copies.put(added.copyId(), added);
            }
        }
        for (Loan sample : samples.loans()) {
            BookCopy copy = copies.get(sample.getCopy().copyId());
            if (loanIds.contains(sample.getLoanId()) || copy == null
                    || !copy.book().getIsbn().equals(sample.getCopy().book().getIsbn())) {
                continue;
            }
            if (sample.isActive() && (copy.book().isArchived() || borrowedCopies.contains(copy.copyId()))) {
                continue;
            }
            Member member = members.get(sample.getMember().getMemberId());
            Loan added = new Loan(sample.getLoanId(), copy, member, sample.getCheckoutDate(), sample.getDueDate());
            if (!sample.isActive()) {
                added.markReturned(sample.getReturnDate());
            }
            target.addLoan(added);
            loanIds.add(added.getLoanId());
            if (added.isActive()) {
                borrowedCopies.add(copy.copyId());
            }
        }
    }
}
