package com.daniel.library.ui;


import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;

import java.awt.BorderLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Highlights existing overview values; detailed, searchable/exportable totals remain underneath. */
final class OverviewDashboard extends JPanel {
    private final Map<String, JLabel> values = new LinkedHashMap<>();

    OverviewDashboard(RecordTable details) {
        super(new BorderLayout(0, 20));
        setOpaque(false);

        JPanel cards = new JPanel(new GridLayout(1, 4, 14, 0));
        cards.setOpaque(false);

        addCard(cards, "Book titles", "Book titles (including archived)", "Titles in your catalog");
        addCard(cards, "Available copies", "Available copies", "Ready to be borrowed");
        addCard(cards, "Members", "Registered members", "Registered readers");
        addCard(cards, "Overdue loans", "Overdue loans", "Copies that need returning");

        add(cards, BorderLayout.NORTH);
        add(details, BorderLayout.CENTER);
    }

    void setRows(List<Object[]> rows) {
        for (Object[] row : rows) {
            JLabel value = values.get(row[0].toString());
            if (value != null) {
                value.setText(row[1].toString());
                value.setForeground(row[0].equals("Overdue loans") && !row[1].toString().equals("0")
                        ? LibraryTheme.ERROR : LibraryTheme.INK);
            }
        }
    }

    private void addCard(JPanel cards, String label, String key, String explanation) {
        JPanel card = new JPanel(new BorderLayout(0, 6));
        card.setBackground(LibraryTheme.SURFACE);
        card.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(LibraryTheme.BORDER),
                BorderFactory.createEmptyBorder(16, 16, 16, 16)));

        JLabel heading = new JLabel(label);
        heading.setFont(LibraryTheme.font(12, Font.BOLD));
        heading.setForeground(LibraryTheme.MUTED);

        JLabel value = new JLabel("—");
        value.setFont(LibraryTheme.font(32, Font.BOLD));

        JLabel help = new JLabel(explanation);
        help.setFont(LibraryTheme.font(11, Font.PLAIN));
        help.setForeground(LibraryTheme.MUTED);
        help.setToolTipText(explanation);

        card.add(heading, BorderLayout.NORTH);
        card.add(value, BorderLayout.CENTER);
        card.add(help, BorderLayout.SOUTH);

        values.put(key, value);
        cards.add(card);
    }
}
