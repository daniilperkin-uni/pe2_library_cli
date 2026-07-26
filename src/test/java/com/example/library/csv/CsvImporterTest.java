package com.example.library.csv;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CsvImporterTest {

    private List<String[]> parse(String csv, boolean hasHeader) throws IOException {
        return CsvImporter.parse(
                new ByteArrayInputStream(csv.getBytes(StandardCharsets.UTF_8)), hasHeader);
    }

    @Test
    void parse_skips_header_when_requested() throws IOException {
        List<String[]> rows = parse("a,b,c\n1,2,3\n", true);
        assertThat(rows).hasSize(1);
        assertThat(rows.get(0)).containsExactly("1", "2", "3");
    }

    @Test
    void parse_keeps_first_line_when_no_header() throws IOException {
        List<String[]> rows = parse("1,2,3\n", false);
        assertThat(rows).hasSize(1);
        assertThat(rows.get(0)).containsExactly("1", "2", "3");
    }

    @Test
    void parse_handles_quoted_fields_with_commas() throws IOException {
        List<String[]> rows = parse("h1,h2\n\"a,b\",\"c\"\n", true);
        assertThat(rows).hasSize(1);
        assertThat(rows.get(0)).containsExactly("a,b", "c");
    }

    @Test
    void parse_handles_escaped_quotes_inside_quoted_field() throws IOException {
        List<String[]> rows = parse("h\n\"He said \"\"hi\"\"\"\n", true);
        assertThat(rows).hasSize(1);
        assertThat(rows.get(0)[0]).isEqualTo("He said \"hi\"");
    }

    @Test
    void parse_skips_empty_lines() throws IOException {
        List<String[]> rows = parse("h1,h2\n\n1,2\n\n3,4\n", true);
        assertThat(rows).hasSize(2);
        assertThat(rows.get(0)).containsExactly("1", "2");
        assertThat(rows.get(1)).containsExactly("3", "4");
    }

    @Test
    void parse_single_field_row() throws IOException {
        List<String[]> rows = parse("solo\n", false);
        assertThat(rows).hasSize(1);
        assertThat(rows.get(0)).containsExactly("solo");
    }

    @Test
    void parse_trailing_empty_field() throws IOException {
        List<String[]> rows = parse("1,no,\n", false);
        assertThat(rows).hasSize(1);
        assertThat(rows.get(0)).containsExactly("1", "no", "");
    }

    @Test
    void parse_null_input_throws_illegal_argument() {
        assertThatThrownBy(() -> CsvImporter.parse(null, false))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void parse_malformed_unclosed_quote_throws_io_exception() {
        // opening quote never closed
        assertThatThrownBy(() -> parse("\"unclosed\n", false))
                .isInstanceOf(IOException.class)
                .hasMessageContaining("unclosed");
    }
}
