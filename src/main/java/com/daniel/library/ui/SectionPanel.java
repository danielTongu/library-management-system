package com.daniel.library.ui;


import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextArea;

import java.awt.BorderLayout;
import java.awt.Font;

/** Consistent page heading, explanation, actions, and records. */
final class SectionPanel extends JPanel {
    SectionPanel(LibrarySection section, JComponent records, JPanel actions) {
        super(new BorderLayout(0, 18));
        setOpaque(false);
        setBorder(BorderFactory.createEmptyBorder(24, 26, 20, 26));

        JLabel title = new JLabel(section.title());
        title.setFont(LibraryTheme.font(28, Font.BOLD));
        title.setForeground(LibraryTheme.INK);

        JTextArea introduction = new JTextArea(section.guidance(), 3, 0);
        introduction.setEditable(false);
        introduction.setFocusable(false);
        introduction.setLineWrap(true);
        introduction.setWrapStyleWord(true);
        introduction.setOpaque(false);
        introduction.setBackground(LibraryTheme.CANVAS);
        introduction.setFont(LibraryTheme.font(13, Font.PLAIN));
        introduction.setForeground(LibraryTheme.MUTED);
        introduction.setBorder(BorderFactory.createEmptyBorder(8, 0, 0, 0));

        JPanel heading = new JPanel(new BorderLayout());
        heading.setOpaque(false);
        heading.add(title, BorderLayout.NORTH);
        heading.add(introduction, BorderLayout.CENTER);

        if (actions.getComponentCount() > 0) {
            actions.setOpaque(false);
            actions.setBorder(BorderFactory.createEmptyBorder(8, 0, 0, 0));
            heading.add(actions, BorderLayout.SOUTH);
        }

        add(heading, BorderLayout.NORTH);
        add(records, BorderLayout.CENTER);
    }
}
