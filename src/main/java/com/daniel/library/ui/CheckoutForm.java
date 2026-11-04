package com.daniel.library.ui;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;

import java.awt.Component;
import java.awt.Dimension;

/** Selects checkout identities from the latest display snapshot without reading storage on Swing's event thread. */
final class CheckoutForm extends JPanel {
    private final JComboBox<Choice> copies = new JComboBox<>();
    private final JComboBox<Choice> members = new JComboBox<>();

    CheckoutForm(LibraryRecords records, String selectedCopyId) {
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        setBackground(LibraryTheme.SURFACE);
        setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));
        for (Object[] row : records.copies()) {
            if (row[LibrarySection.COPIES.columnIndex("Status")].equals("Available")) {
                copies.addItem(new Choice(row[LibrarySection.COPIES.columnIndex("Copy ID")].toString(),
                        row[LibrarySection.COPIES.columnIndex("Title")].toString()));
            }
        }
        for (Object[] row : records.members()) {
            members.addItem(new Choice(row[LibrarySection.MEMBERS.columnIndex("Member ID")].toString(),
                    row[LibrarySection.MEMBERS.columnIndex("Name")].toString()));
        }
        copies.setSelectedIndex(-1);
        members.setSelectedIndex(-1);
        for (int index = 0; index < copies.getItemCount(); index++) {
            if (copies.getItemAt(index).id().equals(selectedCopyId)) {
                copies.setSelectedIndex(index);
                break;
            }
        }
        copies.setName("checkoutCopy");
        members.setName("checkoutMember");
        addChoice("Available copy (title — Copy ID)", copies);
        addChoice("Borrower (name — Member ID)", members);
    }

    boolean hasAvailableCopies() {
        return copies.getItemCount() > 0;
    }

    boolean hasMembers() {
        return members.getItemCount() > 0;
    }

    String selectedCopyId() {
        return copies.getSelectedItem() instanceof Choice choice ? choice.id() : null;
    }

    String selectedMemberId() {
        return members.getSelectedItem() instanceof Choice choice ? choice.id() : null;
    }

    private void addChoice(String text, JComboBox<Choice> choices) {
        JLabel label = new JLabel(text);
        label.setLabelFor(choices);
        label.setAlignmentX(Component.LEFT_ALIGNMENT);
        label.setForeground(LibraryTheme.INK);
        LibraryTheme.comboBox(choices);
        choices.setAlignmentX(Component.LEFT_ALIGNMENT);
        choices.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
        choices.setPreferredSize(new Dimension(480, 40));
        choices.setPrototypeDisplayValue(new Choice("M000", "Select a record"));
        add(label);
        add(Box.createVerticalStrut(6));
        add(choices);
        add(Box.createVerticalStrut(16));
    }

    private record Choice(String id, String label) {
        @Override
        public String toString() {
            return label + " — " + id;
        }
    }
}
