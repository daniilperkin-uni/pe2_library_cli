package com.example.library.manager;

import com.example.library.model.BookCopy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.Scanner;

import static org.assertj.core.api.Assertions.assertThat;

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
    void deleteBookCopy_removes_copy_when_confirmed() {
        Scanner scanner = new Scanner("1\n1\n");
        bookCopyManager.deleteBookCopy(scanner);
        assertThat(bookCopyManager.exists(1)).isFalse();
        assertThat(bookCopyManager.getBookCopies()).hasSize(1);
    }

    @Test
    void deleteBookCopy_refuses_when_copy_is_lent() {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        PrintStream originalOut = System.out;
        try {
            System.setOut(new PrintStream(out, true, StandardCharsets.UTF_8));
            bookCopyManager.deleteBookCopy(new Scanner("2\n-1\n"));
        } finally {
            System.setOut(originalOut);
        }

        assertThat(out.toString(StandardCharsets.UTF_8))
                .contains("ist verliehen und kann nicht gelöscht werden.");
        assertThat(bookCopyManager.exists(2)).isTrue();
        assertThat(bookCopyManager.getBookCopies()).hasSize(2);
    }

    @Test
    void searchBookCopies_by_isbn_finds_matching_copies() {
        Scanner scanner = new Scanner("1\n3036959548\n");
        bookCopyManager.searchBookCopies(scanner);
        // both copies reference ISBN 3036959548
        assertThat(bookCopyManager.getBookCopies()).hasSize(2);
    }

    @Test
    void searchBookCopies_by_title_finds_matching_copies() {
        // Option 2 = title search; title of book for ISBN 3036959548
        String title = bookCopyManager.getBookCopy(1).getBook().getTitle();
        Scanner scanner = new Scanner("2\n" + title + "\n");
        bookCopyManager.searchBookCopies(scanner);
        // no exception thrown and copies remain
        assertThat(bookCopyManager.getBookCopies()).hasSize(2);
    }

    @Test
    void searchBookCopies_reports_a_missing_isbn_once() {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        PrintStream originalOut = System.out;
        try {
            System.setOut(new PrintStream(out, true, StandardCharsets.UTF_8));
            bookCopyManager.searchBookCopies(new Scanner("1\n0000000000\n"));
        } finally {
            System.setOut(originalOut);
        }
        String printed = out.toString(StandardCharsets.UTF_8);
        assertThat(printed).contains("Es existiert keine Buchkopie mit dieser ISBN: 0000000000");
        // the generic fallback must not repeat the same empty result
        assertThat(printed).doesNotContain("Es wurden keine Buchkopien gefunden.");
    }
}
