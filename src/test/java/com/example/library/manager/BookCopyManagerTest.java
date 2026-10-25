package com.example.library.manager;

import com.example.library.model.BookCopy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.Scanner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class BookCopyManagerTest {

    private BookManager bookManager;
    private BookCopyManager bookCopyManager;
    private CustomerManager customerManager;

    @BeforeEach
    void setUp() {
        bookManager = new BookManager();
        customerManager = new CustomerManager();
        bookCopyManager = new BookCopyManager();

        bookManager.importBooks("/bücher.csv");
        customerManager.importCustomers("/benutzer.csv");
        bookCopyManager.importBookCopies("/buchkopien.csv", bookManager, customerManager);
    }

    @Test
    void importBookCopies_loads_all_rows() {
        assertThat(bookCopyManager.getBookCopies()).hasSize(2);
    }

    @Test
    void importBookCopies_links_book_reference() {
        BookCopy copy = bookCopyManager.getBookCopy(1);
        assertThat(copy).isNotNull();
        assertThat(copy.getBook()).isNotNull();
        assertThat(copy.getBook().getIsbn()).isEqualTo("3036959548");
    }

    @Test
    void importBookCopies_sets_lent_state_and_links_customer() {
        BookCopy lent = bookCopyManager.getBookCopy(2);
        assertThat(lent.isLent()).isTrue();
        assertThat(lent.getCustomerID()).isEqualTo(2);
        assertThat(customerManager.getCustomer(2).getBookCopies()).contains(lent);
    }

    @Test
    void importBookCopies_available_copy_has_no_loan_data() {
        BookCopy available = bookCopyManager.getBookCopy(1);
        assertThat(available.isLent()).isFalse();
        assertThat(available.getCustomerID()).isEqualTo(-1);
        assertThat(available.getLoanDate()).isNull();
    }

    @Test
    void exists_and_getBookCopy_lookup_by_id() {
        assertThat(bookCopyManager.exists(1)).isTrue();
        assertThat(bookCopyManager.exists(999)).isFalse();
        assertThat(bookCopyManager.getBookCopy(999)).isNull();
    }

    @Test
    void removeBookCopy_removes_the_copy() {
        assertThat(bookCopyManager.removeBookCopy(1)).isTrue();
        assertThat(bookCopyManager.exists(1)).isFalse();
        assertThat(bookCopyManager.getBookCopies()).hasSize(1);
    }

    @Test
    void deletionBlocker_refuses_a_lent_copy() {
        assertThat(bookCopyManager.deletionBlocker(2))
                .contains("ist verliehen und kann nicht gelöscht werden.");
        assertThat(bookCopyManager.deletionBlocker(1)).isNull();
        assertThat(bookCopyManager.deletionBlocker(999))
                .contains("Es existiert keine Buchkopie mit dieser ID: 999");
    }

    @Test
    void findByIsbn_returns_every_copy_of_the_book() {
        // both copies reference ISBN 3036959548
        assertThat(bookCopyManager.findByIsbn("3036959548")).hasSize(2);
        assertThat(bookCopyManager.findByIsbn("0000000000")).isEmpty();
    }

    @Test
    void findByTitle_ignores_case() {
        String title = bookCopyManager.getBookCopy(1).getBook().getTitle();
        assertThat(bookCopyManager.findByTitle(title)).hasSize(2);
        assertThat(bookCopyManager.findByTitle(title.toUpperCase())).hasSize(2);
        assertThat(bookCopyManager.findByTitle("kein solcher titel")).isEmpty();
    }

    @Test
    void findByAuthor_returns_the_author_s_copies() {
        String author = bookCopyManager.getBookCopy(1).getBook().getAuthors().get(0);
        assertThat(bookCopyManager.findByAuthor(author)).isNotEmpty();
        assertThat(bookCopyManager.findByAuthor("Niemand")).isEmpty();
    }

    @Test
    void addBookCopy_assigns_next_id_and_links_book() {
        BookCopy copy = bookCopyManager.addBookCopy("3551551677", "REGAL7", bookManager);
        assertThat(copy.getId()).isEqualTo(3);
        assertThat(bookCopyManager.getBookCopy(3)).isSameAs(copy);
        assertThat(copy.getBook().getIsbn()).isEqualTo("3551551677");
        assertThat(copy.getShelfLocation()).isEqualTo("REGAL7");
        assertThat(copy.isLent()).isFalse();
        assertThat(copy.getCustomerID()).isEqualTo(-1);
        assertThat(copy.getLoanDate()).isNull();
        assertThat(copy.getAddedToLibrary()).isNotNull();
    }

    @Test
    void addBookCopy_rejects_unknown_isbn() {
        assertThatThrownBy(() -> bookCopyManager.addBookCopy("0000000000", "REGAL7", bookManager))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Es existiert kein Buch mit dieser ISBN: 0000000000");
        assertThat(bookCopyManager.getBookCopies()).hasSize(2);
    }

    @Test
    void addBookCopy_rejects_blank_shelf_location() {
        assertThatThrownBy(() -> bookCopyManager.addBookCopy("3551551677", "  ", bookManager))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Standort darf nicht leer");
        assertThat(bookCopyManager.getBookCopies()).hasSize(2);
    }
}
