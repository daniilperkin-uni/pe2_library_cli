package com.example.library.cli;

import com.example.library.csv.CsvExporter;
import com.example.library.manager.BookCopyManager;
import com.example.library.manager.BookManager;
import com.example.library.manager.CustomerManager;
import com.example.library.manager.ReservationManager;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class LibraryCLIStateLoadTest {

    @Test
    void loadState_usesClasspathFixtures_whenNoSavedFiles(@TempDir Path dir) {
        BookManager books = new BookManager();
        CustomerManager customers = new CustomerManager();
        BookCopyManager copies = new BookCopyManager();
        ReservationManager reservations = new ReservationManager();

        LibraryCLI.loadState(dir, books, customers, copies, reservations);

        assertThat(books.getBooks()).hasSize(3);
        assertThat(customers.getCustomers()).hasSize(2);
        assertThat(copies.getBookCopies()).hasSize(2);
    }

    @Test
    void loadState_loadsSavedState(@TempDir Path dir) throws IOException {
        BookManager books = new BookManager();
        CustomerManager customers = new CustomerManager();
        BookCopyManager copies = new BookCopyManager();
        books.importBooks("/bücher.csv");
        customers.importCustomers("/benutzer.csv");
        copies.importBookCopies("/buchkopien.csv", books, customers);
        CsvExporter.save(dir, books, customers, copies, new ReservationManager());

        BookManager books2 = new BookManager();
        CustomerManager customers2 = new CustomerManager();
        BookCopyManager copies2 = new BookCopyManager();
        LibraryCLI.loadState(dir, books2, customers2, copies2, new ReservationManager());

        assertThat(books2.getBooks()).hasSize(3);
        assertThat(copies2.getBookCopy(2).isLent()).isTrue();
        assertThat(customers2.getCustomer(2).getBookCopies()).isNotEmpty();
    }

    @Test
    void loadState_throws_whenSavedStateReferencesMissingCustomer(@TempDir Path dir)
            throws IOException {
        BookManager books = new BookManager();
        CustomerManager customers = new CustomerManager();
        BookCopyManager copies = new BookCopyManager();
        books.importBooks("/bücher.csv");
        customers.importCustomers("/benutzer.csv");
        copies.importBookCopies("/buchkopien.csv", books, customers);
        CsvExporter.save(dir, books, customers, copies, new ReservationManager());

        // drop customer 2 from the saved file while copy 2 still refers to them
        Files.writeString(dir.resolve(CsvExporter.CUSTOMERS_FILE),
                "id,name,firstName,address,zipCode,city,feesPayed\n"
                        + "123,Mustermann,Max,Teststraße 1,12345,Musterstadt,yes\n",
                StandardCharsets.UTF_8);

        BookManager books2 = new BookManager();
        CustomerManager customers2 = new CustomerManager();
        BookCopyManager copies2 = new BookCopyManager();

        assertThatThrownBy(() -> LibraryCLI.loadState(dir, books2, customers2, copies2,
                new ReservationManager()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Kunde mit ID 2");
    }

    @Test
    void loadState_throws_whenSavedReservationReferencesMissingCustomer(@TempDir Path dir)
            throws IOException {
        BookManager books = new BookManager();
        CustomerManager customers = new CustomerManager();
        BookCopyManager copies = new BookCopyManager();
        books.importBooks("/bücher.csv");
        customers.importCustomers("/benutzer.csv");
        copies.importBookCopies("/buchkopien.csv", books, customers);
        CsvExporter.save(dir, books, customers, copies, new ReservationManager());

        // Eine Vormerkung auf einen nicht mehr vorhandenen Kunden: früher
        // wurde die Zeile beim Start still wieder eingelesen.
        Files.writeString(dir.resolve(CsvExporter.RESERVATIONS_FILE),
                "id,bookIsbn,customerId,createdAt\n"
                        + "1,3036959548,999,2024-01-01\n", StandardCharsets.UTF_8);

        BookManager books2 = new BookManager();
        CustomerManager customers2 = new CustomerManager();
        BookCopyManager copies2 = new BookCopyManager();

        assertThatThrownBy(() -> LibraryCLI.loadState(dir, books2, customers2, copies2,
                new ReservationManager()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Es existiert kein Kunde mit ID 999");
    }

    @Test
    void loadState_restores_theReservationQueue(@TempDir Path dir) throws IOException {
        BookManager books = new BookManager();
        CustomerManager customers = new CustomerManager();
        BookCopyManager copies = new BookCopyManager();
        books.importBooks("/bücher.csv");
        customers.importCustomers("/benutzer.csv");
        copies.importBookCopies("/buchkopien.csv", books, customers);
        ReservationManager reservations = new ReservationManager();
        reservations.reserve("3036959548", 2, books, customers);
        reservations.reserve("3036959548", 123, books, customers);
        CsvExporter.save(dir, books, customers, copies, reservations);

        ReservationManager reservations2 = new ReservationManager();
        LibraryCLI.loadState(dir, new BookManager(), new CustomerManager(),
                new BookCopyManager(), reservations2);

        assertThat(reservations2.queueFor("3036959548")).hasSize(2);
        assertThat(reservations2.all()).extracting(r -> r.customerId())
                .containsExactly(2, 123);
    }

    @Test
    void loadState_restores_added_book_and_copy(@TempDir Path dir) throws IOException {
        BookManager books = new BookManager();
        CustomerManager customers = new CustomerManager();
        BookCopyManager copies = new BookCopyManager();
        books.importBooks("/bücher.csv");
        customers.importCustomers("/benutzer.csv");
        books.addBook("9783442267744", "Die Verwandlung",
                java.util.List.of("Franz Kafka"), 2003, "München", "dtv", 2);
        copies.addBookCopy("9783442267744", "REGAL7", books);
        CsvExporter.save(dir, books, customers, copies, new ReservationManager());

        BookManager books2 = new BookManager();
        CustomerManager customers2 = new CustomerManager();
        BookCopyManager copies2 = new BookCopyManager();
        LibraryCLI.loadState(dir, books2, customers2, copies2, new ReservationManager());

        assertThat(books2.getBook("9783442267744")).isNotNull();
        assertThat(copies2.getBookCopies()).hasSize(1);
        assertThat(copies2.getBookCopy(1).getBook().getIsbn()).isEqualTo("9783442267744");
        assertThat(copies2.getBookCopy(1).isLent()).isFalse();
    }
}
