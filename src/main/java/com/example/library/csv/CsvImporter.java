package com.example.library.csv;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * A shared CSV reader that correctly handles quoted fields and UTF-8 encoding.
 *
 * <p>This parser supports the RFC 4180 subset used by the library's CSV
 * fixtures: fields may be enclosed in double quotes, and a pair of double
 * quotes inside a quoted field represents a literal quote character.</p>
 */
public final class CsvImporter {

    /**
     * One parsed CSV row together with the physical line it came from.
     *
     * <p>Importers use the line number to report a broken record precisely
     * instead of failing with an anonymous parse error.</p>
     *
     * @param lineNumber 1-based line number in the source
     * @param fields     the parsed field values
     */
    public record Row(int lineNumber, String[] fields) {
    }

    /**
     * This utility class cannot be instantiated.
     */
    private CsvImporter() {
    }

    /**
     * Checks that a row carries at least the expected number of columns.
     *
     * <p>Without this guard a truncated record (for example after a killed
     * write) would blow up later with a bare
     * {@link ArrayIndexOutOfBoundsException}, and the application could not
     * even say which file broke. The message names the source and line.</p>
     *
     * @param row      the row to check
     * @param source   the file path or classpath resource being imported
     * @param expected minimum number of columns
     * @throws IllegalArgumentException when the row has too few columns
     */
    public static void requireColumns(Row row, String source, int expected) {
        int found = row.fields().length;
        if (found < expected) {
            throw new IllegalArgumentException("Ungültiger Datensatz in " + source
                    + ", Zeile " + row.lineNumber() + ": erwartet mindestens " + expected
                    + " Spalten, gefunden " + found + ".");
        }
    }

    /**
     * Opens a CSV source: a filesystem path if such a file exists, otherwise a
     * classpath resource. This lets saved state override the bundled fixtures.
     *
     * @param anchor       class used to resolve classpath resources
     * @param resourcePath file path or classpath resource path
     * @return an open stream, or {@code null} if neither exists
     */
    public static InputStream open(Class<?> anchor, String resourcePath) {
        java.nio.file.Path file = java.nio.file.Path.of(resourcePath);
        if (java.nio.file.Files.isRegularFile(file)) {
            try {
                return java.nio.file.Files.newInputStream(file);
            } catch (IOException e) {
                throw new java.io.UncheckedIOException(e);
            }
        }
        return anchor.getResourceAsStream(resourcePath);
    }

    /**
     * Parses a CSV stream into a list of rows, where each row is an array of
     * field strings.
     *
     * @param input     the input stream to read from (UTF-8 encoded)
     * @param hasHeader if {@code true}, the first non-empty line is skipped
     * @return a list of parsed rows
     * @throws IOException              if an I/O error occurs or a quoted field is not properly closed
     * @throws IllegalArgumentException if {@code input} is {@code null}
     */
    public static List<String[]> parse(InputStream input, boolean hasHeader) throws IOException {
        List<String[]> rows = new ArrayList<>();
        for (Row row : parseRows(input, hasHeader)) {
            rows.add(row.fields());
        }
        return rows;
    }

    /**
     * Parses a CSV stream into rows that remember their physical line number.
     *
     * <p>Blank lines and the header are skipped, but the numbering still
     * follows the file, so a reported line can be found in an editor.</p>
     *
     * @param input     the input stream to read from (UTF-8 encoded)
     * @param hasHeader if {@code true}, the first non-empty line is skipped
     * @return a list of parsed rows with their line numbers
     * @throws IOException              if an I/O error occurs or a quoted field is not properly closed
     * @throws IllegalArgumentException if {@code input} is {@code null}
     */
    public static List<Row> parseRows(InputStream input, boolean hasHeader) throws IOException {
        if (input == null) {
            throw new IllegalArgumentException("Input stream must not be null");
        }
        List<Row> rows = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(input, StandardCharsets.UTF_8))) {
            String line;
            int lineNumber = 0;
            boolean firstLine = true;
            while ((line = reader.readLine()) != null) {
                lineNumber++;
                if (line.isEmpty()) {
                    continue;
                }
                if (firstLine && hasHeader) {
                    firstLine = false;
                    continue;
                }
                firstLine = false;
                rows.add(new Row(lineNumber, parseLine(line)));
            }
        }
        return rows;
    }

    /**
     * Parses a single CSV line into an array of fields, honouring quoted
     * fields and embedded escaped quotes.
     *
     * @param line the raw CSV line
     * @return an array of field values
     * @throws IOException if a quoted field is opened but never closed
     */
    static String[] parseLine(String line) throws IOException {
        List<String> fields = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean inQuotes = false;
        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (inQuotes) {
                if (c == '"') {
                    if (i + 1 < line.length() && line.charAt(i + 1) == '"') {
                        current.append('"');
                        i++;
                    } else {
                        inQuotes = false;
                    }
                } else {
                    current.append(c);
                }
            } else {
                if (c == '"') {
                    inQuotes = true;
                } else if (c == ',') {
                    fields.add(current.toString());
                    current = new StringBuilder();
                } else {
                    current.append(c);
                }
            }
        }
        if (inQuotes) {
            throw new IOException("Malformed CSV: unclosed quoted field in line: " + line);
        }
        fields.add(current.toString());
        return fields.toArray(new String[0]);
    }
}
