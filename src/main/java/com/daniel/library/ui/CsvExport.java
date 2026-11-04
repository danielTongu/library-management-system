package com.daniel.library.ui;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

/**
 * Shared UTF-8 CSV format. Existing files are never replaced.
 */
public final class CsvExport {
    private CsvExport() {
    }

    /**
     * Writes a UTF-8 CSV report through a temporary file in the destination directory.
     * All fields are quoted; embedded quotes are doubled. Existing files are not replaced.
     *
     * @param destination new report filename; .csv is appended when absent
     * @param columns     ordered report headers
     * @param rows        values in displayed row and column order
     * @return absolute path of the completed report
     * @throws IllegalArgumentException if rows do not match the column count
     * @throws IOException              if the destination is invalid, already exists, or cannot be written
     */
    public static Path write(Path destination, List<String> columns, List<Object[]> rows) throws IOException {
        if (columns.isEmpty() || rows.stream().anyMatch(row -> row.length != columns.size())) {
            throw new IllegalArgumentException("Export rows must match their column headers.");
        }

        Path target = destination.toAbsolutePath().normalize();
        if (target.getFileName() == null || Files.isDirectory(target)) {
            throw new IOException("Choose a filename rather than a folder.");
        }

        if (!target.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(".csv")) {
            target = target.resolveSibling(target.getFileName() + ".csv");
        }
        if (Files.exists(target)) {
            throw new IOException("File already exists. Choose a new filename.");
        }

        Path temporary = Files.createTempFile(target.getParent(), ".library-export-", ".tmp");
        try {
            try (BufferedWriter writer = Files.newBufferedWriter(temporary, StandardCharsets.UTF_8)) {
                writeRow(writer, columns.toArray());
                for (Object[] row : rows) {
                    writeRow(writer, row);
                }
            }
            return Files.move(temporary, target);
        } finally {
            Files.deleteIfExists(temporary);
        }
    }

    private static void writeRow(BufferedWriter writer, Object[] values) throws IOException {
        String line = Arrays.stream(values)
                .map(value -> value == null ? "" : value.toString())
                .map(value -> "\"" + value.replace("\"", "\"\"") + "\"")
                .collect(Collectors.joining(","));

        writer.write(line);
        writer.write("\r\n");
    }
}
