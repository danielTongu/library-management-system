package com.daniel.library.ui;

import com.daniel.library.model.Book;
import com.daniel.library.model.Loan;
import com.daniel.library.model.Member;
import com.daniel.library.service.Library;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingWorker;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GraphicsEnvironment;
import java.awt.Rectangle;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.function.Supplier;
import java.util.function.Predicate;

/**
 * Desktop presentation; record operations run off the Swing event thread.
 * Local layout preferences are stored separately from library records.
 * @serial exclude
 */
public class LibraryWindow extends JFrame {
    private final Library library;
    private final boolean persistent;
    private final JLabel status = new JLabel("Loading records...");
    private final List<JButton> buttons = new ArrayList<>();
    private final RecordTable overview = new RecordTable(LibrarySection.OVERVIEW);
    private final RecordTable books = new RecordTable(LibrarySection.BOOKS);
    private final RecordTable copies = new RecordTable(LibrarySection.COPIES);
    private final RecordTable members = new RecordTable(LibrarySection.MEMBERS);
    private final RecordTable loans = new RecordTable(LibrarySection.LOANS);
    private final Map<LibrarySection, RecordTable> tables = Map.of(LibrarySection.OVERVIEW, overview,
            LibrarySection.BOOKS, books, LibrarySection.COPIES, copies, LibrarySection.MEMBERS, members,
            LibrarySection.LOANS, loans);
    private final UiPreferences preferences = new UiPreferences();
    private final OverviewDashboard dashboard = new OverviewDashboard(overview);
    private final LibraryWorkspace workspace;
    private LibraryRecords currentRecords;
    private boolean busy;

    /**
     * Constructs the workspace and starts its initial asynchronous refresh.
     * Call on the Swing event thread in a graphical environment.
     * @param library service used for every operation
     * @param persistent true for saved MySQL records, false for a temporary session
     */
    public LibraryWindow(Library library, boolean persistent) {
        super("Library Management System");
        this.library = library;
        this.persistent = persistent;

        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent event) {
                closeWindow();
            }
        });

        Map<LibrarySection, JPanel> pages = new EnumMap<>(LibrarySection.class);
        pages.put(LibrarySection.OVERVIEW, new SectionPanel(LibrarySection.OVERVIEW, dashboard, new JPanel()));
        pages.put(LibrarySection.BOOKS, bookPanel());
        pages.put(LibrarySection.COPIES, copyPanel());
        pages.put(LibrarySection.MEMBERS, memberPanel());
        pages.put(LibrarySection.LOANS, loanPanel());

        workspace = new LibraryWorkspace(persistent, pages, status, button("Refresh all", () -> perform(() -> "Records refreshed.")));
        setContentPane(workspace);
        Rectangle available = GraphicsEnvironment.getLocalGraphicsEnvironment().getMaximumWindowBounds();
        setMinimumSize(new Dimension(Math.min(1040, Math.max(1, available.width - 24)),
                Math.min(660, Math.max(1, available.height - 24))));
        setSize(preferences.windowSize(available));
        tables.forEach((section, table) -> table.restoreColumnWidths(preferences, section));
        setLocation(available.x + (available.width - getWidth()) / 2, available.y + (available.height - getHeight()) / 2);

        perform(() -> "Ready.");
    }

    private JPanel bookPanel() {
        JPanel actions = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        actions.add(button("Add book", () -> {
            String[] values = form("Add book", "ISBN", "Title", "Author");
            if (values != null) {
                perform(() -> {
                    library.addBook(new Book(values[0], values[1], values[2]));
                    return "Book added.";
                });
            }
        }));
        actions.add(selectionButton("Add copy", books, this::addCopyForSelectedBook,
                row -> row[LibrarySection.BOOKS.statusColumn()].equals("Active")));
        actions.add(selectionButton("Edit selected book", books, this::editBook, row -> true));
        actions.add(selectionButton("Archive selected book", books, () -> changeBookArchive(true),
                row -> row[LibrarySection.BOOKS.statusColumn()].equals("Active")
                        && row[LibrarySection.BOOKS.columnIndex("Current borrowers")].equals("None")));
        actions.add(selectionButton("Restore selected book", books, () -> changeBookArchive(false),
                row -> row[LibrarySection.BOOKS.statusColumn()].equals("Archived")));

        return new SectionPanel(LibrarySection.BOOKS, books, actions);
    }

    private void addCopyForSelectedBook() {
        String[] selected = selectedRecord(books);
        if (selected == null) {
            return;
        }
        String isbn = selected[LibrarySection.BOOKS.columnIndex("ISBN")];
        String title = selected[LibrarySection.BOOKS.columnIndex("Title")];
        String[] values = form("Add copy: " + title + " (" + isbn + ")", "Copy ID");
        if (values != null) {
            perform(() -> {
                library.addCopy(values[0], isbn);
                return "Copy added for " + title + ".";
            });
        }
    }

    private void changeBookArchive(boolean archive) {
        LibrarySection section = LibrarySection.BOOKS;
        String[] selected = selectedRecord(books);
        if (selected == null) {
            return;
        }

        String action = archive ? "Archive" : "Restore";
        int choice = JOptionPane.showConfirmDialog(this, action + " " + selected[section.columnIndex("Title")] + "?", action + " book", JOptionPane.YES_NO_OPTION);

        if (choice == JOptionPane.YES_OPTION) {
            perform(() -> {
                if (archive) {
                    library.archiveBook(selected[section.columnIndex("ISBN")]);
                } else {
                    library.restoreBook(selected[section.columnIndex("ISBN")]);
                }
                return archive ? "Book archived. Copies cannot be borrowed until restored." : "Book restored.";
            });
        }
    }

    private JPanel copyPanel() {
        JPanel actions = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        actions.add(button("Add copy", () -> {
            String[] values = form("Add physical copy", "Copy ID", "Book ISBN");
            if (values != null) {
                perform(() -> {
                    library.addCopy(values[0], values[1]);
                    return "Copy added.";
                });
            }
        }));
        actions.add(selectionButton("Check out selected copy", copies, this::checkoutSelectedCopy,
                row -> row[LibrarySection.COPIES.statusColumn()].equals("Available")));
        return new SectionPanel(LibrarySection.COPIES, copies, actions);
    }

    private void checkoutSelectedCopy() {
        LibrarySection section = LibrarySection.COPIES;
        String[] selected = selectedRecord(copies);
        if (selected == null) {
            return;
        }
        if (!selected[section.columnIndex("Status")].equals("Available")) {
            JOptionPane.showMessageDialog(this, "Select an available copy. Borrowed or archived copies cannot be checked out.");
            return;
        }
        checkoutCopy(selected[section.columnIndex("Copy ID")]);
    }

    private void checkoutCopy(String selectedCopyId) {
        if (currentRecords == null) {
            JOptionPane.showMessageDialog(this, "Refresh records before checking out a copy.");
            return;
        }
        CheckoutForm form = new CheckoutForm(currentRecords, selectedCopyId);
        if (!form.hasAvailableCopies()) {
            JOptionPane.showMessageDialog(this, "No copies are available. Add a copy, return a borrowed copy, or restore an archived title.");
            return;
        }
        if (!form.hasMembers()) {
            JOptionPane.showMessageDialog(this, "Register a member in Members before checking out a copy.");
            return;
        }
        while (JOptionPane.showConfirmDialog(this, form, "Check out copy", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE) == JOptionPane.OK_OPTION) {
            String copyId = form.selectedCopyId();
            String memberId = form.selectedMemberId();
            if (copyId != null && memberId != null) {
                perform(() -> "Checked out " + copyId + ". Due: " + library.checkoutBook(copyId, memberId).getDueDate());
                return;
            }
            JOptionPane.showMessageDialog(this, "Choose an available copy and a borrower.");
        }
    }

    private void returnSelectedLoan() {
        LibrarySection section = LibrarySection.LOANS;
        String[] selected = selectedRecord(loans);
        if (selected == null) {
            return;
        }
        if (selected[section.columnIndex("Status")].equals("Returned")) {
            JOptionPane.showMessageDialog(this, "This loan is already returned. Select an active or overdue loan.");
            return;
        }
        String copyId = selected[section.columnIndex("Copy ID")];
        String title = selected[section.columnIndex("Title")];
        String borrower = selected[section.columnIndex("Member")] + " (" + selected[section.columnIndex("Member ID")] + ")";
        confirmReturn(copyId, title, borrower);
    }

    private void confirmReturn(String copyId, String title, String borrower) {
        String message = "Return " + copyId + ": " + title + "\nBorrower: " + borrower + "?";
        int choice = JOptionPane.showConfirmDialog(this, message, "Return copy", JOptionPane.YES_NO_OPTION);
        if (choice == JOptionPane.YES_OPTION) {
            returnCopy(copyId);
        }
    }

    private void returnCopy(String copyId) {
        perform(() -> {
            Loan returned = library.returnBook(copyId);
            return "Copy " + copyId + " returned. Fine: " + library.calculateFine(returned);
        });
    }

    private JPanel memberPanel() {
        JPanel actions = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        actions.add(button("Register member", () -> {
            String[] values = form("Register member", "Member ID", "Name", "Email");
            if (values != null) {
                perform(() -> {
                    library.registerMember(new Member(values[0], values[1], values[2]));
                    return "Member registered.";
                });
            }
        }));
        actions.add(selectionButton("Edit selected member", members, this::editMember, row -> true));
        actions.add(selectionButton("View current loans", members, this::viewMemberLoans, row -> true));
        return new SectionPanel(LibrarySection.MEMBERS, members, actions);
    }

    private void viewMemberLoans() {
        LibrarySection section = LibrarySection.MEMBERS;
        String[] selected = selectedRecord(members);
        if (selected != null) {
            loans.showCurrentLoans(selected[section.columnIndex("Member ID")], selected[section.columnIndex("Name")]);
            workspace.showSection(LibrarySection.LOANS);
        }
    }

    private void editBook() {
        LibrarySection section = LibrarySection.BOOKS;
        String[] selected = selectedRecord(books);
        if (selected == null) {
            return;
        }

        String[] values = populatedForm(
                "Edit book: " + selected[section.columnIndex("ISBN")],
                new String[]{"Title", "Author"},
                new String[]{selected[section.columnIndex("Title")], selected[section.columnIndex("Author")]}
        );

        if (values != null) {
            perform(() -> {
                library.updateBook(selected[section.columnIndex("ISBN")], values[0], values[1]);
                return "Book updated.";
            });
        }
    }

    private void editMember() {
        LibrarySection section = LibrarySection.MEMBERS;
        String[] selected = selectedRecord(members);
        if (selected == null) {
            return;
        }

        String[] values = populatedForm(
                "Edit member: " + selected[section.columnIndex("Member ID")],
                new String[]{"Name", "Email"},
                new String[]{selected[section.columnIndex("Name")], selected[section.columnIndex("Email")]}
        );

        if (values != null) {
            perform(() -> {
                library.updateMember(selected[section.columnIndex("Member ID")], values[0], values[1]);
                return "Member updated.";
            });
        }
    }

    private String[] selectedRecord(RecordTable table) {
        String[] selected = table.selectedRecord();
        if (selected == null) {
            JOptionPane.showMessageDialog(this, "Select a record in the table first.");
        }
        return selected;
    }

    private JPanel loanPanel() {
        JPanel actions = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        actions.add(selectionButton("Return selected loan", loans, this::returnSelectedLoan,
                row -> row[LibrarySection.LOANS.statusColumn()].equals("Active")
                        || row[LibrarySection.LOANS.statusColumn()].equals("Overdue"))
        );

        return new SectionPanel(LibrarySection.LOANS, loans, actions);
    }

    private JButton button(String label, Runnable action) {
        boolean primary = label.startsWith("Add ") || label.startsWith("Register ")
                || label.startsWith("Check out") || label.equals("Return selected loan");

        JButton button = LibraryTheme.button(label, primary);
        button.addActionListener(event -> action.run());
        buttons.add(button);
        return button;
    }

    private JButton selectionButton(String label, RecordTable table, Runnable action, Predicate<String[]> eligible) {
        JButton button = button(label, action);
        button.setToolTipText("Select an eligible record to " + label.toLowerCase(java.util.Locale.ROOT));
        table.bindSelectionAction(button, eligible);
        return button;
    }

    private String[] form(String title, String... labels) {
        return populatedForm(title, labels, new String[labels.length]);
    }

    private String[] populatedForm(String title, String[] labels, String[] initialValues) {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBackground(LibraryTheme.SURFACE);
        panel.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));

        List<JTextField> fields = new ArrayList<>();

        for (int index = 0; index < labels.length; index++) {
            JTextField field = new JTextField(30);
            LibraryTheme.textField(field);
            field.setText(initialValues[index]);
            field.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
            field.setPreferredSize(new Dimension(380, 40));
            field.setFont(LibraryTheme.font(14, Font.PLAIN));

            JLabel label = new JLabel(labels[index]);
            label.setFont(LibraryTheme.font(12, Font.BOLD));
            label.setLabelFor(field);
            label.setAlignmentX(Component.LEFT_ALIGNMENT);

            field.setAlignmentX(Component.LEFT_ALIGNMENT);
            fields.add(field);

            panel.add(label);
            panel.add(Box.createVerticalStrut(6));
            panel.add(field);
            panel.add(Box.createVerticalStrut(16));
        }
        int choice = JOptionPane.showConfirmDialog(this, panel, title, JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);

        return choice == JOptionPane.OK_OPTION ?
                fields.stream().map(field -> field.getText().trim()).toArray(String[]::new)
                : null;
    }

    private void perform(Supplier<String> action) {
        setBusy(true);
        status.setForeground(LibraryTheme.ACCENT);
        status.setText("Working...");

        new SwingWorker<LibraryRecords, Void>() {
            @Override
            protected LibraryRecords doInBackground() {
                String message = action.get();
                // A successful write is not undone if the following display refresh fails.
                try {
                    return LibraryRecords.load(library, message);
                } catch (RuntimeException exception) {
                    throw new IllegalStateException(message + " Refresh failed; use Refresh all before retrying.", exception);
                }
            }

            @Override
            protected void done() {
                try {
                    LibraryRecords records = get();
                    currentRecords = records;
                    overview.setRows(records.overview());
                    dashboard.setRows(records.overview());
                    books.setRows(records.books());
                    copies.setRows(records.copies());
                    members.setRows(records.members());
                    loans.setRows(records.loans());
                    status.setForeground(LibraryTheme.MUTED);
                    status.setText(records.message());
                } catch (InterruptedException exception) {
                    Thread.currentThread().interrupt();
                    showError("Operation interrupted. Refresh records before retrying.");
                } catch (ExecutionException exception) {
                    showError(exception.getCause().getMessage());
                } finally {
                    setBusy(false);
                }
            }
        }.execute();
    }

    private void setBusy(boolean value) {
        busy = value;
        workspace.setBusy(value);
        buttons.forEach(button -> button.setEnabled(!value));
        tables.values().forEach(table -> table.setActionsBusy(value));
    }

    private void showError(String message) {
        status.setForeground(LibraryTheme.ERROR);
        status.setText("Operation failed. " + message);
        JOptionPane.showMessageDialog(this, message, "Library", JOptionPane.ERROR_MESSAGE);
    }

    private void closeWindow() {
        if (busy || List.of(overview, books, copies, members, loans).stream().anyMatch(RecordTable::isExporting)) {
            JOptionPane.showMessageDialog(this, "Wait for the current operation to finish before closing.");
            return;
        }
        if (!persistent) {
            int choice = JOptionPane.showConfirmDialog(this, "Close and discard in-memory records?", "Close library",
                    JOptionPane.YES_NO_OPTION);
            if (choice != JOptionPane.YES_OPTION) {
                return;
            }
        }
        if ((getExtendedState() & MAXIMIZED_BOTH) == 0) {
            preferences.rememberWindowSize(getSize());
        }
        tables.forEach((section, table) -> table.rememberColumnWidths(preferences, section));
        preferences.save();
        dispose();
    }

}
