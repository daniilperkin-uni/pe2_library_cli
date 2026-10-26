package com.example.library.csv;

import com.example.library.manager.BookCopyManager;
import com.example.library.manager.BookManager;
import com.example.library.manager.CustomerManager;
import com.example.library.manager.ReservationManager;
import com.example.library.model.Book;
import com.example.library.model.BookCopy;
import com.example.library.model.Customer;
import com.example.library.model.Reservation;
import com.example.library.service.LoanService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

class CsvExporterTest {

    /** Same pattern CsvExporter uses for addedToLibrary. */
    private static final SimpleDateFormat FORMAT = new SimpleDateFormat("yyyy-MM-dd");

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

        // Jedes Feld jedes Objekts, nicht nur Titel und Autor: eine vertauschte
        // Spalte in CsvExporter (Jahr/Stadt/Verlag/Auflage, Regal/Ort, Gebühren)
        // fällt sonst durch, weil der Import die Werte einfach anders zuordnet.
        assertThat(books2.getBooks().keySet()).isEqualTo(books.getBooks().keySet());
        books.getBooks().forEach((isbn, expected) -> {
            Book actual = books2.getBook(isbn);
            assertThat(actual).isNotNull();
            assertThat(actual.getIsbn()).isEqualTo(expected.getIsbn());
            assertThat(actual.getTitle()).isEqualTo(expected.getTitle());
            assertThat(actual.getAuthors()).isEqualTo(expected.getAuthors());
            assertThat(actual.getYear()).isEqualTo(expected.getYear());
            assertThat(actual.getCity()).isEqualTo(expected.getCity());
            assertThat(actual.getPublisher()).isEqualTo(expected.getPublisher());
            assertThat(actual.getEdition()).isEqualTo(expected.getEdition());
        });

        assertThat(customers2.getCustomers().keySet())
                .isEqualTo(customers.getCustomers().keySet());
        customers.getCustomers().forEach((id, expected) -> {
            Customer actual = customers2.getCustomer(id);
            assertThat(actual).isNotNull();
            assertThat(actual.getId()).isEqualTo(expected.getId());
            assertThat(actual.getName()).isEqualTo(expected.getName());
            assertThat(actual.getFirstName()).isEqualTo(expected.getFirstName());
            assertThat(actual.getAddress()).isEqualTo(expected.getAddress());
            assertThat(actual.getZipCode()).isEqualTo(expected.getZipCode());
            assertThat(actual.getCity()).isEqualTo(expected.getCity());
            assertThat(actual.isFeesPayed()).isEqualTo(expected.isFeesPayed());
        });

        assertThat(copies2.getBookCopies().keySet()).isEqualTo(copies.getBookCopies().keySet());
        copies.getBookCopies().forEach((id, expected) -> {
            BookCopy actual = copies2.getBookCopy(id);
            assertThat(actual).isNotNull();
            assertThat(actual.getId()).isEqualTo(expected.getId());
            assertThat(actual.getBook().getIsbn()).isEqualTo(expected.getBook().getIsbn());
            assertThat(actual.getShelfLocation()).isEqualTo(expected.getShelfLocation());
            // addedToLibrary geht als yyyy-MM-dd über die CSV, die Uhrzeit
            // ist also weg — verglichen wird deshalb der formatierte Tag.
            assertThat(FORMAT.format(actual.getAddedToLibrary()))
                    .isEqualTo(FORMAT.format(expected.getAddedToLibrary()));
            assertThat(actual.isLent()).isEqualTo(expected.isLent());
            assertThat(actual.getLoanDate()).isEqualTo(expected.getLoanDate());
            assertThat(actual.getCustomerID()).isEqualTo(expected.getCustomerID());
        });

        // Die Ausleihe muss auch am Kunden wieder hängen, nicht nur an der Kopie.
        // Kunde 2 hat beide Kopien: Nummer 1 frisch ausgeliehen, Nummer 2 aus
        // dem Fixture. Der Import muss beide wieder in seiner Liste führen.
        assertThat(customers2.getCustomer(2).getBookCopies())
                .containsExactlyInAnyOrder(copies2.getBookCopy(1), copies2.getBookCopy(2));
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
        Reservation first = reservations.reserve("3036959548", 2);
        Reservation second = reservations.reserve("3036959548", 123);

        CsvExporter.save(dir, books, customers, copies, reservations);

        // Beim Laden müssen Buch und Kunde der Vormerkung existieren, sonst
        // lehnt der Import die Zeile ab.
        BookManager restoredBooks = new BookManager();
        CustomerManager restoredCustomers = new CustomerManager();
        restoredBooks.importBooks("/bücher.csv");
        restoredCustomers.importCustomers("/benutzer.csv");
        ReservationManager restored = new ReservationManager();
        restored.importReservations(dir.resolve(CsvExporter.RESERVATIONS_FILE).toString(),
                restoredBooks, restoredCustomers);

        assertThat(restored.all()).containsExactly(first, second);
        // restored ids must not be handed out again for a new reservation
        assertThat(restored.reserve("3036959548", 4).id()).isEqualTo(3);
    }

    @Test
    void saveReplacesEveryFileAndLeavesNoTemporaryFiles(@TempDir Path dir) throws Exception {
        BookManager books = new BookManager();
        CustomerManager customers = new CustomerManager();
        BookCopyManager copies = new BookCopyManager();
        books.importBooks("/bücher.csv");
        customers.importCustomers("/benutzer.csv");
        copies.importBookCopies("/buchkopien.csv", books, customers);

        CsvExporter.save(dir, books, customers, copies, new ReservationManager());

        List<String> names;
        try (Stream<Path> files = Files.list(dir)) {
            names = files.map(path -> path.getFileName().toString()).toList();
        }
        assertThat(names).contains(CsvExporter.BOOKS_FILE, CsvExporter.CUSTOMERS_FILE,
                CsvExporter.COPIES_FILE, CsvExporter.RESERVATIONS_FILE);
        assertThat(names).noneMatch(name -> name.endsWith(".tmp"));
        assertThat(Files.readAllLines(dir.resolve(CsvExporter.BOOKS_FILE),
                StandardCharsets.UTF_8)).hasSize(4);
    }

    @Test
    void quotesFieldsThatNeedIt() {
        assertThat(CsvExporter.q("a,b")).isEqualTo("\"a,b\"");
        assertThat(CsvExporter.q("say \"hi\"")).isEqualTo("\"say \"\"hi\"\"\"");
        assertThat(CsvExporter.q("plain")).isEqualTo("plain");
    }
}
