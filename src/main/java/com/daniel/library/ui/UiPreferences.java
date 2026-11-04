package com.daniel.library.ui;

import java.awt.Dimension;
import java.awt.Rectangle;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

/** Local window and column sizes; optional settings never contain library records or credentials. */
final class UiPreferences {
    private final Path file;
    private final Properties settings = new Properties();

    UiPreferences() {
        this(Path.of(System.getProperty("user.home"), ".library-management-system", "ui.properties"));
    }

    UiPreferences(Path file) {
        this.file = file;
        try (InputStream input = Files.newInputStream(file)) {
            settings.load(input);
        } catch (IOException | IllegalArgumentException | SecurityException ignored) {
            settings.clear();
            // Missing, malformed, or inaccessible preferences fall back to the normal layout.
        }
    }

    Dimension windowSize(Rectangle screen) {
        int maximumWidth = Math.max(1, screen.width - 24);
        int maximumHeight = Math.max(1, screen.height - 24);
        int width = bounded("window.width", 1220, Math.min(1040, maximumWidth), maximumWidth);
        int height = bounded("window.height", 800, Math.min(660, maximumHeight), maximumHeight);
        return new Dimension(width, height);
    }

    void rememberWindowSize(Dimension size) {
        settings.setProperty("window.width", Integer.toString(size.width));
        settings.setProperty("window.height", Integer.toString(size.height));
    }

    int columnWidth(LibrarySection section, String header, int fallback, int minimum) {
        return bounded(columnKey(section, header), fallback, minimum, 2000);
    }

    void rememberColumnWidth(LibrarySection section, String header, int width) {
        settings.setProperty(columnKey(section, header), Integer.toString(width));
    }

    void save() {
        try {
            Files.createDirectories(file.toAbsolutePath().getParent());
            try (OutputStream output = Files.newOutputStream(file)) {
                settings.store(output, "Library window and column sizes");
            }
        } catch (IOException | SecurityException ignored) {
            // A layout preference failure must not prevent closing or affect saved library data.
        }
    }

    private int bounded(String key, int fallback, int minimum, int maximum) {
        int value;
        try {
            value = Integer.parseInt(settings.getProperty(key, Integer.toString(fallback)));
        } catch (NumberFormatException exception) {
            value = fallback;
        }
        return Math.clamp(value, minimum, maximum);
    }

    private static String columnKey(LibrarySection section, String header) {
        return "column." + section.name() + "." + header;
    }
}
