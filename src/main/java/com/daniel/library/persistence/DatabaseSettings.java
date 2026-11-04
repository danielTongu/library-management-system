package com.daniel.library.persistence;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Map;

/**
 * Runtime JDBC configuration. Credentials are intentionally hidden from toString().
 *
 * @param url      MySQL JDBC connection URL
 * @param username database account name
 * @param password account password; callers must not log or commit it
 */
public record DatabaseSettings(String url, String username, String password) {
    /**
     * Reads LIBRARY_DB_URL, LIBRARY_DB_USER, and LIBRARY_DB_PASSWORD.
     * URL defaults to localhost/library_management and user to library_user.
     *
     * @return configured connection settings
     * @throws IllegalArgumentException if the password is missing or blank
     */
    public static DatabaseSettings fromEnvironment() {
        Map<String, String> env = System.getenv();
        String url = env.getOrDefault("LIBRARY_DB_URL", "jdbc:mysql://localhost:3306/library_management");
        String username = env.getOrDefault("LIBRARY_DB_USER", "library_user");
        String password = env.get("LIBRARY_DB_PASSWORD");

        if (password == null || password.isBlank()) {
            throw new IllegalArgumentException("Set LIBRARY_DB_PASSWORD in the IntelliJ run configuration.");
        }

        return new DatabaseSettings(url, username, password);
    }

    /**
     * Opens a connection that the caller must close.
     *
     * @return new JDBC connection
     * @throws SQLException if no driver is available or the server rejects the connection
     */
    public Connection openConnection() throws SQLException {
        return DriverManager.getConnection(url, username, password);
    }

    @Override
    public String toString() {
        return "DatabaseSettings[credentials hidden]";
    }
}
