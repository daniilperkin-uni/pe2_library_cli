package com.example.library.model;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class BookTest {

    private Book sample() {
        return new Book("978-3-12-345678-9", "Clean Code",
                List.of("Robert C. Martin"), 2008, "München",
                "mitp Verlag", 2);
    }

    @Test
    void constructor_populates_all_fields() {
        Book book = sample();
        assertThat(book.getIsbn()).isEqualTo("978-3-12-345678-9");
        assertThat(book.getTitle()).isEqualTo("Clean Code");
        assertThat(book.getAuthors()).containsExactly("Robert C. Martin");
        assertThat(book.getYear()).isEqualTo(2008);
        assertThat(book.getCity()).isEqualTo("München");
        assertThat(book.getPublisher()).isEqualTo("mitp Verlag");
        assertThat(book.getEdition()).isEqualTo(2);
    }

    @Test
    void getAuthors_returns_multiple_authors() {
        Book book = new Book("isbn", "Title",
                List.of("Autor A", "Autor B"), 2020, "X", "Y", 1);
        assertThat(book.getAuthors()).hasSize(2);
    }

    @Test
    void toString_contains_title_authors_year_and_isbn() {
        Book book = sample();
        String s = book.toString();
        assertThat(s).contains("Clean Code");
        assertThat(s).contains("Robert C. Martin");
        assertThat(s).contains("2008");
        assertThat(s).contains("978-3-12-345678-9");
    }
}
