package com.daniel.library.ui;

import static com.daniel.library.ui.LibrarySection.LOANS;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;

import java.awt.Component;
import java.awt.Container;
import java.util.List;

/** Exercises real Swing search and dropdown controls without a native window. */
public class TableFilterChecks {
    public static void main(String[] args) throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            checkFilters();
            checkMemberLoans();
            checkDefaultViewsAndActions();
        });
        System.out.println("All desktop search and filter checks passed.");
    }

    private static void checkFilters() {
        RecordTable panel = new RecordTable("Copy ID", "Title", "Status");
        panel.addStatusFilter(2, "All", "Available", "Checked out");
        List<Object[]> records = List.of(new Object[]{"C001", "Effective Java", "Available"},
                new Object[]{"C002", "Java [Basics]", "Checked out"},
                new Object[]{"C003", "SQL Guide", "Available"});
        panel.setRows(records);
        JTable table = (JTable) ((JScrollPane) panel.getComponent(0)).getViewport().getView();
        JTextField search = (JTextField) find(panel, "recordSearch");
        JComboBox<?> status = (JComboBox<?>) find(panel, "recordStatus");
        JButton clear = (JButton) find(panel, "clearFilters");

        search.setText("JAVA");
        check(table.getRowCount() == 2, "Case-insensitive search");
        status.setSelectedItem("Available");
        check(table.getRowCount() == 1 && table.getValueAt(0, 0).equals("C001"), "Search and status combine");
        search.setText("[Basics]");
        check(table.getRowCount() == 0, "Status excludes otherwise matching title");
        status.setSelectedItem("All");
        check(table.getRowCount() == 1, "Search treats punctuation literally");
        search.setText("C003");
        check(table.getRowCount() == 1 && table.getValueAt(0, 1).equals("SQL Guide"), "Search matches ID column");
        search.setText("no matching record");
        check(table.getRowCount() == 0, "No-match search");
        check(table.getModel().getRowCount() == 3, "Filtering does not delete records");

        clear.doClick();
        check(search.getText().isEmpty() && table.getRowCount() == 3, "Clear restores all rows");
        search.setText("  java  ");
        status.setSelectedItem("Checked out");
        panel.setRows(records);
        check(table.getRowCount() == 1 && status.getSelectedItem().equals("Checked out"), "Filters survive refresh");
        panel.setRows(List.of());
        check(table.getRowCount() == 0, "Empty refresh works with active filters");
        clear.doClick();
        panel.setRows(records);
        check(table.getRowCount() == 3 && status.getSelectedItem().equals("All"), "Clear resets dropdown too");
        table.getRowSorter().toggleSortOrder(0);
        table.getRowSorter().toggleSortOrder(0);
        table.setRowSelectionInterval(0, 0);
        check(panel.selectedRecord()[0].equals("C003"), "Selection uses sorted row's actual record");
        search.setText("Java");
        table.setRowSelectionInterval(0, 0);
        check(panel.selectedRecord()[0].equals("C002"), "Selection uses filtered row's actual record");
        check(panel.displayedRows().size() == 2 && panel.displayedRows().getFirst()[0].equals("C002"),
                "Export rows preserve the filtered display and descending sort");
        table.clearSelection();
        check(panel.selectedRecord() == null, "No selection is explicit");

        RecordTable loans = new RecordTable("Copy", "Status");
        loans.addStatusFilter(1, "All", "Active", "Overdue", "Returned");
        loans.setRows(List.of(new Object[]{"C001", "Active"}, new Object[]{"C002", "Overdue"},
                new Object[]{"C003", "Returned"}));
        JComboBox<?> loanStatus = (JComboBox<?>) find(loans, "recordStatus");
        JTable loanTable = (JTable) ((JScrollPane) loans.getComponent(0)).getViewport().getView();
        for (String choice : List.of("Active", "Overdue", "Returned")) {
            loanStatus.setSelectedItem(choice);
            check(loanTable.getRowCount() == 1 && loanTable.getValueAt(0, 1).equals(choice), "Exact loan status");
        }
        RecordTable amounts = new RecordTable("Fine");
        amounts.setRows(List.of(new Object[]{new java.math.BigDecimal("10.00")},
                new Object[]{new java.math.BigDecimal("2.00")}, new Object[]{new java.math.BigDecimal("0.50")}));
        JTable amountTable = (JTable) ((JScrollPane) amounts.getComponent(0)).getViewport().getView();
        amountTable.getRowSorter().toggleSortOrder(0);
        check(amountTable.getValueAt(0, 0).toString().equals("0.50")
                && amountTable.getValueAt(1, 0).toString().equals("2.00"), "Fines sort numerically");
        amountTable.getRowSorter().toggleSortOrder(0);
        check(amountTable.getValueAt(0, 0).toString().equals("10.00"), "Descending fines sort numerically");
    }

    private static void checkMemberLoans() {
        RecordTable panel = new RecordTable(LOANS);
        List<Object[]> records = List.of(
                loanRow("C001", "M001", "Active", 0, "First Book"),
                loanRow("C002", "M001", "Overdue", 1, "Late Book"),
                loanRow("C003", "M001", "Returned", 0, "Old Book"),
                loanRow("C004", "M0010", "Active", 0, "Other Borrower"),
                loanRow("C005", "m001", "Active", 0, "Case-sensitive ID"));
        panel.setRows(records);
        JTable table = (JTable) ((JScrollPane) panel.getComponent(0)).getViewport().getView();
        JTextField search = (JTextField) find(panel, "recordSearch");
        JComboBox<?> status = (JComboBox<?>) find(panel, "recordStatus");
        JButton clear = (JButton) find(panel, "clearFilters");
        search.setText("Old Book");
        status.setSelectedItem("Returned");
        panel.showCurrentLoans("M001", "Alex");
        check(search.getText().isEmpty() && status.getSelectedItem().equals("Current"),
                "Member link resets old filters");
        check(table.getRowCount() == 2, "Current includes active and overdue, excludes returned and similar IDs");
        check(find(panel, "memberScope").isVisible(), "Member scope is visible");
        check(panel.displayedRows().stream().allMatch(row -> row[LOANS.columnIndex("Member ID")].equals("M001")),
                "Export respects exact member scope");
        table.getRowSorter().toggleSortOrder(LOANS.columnIndex("Copy ID"));
        table.getRowSorter().toggleSortOrder(LOANS.columnIndex("Copy ID"));
        table.setRowSelectionInterval(0, 0);
        check(panel.selectedRecord()[LOANS.columnIndex("Copy ID")].equals("C002"),
                "Sorted scoped selection identifies the intended return");
        search.setText("First");
        check(table.getRowCount() == 1, "Text search narrows the member scope");
        search.setText("");
        status.setSelectedItem("Returned");
        check(table.getRowCount() == 1 && table.getValueAt(0, LOANS.columnIndex("Copy ID")).equals("C003"),
                "History remains limited to that member");
        status.setSelectedItem("Current");
        Object[] returned = records.getFirst().clone();
        returned[LOANS.statusColumn()] = "Returned";
        panel.setRows(List.of(returned, records.get(1), records.get(2), records.get(3), records.get(4)));
        check(table.getRowCount() == 1 && table.getValueAt(0, LOANS.columnIndex("Copy ID")).equals("C002"),
                "Refresh retains scope and removes returns");
        panel.showCurrentLoans("M003", "No loans");
        check(table.getRowCount() == 0 && panel.selectedRecord() == null, "Member without loans shows empty view");
        panel.showCurrentLoans("M0010", "Alex");
        check(table.getRowCount() == 1 && table.getValueAt(0, LOANS.columnIndex("Copy ID")).equals("C004"),
                "Another same-name member replaces scope");
        clear.doClick();
        check(table.getRowCount() == 5 && !find(panel, "memberScope").isVisible(),
                "Clear removes scope and restores history");
        status.setSelectedItem("Current");
        check(table.getRowCount() == 3, "Current can show every member's active and overdue loans");
    }

    private static void checkDefaultViewsAndActions() {
        RecordTable loans = new RecordTable(LibrarySection.LOANS);
        List<Object[]> records = List.of(loanRow("C001", "M001", "Active", 0, "Active Book"),
                loanRow("C002", "M001", "Overdue", 1, "Late Book"),
                loanRow("C003", "M001", "Returned", 0, "Returned Book"));
        loans.setRows(records);
        JTable table = (JTable) ((JScrollPane) loans.getComponent(0)).getViewport().getView();
        JComboBox<?> status = (JComboBox<?>) find(loans, "recordStatus");
        JButton action = new JButton("Return selected loan");
        loans.bindSelectionAction(action, row -> row[LOANS.statusColumn()].equals("Active")
                || row[LOANS.statusColumn()].equals("Overdue"));
        check(status.getSelectedItem().equals("Current") && table.getRowCount() == 2,
                "Loans starts with active and overdue borrowing, excluding history");
        check(!action.isEnabled(), "Return requires a selection");
        table.setRowSelectionInterval(0, 0);
        check(action.isEnabled(), "An active loan can be returned");
        table.setRowSelectionInterval(1, 1);
        check(action.isEnabled(), "An overdue loan can be returned");
        loans.setActionsBusy(true);
        check(!action.isEnabled(), "Background work disables selection actions");
        loans.setActionsBusy(false);
        check(action.isEnabled(), "Finishing work rechecks the current selection");
        status.setSelectedItem("Returned");
        table.setRowSelectionInterval(0, 0);
        check(!action.isEnabled(), "Returned history cannot be returned again");
        status.setSelectedItem("Current");
        table.setRowSelectionInterval(0, 0);
        loans.setRows(List.<Object[]>of(records.get(2)));
        check(!action.isEnabled() && table.getRowCount() == 0, "Refresh removes finished loans and disables actions");

        RecordTable books = new RecordTable(LibrarySection.BOOKS);
        books.setRows(List.of(new Object[]{"123", "Borrowed", "Author", "Active", 1L, "Alex (M001)"},
                new Object[]{"456", "Available", "Author", "Active", 1L, "None"},
                new Object[]{"789", "Archived", "Author", "Archived", 1L, "None"}));
        JTable bookTable = (JTable) ((JScrollPane) books.getComponent(0)).getViewport().getView();
        JComboBox<?> bookStatus = (JComboBox<?>) find(books, "recordStatus");
        JButton archive = new JButton("Archive selected book");
        JButton restore = new JButton("Restore selected book");
        JButton addCopy = new JButton("Add copy");
        books.bindSelectionAction(addCopy, row -> row[LibrarySection.BOOKS.statusColumn()].equals("Active"));
        books.bindSelectionAction(archive, row -> row[LibrarySection.BOOKS.statusColumn()].equals("Active")
                && row[LibrarySection.BOOKS.columnIndex("Current borrowers")].equals("None"));
        books.bindSelectionAction(restore, row -> row[LibrarySection.BOOKS.statusColumn()].equals("Archived"));
        check(bookStatus.getSelectedItem().equals("Active") && bookTable.getRowCount() == 2,
                "Books starts with active titles");
        bookTable.setRowSelectionInterval(0, 0);
        check(!archive.isEnabled() && !restore.isEnabled() && addCopy.isEnabled(),
                "Borrowed title can gain copies but cannot be archived or restored");
        bookTable.setRowSelectionInterval(1, 1);
        check(archive.isEnabled() && !restore.isEnabled(), "An unborrowed active title can only be archived");
        bookStatus.setSelectedItem("Archived");
        bookTable.setRowSelectionInterval(0, 0);
        check(!archive.isEnabled() && restore.isEnabled() && !addCopy.isEnabled(),
                "Archived titles can be restored but cannot gain copies");
        ((JButton) find(books, "clearFilters")).doClick();
        check(bookTable.getRowCount() == 3, "Clear filters deliberately includes archived titles");

        RecordTable copies = new RecordTable(LibrarySection.COPIES);
        copies.setRows(List.of(new Object[]{"C001", "123", "Borrowed", "Author", "Checked out", "M001", "Alex", null},
                new Object[]{"C002", "123", "Available", "Author", "Available", "", "", null},
                new Object[]{"C003", "789", "Archived", "Author", "Archived", "", "", null}));
        JTable copyTable = (JTable) ((JScrollPane) copies.getComponent(0)).getViewport().getView();
        JButton checkout = new JButton("Check out selected copy");
        copies.bindSelectionAction(checkout, row -> row[LibrarySection.COPIES.statusColumn()].equals("Available"));
        for (int index = 0; index < 3; index++) {
            copyTable.setRowSelectionInterval(index, index);
            check(checkout.isEnabled() == (index == 1), "Only available selected copies enable checkout");
        }
        ((JTextField) find(copies, "recordSearch")).setText("missing");
        check(!checkout.isEnabled(), "Filtering away the selection disables checkout");
    }

    private static Object[] loanRow(String copyId, String memberId, String state, int fine, String title) {
        return new Object[]{"loan-" + copyId, copyId, "123", title, "Author", memberId, "Alex", null, null, null, state, fine};
    }

    private static Component find(Container parent, String name) {
        for (Component child : parent.getComponents()) {
            if (name.equals(child.getName())) {
                return child;
            }
            if (child instanceof Container container) {
                Component match = find(container, name);
                if (match != null) {
                    return match;
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
