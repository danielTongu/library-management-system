package com.daniel.library.ui;

import static com.daniel.library.ui.LibrarySection.BOOKS;
import static com.daniel.library.ui.LibrarySection.COPIES;
import static com.daniel.library.ui.LibrarySection.LOANS;
import static com.daniel.library.ui.LibrarySection.MEMBERS;
import static com.daniel.library.ui.LibrarySection.OVERVIEW;

import com.daniel.library.model.Book;
import com.daniel.library.model.Member;
import com.daniel.library.persistence.InMemoryLibraryRepository;
import com.daniel.library.service.Library;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.SwingUtilities;

import java.awt.Component;
import java.awt.Container;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.EnumMap;

/** Checks desktop data and table behavior without requiring a native window or database. */
public class DesktopChecks {
    public static void main(String[] args) throws Exception {
        Clock clock = Clock.fixed(Instant.parse("2026-10-01T00:00:00Z"), ZoneOffset.UTC);
        Library library = new Library(new BigDecimal("0.50"), clock);
        check(LibraryRecords.load(library, "Empty").books().isEmpty(), "Empty catalog");
        library.addBook(new Book("123", "Test Book", "Author"));
        library.addCopy("C001", "123");
        library.registerMember(new Member("M001", "Daniel", "daniel@example.com"));
        library.checkoutBook("C001", "M001");
        LibraryRecords active = LibraryRecords.load(library, "Ready");
        check(active.books().size() == 1 && active.members().size() == 1, "Catalog and member rows");
        check(active.copies().getFirst()[COPIES.columnIndex("Status")].equals("Checked out"), "Copy status");
        check(active.loans().getFirst()[LOANS.columnIndex("Status")].equals("Active"), "Active loan status");
        Object[] loan = active.loans().getFirst();
        check(loan[LOANS.columnIndex("Loan ID")].equals(library.getLoans().getFirst().getLoanId()),
                "Loan row preserves the historical loan identity");
        check(loan[LOANS.columnIndex("Checked out")].equals(LocalDate.of(2026, 10, 1)),
                "Checkout date is shown as a sortable date");
        check(loan[LOANS.columnIndex("Copy ID")].equals("C001")
                && loan[LOANS.columnIndex("Member ID")].equals("M001"), "Loan includes both foreign keys");
        check(loan[LOANS.columnIndex("ISBN")].equals("123")
                && loan[LOANS.columnIndex("Author")].equals("Author"), "Loan resolves copy-to-book details");
        check(active.books().getFirst()[BOOKS.columnIndex("Copies")].equals(1L),
                "Book shows a numeric physical copy count");

        Library later = new Library(new BigDecimal("0.50"),
                Clock.fixed(Instant.parse("2026-10-18T00:00:00Z"), ZoneOffset.UTC), seededRepository());
        LibraryRecords overdue = LibraryRecords.load(later, "Late");
        check(overdue.loans().getFirst()[LOANS.columnIndex("Status")].equals("Overdue"), "Overdue loan status");
        check(overdue.members().getFirst()[MEMBERS.columnIndex("Current loans")].equals(1),
                "Overdue borrowing remains in member possession");
        check(overdue.copies().getFirst()[COPIES.columnIndex("Member ID")].equals("M001"),
                "Overdue copies still identify their borrower");
        check(overdue.loans().getFirst()[LOANS.columnIndex("Fine")].equals(new BigDecimal("1.50")), "Overdue fine");
        later.returnBook("C001");
        LibraryRecords returned = LibraryRecords.load(later, "Returned");
        check(returned.loans().getFirst()[LOANS.columnIndex("Status")].equals("Returned"), "Returned loan status");
        check(returned.loans().getFirst()[LOANS.columnIndex("Returned")] != null, "Return date displayed");
        check(returned.copies().getFirst()[COPIES.columnIndex("Status")].equals("Available"),
                "Copy available after return");

        checkBorrowerVisibility(clock);

        SwingUtilities.invokeAndWait(() -> {
            LibraryTheme.apply();
            checkCheckoutChoices(clock);
            RecordTable panel = new RecordTable(BOOKS);
            panel.setRows(active.books());
            JTable table = (JTable) ((JScrollPane) panel.getComponent(0)).getViewport().getView();
            check(table.getRowCount() == 1, "Table receives rows");
            check(!table.isCellEditable(0, 0), "Records cannot be edited outside Library API");
            JButton details = findButton(panel, "View details");
            check(details != null && !details.isEnabled(), "Details waits for a selected record");
            table.setRowSelectionInterval(0, 0);
            check(details.isEnabled(), "Selected record can open full details");
            table.clearSelection();
            check(!details.isEnabled(), "Clearing selection disables details");
            EnumMap<LibrarySection, JPanel> pages = new EnumMap<>(LibrarySection.class);
            for (LibrarySection section : LibrarySection.values()) {
                pages.put(section, new JPanel());
            }
            LibraryWorkspace workspace = new LibraryWorkspace(false, pages, new JLabel("Ready."),
                    LibraryTheme.button("Refresh all", false));
            check(pages.get(OVERVIEW).isVisible(), "Workspace opens on Overview");
            for (LibrarySection section : LibrarySection.values()) {
                JButton navigation = findButton(workspace, section.title());
                check(navigation != null, "Every section has a navigation button");
                navigation.doClick();
                check(pages.get(section).isVisible(), "Navigation opens the selected section");
                check(pages.values().stream().filter(Component::isVisible).count() == 1, "Only one page is shown");
            }
        });
        System.out.println("All desktop data and table checks passed.");
    }

    private static void checkCheckoutChoices(Clock clock) {
        Library library = new Library(new BigDecimal("0.50"), clock);
        library.addBook(new Book("123", "Available Title", "Author"));
        library.addBook(new Book("456", "Archived Title", "Author"));
        library.addCopy("C001", "123");
        library.addCopy("C002", "123");
        library.addCopy("C003", "456");
        library.registerMember(new Member("M001", "Alex", "first@example.com"));
        library.registerMember(new Member("M002", "Alex", "second@example.com"));
        library.checkoutBook("C002", "M001");
        library.archiveBook("456");
        CheckoutForm form = new CheckoutForm(LibraryRecords.load(library, "Ready"), "C001");
        JComboBox<?> copies = (JComboBox<?>) findNamed(form, "checkoutCopy");
        JComboBox<?> members = (JComboBox<?>) findNamed(form, "checkoutMember");
        check(form.hasAvailableCopies() && copies.getItemCount() == 1, "Picker excludes borrowed and archived copies");
        check(form.selectedCopyId().equals("C001"), "Selected copy is preselected by exact ID");
        check(copies.getSelectedItem().toString().equals("Available Title — C001"),
                "Copy option identifies title and copy");
        check(form.selectedMemberId() == null, "Borrower must be explicitly chosen");
        check(members.getItemAt(0).toString().equals("Alex — M001")
                && members.getItemAt(1).toString().equals("Alex — M002"), "Same-name borrowers show distinct IDs");
        members.setSelectedIndex(1);
        check(form.selectedMemberId().equals("M002"), "Picker returns identity, not display text");
        check(library.getLoans().size() == 1, "Opening and changing the form does not create a loan");
        library.checkoutBook(form.selectedCopyId(), form.selectedMemberId());
        check(library.getActiveLoansForMember("M002").size() == 1, "Chosen copy is lent to the chosen member");
        CheckoutForm borrowed = new CheckoutForm(LibraryRecords.load(library, "Borrowed"), "C001");
        check(!borrowed.hasAvailableCopies() && borrowed.selectedCopyId() == null,
                "Refresh excludes newly borrowed copy");
        library.returnBook("C001");
        CheckoutForm returned = new CheckoutForm(LibraryRecords.load(library, "Returned"), null);
        check(returned.hasAvailableCopies() && returned.selectedCopyId() == null,
                "Returned copy is offered without guessing selection");
        CheckoutForm empty = new CheckoutForm(LibraryRecords.load(new Library(), "Empty"), null);
        check(!empty.hasAvailableCopies() && !empty.hasMembers(), "Empty setup can explain missing prerequisites");
    }

    private static Component findNamed(Container parent, String name) {
        for (Component child : parent.getComponents()) {
            if (name.equals(child.getName())) {
                return child;
            }
            if (child instanceof Container container) {
                Component found = findNamed(container, name);
                if (found != null) {
                    return found;
                }
            }
        }
        return null;
    }

    private static void checkBorrowerVisibility(Clock clock) {
        Library library = new Library(new BigDecimal("0.50"), clock);
        library.addBook(new Book("123", "Shared Title", "Author"));
        library.addBook(new Book("456", "Other Title", "Author"));
        library.addCopy("C001", "123");
        library.addCopy("C002", "123");
        library.addCopy("C003", "456");
        library.registerMember(new Member("M001", "Alex", "first@example.com"));
        library.registerMember(new Member("M002", "Alex", "second@example.com"));
        library.registerMember(new Member("M003", "Sam", "sam@example.com"));
        library.checkoutBook("C001", "M001");
        library.checkoutBook("C002", "M002");
        library.checkoutBook("C003", "M001");
        LibraryRecords rows = LibraryRecords.load(library, "Ready");
        Object[] copy = rows.copies().getFirst();
        check(copy[COPIES.columnIndex("Member ID")].equals("M001") && copy[COPIES.columnIndex("Borrower")].equals("Alex"),
                "Copy identifies borrower by name and unique ID");
        check(copy[COPIES.columnIndex("Due")].equals(LocalDate.of(2026, 10, 15)),
                "Copy shows due date as a sortable date");
        check(rows.books().getFirst()[BOOKS.columnIndex("Current borrowers")].toString().contains("Alex (M001), C001"),
                "Title lists first borrower");
        check(rows.books().getFirst()[BOOKS.columnIndex("Current borrowers")].toString().contains("Alex (M002), C002"),
                "Title lists other borrowed copy");
        Object[] member = rows.members().getFirst();
        check(member[MEMBERS.columnIndex("Current loans")].equals(2), "Member current count includes each copy");
        check(rows.members().get(1)[MEMBERS.columnIndex("Current loans")].equals(1),
                "Same-name members have separate loan counts");
        check(rows.members().getLast()[MEMBERS.columnIndex("Current loans")].equals(0), "No borrowing is explicit");
        check(rows.books().getFirst()[BOOKS.columnIndex("Copies")].equals(2L), "Title counts all physical copies");
        check(rows.loans().getFirst()[LOANS.columnIndex("Title")].equals("Shared Title"),
                "Loan history includes title");
        check(RecordFilter.matches(copy, "M001", COPIES.statusColumn(), "Checked out"),
                "Copies searchable by borrower ID");
        library.returnBook("C001");
        rows = LibraryRecords.load(library, "Returned");
        copy = rows.copies().getFirst();
        check(copy[COPIES.columnIndex("Member ID")].equals("") && copy[COPIES.columnIndex("Borrower")].equals("") && copy[COPIES.columnIndex("Due")] == null,
                "Return clears current borrower and due date");
        check(rows.members().getFirst()[MEMBERS.columnIndex("Current loans")].equals(1),
                "Return reduces member current count");
        check(!rows.books().getFirst()[BOOKS.columnIndex("Current borrowers")].toString().contains("M001"),
                "Returned borrower leaves title summary");
        check(rows.loans().getFirst()[LOANS.columnIndex("Member")].equals("Alex"),
                "Returned history retains original borrower");
        library.checkoutBook("C001", "M002");
        rows = LibraryRecords.load(library, "Borrowed again");
        check(rows.copies().getFirst()[COPIES.columnIndex("Member ID")].equals("M002"),
                "Reborrow shows current borrower, not historical borrower");
        check(rows.members().get(1)[MEMBERS.columnIndex("Current loans")].equals(2),
                "Reborrow updates the correct member");
        for (LibrarySection section : LibrarySection.values()) {
            check(rows.rows(section).stream().allMatch(row -> row.length == section.columns().size()),
                    "Borrowing details align with displayed and exported columns");
        }
    }

    private static InMemoryLibraryRepository seededRepository() {
        InMemoryLibraryRepository repository = new InMemoryLibraryRepository();
        Library library = new Library(new BigDecimal("0.50"),
                Clock.fixed(Instant.parse("2026-10-01T00:00:00Z"), ZoneOffset.UTC), repository);
        library.addBook(new Book("123", "Test Book", "Author"));
        library.addCopy("C001", "123");
        library.registerMember(new Member("M001", "Daniel", "daniel@example.com"));
        library.checkoutBook("C001", "M001");
        return repository;
    }

    private static JButton findButton(Container parent, String label) {
        for (Component child : parent.getComponents()) {
            if (child instanceof JButton button && button.getText().equals(label)) {
                return button;
            }
            if (child instanceof Container container) {
                JButton found = findButton(container, label);
                if (found != null) {
                    return found;
                }
            }
        }
        return null;
    }

    private static void check(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }
}
