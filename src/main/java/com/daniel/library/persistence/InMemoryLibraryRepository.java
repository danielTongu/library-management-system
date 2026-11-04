package com.daniel.library.persistence;

import com.daniel.library.model.Book;
import com.daniel.library.model.BookCopy;
import com.daniel.library.model.Loan;
import com.daniel.library.model.Member;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Temporary, non-thread-safe storage using shared domain objects and copied record lists.
 * It does not save across restarts. Service validation is required before writes.
 */
public class InMemoryLibraryRepository implements LibraryRepository {
    private final List<Book> books = new ArrayList<>();
    private final List<BookCopy> copies = new ArrayList<>();
    private final List<Member> members = new ArrayList<>();
    private final List<Loan> loans = new ArrayList<>();

    /**
     * Creates empty temporary storage.
     */
    public InMemoryLibraryRepository() {
    }

    @Override
    public LibraryData load() {
        return new LibraryData(List.copyOf(books), List.copyOf(copies), List.copyOf(members), List.copyOf(loans));
    }

    @Override
    public void addBook(Book book) {
        books.add(book);
    }

    @Override
    public void updateBook(Book book) {
        Book stored = books.stream().filter(entry -> entry.getIsbn().equals(book.getIsbn())).findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Book not found."));
        stored.updateDetails(book.getTitle(), book.getAuthor());
    }

    @Override
    public void setBookArchived(String isbn, boolean archived) {
        Book book = books.stream().filter(entry -> entry.getIsbn().equals(isbn)).findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Book not found."));
        book.setArchived(archived);
    }

    @Override
    public void addCopy(BookCopy copy) {
        copies.add(copy);
    }

    @Override
    public void addMember(Member member) {
        members.add(member);
    }

    @Override
    public void updateMember(Member member) {
        Member stored = members.stream().filter(entry -> entry.getMemberId().equals(member.getMemberId())).findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Member not found."));
        stored.updateDetails(member.getName(), member.getEmail());
    }

    @Override
    public void addLoan(Loan loan) {
        loans.add(loan);
    }

    @Override
    public void returnLoan(Loan loan, LocalDate returnDate) {
        loan.markReturned(returnDate);
    }
}
