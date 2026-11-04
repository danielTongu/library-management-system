package com.daniel.library.ui;

import com.daniel.library.service.Library;

import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

import java.awt.GraphicsEnvironment;

/** Starts Swing on its event thread with the shared workspace theme. */
public final class LibraryApp {
    private LibraryApp() {
    }

    /**
     * Schedules themed desktop construction on the Swing event thread.
     * @param library business API for the chosen storage
     * @param persistent true when records are saved to MySQL
     * @throws IllegalStateException when no graphical display is available
     */
    public static void start(Library library, boolean persistent) {
        if (GraphicsEnvironment.isHeadless()) {
            throw new IllegalStateException("The library requires a graphical display. Run it in IntelliJ on your desktop.");
        }
        SwingUtilities.invokeLater(() -> {
            LibraryTheme.apply();
            new LibraryWindow(library, persistent).setVisible(true);
        });
    }

    /**
     * Displays a startup error, using stderr only in a headless environment.
     * @param message user-readable failure explanation
     */
    public static void showStartupError(String message) {
        if (GraphicsEnvironment.isHeadless()) {
            System.err.println(message);
            return;
        }
        SwingUtilities.invokeLater(() -> {
            LibraryTheme.apply();
            JOptionPane.showMessageDialog(null, message, "Unable to open library", JOptionPane.ERROR_MESSAGE);
        });
    }
}
