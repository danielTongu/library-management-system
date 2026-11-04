package com.daniel.library.ui;


import javax.swing.BorderFactory;
import javax.swing.JTable;
import javax.swing.SwingConstants;
import javax.swing.table.DefaultTableCellRenderer;

import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Graphics;
import java.awt.RenderingHints;

/**
 * Alternating rows, readable identifiers, and textual status badges.
 */
final class RecordCellRenderer extends DefaultTableCellRenderer {
    private boolean badge;
    private Color badgeBackground;
    private Color badgeForeground;

    @Override
    public Component getTableCellRendererComponent(JTable table, Object value, boolean selected, boolean focus, int row, int column) {
        super.getTableCellRendererComponent(table, value, selected, false, row, column);
        setOpaque(true);
        setBackground(selected ? LibraryTheme.SELECTED : row % 2 == 0 ? Color.WHITE : LibraryTheme.STRIPE);
        setForeground(LibraryTheme.INK);
        setBorder(BorderFactory.createEmptyBorder(0, 14, 0, 14));

        String header = table.getColumnName(column);
        boolean identifier = header.equals("ISBN") || header.equals("Copy ID") || header.equals("Member ID");
        setFont(identifier ? new Font(Font.MONOSPACED, Font.PLAIN, 12) : LibraryTheme.font(13, Font.PLAIN));
        setHorizontalAlignment(value instanceof Number || header.equals("Value") ? SwingConstants.RIGHT : SwingConstants.LEFT);
        badge = header.equals("Status");

        String text = value == null ? "" : value.toString();
        setToolTipText(text.isEmpty() ? null : text);

        badgeBackground = new Color(0xEDF1F2);
        badgeForeground = new Color(0x56696E);

        switch (text) {
            case "Active", "Available" -> {
                badgeBackground = new Color(0xE1F2EB);
                badgeForeground = new Color(0x216B53);
            }
            case "Overdue" -> {
                badgeBackground = new Color(0xFCE8E7);
                badgeForeground = LibraryTheme.ERROR;
            }
            case "Checked out" -> {
                badgeBackground = new Color(0xFFF1D6);
                badgeForeground = new Color(0x8B601C);
            }
            case "Returned" -> {
                badgeBackground = new Color(0xE8EFFA);
                badgeForeground = new Color(0x3E6191);
            }
            default -> {
            }
        }
        return this;
    }

    @Override
    protected void paintComponent(Graphics graphics) {
        if (!badge) {
            super.paintComponent(graphics);
            return;
        }
        Graphics2D canvas = (Graphics2D) graphics.create();
        canvas.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        canvas.setColor(getBackground());
        canvas.fillRect(0, 0, getWidth(), getHeight());
        canvas.setFont(LibraryTheme.font(11, Font.BOLD));

        int width = canvas.getFontMetrics().stringWidth(getText()) + 20;
        int top = (getHeight() - 24) / 2;

        canvas.setColor(badgeBackground);
        canvas.fillRoundRect(12, top, width, 24, 12, 12);
        canvas.setColor(badgeForeground);
        canvas.drawString(getText(), 22, top + (24 - canvas.getFontMetrics().getHeight()) / 2 + canvas.getFontMetrics().getAscent());
        canvas.dispose();
    }
}
