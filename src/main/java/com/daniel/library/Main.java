package com.daniel.library;

import com.daniel.library.demo.SampleData;
import com.daniel.library.persistence.DatabaseSettings;
import com.daniel.library.persistence.JdbcLibraryRepository;
import com.daniel.library.service.Library;
import com.daniel.library.ui.LibraryApp;

import java.util.Arrays;
import java.util.List;

/** Starts the desktop and selects temporary or MySQL storage from launch arguments. */
public final class Main {
    private Main() {
    }
    // IntelliJ: Run > Edit Configurations > Main > Program arguments.
    // No arguments: temporary desktop library. --sample: desktop with sample data. --database: MySQL desktop.
    // --database --sample adds missing example records to MySQL.
    /**
     * Starts the GUI after validating launch options and initializing the selected storage.
     * @param args optional --sample and --database flags
     */
    public static void main(String[] args) {
        List<String> options = Arrays.asList(args);
        if (options.stream().anyMatch(option -> !List.of("--database", "--sample").contains(option))) {
            LibraryApp.showStartupError("Use no arguments, --sample, --database, or --database --sample.");
            return;
        }

        boolean persistent = options.contains("--database");
        try {
            Library library;
            if (persistent) {
                JdbcLibraryRepository repository = new JdbcLibraryRepository(DatabaseSettings.fromEnvironment());
                repository.initializeSchema();
                if (options.contains("--sample")) {
                    SampleData.populate(repository);
                }
                library = new Library(repository);
            } else {
                library = options.contains("--sample") ? new Library(SampleData.repository()) : new Library();
            }
            LibraryApp.start(library, persistent);
        } catch (IllegalArgumentException | IllegalStateException exception) {
            LibraryApp.showStartupError("Unable to start library: " + exception.getMessage());
        }
    }
}
