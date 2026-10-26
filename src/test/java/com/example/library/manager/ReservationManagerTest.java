package com.example.library.manager;

import com.example.library.model.Customer;
import com.example.library.model.Reservation;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for the FIFO reservation queue in {@link ReservationManager}.
 */
class ReservationManagerTest {

    @Test
    @DisplayName("Queue serves requests for one book in FIFO order")
    void fifoOrderForBook(@TempDir Path dir) throws IOException {
        // Die IDs laufen gegen die Zeitstempel: 3 ist der älteste Eintrag,
        // 1 der jüngste. Eine Sortierung nur nach id würde die Reihenfolge
        // umdrehen — der Test prüft also wirklich den createdAt-Vergleich.
        String isbn = "3036959548";
        Path csv = dir.resolve("reservations.csv");
        Files.writeString(csv, "id,bookIsbn,customerId,createdAt\n"
                + "1," + isbn + ",100,2024-03-01\n"
                + "2," + isbn + ",101,2024-02-01\n"
                + "3," + isbn + ",102,2024-01-01\n", StandardCharsets.UTF_8);

        ReservationManager manager = new ReservationManager();
        manager.importReservations(csv.toString(), books(), customersWithIds(100, 101, 102));

        var queue = manager.queueFor(isbn);
        assertEquals(List.of(
                new Reservation(3, isbn, 102, LocalDate.of(2024, 1, 1)),
                new Reservation(2, isbn, 101, LocalDate.of(2024, 2, 1)),
                new Reservation(1, isbn, 100, LocalDate.of(2024, 3, 1))), queue);
        assertEquals(3, manager.queueSize(isbn));
    }

    @Test
    @DisplayName("Reservations for different books do not mix")
    void separateBooks() {
        ReservationManager manager = new ReservationManager();
        manager.reserve("isbn-1", 100);
        manager.reserve("isbn-2", 100);
        assertEquals(1, manager.queueFor("isbn-1").size());
        assertEquals(1, manager.queueFor("isbn-2").size());
    }

    @Test
    @DisplayName("Cancelling removes the entry and shrinks the queue")
    void cancelRemovesEntry() {
        ReservationManager manager = new ReservationManager();
        Reservation toCancel = manager.reserve("isbn-3", 100);
        manager.reserve("isbn-3", 101);

        var removed = manager.cancel(toCancel.id());
        assertTrue(removed.isPresent());
        assertEquals(toCancel, removed.get());
        assertEquals(1, manager.queueSize("isbn-3"));
    }

    @Test
    @DisplayName("Cancelling an unknown id removes nothing")
    void cancelUnknownId() {
        ReservationManager manager = new ReservationManager();
        assertTrue(manager.cancel(999).isEmpty());
    }

    @Test
    @DisplayName("Blank ISBN and non-positive customer id are rejected")
    void invalidInputsRejected() {
        ReservationManager manager = new ReservationManager();
        assertThrows(IllegalArgumentException.class, () -> manager.reserve("", 100));
        assertThrows(IllegalArgumentException.class, () -> manager.reserve(null, 100));
        assertThrows(IllegalArgumentException.class, () -> manager.reserve("isbn", -5));
    }

    @Test
    @DisplayName("Validating a manually placed reservation with a fixed date")
    void manualDateReservation() {
        // createdAt kommt aus einem eigenen Uhrzeit-Abruf in der Produktion.
        // Der Test klammert ihn deshalb zwischen zwei Abrufe ein, statt auf
        // exakte Gleichheit zu bestehen: bei einem Mitternachtswechsel
        // zwischen beiden Aufrufen wäre das Ergebnis sonst einen Tag daneben.
        LocalDate before = LocalDate.now();
        Reservation reservation = Reservation.now(5, "isbn", 7);
        LocalDate after = LocalDate.now();

        assertThat(reservation.createdAt()).isBetween(before, after);
        assertEquals(5, reservation.id());
        assertEquals("isbn", reservation.bookIsbn());
        assertEquals(7, reservation.customerId());
    }

    @Test
    @DisplayName("No reservations on a fresh manager")
    void freshManagerEmpty() {
        ReservationManager manager = new ReservationManager();
        assertTrue(manager.all().isEmpty());
    }

    @Test
    @DisplayName("The same customer cannot queue twice for the same title")
    void duplicateReservationRejected() {
        ReservationManager manager = new ReservationManager();
        manager.reserve("isbn-7", 100);

        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> manager.reserve("isbn-7", 100));

        assertTrue(error.getMessage().contains("hat bereits eine Vormerkung für ISBN isbn-7"));
        assertEquals(1, manager.queueSize("isbn-7"));
    }

    @Test
    @DisplayName("A customer may queue for two different titles")
    void sameCustomerTwoTitles() {
        ReservationManager manager = new ReservationManager();
        manager.reserve("isbn-1", 100);
        manager.reserve("isbn-2", 100);
        assertEquals(2, manager.all().size());
    }

    @Test
    @DisplayName("Import rejects a reservation for an unknown customer")
    void importRejectsUnknownCustomer(@TempDir Path dir) throws IOException {
        Path csv = dir.resolve("reservations.csv");
        Files.writeString(csv, "id,bookIsbn,customerId,createdAt\n"
                + "1,3036959548,999,2024-01-01\n", StandardCharsets.UTF_8);

        ReservationManager manager = new ReservationManager();
        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> manager.importReservations(csv.toString(), books(), customers()));

        assertTrue(error.getMessage().contains("Es existiert kein Kunde mit ID 999"));
        assertTrue(error.getMessage().contains("Zeile 2"));
        assertTrue(manager.all().isEmpty());
    }

    @Test
    @DisplayName("Import rejects a reservation for an unknown ISBN")
    void importRejectsUnknownBook(@TempDir Path dir) throws IOException {
        Path csv = dir.resolve("reservations.csv");
        Files.writeString(csv, "id,bookIsbn,customerId,createdAt\n"
                + "1,0000000000,2,2024-01-01\n", StandardCharsets.UTF_8);

        ReservationManager manager = new ReservationManager();
        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> manager.importReservations(csv.toString(), books(), customers()));

        assertTrue(error.getMessage().contains("Es existiert kein Buch mit ISBN 0000000000"));
        assertTrue(manager.all().isEmpty());
    }

    @Test
    @DisplayName("Import reports a truncated row with file and line")
    void importRejectsTruncatedRow(@TempDir Path dir) throws IOException {
        Path csv = dir.resolve("reservations.csv");
        Files.writeString(csv, "id,bookIsbn,customerId,createdAt\n"
                + "1,3036959548\n", StandardCharsets.UTF_8);

        ReservationManager manager = new ReservationManager();
        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> manager.importReservations(csv.toString(), books(), customers()));

        assertTrue(error.getMessage().contains(csv.toString()));
        assertTrue(error.getMessage().contains("Zeile 2"));
        assertTrue(error.getMessage().contains("erwartet mindestens 4 Spalten, gefunden 2"));
    }

    @Test
    @DisplayName("Import accepts rows whose book and customer exist")
    void importAcceptsKnownReferences(@TempDir Path dir) throws IOException {
        Path csv = dir.resolve("reservations.csv");
        Files.writeString(csv, "id,bookIsbn,customerId,createdAt\n"
                + "5,3036959548,2,2024-05-01\n", StandardCharsets.UTF_8);

        ReservationManager manager = new ReservationManager();
        manager.importReservations(csv.toString(), books(), customers());

        assertEquals(1, manager.queueSize("3036959548"));
        // nextId bleibt hinter der importierten ID zurück.
        assertEquals(6, manager.reserve("3036959548", 123).id());
    }

    private static BookManager books() {
        BookManager books = new BookManager();
        books.importBooks("/bücher.csv");
        return books;
    }

    private static CustomerManager customers() {
        CustomerManager customers = new CustomerManager();
        customers.importCustomers("/benutzer.csv");
        return customers;
    }

    /** Customers that exist only in memory, so a test can pick its own ids. */
    private static CustomerManager customersWithIds(int... ids) {
        CustomerManager customers = new CustomerManager();
        for (int id : ids) {
            customers.getCustomers().put(id, new Customer(id, "Mustermann", "Max",
                    "Teststraße 1", "12345", "Musterstadt", true, new ArrayList<>()));
        }
        return customers;
    }
}