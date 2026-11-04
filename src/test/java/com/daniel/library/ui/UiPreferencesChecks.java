package com.daniel.library.ui;

import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.SwingUtilities;

import java.awt.Dimension;
import java.awt.Rectangle;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;

/** Checks layout persistence in disposable files, never the user's actual preferences. */
public class UiPreferencesChecks {
    public static void main(String[] args) throws Exception {
        Path directory = Files.createTempDirectory("library-ui-settings-");
        try {
            Path file = directory.resolve("ui.properties");
            UiPreferences settings = new UiPreferences(file);
            Rectangle screen = new Rectangle(0, 0, 1600, 1000);
            check(settings.windowSize(screen).equals(new Dimension(1220, 800)), "Missing settings use defaults");
            settings.rememberWindowSize(new Dimension(1300, 850));
            SwingUtilities.invokeAndWait(() -> {
                RecordTable original = new RecordTable(LibrarySection.BOOKS);
                JTable table = (JTable) ((JScrollPane) original.getComponent(0)).getViewport().getView();
                table.getColumnModel().moveColumn(0, 2);
                table.getColumn("Title").setWidth(333);
                table.getColumn("ISBN").setWidth(155);
                original.rememberColumnWidths(settings, LibrarySection.BOOKS);
            });
            settings.save();
            UiPreferences loaded = new UiPreferences(file);
            check(loaded.windowSize(screen).equals(new Dimension(1300, 850)), "Window size survives a new session");
            SwingUtilities.invokeAndWait(() -> {
                RecordTable restored = new RecordTable(LibrarySection.BOOKS);
                restored.restoreColumnWidths(loaded, LibrarySection.BOOKS);
                JTable table = (JTable) ((JScrollPane) restored.getComponent(0)).getViewport().getView();
                check(table.getColumn("Title").getPreferredWidth() == 333, "Title width is restored by header");
                check(table.getColumn("ISBN").getPreferredWidth() == 155, "Column movement does not corrupt widths");
            });
            check(loaded.columnWidth(LibrarySection.COPIES, "Title", 190, 80) == 190,
                    "Section widths are independent");
            Files.writeString(file, "window.width=99999\nwindow.height=-20\ncolumn.BOOKS.Title=bad\n");
            UiPreferences invalid = new UiPreferences(file);
            check(invalid.windowSize(new Rectangle(0, 0, 900, 600)).equals(new Dimension(876, 576)),
                    "Saved sizes fit smaller screens while respecting available space");
            check(invalid.columnWidth(LibrarySection.BOOKS, "Title", 190, 80) == 190,
                    "Invalid column sizes fall back safely");
            invalid.rememberColumnWidth(LibrarySection.BOOKS, "Title", -5);
            check(invalid.columnWidth(LibrarySection.BOOKS, "Title", 190, 80) == 80, "Columns retain minimum width");
            invalid.rememberColumnWidth(LibrarySection.BOOKS, "Title", 999999);
            check(invalid.columnWidth(LibrarySection.BOOKS, "Title", 190, 80) == 2000, "Oversized widths are bounded");
            Files.writeString(file, "broken=\\uZZZZ\n");
            check(new UiPreferences(file).windowSize(screen).equals(new Dimension(1220, 800)),
                    "Malformed preferences do not block startup");
            Path blocked = directory.resolve("not-a-directory");
            Files.writeString(blocked, "file");
            new UiPreferences(blocked.resolve("ui.properties")).save();
            check(Files.readString(blocked).equals("file"), "An unwritable settings location does not overwrite data");
        } finally {
            try (var paths = Files.walk(directory)) {
                for (Path path : paths.sorted(Comparator.reverseOrder()).toList()) {
                    Files.deleteIfExists(path);
                }
            }
        }
        System.out.println("All UI preference checks passed.");
    }

    private static void check(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }
}
