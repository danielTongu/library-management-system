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

/**
 * Desktop presentation; record operations run off the Swing event thread.
 * Window state is not a persistence format; only repository records are saved.
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
    private final OverviewDashboard dashboard = new OverviewDashboard(overview);
    private final LibraryWorkspace workspace;
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
        setMinimumSize(new Dimension(1040, 660));

        Rectangle available = GraphicsEnvironment.getLocalGraphicsEnvironment().getMaximumWindowBounds();
        setSize(Math.min(1220, available.width - 24), Math.min(800, available.height - 24));
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
        actions.add(button("Edit selected book", this::editBook));
        actions.add(button("Archive selected book", () -> changeBookArchive(true)));
        actions.add(button("Restore selected book", () -> changeBookArchive(false)));

        return new SectionPanel(LibrarySection.BOOKS, books, actions);
    }

    private void changeBookArchive(boolean archive) {
        String[] selected = selectedRecord(books);
        if (selected == null) {
            return;
        }

        String action = archive ? "Archive" : "Restore";
        int choice = JOptionPane.showConfirmDialog(this, action + " " + selected[1] + "?", action + " book", JOptionPane.YES_NO_OPTION);

        if (choice == JOptionPane.YES_OPTION) {
            perform(() -> {
                if (archive) {
                    library.archiveBook(selected[0]);
                } else {
                    library.restoreBook(selected[0]);
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
        return new SectionPanel(LibrarySection.COPIES, copies, actions);
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
        actions.add(button("Edit selected member", this::editMember));
        return new SectionPanel(LibrarySection.MEMBERS, members, actions);
    }

    private void editBook() {
        String[] selected = selectedRecord(books);
        if (selected == null) {
            return;
        }
        String[] values = populatedForm("Edit book: " + selected[0],
                new String[]{"Title", "Author"}, new String[]{selected[1], selected[2]});
        if (values != null) {
            perform(() -> {
                library.updateBook(selected[0], values[0], values[1]);
                return "Book updated.";
            });
        }
    }

    private void editMember() {
        String[] selected = selectedRecord(members);
        if (selected == null) {
            return;
        }
        String[] values = populatedForm("Edit member: " + selected[0],
                new String[]{"Name", "Email"}, new String[]{selected[1], selected[2]});
        if (values != null) {
            perform(() -> {
                library.updateMember(selected[0], values[0], values[1]);
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
        actions.add(button("Check out copy", () -> {
            String[] values = form("Check out copy", "Copy ID", "Member ID");
            if (values != null) {
                perform(() -> "Checked out. Due: " + library.checkoutBook(values[0], values[1]).getDueDate());
            }
        }));
        actions.add(button("Return copy", () -> {
            String[] values = form("Return copy", "Copy ID");
            if (values != null) {
                perform(() -> {
                    Loan returned = library.returnBook(values[0]);
                    return "Copy returned. Fine: " + library.calculateFine(returned);
                });
            }
        }));
        return new SectionPanel(LibrarySection.LOANS, loans, actions);
    }

    private JButton button(String label, Runnable action) {
        JButton button = LibraryTheme.button(label, label.startsWith("Add ") || label.startsWith("Register ") || label.startsWith("Check out"));
        button.addActionListener(event -> action.run());
        buttons.add(button);
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
        dispose();
    }

}
