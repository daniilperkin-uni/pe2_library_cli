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
     * This utility class cannot be instantiated.
     */
    private CsvImporter() {
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
        if (input == null) {
            throw new IllegalArgumentException("Input stream must not be null");
        }
        List<String[]> rows = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(input, StandardCharsets.UTF_8))) {
            String line;
            boolean firstLine = true;
            while ((line = reader.readLine()) != null) {
                if (line.isEmpty()) {
                    continue;
                }
                if (firstLine && hasHeader) {
                    firstLine = false;
                    continue;
                }
                firstLine = false;
                rows.add(parseLine(line));
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
