package com.daniel.library.ui;


import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;

import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Graphics;
import java.awt.RenderingHints;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Application shell, independently renderable without constructing a native JFrame.
 */
final class LibraryWorkspace extends JPanel {
    private final CardLayout layout = new CardLayout();
    private final JPanel pages = new JPanel(layout);
    private final Map<LibrarySection, JButton> navigation = new EnumMap<>(LibrarySection.class);
    private final JProgressBar progress = new JProgressBar();

    LibraryWorkspace(boolean persistent, Map<LibrarySection, JPanel> sections, JLabel status, JButton refresh) {
        super(new BorderLayout());
        setBackground(LibraryTheme.CANVAS);
        pages.setOpaque(false);
        sections.forEach((section, panel) -> pages.add(panel, section.name()));

        JPanel main = new JPanel(new BorderLayout());
        main.setOpaque(false);
        main.add(header(persistent), BorderLayout.NORTH);
        main.add(pages, BorderLayout.CENTER);
        main.add(footer(status, refresh), BorderLayout.SOUTH);

        add(sidebar(), BorderLayout.WEST);
        add(main, BorderLayout.CENTER);

        showSection(LibrarySection.OVERVIEW);
    }

    void showSection(LibrarySection section) {
        layout.show(pages, section.name());
        navigation.forEach((key, button) -> {
            button.putClientProperty("navigation.selected", key == section);
            button.setForeground(key == section ? Color.WHITE : new Color(0xC0D1D3));
            button.repaint();
        });
    }

    void setBusy(boolean busy) {
        progress.setVisible(busy);
    }

    private JPanel sidebar() {
        JPanel sidebar = new JPanel(new BorderLayout());
        sidebar.setBackground(LibraryTheme.SIDEBAR);
        sidebar.setPreferredSize(new Dimension(196, 0));
        sidebar.setBorder(BorderFactory.createEmptyBorder(28, 16, 22, 16));

        JLabel brand = new JLabel("Library", new SectionIcon(LibrarySection.BOOKS), JLabel.LEFT);
        brand.setIconTextGap(12);
        brand.setForeground(Color.WHITE);
        brand.setFont(LibraryTheme.font(23, Font.BOLD));

        JPanel menu = new JPanel();
        menu.setOpaque(false);
        menu.setLayout(new BoxLayout(menu, BoxLayout.Y_AXIS));
        menu.setBorder(BorderFactory.createEmptyBorder(34, 0, 0, 0));

        for (LibrarySection section : List.of(LibrarySection.OVERVIEW, LibrarySection.BOOKS, LibrarySection.COPIES, LibrarySection.MEMBERS, LibrarySection.LOANS)) {
            JButton button = LibraryTheme.navigation(section.title());
            button.setHorizontalAlignment(JButton.LEFT);
            button.setIcon(new SectionIcon(section));
            button.setIconTextGap(12);
            button.setAlignmentX(Component.LEFT_ALIGNMENT);
            button.setMaximumSize(new Dimension(Integer.MAX_VALUE, 44));
            button.addActionListener(event -> showSection(section));
            navigation.put(section, button);
            menu.add(button);
            menu.add(Box.createVerticalStrut(6));
        }

        JLabel help = new JLabel("<html>Your library, organized.<br>Books, readers, and<br>borrowing.</html>");
        help.setOpaque(false);
        help.setForeground(new Color(0xA6BEC1));
        help.setFont(LibraryTheme.font(12, Font.PLAIN));
        help.setBorder(BorderFactory.createEmptyBorder(16, 8, 0, 8));

        sidebar.add(brand, BorderLayout.NORTH);
        sidebar.add(menu, BorderLayout.CENTER);
        sidebar.add(help, BorderLayout.SOUTH);

        return sidebar;
    }

    private JPanel header(boolean persistent) {
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(LibraryTheme.SURFACE);
        header.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, LibraryTheme.BORDER),
                BorderFactory.createEmptyBorder(14, 26, 14, 26)));

        JLabel title = new JLabel("LIBRARY WORKSPACE");
        title.setFont(LibraryTheme.font(11, Font.BOLD));
        title.setForeground(LibraryTheme.MUTED);

        JPanel mode = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        mode.setOpaque(false);

        JLabel explanation = new JLabel(persistent ? "Changes save automatically" : "Records clear when you close");
        explanation.setFont(LibraryTheme.font(12, Font.PLAIN));
        explanation.setForeground(LibraryTheme.MUTED);

        JLabel badge = new JLabel(persistent ? "Saved library" : "Temporary library");
        badge.setOpaque(true);
        badge.setBackground(persistent ? LibraryTheme.SELECTED : new Color(0xFFF2D8));
        badge.setForeground(persistent ? LibraryTheme.ACCENT : new Color(0x8B601C));
        badge.setFont(LibraryTheme.font(11, Font.BOLD));
        badge.setBorder(BorderFactory.createEmptyBorder(6, 10, 6, 10));
        badge.setToolTipText(persistent ? "Connected to MySQL" : "In-memory session; records are temporary");

        mode.add(explanation);
        mode.add(badge);

        header.add(title, BorderLayout.WEST);
        header.add(mode, BorderLayout.EAST);
        return header;
    }

    private JPanel footer(JLabel status, JButton refresh) {
        JPanel footer = new JPanel(new BorderLayout(14, 0));
        footer.setBackground(LibraryTheme.SURFACE);
        footer.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 0, 0, LibraryTheme.BORDER),
                BorderFactory.createEmptyBorder(10, 26, 10, 26)));

        status.setFont(LibraryTheme.font(12, Font.PLAIN));
        status.setForeground(LibraryTheme.MUTED);

        progress.setIndeterminate(true);
        progress.setPreferredSize(new Dimension(90, 5));
        progress.setVisible(false);

        JPanel controls = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 0));
        controls.setOpaque(false);
        controls.add(progress);
        controls.add(refresh);

        footer.add(status, BorderLayout.CENTER);
        footer.add(controls, BorderLayout.EAST);
        return footer;
    }

    private record SectionIcon(LibrarySection section) implements Icon {
        @Override
        public void paintIcon(Component component, Graphics graphics, int x, int y) {
            Graphics2D canvas = (Graphics2D) graphics.create();
            canvas.translate(x, y);
            canvas.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            canvas.setColor(component.getForeground());
            canvas.setStroke(new BasicStroke(1.5f));

            switch (section) {
                case OVERVIEW -> {
                    for (int row = 0; row < 2; row++) {
                        for (int column = 0; column < 2; column++) {
                            canvas.drawRoundRect(2 + column * 10, 2 + row * 10, 6, 6, 2, 2);
                        }
                    }
                }
                case BOOKS -> {
                    canvas.drawRoundRect(2, 2, 5, 17, 2, 2);
                    canvas.drawRoundRect(10, 2, 9, 17, 2, 2);
                    canvas.drawLine(12, 7, 17, 7);
                }
                case COPIES -> {
                    canvas.drawRoundRect(6, 2, 13, 14, 2, 2);
                    canvas.drawRoundRect(2, 6, 13, 14, 2, 2);
                }
                case MEMBERS -> {
                    canvas.drawOval(7, 2, 7, 7);
                    canvas.drawArc(3, 12, 15, 15, 0, 180);
                }
                case LOANS -> {
                    canvas.drawLine(2, 7, 19, 7);
                    canvas.drawLine(15, 3, 19, 7);
                    canvas.drawLine(19, 7, 15, 11);
                    canvas.drawLine(19, 16, 2, 16);
                    canvas.drawLine(6, 12, 2, 16);
                    canvas.drawLine(2, 16, 6, 20);
                }
            }
            canvas.dispose();
        }

        @Override
        public int getIconWidth() {
            return 22;
        }

        @Override
        public int getIconHeight() {
            return 22;
        }
    }
}
