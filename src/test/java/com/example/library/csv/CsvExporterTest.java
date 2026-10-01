package com.example.library.csv;

import com.example.library.manager.BookCopyManager;
import com.example.library.manager.BookManager;
import com.example.library.manager.CustomerManager;
import com.example.library.manager.ReservationManager;
import com.example.library.model.Reservation;
import com.example.library.service.LoanService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class CsvExporterTest {

    @Test
    void savedStateRoundTripsThroughImporters(@TempDir Path dir) throws Exception {
        BookManager books = new BookManager();
        CustomerManager customers = new CustomerManager();
        BookCopyManager copies = new BookCopyManager();
        books.importBooks("/bücher.csv");
        customers.importCustomers("/benutzer.csv");
        copies.importBookCopies("/buchkopien.csv", books, customers);
        new LoanService(copies, customers).loan(1, 2);

        CsvExporter.save(dir, books, customers, copies, new ReservationManager());

        BookManager books2 = new BookManager();
        CustomerManager customers2 = new CustomerManager();
        BookCopyManager copies2 = new BookCopyManager();
        books2.importBooks(dir.resolve(CsvExporter.BOOKS_FILE).toString());
        customers2.importCustomers(dir.resolve(CsvExporter.CUSTOMERS_FILE).toString());
        copies2.importBookCopies(dir.resolve(CsvExporter.COPIES_FILE).toString(),
                books2, customers2);

        assertThat(books2.getBooks()).hasSameSizeAs(books.getBooks());
        assertThat(books2.getBooks().keySet()).isEqualTo(books.getBooks().keySet());
        assertThat(customers2.getCustomers()).hasSameSizeAs(customers.getCustomers());
        assertThat(copies2.getBookCopies()).hasSameSizeAs(copies.getBookCopies());
        assertThat(copies2.getBookCopy(1).isLent()).isTrue();
        assertThat(copies2.getBookCopy(1).getCustomerID()).isEqualTo(2);
        assertThat(customers2.getCustomer(2).getBookCopies()).isNotEmpty();
        books.getBooks().forEach((isbn, b) -> {
            assertThat(books2.getBook(isbn).getTitle()).isEqualTo(b.getTitle());
            assertThat(books2.getBook(isbn).getAuthors()).isEqualTo(b.getAuthors());
        });
    }

    @Test
    void reservationsSurviveTheSaveLoadRoundTrip(@TempDir Path dir) throws Exception {
        BookManager books = new BookManager();
        CustomerManager customers = new CustomerManager();
        BookCopyManager copies = new BookCopyManager();
        books.importBooks("/bücher.csv");
        customers.importCustomers("/benutzer.csv");
        copies.importBookCopies("/buchkopien.csv", books, customers);
        ReservationManager reservations = new ReservationManager();
        Reservation first = reservations.reserve("isbn-7", 2);
        Reservation second = reservations.reserve("isbn-7", 3);

        CsvExporter.save(dir, books, customers, copies, reservations);

        ReservationManager restored = new ReservationManager();
        restored.importReservations(dir.resolve(CsvExporter.RESERVATIONS_FILE).toString());

        assertThat(restored.all()).containsExactly(first, second);
        // restored ids must not be handed out again for a new reservation
        assertThat(restored.reserve("isbn-7", 4).id()).isEqualTo(3);
    }

    @Test
    void quotesFieldsThatNeedIt() {
        assertThat(CsvExporter.q("a,b")).isEqualTo("\"a,b\"");
        assertThat(CsvExporter.q("say \"hi\"")).isEqualTo("\"say \"\"hi\"\"\"");
        assertThat(CsvExporter.q("plain")).isEqualTo("plain");
    }
}
