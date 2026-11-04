package com.daniel.library.model;

import java.util.regex.Pattern;

/**
 * A registered borrower with a stable ID and editable contact details.
 * Email validation checks basic syntax only; it does not verify delivery.
 */
public class Member {
    private static final Pattern EMAIL_PATTERN = Pattern.compile("[^\\s@]+@[^\\s@]+\\.[^\\s@]+");

    private final String memberId;
    private String name;
    private String email;

    /**
     * Creates a borrower with validated contact details.
     * @param memberId nonblank, stable borrower identifier
     * @param name nonblank display name
     * @param email address in the supported basic email format
     * @throws IllegalArgumentException if an identifier or contact field is invalid
     */
    public Member(String memberId, String name, String email) {
        Validation.requireText(memberId, "Member ID");
        this.memberId = memberId;
        updateDetails(name, email);
    }

    /**
     * Validates both contact fields before changing either value.
     * @param name nonblank display name
     * @param email address in the supported basic email format
     * @throws IllegalArgumentException if either replacement field is invalid
     */
    public void updateDetails(String name, String email) {
        Validation.requireText(name, "Name");
        Validation.requireText(email, "Email");
        if (!EMAIL_PATTERN.matcher(email).matches()) {
            throw new IllegalArgumentException("Email must have a format such as name@example.com.");
        }
        this.name = name;
        this.email = email;
    }

    /** {@return the stable borrower identifier} */
    public String getMemberId() {
        return memberId;
    }

    /** {@return the current display name} */
    public String getName() {
        return name;
    }

    /** {@return the current email address} */
    public String getEmail() {
        return email;
    }

    @Override
    public String toString() {
        return memberId + ": " + name + " (" + email + ")";
    }
}
