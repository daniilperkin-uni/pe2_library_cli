package com.example.library.manager;

import com.example.library.model.Book;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Scanner;

import static org.assertj.core.api.Assertions.assertThat;

class BookManagerTest {

    private BookManager bookManager;

    @BeforeEach
    void setUp() {
        bookManager = new BookManager();
        bookManager.importBooks("/bücher.csv");
    }

    @Test
    void importBooks_loads_all_rows_from_classpath() {
        assertThat(bookManager.getBooks()).hasSize(3);
    }

    @Test
    void importBooks_populates_fields_correctly() {
        Book hp = bookManager.getBook("3551551677");
        assertThat(hp).isNotNull();
        assertThat(hp.getTitle()).isEqualTo("Harry Potter und der Stein der Weisen");
        assertThat(hp.getAuthors()).containsExactly("Joanne K. Rowling");
        assertThat(hp.getYear()).isEqualTo(1998);
        assertThat(hp.getPublisher()).isEqualTo("Carlsen Verlag GmbH");
        assertThat(hp.getEdition()).isEqualTo(1);
    }

    @Test
    void importBooks_handles_quoted_field_with_escaped_quotes() {
        Book anhalter = bookManager.getBook("3036959548");
        assertThat(anhalter).isNotNull();
        assertThat(anhalter.getTitle())
                .isEqualTo("Per Anhalter durch die Galaxis: Band 1 der fünfbändigen \"Intergalaktischen Trilogie\"");
    }

    @Test
    void exists_returns_true_for_known_isbn() {
        assertThat(bookManager.exists("3608987010")).isTrue();
    }

    @Test
    void exists_returns_false_for_unknown_isbn() {
        assertThat(bookManager.exists("does-not-exist")).isFalse();
    }

    @Test
    void getBook_returns_null_for_unknown_isbn() {
        assertThat(bookManager.getBook("nope")).isNull();
    }

    @Test
    void deleteBook_removes_book_when_confirmed() {
        Scanner scanner = new Scanner("3551551677\n1\n");
        bookManager.deleteBook(scanner);
        assertThat(bookManager.exists("3551551677")).isFalse();
        assertThat(bookManager.getBooks()).hasSize(2);
    }

    @Test
    void deleteBook_cancels_when_minus_one_entered() {
        Scanner scanner = new Scanner("-1\n");
        bookManager.deleteBook(scanner);
        assertThat(bookManager.getBooks()).hasSize(3);
    }
}
