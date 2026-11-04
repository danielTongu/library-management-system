package com.daniel.library.model;

/** Shared model validation; identifiers retain their exact spelling and case. */
final class Validation {
    private Validation() {
    }

    static void requireText(String value, String label) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(label + " is required.");
        }
    }
}
