package com.daniel.library.model;

/**
 * A catalog title with a stable ISBN and editable bibliographic details.
 * Physical copies refer to this object. Archive transitions should go through
 * {@link com.daniel.library.service.Library}, which checks active borrowing first.
 */
public class Book {
    private final String isbn;
    private String title;
    private String author;
    private boolean archived;

    /**
     * Creates an active catalog title. ISBNs are required text, not checksum-validated.
     * @param isbn stable, case-sensitive catalog identifier
     * @param title nonblank title
     * @param author nonblank author
     * @throws IllegalArgumentException if a required field is missing or blank
     */
    public Book(String isbn, String title, String author) {
        Validation.requireText(isbn, "ISBN");
        this.isbn = isbn;
        updateDetails(title, author);
    }

    /**
     * Validates both replacement fields before changing either value.
     * @param title nonblank title
     * @param author nonblank author
     * @throws IllegalArgumentException if either replacement is missing or blank
     */
    public void updateDetails(String title, String author) {
        Validation.requireText(title, "Title");
        Validation.requireText(author, "Author");
        this.title = title;
        this.author = author;
    }

    /** {@return the stable ISBN} */
    public String getIsbn() {
        return isbn;
    }

    /** {@return the current title} */
    public String getTitle() {
        return title;
    }

    /** {@return the current author} */
    public String getAuthor() {
        return author;
    }

    /** {@return whether the title is unavailable for lending} */
    public boolean isArchived() {
        return archived;
    }

    /**
     * Sets the stored archive flag without applying lending rules or saving to storage.
     * Use the Library service to archive or restore a title in the application.
     * @param archived true to mark the title archived
     */
    public void setArchived(boolean archived) {
        this.archived = archived;
    }

    @Override
    public String toString() {
        return title + " by " + author + " (ISBN: " + isbn + ")";
    }
}
