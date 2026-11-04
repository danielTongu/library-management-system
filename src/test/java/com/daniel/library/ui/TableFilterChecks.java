package com.daniel.library.ui;

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
        SwingUtilities.invokeAndWait(TableFilterChecks::checkFilters);
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
