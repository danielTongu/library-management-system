package com.daniel.library.ui;

import com.daniel.library.model.Book;
import com.daniel.library.model.Member;
import com.daniel.library.persistence.InMemoryLibraryRepository;
import com.daniel.library.service.Library;

import javax.swing.JButton;
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
        check(active.copies().getFirst()[3].equals("Checked out"), "Copy status");
        check(active.loans().getFirst()[5].equals("Active"), "Active loan status");

        Library later = new Library(new BigDecimal("0.50"),
                Clock.fixed(Instant.parse("2026-10-18T00:00:00Z"), ZoneOffset.UTC), seededRepository());
        LibraryRecords overdue = LibraryRecords.load(later, "Late");
        check(overdue.loans().getFirst()[5].equals("Overdue"), "Overdue loan status");
        check(overdue.loans().getFirst()[6].equals(new BigDecimal("1.50")), "Overdue fine");
        later.returnBook("C001");
        LibraryRecords returned = LibraryRecords.load(later, "Returned");
        check(returned.loans().getFirst()[5].equals("Returned"), "Returned loan status");
        check(returned.loans().getFirst()[4] != null, "Return date displayed");
        check(returned.copies().getFirst()[3].equals("Available"), "Copy available after return");

        SwingUtilities.invokeAndWait(() -> {
            LibraryTheme.apply();
            RecordTable panel = new RecordTable(LibrarySection.BOOKS);
            panel.setRows(active.books());
            JTable table = (JTable) ((JScrollPane) panel.getComponent(0)).getViewport().getView();
            check(table.getRowCount() == 1, "Table receives rows");
            check(!table.isCellEditable(0, 0), "Records cannot be edited outside Library API");
            EnumMap<LibrarySection, JPanel> pages = new EnumMap<>(LibrarySection.class);
            for (LibrarySection section : LibrarySection.values()) {
                pages.put(section, new JPanel());
            }
            LibraryWorkspace workspace = new LibraryWorkspace(false, pages, new JLabel("Ready."),
                    LibraryTheme.button("Refresh all", false));
            check(pages.get(LibrarySection.OVERVIEW).isVisible(), "Workspace opens on Overview");
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
