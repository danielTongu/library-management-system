package com.daniel.library.ui;

import com.daniel.library.demo.SampleData;
import com.daniel.library.service.Library;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/** Verifies CSV interoperability, file preservation, and exporting filtered desktop records. */
public class CsvExportChecks {
    public static void main(String[] args) throws Exception {
        Path folder = Files.createTempDirectory("library-export-check-");
        try {
            List<Object[]> rows = List.of(new Object[]{"Java, 2nd", "Quote \"inside\"\nline", new BigDecimal("1.50")},
                    new Object[]{"Émile", null, new BigDecimal("0.00")});
            Path saved = CsvExport.write(folder.resolve("records"), List.of("Title", "Note", "Fine"), rows);
            String expected = "\"Title\",\"Note\",\"Fine\"\r\n"
                    + "\"Java, 2nd\",\"Quote \"\"inside\"\"\nline\",\"1.50\"\r\n"
                    + "\"Émile\",\"\",\"0.00\"\r\n";
            check(saved.getFileName().toString().equals("records.csv"), "CSV extension added");
            check(Files.readString(saved).equals(expected), "UTF-8, commas, quotes, multiline values, and nulls");
            expectIoFailure(() -> CsvExport.write(saved, List.of("Other"), List.of()));
            check(Files.readString(saved).equals(expected), "Existing file unchanged");
            expectIoFailure(() -> CsvExport.write(folder, List.of("Other"), List.of()));
            expectIoFailure(() -> CsvExport.write(folder.resolve("missing/records.csv"), List.of("Other"), List.of()));
            Path empty = CsvExport.write(folder.resolve("empty.CSV"), List.of("ISBN"), List.of());
            check(Files.readString(empty).equals("\"ISBN\"\r\n"), "Empty exports contain headers only");
            try {
                CsvExport.write(folder.resolve("invalid.csv"), List.of("One"), rows);
                throw new AssertionError("Mismatched columns must fail");
            } catch (IllegalArgumentException expectedFailure) {
                check(!Files.exists(folder.resolve("invalid.csv")), "Invalid export creates no file");
            }

            Library library = new Library(SampleData.repository());
            LibraryRecords records = LibraryRecords.load(library, "Export");
            Path filteredPath = folder.resolve("filtered books.csv");
            List<Object[]> filtered = records.books().stream()
                    .filter(row -> RecordFilter.matches(row, "Clean", 3, "Active")).toList();
            CsvExport.write(filteredPath, LibrarySection.BOOKS.columns(), filtered);
            String csv = Files.readString(filteredPath);
            check(csv.contains("Clean Code") && !csv.contains("Effective Java"), "Filtered desktop records exported");
            check(library.getBooks().size() == 3, "Export leaves library unchanged");
            Path overviewPath = folder.resolve("overview.csv");
            CsvExport.write(overviewPath, LibrarySection.OVERVIEW.columns(), records.overview());
            String overview = Files.readString(overviewPath);
            check(overview.startsWith("\"Measure\",\"Value\"\r\n"), "Overview exports its own columns");
            check(overview.contains("\"Available copies\",\"2\""), "Overview exports calculated totals");
            try (var files = Files.list(folder)) {
                check(files.noneMatch(path -> path.getFileName().toString().endsWith(".tmp")), "No temporary files left");
            }
            System.out.println("All CSV export checks passed.");
        } finally {
            try (var files = Files.list(folder)) {
                for (Path file : files.toList()) {
                    Files.delete(file);
                }
            }
            Files.delete(folder);
        }
    }

    private static void expectIoFailure(FileAction action) throws IOException {
        try {
            action.run();
            throw new AssertionError("Invalid destination must fail");
        } catch (IOException expected) {
            // Expected: existing file, folder, or nonexistent parent folder.
        }
    }

    private static void check(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }

    @FunctionalInterface
    private interface FileAction {
        void run() throws IOException;
    }
}
