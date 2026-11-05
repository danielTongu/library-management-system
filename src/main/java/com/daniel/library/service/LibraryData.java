package com.daniel.library.service;

import com.daniel.library.model.Book;
import com.daniel.library.model.BookCopy;
import com.daniel.library.model.Loan;
import com.daniel.library.model.Member;

import java.util.List;

/**
 * Related records returned by one repository read. Lists must be read-only; objects are not deep copies.
 *
 * @param books   catalog titles, including archived entries
 * @param copies  physical items referencing the loaded books
 * @param members registered borrowers
 * @param loans   history referencing the loaded copies and members
 */
public record LibraryData(
        List<Book> books,
        List<BookCopy> copies,
        List<Member> members,
        List<Loan> loans
) {
}
