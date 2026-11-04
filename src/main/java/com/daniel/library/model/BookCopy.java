package com.daniel.library.model;

/**
 * One physical item with immutable identity/reference components.
 * The referenced Book remains mutable so catalog edits are visible through existing copies.
 * @param copyId nonblank physical-item identifier
 * @param book existing catalog title
 */
public record BookCopy(String copyId, Book book) {
    /**
     * Creates a physical copy linked to its catalog title.
     * @param copyId nonblank physical-item identifier
     * @param book existing catalog title
     * @throws IllegalArgumentException if the ID is blank or the book is missing
     */
    public BookCopy {
        Validation.requireText(copyId, "Copy ID");
        if (book == null) {
            throw new IllegalArgumentException("Book is required.");
        }
    }

    @Override
    public String toString() {
        return copyId + ": " + book;
    }
}
