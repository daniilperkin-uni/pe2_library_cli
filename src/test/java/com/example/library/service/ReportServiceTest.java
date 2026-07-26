package com.example.library.service;

import com.example.library.manager.BookCopyManager;
import com.example.library.manager.BookManager;
import com.example.library.manager.CustomerManager;
import com.example.library.model.Book;
import com.example.library.model.BookCopy;
import com.example.library.model.Customer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ReportServiceTest {

    private BookManager bookManager;
    private BookCopyManager bookCopyManager;
    private CustomerManager customerManager;
    private ReportService reportService;

    @BeforeEach
    void setUp() {
        bookManager = new BookManager();
        bookCopyManager = new BookCopyManager();
        customerManager = new CustomerManager();
        reportService = new ReportService(bookManager, bookCopyManager, customerManager);

        bookManager.getBooks().put("111", new Book("111", "Book One",
                List.of("Author A"), 2001, "CityA", "Publisher A", 1));
        bookManager.getBooks().put("222", new Book("222", "Book Two",
                List.of("Author B"), 2002, "CityB", "Publisher B", 1));

        Book bookA = bookManager.getBook("111");
        Book bookB = bookManager.getBook("222");

        // copy 1: available, publisher A
        bookCopyManager.getBookCopies().put(1, new BookCopy(1, bookA, "A1",
                new Date(), false, null, -1));
        // copy 2: lent to customer 1, publisher A
        bookCopyManager.getBookCopies().put(2, new BookCopy(2, bookA, "A2",
                new Date(), true, LocalDate.now(), 1));
        // copy 3: lent to customer 1, publisher B
        bookCopyManager.getBookCopies().put(3, new BookCopy(3, bookB, "B1",
                new Date(), true, LocalDate.now(), 1));

        Customer c1 = new Customer(1, "Mustermann", "Max", "Teststraße 1",
                "12345", "Musterstadt", true, new ArrayList<>());
        c1.addBookCopy(bookCopyManager.getBookCopy(2));
        c1.addBookCopy(bookCopyManager.getBookCopy(3));
        customerManager.getCustomers().put(1, c1);

        Customer c2 = new Customer(2, "Schmidt", "Anna", "Beispielweg 42",
                "54321", "Musterstadt", false, new ArrayList<>());
        customerManager.getCustomers().put(2, c2);
    }

    @Test
    void getAllBooks_returns_every_book() {
        List<String> books = reportService.getAllBooks();
        assertThat(books).hasSize(2);
        assertThat(books).anyMatch(line -> line.contains("Book One"));
        assertThat(books).anyMatch(line -> line.contains("Book Two"));
    }

    @Test
    void getLentBookCopies_returns_only_lent_copies() {
        List<String> lent = reportService.getLentBookCopies();
        assertThat(lent).hasSize(2);
        assertThat(lent).allMatch(line -> line.contains("lent=true"));
    }

    @Test
    void getAvailableBookCopies_returns_only_available_copies() {
        List<String> available = reportService.getAvailableBookCopies();
        assertThat(available).hasSize(1);
        assertThat(available).allMatch(line -> line.contains("lent=false"));
    }

    @Test
    void getAllCustomers_returns_every_customer() {
        List<String> customers = reportService.getAllCustomers();
        assertThat(customers).hasSize(2);
        assertThat(customers).anyMatch(line -> line.contains("Mustermann"));
        assertThat(customers).anyMatch(line -> line.contains("Schmidt"));
    }

    @Test
    void getLentBookCopiesOfCustomer_returns_customer_borrowed_copies() {
        List<String> copies = reportService.getLentBookCopiesOfCustomer(1);
        assertThat(copies).hasSize(2);
    }

    @Test
    void getLentBookCopiesOfCustomer_empty_for_customer_with_no_loans() {
        List<String> copies = reportService.getLentBookCopiesOfCustomer(2);
        assertThat(copies).isEmpty();
    }

    @Test
    void getLentBookCopiesOfCustomer_throws_for_unknown_customer() {
        // BUG 3 fix: unknown customer is rejected with an exception (null check)
        assertThatThrownBy(() -> reportService.getLentBookCopiesOfCustomer(999))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Kunde");
    }

    @Test
    void getBookCopiesOfPublisher_counts_and_percentages() {
        List<String> report = reportService.getBookCopiesOfPublisher();
        // sorted alphabetically: Publisher A, Publisher B
        assertThat(report).hasSize(2);
        // build expected with the same default-locale formatting the service uses
        String expectedA = String.format("Publisher A: %d Buchkopien (%.1f%%)", 2, 100.0 * 2 / 3);
        String expectedB = String.format("Publisher B: %d Buchkopien (%.1f%%)", 1, 100.0 * 1 / 3);
        assertThat(report.get(0)).isEqualTo(expectedA);
        assertThat(report.get(1)).isEqualTo(expectedB);
    }

    @Test
    void getBookCopiesOfPublisher_reports_zero_for_publisher_without_copies() {
        bookManager.getBooks().put("333", new Book("333", "Book Three",
                List.of("Author C"), 2003, "CityC", "Publisher C", 1));

        List<String> report = reportService.getBookCopiesOfPublisher();
        String expectedC = String.format("Publisher C: %d Buchkopien (%.1f%%)", 0, 0.0);
        assertThat(report).contains(expectedC);
    }

    @Test
    void getBookCopiesOfPublisher_empty_when_no_data() {
        ReportService empty = new ReportService(new BookManager(),
                new BookCopyManager(), new CustomerManager());
        List<String> report = empty.getBookCopiesOfPublisher();
        assertThat(report).hasSize(1);
        assertThat(report.get(0)).contains("Keine Buchkopien");
    }
}
