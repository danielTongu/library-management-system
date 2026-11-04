package com.daniel.library.ui;


import javax.swing.BorderFactory;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.RowFilter;
import javax.swing.SortOrder;
import javax.swing.SwingWorker;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.plaf.basic.BasicButtonUI;
import javax.swing.plaf.basic.BasicComboBoxUI;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableRowSorter;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Graphics;
import java.awt.RenderingHints;
import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutionException;
import java.util.stream.IntStream;

/**
 * Reusable read-only records with literal search, status filtering, sorting, and CSV export.
 *
 * @serial exclude
 */
class RecordTable extends JPanel {
    private final DefaultTableModel model;
    private final JTable table;
    private final TableRowSorter<DefaultTableModel> sorter;
    private final JTextField search = new JTextField(19);
    private final JLabel count = new JLabel();
    private final JPanel filters = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 12));
    private JComboBox<String> status;
    private int statusColumn;
    private String defaultFilename = "library-records.csv";
    private boolean exporting;

    /**
     * Builds a table with the section columns and optional status choices.
     *
     * @param section presentation definition for this table
     */
    RecordTable(LibrarySection section) {
        this(section.columns().toArray(String[]::new));
        defaultFilename = section.title().toLowerCase(Locale.ROOT) + ".csv";
        if (!section.statuses().isEmpty()) {
            addStatusFilter(section.statusColumn(), section.statuses().toArray(String[]::new));
        }
    }

    /**
     * Builds a reusable table with explicitly ordered headers.
     *
     * @param columns displayed and exported column names
     */
    RecordTable(String... columns) {
        super(new BorderLayout());
        model = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        table = new JTable(model) {
            @Override
            protected void paintComponent(Graphics graphics) {
                super.paintComponent(graphics);
                if (getRowCount() == 0 && getHeight() > 90) {
                    Graphics2D canvas = (Graphics2D) graphics.create();
                    canvas.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
                    canvas.setColor(LibraryTheme.MUTED);
                    canvas.setFont(LibraryTheme.font(15, Font.PLAIN));
                    String message = model.getRowCount() == 0 ? "No records yet" : "No matching records";

                    int left = Math.max(20, (getWidth() - canvas.getFontMetrics().stringWidth(message)) / 2);
                    canvas.drawString(message, left, Math.min(130, getHeight() / 2));
                    canvas.dispose();
                }
            }
        };
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        sorter = new TableRowSorter<>(model);

        for (int column = 0; column < columns.length; column++) {
            sorter.setComparator(column, RecordTable::compareValues);
        }

        table.setRowSorter(sorter);
        table.setFillsViewportHeight(true);
        table.setRowHeight(42);
        table.setShowGrid(false);
        table.setIntercellSpacing(new Dimension(0, 0));
        table.setBackground(LibraryTheme.SURFACE);
        table.setForeground(LibraryTheme.INK);
        table.setDefaultRenderer(Object.class, new RecordCellRenderer());
        table.getTableHeader().setPreferredSize(new Dimension(0, 42));

        table.getTableHeader().setDefaultRenderer((grid, value, selected, focus, row, column) -> {
            JLabel label = new DefaultTableCellRenderer();
            label.setOpaque(true);
            label.setBackground(LibraryTheme.STRIPE);
            label.setForeground(LibraryTheme.MUTED);
            label.setFont(LibraryTheme.font(12, Font.BOLD));
            label.setHorizontalAlignment(value.equals("Value") || value.equals("Fine") ? JLabel.RIGHT : JLabel.LEFT);
            label.setBorder(BorderFactory.createEmptyBorder(0, 14, 0, 10));
            String text = value.toString();

            if (!sorter.getSortKeys().isEmpty()) {
                var key = sorter.getSortKeys().getFirst();
                if (grid.convertColumnIndexToModel(column) == key.getColumn()) {
                    text += key.getSortOrder() == SortOrder.ASCENDING ? " ↑" : " ↓";
                }
            }

            label.setText(text);
            return label;
        });

        for (int index = 0; index < columns.length; index++) {
            var column = table.getColumnModel().getColumn(index);
            column.setMinWidth(columns[index].equals("Status") ? 110 : 80);
            column.setPreferredWidth(columns[index].equals("Title") || columns[index].equals("Email") ? 240 : 150);
        }

        setBackground(LibraryTheme.SURFACE);
        setBorder(BorderFactory.createLineBorder(LibraryTheme.BORDER));

        JScrollPane scroll = new JScrollPane(table);
        scroll.setBorder(BorderFactory.createMatteBorder(1, 0, 1, 0, LibraryTheme.BORDER));
        scroll.getViewport().setBackground(LibraryTheme.SURFACE);
        scroll.setColumnHeaderView(table.getTableHeader());
        LibraryTheme.scrollPane(scroll);
        add(scroll, BorderLayout.CENTER);

        JLabel searchLabel = new JLabel("Search records");
        searchLabel.setForeground(LibraryTheme.MUTED);
        filters.setBackground(LibraryTheme.SURFACE);
        filters.setBorder(BorderFactory.createEmptyBorder(0, 4, 0, 4));
        search.setPreferredSize(new Dimension(210, 36));
        LibraryTheme.textField(search);
        searchLabel.setLabelFor(search);
        search.setName("recordSearch");
        search.setToolTipText("Search any column; text is matched literally, ignoring case.");
        filters.add(searchLabel);
        filters.add(search);

        JButton clear = LibraryTheme.button("Clear filters", false);
        clear.setName("clearFilters");
        clear.addActionListener(event -> {
            search.setText("");
            if (status != null) {
                status.setSelectedIndex(0);
            }
        });
        filters.add(clear);
        add(filters, BorderLayout.NORTH);

        JButton export = LibraryTheme.button("Export CSV", false);
        export.addActionListener(event -> exportRecords(export));
        export.setToolTipText("Export displayed records to CSV");
        JPanel footer = new JPanel(new BorderLayout());
        footer.setBackground(LibraryTheme.SURFACE);
        footer.setBorder(BorderFactory.createEmptyBorder(10, 14, 10, 14));
        count.setForeground(LibraryTheme.MUTED);
        count.setFont(LibraryTheme.font(12, Font.PLAIN));
        footer.add(count, BorderLayout.WEST);
        footer.add(export, BorderLayout.EAST);
        add(footer, BorderLayout.SOUTH);

        search.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent event) {
                applyFilters();
            }

            @Override
            public void removeUpdate(DocumentEvent event) {
                applyFilters();
            }

            @Override
            public void changedUpdate(DocumentEvent event) {
                applyFilters();
            }
        });
        applyFilters();
    }

    /**
     * Replaces the displayed dataset while retaining search and status choices.
     * Call on the Swing event thread.
     *
     * @param rows column-aligned presentation values
     */
    void setRows(List<Object[]> rows) {
        model.setRowCount(0);
        rows.forEach(model::addRow);
        applyFilters();
    }

    List<Object[]> displayedRows() {
        List<Object[]> rows = new ArrayList<>();
        for (int row = 0; row < table.getRowCount(); row++) {
            Object[] values = new Object[table.getColumnCount()];
            for (int column = 0; column < values.length; column++) {
                values[column] = table.getValueAt(row, column);
            }
            rows.add(values);
        }
        return rows;
    }

    private void exportRecords(JButton button) {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Export displayed records");
        chooser.setSelectedFile(new File(defaultFilename));
        chooser.setFileFilter(new FileNameExtensionFilter("CSV files", "csv"));
        if (chooser.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) {
            return;
        }
        List<String> columns = IntStream.range(0, table.getColumnCount()).mapToObj(table::getColumnName).toList();
        List<Object[]> rows = displayedRows();
        Path path = chooser.getSelectedFile().toPath();

        exporting = true;
        button.setEnabled(false);
        new SwingWorker<Path, Void>() {
            @Override
            protected Path doInBackground() throws Exception {
                return CsvExport.write(path, columns, rows);
            }

            @Override
            protected void done() {
                try {
                    Path saved = get();
                    JOptionPane.showMessageDialog(RecordTable.this, "Exported " + rows.size() + " records to " + saved);
                } catch (InterruptedException exception) {
                    Thread.currentThread().interrupt();
                    JOptionPane.showMessageDialog(RecordTable.this, "Export interrupted.");
                } catch (ExecutionException exception) {
                    JOptionPane.showMessageDialog(RecordTable.this, exception.getCause().getMessage(), "Unable to export", JOptionPane.ERROR_MESSAGE);
                } finally {
                    exporting = false;
                    button.setEnabled(true);
                }
            }
        }.execute();
    }

    /**
     * Allows the workspace to defer closing while a report is being written.
     *
     * @return true while an export worker is active
     */
    boolean isExporting() {
        return exporting;
    }

    /**
     * Resolves the selected view row back to its underlying record.
     *
     * @return values in model-column order, or null when there is no selection
     */
    String[] selectedRecord() {
        int selected = table.getSelectedRow();
        if (selected < 0) {
            return null;
        }
        int row = table.convertRowIndexToModel(selected);
        String[] values = new String[model.getColumnCount()];
        for (int column = 0; column < values.length; column++) {
            Object value = model.getValueAt(row, column);
            values[column] = value == null ? "" : value.toString();
        }
        return values;
    }

    void addStatusFilter(int column, String... choices) {
        statusColumn = column;
        status = new JComboBox<>(choices);

        status.setUI(new BasicComboBoxUI() {
            @Override
            protected JButton createArrowButton() {
                JButton arrow = new JButton("▾");
                arrow.setUI(new BasicButtonUI());
                arrow.setContentAreaFilled(false);
                arrow.setBorder(BorderFactory.createEmptyBorder(0, 8, 0, 8));
                arrow.setForeground(LibraryTheme.MUTED);
                return arrow;
            }
        });

        status.setBackground(LibraryTheme.SURFACE);
        status.setForeground(LibraryTheme.INK);
        status.setBorder(BorderFactory.createLineBorder(LibraryTheme.BORDER, 1, true));

        status.setRenderer((list, value, index, selected, focus) -> {
            JLabel label = (JLabel) new DefaultListCellRenderer().getListCellRendererComponent(list, value, index, selected, focus);
            label.setBackground(selected ? LibraryTheme.SELECTED : LibraryTheme.SURFACE);
            label.setForeground(LibraryTheme.INK);
            label.setBorder(BorderFactory.createEmptyBorder(7, 10, 7, 10));
            return label;
        });

        status.setName("recordStatus");
        status.setPreferredSize(new Dimension(130, 36));

        JLabel label = new JLabel("Status");
        label.setForeground(LibraryTheme.MUTED);
        label.setLabelFor(status);

        filters.add(label, 2);
        filters.add(status, 3);
        status.addActionListener(event -> applyFilters());
        applyFilters();
    }

    private void applyFilters() {
        String query = search.getText().strip().toLowerCase(Locale.ROOT);
        String selected = status == null || status.getSelectedIndex() == 0 ? null : (String) status.getSelectedItem();

        sorter.setRowFilter(new RowFilter<>() {
            @Override
            public boolean include(Entry<? extends DefaultTableModel, ? extends Integer> entry) {
                Object[] values = new Object[entry.getValueCount()];
                for (int column = 0; column < entry.getValueCount(); column++) {
                    values[column] = entry.getValue(column);
                }
                return RecordFilter.matches(values, query, status == null ? -1 : statusColumn, selected);
            }
        });

        count.setText(sorter.getViewRowCount() + " of " + model.getRowCount() + " records");
    }

    private static int compareValues(Object left, Object right) {
        // Table columns contain Objects; compare numbers as values, not formatted strings.
        if (left instanceof Number && right instanceof Number) {
            return new java.math.BigDecimal(left.toString()).compareTo(new java.math.BigDecimal(right.toString()));
        }
        if (left instanceof java.time.LocalDate first && right instanceof java.time.LocalDate second) {
            return first.compareTo(second);
        }
        return java.text.Collator.getInstance().compare(left.toString(), right.toString());
    }
}
