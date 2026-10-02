package com.example.library.manager;

import com.example.library.model.Book;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Scanner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

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
        bookManager.deleteBook(scanner, new BookCopyManager());
        assertThat(bookManager.exists("3551551677")).isFalse();
        assertThat(bookManager.getBooks()).hasSize(2);
    }

    @Test
    void deleteBook_cancels_when_minus_one_entered() {
        Scanner scanner = new Scanner("-1\n");
        bookManager.deleteBook(scanner, new BookCopyManager());
        assertThat(bookManager.getBooks()).hasSize(3);
    }

    @Test
    void deleteBook_refuses_when_copies_reference_the_isbn() {
        CustomerManager customerManager = new CustomerManager();
        customerManager.importCustomers("/benutzer.csv");
        BookCopyManager bookCopyManager = new BookCopyManager();
        bookCopyManager.importBookCopies("/buchkopien.csv", bookManager, customerManager);

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        PrintStream originalOut = System.out;
        try {
            System.setOut(new PrintStream(out, true, StandardCharsets.UTF_8));
            bookManager.deleteBook(new Scanner("3036959548\n-1\n"), bookCopyManager);
        } finally {
            System.setOut(originalOut);
        }

        assertThat(out.toString(StandardCharsets.UTF_8))
                .contains("hat noch Buchkopien und kann nicht gelöscht werden.");
        assertThat(bookManager.exists("3036959548")).isTrue();
        assertThat(bookManager.getBooks()).hasSize(3);
    }

    @Test
    void addBook_stores_all_fields() {
        Book book = bookManager.addBook("9783442267744", "Die Verwandlung",
                List.of("Franz Kafka"), 2003, "München", "dtv", 2);
        assertThat(book.getIsbn()).isEqualTo("9783442267744");
        assertThat(bookManager.getBook("9783442267744")).isSameAs(book);
        assertThat(book.getTitle()).isEqualTo("Die Verwandlung");
        assertThat(book.getAuthors()).containsExactly("Franz Kafka");
        assertThat(book.getYear()).isEqualTo(2003);
        assertThat(book.getEdition()).isEqualTo(2);
        assertThat(bookManager.getBooks()).hasSize(4);
    }

    @Test
    void addBook_rejects_duplicate_isbn() {
        assertThatThrownBy(() -> bookManager.addBook("3551551677", "Doppelt",
                List.of("Autor"), 2000, "Ort", "Verlag", 1))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Es existiert bereits ein Buch mit dieser ISBN: 3551551677");
        assertThat(bookManager.getBooks()).hasSize(3);
    }

    @Test
    void addBook_rejects_blank_isbn_and_title() {
        assertThatThrownBy(() -> bookManager.addBook("  ", "Titel",
                List.of("Autor"), 2000, "Ort", "Verlag", 1))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("ISBN darf nicht leer");
        assertThatThrownBy(() -> bookManager.addBook("999", "  ",
                List.of("Autor"), 2000, "Ort", "Verlag", 1))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Titel darf nicht leer");
    }
}
