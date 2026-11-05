package com.daniel.library.ui;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JLabel;
import javax.swing.JComponent;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;
import javax.swing.plaf.basic.BasicButtonUI;
import javax.swing.plaf.basic.BasicComboBoxUI;
import javax.swing.plaf.basic.BasicScrollBarUI;
import javax.swing.plaf.basic.BasicTextFieldUI;
import javax.swing.plaf.nimbus.NimbusLookAndFeel;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Graphics;
import java.awt.Rectangle;
import java.awt.RenderingHints;

/** Shared desktop colors, typography, and controls; no additional look-and-feel dependency. */
final class LibraryTheme {
    /** Workspace page background. */
    static final Color CANVAS = new Color(0xF3F6F5);

    /** Control and table surface background. */
    static final Color SURFACE = Color.WHITE;

    /** Primary text color. */
    static final Color INK = new Color(0x1D3339);

    /** Secondary text color. */
    static final Color MUTED = new Color(0x5F7379);

    /** Subtle control and card outlines. */
    static final Color BORDER = new Color(0xDCE5E3);

    /** Primary action and progress color. */
    static final Color ACCENT = new Color(0x14796D);

    /** Navigation background. */
    static final Color SIDEBAR = new Color(0x142D34);

    /** Record selected background. */
    static final Color SELECTED = new Color(0xE1F1EC);

    /** Alternating table-row background. */
    static final Color STRIPE = new Color(0xF8FAF9);

    /** Error and overdue emphasis color. */
    static final Color ERROR = new Color(0xA83F40);

    private LibraryTheme() {
    }

    /**
     * Installs shared look-and-feel defaults before constructing the workspace.
     * Call on the Swing event thread.
     */
    static void apply() {
        try {
            UIManager.setLookAndFeel(new NimbusLookAndFeel());
        } catch (UnsupportedLookAndFeelException exception) {
            // Explicit component styling still applies if Nimbus is unavailable.
        }

        UIManager.put("defaultFont", font(13, Font.PLAIN));
        UIManager.put("control", CANVAS);
        UIManager.put("text", INK);
        UIManager.put("nimbusBase", ACCENT);
        UIManager.put("nimbusBlueGrey", BORDER);
        UIManager.put("nimbusFocus", ACCENT);
        UIManager.put("nimbusSelectionBackground", SELECTED);
        UIManager.put("Panel.background", CANVAS);
        UIManager.put("Label.foreground", INK);
        UIManager.put("Label.font", font(13, Font.PLAIN));
        UIManager.put("Button.font", font(13, Font.PLAIN));
        UIManager.put("ScrollBar.width", 10);
        UIManager.put("TextField.font", font(14, Font.PLAIN));
        UIManager.put("ComboBox.font", font(13, Font.PLAIN));
        UIManager.put("Table.font", font(13, Font.PLAIN));
        UIManager.put("TableHeader.font", font(12, Font.BOLD));
        UIManager.put("TableHeader.background", STRIPE);
        UIManager.put("OptionPane.background", SURFACE);
        UIManager.put("OptionPane.messageFont", font(14, Font.PLAIN));
    }

    /**
     * Creates a cross-platform logical UI font.
     * @param size point size
     * @param style Font.PLAIN, Font.BOLD, or another supported font style
     * @return the configured font
     */
    static Font font(int size, int style) {
        return new Font(Font.SANS_SERIF, style, size);
    }

    /**
     * Creates a styled action button with keyboard focus indication.
     * @param label visible action label
     * @param primary whether to emphasize the action
     * @return styled button ready for an action listener
     */
    static JButton button(String label, boolean primary) {
        return new StyledButton(label, primary, false);
    }

    /**
     * Creates a sidebar navigation button.
     * @param label visible destination name
     * @return button whose selected state is set by the workspace
     */
    static JButton navigation(String label) {
        return new StyledButton(label, false, true);
    }

    /**
     * Applies consistent field borders, font, and colors.
     * @param field field to style on the Swing event thread
     */
    static void textField(JTextField field) {
        field.setUI(new BasicTextFieldUI());
        field.setFont(font(14, Font.PLAIN));
        field.setBackground(SURFACE);
        field.setForeground(INK);
        field.setCaretColor(ACCENT);

        field.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER, 1, true),
                BorderFactory.createEmptyBorder(7, 10, 7, 10)
        ));
    }

    /**
     * Applies shared dropdown styling and complete-value tooltips.
     * @param choices dropdown to style on the Swing event thread
     * @param <T> choice value type
     */
    static <T> void comboBox(JComboBox<T> choices) {
        choices.setUI(new BasicComboBoxUI() {
            @Override
            protected JButton createArrowButton() {
                JButton arrow = new JButton("▾");
                arrow.setUI(new BasicButtonUI());
                arrow.setContentAreaFilled(false);
                arrow.setBorder(BorderFactory.createEmptyBorder(0, 8, 0, 8));
                arrow.setForeground(MUTED);
                return arrow;
            }
        });

        choices.setFont(font(13, Font.PLAIN));
        choices.setBackground(SURFACE);
        choices.setForeground(INK);
        choices.setBorder(BorderFactory.createLineBorder(BORDER, 1, true));
        DefaultListCellRenderer renderer = new DefaultListCellRenderer();

        choices.setRenderer((list, value, index, selected, focus) -> {
            JLabel label = (JLabel) renderer.getListCellRendererComponent(list, value, index, selected, focus);
            label.setText(value == null ? "Choose…" : value.toString());
            label.setBackground(selected ? SELECTED : SURFACE);
            label.setForeground(INK);
            label.setBorder(BorderFactory.createEmptyBorder(7, 10, 7, 10));
            label.setToolTipText(value == null ? null : value.toString());
            return label;
        });
    }

    /**
     * Applies the compact scrollbar appearance and record-row scrolling increment.
     * @param pane scroll pane to style on the Swing event thread
     */
    static void scrollPane(JScrollPane pane) {
        pane.getVerticalScrollBar().setUI(new ThinScrollBar());
        pane.getHorizontalScrollBar().setUI(new ThinScrollBar());
        pane.getVerticalScrollBar().setUnitIncrement(42);
    }

    private static class ThinScrollBar extends BasicScrollBarUI {
        @Override
        protected JButton createDecreaseButton(int orientation) {
            return emptyButton();
        }

        @Override
        protected JButton createIncreaseButton(int orientation) {
            return emptyButton();
        }

        private JButton emptyButton() {
            JButton button = new JButton();
            button.setPreferredSize(new Dimension(0, 0));
            button.setMinimumSize(new Dimension(0, 0));
            button.setMaximumSize(new Dimension(0, 0));
            return button;
        }

        @Override
        protected void paintTrack(Graphics graphics, JComponent component, Rectangle bounds) {
            graphics.setColor(STRIPE);
            graphics.fillRect(bounds.x, bounds.y, bounds.width, bounds.height);
        }

        @Override
        protected void paintThumb(Graphics graphics, JComponent component, Rectangle bounds) {
            Graphics2D canvas = (Graphics2D) graphics.create();
            canvas.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            canvas.setColor(isThumbRollover() ? MUTED : new Color(0xBACBC6));
            canvas.fillRoundRect(bounds.x + 2, bounds.y + 2, bounds.width - 4, bounds.height - 4, 6, 6);
            canvas.dispose();
        }
    }

    private static class StyledButton extends JButton {
        private final boolean primary;
        private final boolean navigation;

        StyledButton(String label, boolean primary, boolean navigation) {
            super(label);
            this.primary = primary;
            this.navigation = navigation;
            setUI(new BasicButtonUI());
            setFont(font(13, Font.BOLD));
            setForeground(navigation || primary ? Color.WHITE : INK);
            setBorder(BorderFactory.createEmptyBorder(11, 16, 11, 16));
            setContentAreaFilled(false);
            setOpaque(false);
            setRolloverEnabled(true);
            setFocusPainted(false);
        }

        @Override
        protected void paintComponent(Graphics graphics) {
            Graphics2D canvas = (Graphics2D) graphics.create();
            canvas.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            boolean selected = Boolean.TRUE.equals(getClientProperty("navigation.selected"));
            Color background = navigation ? selected ? new Color(0x28505A) : SIDEBAR : primary ? ACCENT : SURFACE;

            if (getModel().isRollover() || getModel().isPressed()) {
                background = navigation ? new Color(0x315B64) : primary ? new Color(0x0D665C) : STRIPE;
            }

            if (!isEnabled()) {
                background = BORDER;
            }

            canvas.setColor(background);
            canvas.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 10, 10);

            if (!navigation && !primary) {
                canvas.setColor(BORDER);
                canvas.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 10, 10);
            }

            if (hasFocus()) {
                canvas.setColor(navigation || primary ? new Color(0xA3D8CA) : ACCENT);
                canvas.drawRoundRect(2, 2, getWidth() - 5, getHeight() - 5, 8, 8);
            }

            canvas.dispose();
            super.paintComponent(graphics);
        }
    }
}
