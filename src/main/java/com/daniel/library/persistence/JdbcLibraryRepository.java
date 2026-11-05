package com.daniel.library.persistence;

import com.daniel.library.model.Book;
import com.daniel.library.model.BookCopy;
import com.daniel.library.model.Loan;
import com.daniel.library.model.Member;
import com.daniel.library.service.LibraryData;
import com.daniel.library.service.LibraryRepository;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * MySQL storage using prepared statements, repeatable-read snapshots, and short-lived connections.
 * Writes are committed individually; multi-step sample population is resumable rather than atomic.
 */
public class JdbcLibraryRepository implements LibraryRepository {
    private final JdbcConnectionSettings settings;

    /**
     * Configures storage without opening a connection.
     *
     * @param settings connection URL and credentials
     */
    public JdbcLibraryRepository(JdbcConnectionSettings settings) {
        this.settings = settings;
    }

    /**
     * Creates missing tables from the bundled database/schema.sql resource.
     * The database and account must already exist; this does not migrate or delete existing tables.
     *
     * @throws IllegalStateException if the resource or MySQL server is unavailable
     */
    public void initializeSchema() {
        try (InputStream input = getClass().getResourceAsStream("/database/schema.sql")) {
            if (input == null) {
                throw new IllegalStateException("Database schema resource is missing.");
            }
            String schema = new String(input.readAllBytes(), StandardCharsets.UTF_8);
            try (Connection connection = settings.openConnection(); Statement statement = connection.createStatement()) {
                for (String sql : schema.split(";")) {
                    // The bundled schema is plain DDL, not a general SQL script or stored-routine runner.
                    if (!sql.isBlank()) {
                        statement.execute(sql);
                    }
                }
            }
        } catch (IOException exception) {
            throw new IllegalStateException("Unable to read database schema.", exception);
        } catch (SQLException exception) {
            throw databaseFailure(exception);
        }
    }

    @Override
    public LibraryData load() {
        try (Connection connection = settings.openConnection()) {
            connection.setTransactionIsolation(Connection.TRANSACTION_REPEATABLE_READ);
            connection.setAutoCommit(false);
            try {
                LibraryData data = readData(connection);
                connection.commit();
                return data;
            } catch (SQLException | RuntimeException exception) {
                connection.rollback();
                throw exception;
            }
        } catch (SQLException exception) {
            throw databaseFailure(exception);
        }
    }

    private LibraryData readData(Connection connection) throws SQLException {
        Map<String, Book> books = new LinkedHashMap<>();
        Map<String, Member> members = new LinkedHashMap<>();
        Map<String, BookCopy> copies = new LinkedHashMap<>();
        List<Loan> loans = new ArrayList<>();

        try (Statement statement = connection.createStatement()) {
            String catalogSql = "SELECT b.isbn, b.title, b.author, COALESCE(a.archived, FALSE) AS archived FROM books b LEFT JOIN book_archive a ON a.isbn = b.isbn ORDER BY b.isbn";
            try (ResultSet rows = statement.executeQuery(catalogSql)) {
                while (rows.next()) {
                    Book book = new Book(rows.getString("isbn"), rows.getString("title"), rows.getString("author"));
                    book.setArchived(rows.getBoolean("archived"));
                    books.put(book.getIsbn(), book);
                }
            }
            try (ResultSet rows = statement.executeQuery("SELECT member_id, name, email FROM members ORDER BY member_id")) {
                while (rows.next()) {
                    Member member = new Member(rows.getString("member_id"), rows.getString("name"), rows.getString("email"));
                    members.put(member.getMemberId(), member);
                }
            }
            try (ResultSet rows = statement.executeQuery("SELECT copy_id, isbn FROM book_copies ORDER BY copy_id")) {
                while (rows.next()) {
                    BookCopy copy = new BookCopy(rows.getString("copy_id"), books.get(rows.getString("isbn")));
                    copies.put(copy.copyId(), copy);
                }
            }
            try (ResultSet rows = statement.executeQuery("SELECT * FROM loans ORDER BY checkout_date, loan_id")) {
                while (rows.next()) {
                    Loan loan = new Loan(
                            rows.getString("loan_id"),
                            copies.get(rows.getString("copy_id")),
                            members.get(rows.getString("member_id")),
                            rows.getDate("checkout_date").toLocalDate(),
                            rows.getDate("due_date").toLocalDate()
                    );

                    Date returned = rows.getDate("return_date");

                    if (returned != null) {
                        loan.markReturned(returned.toLocalDate());
                    }
                    loans.add(loan);
                }
            }
        }
        return new LibraryData(List.copyOf(books.values()), List.copyOf(copies.values()), List.copyOf(members.values()), List.copyOf(loans));
    }

    @Override
    public void addBook(Book book) {
        execute("INSERT INTO books (isbn, title, author) VALUES (?, ?, ?)", statement -> {
            statement.setString(1, book.getIsbn());
            statement.setString(2, book.getTitle());
            statement.setString(3, book.getAuthor());
        });
    }

    @Override
    public void setBookArchived(String isbn, boolean archived) {
        String sql = "INSERT INTO book_archive (isbn, archived) VALUES (?, ?) ON DUPLICATE KEY UPDATE archived = ?";
        try (Connection connection = settings.openConnection(); PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, isbn);
            statement.setBoolean(2, archived);
            statement.setBoolean(3, archived);
            statement.executeUpdate();
        } catch (SQLException exception) {
            throw databaseFailure(exception);
        }
    }

    @Override
    public void updateBook(Book book) {
        execute("UPDATE books SET title = ?, author = ? WHERE isbn = ?", statement -> {
            statement.setString(1, book.getTitle());
            statement.setString(2, book.getAuthor());
            statement.setString(3, book.getIsbn());
        });
    }

    @Override
    public void updateMember(Member member) {
        execute("UPDATE members SET name = ?, email = ? WHERE member_id = ?", statement -> {
            statement.setString(1, member.getName());
            statement.setString(2, member.getEmail());
            statement.setString(3, member.getMemberId());
        });
    }

    @Override
    public void addCopy(BookCopy copy) {
        execute("INSERT INTO book_copies (copy_id, isbn) VALUES (?, ?)", statement -> {
            statement.setString(1, copy.copyId());
            statement.setString(2, copy.book().getIsbn());
        });
    }

    @Override
    public void addMember(Member member) {
        execute("INSERT INTO members (member_id, name, email) VALUES (?, ?, ?)", statement -> {
            statement.setString(1, member.getMemberId());
            statement.setString(2, member.getName());
            statement.setString(3, member.getEmail());
        });
    }

    @Override
    public void addLoan(Loan loan) {
        String sql = "INSERT INTO loans (loan_id, copy_id, member_id, checkout_date, due_date, return_date) VALUES (?, ?, ?, ?, ?, ?)";
        execute(sql, statement -> {
            statement.setString(1, loan.getLoanId());
            statement.setString(2, loan.getCopy().copyId());
            statement.setString(3, loan.getMember().getMemberId());
            statement.setDate(4, Date.valueOf(loan.getCheckoutDate()));
            statement.setDate(5, Date.valueOf(loan.getDueDate()));
            statement.setDate(6, loan.getReturnDate() == null ? null : Date.valueOf(loan.getReturnDate()));
        });
    }

    @Override
    public void returnLoan(Loan loan, LocalDate returnDate) {
        if (!loan.isActive() || returnDate == null || returnDate.isBefore(loan.getCheckoutDate())) {
            throw new IllegalArgumentException("An active loan and a valid return date are required.");
        }
        execute("UPDATE loans SET return_date = ? WHERE loan_id = ? AND return_date IS NULL", statement -> {
            statement.setDate(1, Date.valueOf(returnDate));
            statement.setString(2, loan.getLoanId());
        });
        // Publish the state change only after the database accepted the return.
        loan.markReturned(returnDate);
    }

    private void execute(String sql, StatementParameters parameters) {
        try (Connection connection = settings.openConnection(); PreparedStatement statement = connection.prepareStatement(sql)) {
            parameters.bind(statement);
            if (statement.executeUpdate() != 1) {
                throw new IllegalStateException("Record changed elsewhere. Restart to reload database records.");
            }
        } catch (SQLException exception) {
            throw databaseFailure(exception);
        }
    }

    private IllegalStateException databaseFailure(SQLException exception) {
        String message;
        if (exception.getSQLState() != null && exception.getSQLState().startsWith("23")) {
            message = "Database rejected a duplicate or conflicting record. Restart to refresh records if needed.";
        } else {
            message = "Database operation failed. Check MySQL, credentials, schema, and field lengths.";
        }
        return new IllegalStateException(message, exception);
    }

    @FunctionalInterface
    private interface StatementParameters {
        void bind(PreparedStatement statement) throws SQLException;
    }
}
