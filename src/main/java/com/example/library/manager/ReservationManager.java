package com.example.library.manager;

import com.example.library.csv.CsvImporter;
import com.example.library.model.Reservation;

import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/**
 * Manages the reservation queue for titles that are currently all lent out.
 *
 * <p>Semantics are deliberately thin: a reservation is placed against the
 * ISBN of a book (not a specific copy), the queue is FIFO by
 * {@code createdAt}, and cancelling removes an entry. Auto-notifying the next
 * customer when a copy is returned is deliberately out of scope for this
 * exercise - the queue is a waiting list the library staff consults when a
 * copy comes back.</p>
 */
public class ReservationManager {

    private final List<Reservation> reservations = new ArrayList<>();
    private long nextId = 1;

    /**
     * Adds a reservation to the end of the queue.
     *
     * <p>A customer can wait for a title only once: a second active
     * reservation for the same pair would occupy two places in the FIFO queue
     * and hand the same person the title twice.</p>
     *
     * @param bookIsbn   the ISBN of the requested book
     * @param customerId the id of the waiting customer
     * @return the created reservation
     * @throws IllegalArgumentException when the ISBN is blank, the customer
     *                                  id is not positive, or the pair is
     *                                  already queued
     */
    public Reservation reserve(String bookIsbn, int customerId) {
        if (bookIsbn == null || bookIsbn.isBlank()) {
            throw new IllegalArgumentException("Buch-ISBN muss angegeben werden");
        }
        if (customerId <= 0) {
            throw new IllegalArgumentException("Kunden-ID muss positiv sein: " + customerId);
        }
        if (hasReservation(bookIsbn, customerId)) {
            throw new IllegalArgumentException("Der Kunde mit ID " + customerId
                    + " hat bereits eine Vormerkung für ISBN " + bookIsbn + ".");
        }
        Reservation r = Reservation.now(nextId++, bookIsbn, customerId);
        reservations.add(r);
        return r;
    }

    /**
     * Checks whether a customer already waits for this exact title.
     *
     * @param bookIsbn   the requested book ISBN
     * @param customerId the waiting customer id
     * @return {@code true} when an active reservation for the pair exists
     */
    private boolean hasReservation(String bookIsbn, int customerId) {
        return reservations.stream()
                .anyMatch(r -> r.bookIsbn().equals(bookIsbn) && r.customerId() == customerId);
    }

    /**
     * Checks whether any customer waits for the given title.
     *
     * <p>Used by the deletion checks so a book cannot be removed while its
     * queue entry would dangle.</p>
     *
     * @param bookIsbn the requested book ISBN
     * @return {@code true} when at least one reservation references the ISBN
     */
    public boolean hasReservationForBook(String bookIsbn) {
        return reservations.stream().anyMatch(r -> r.bookIsbn().equals(bookIsbn));
    }

    /**
     * Checks whether the given customer waits for any title.
     *
     * <p>Used by the deletion checks so a customer cannot be removed while a
     * queue entry still points at them.</p>
     *
     * @param customerId the customer id
     * @return {@code true} when at least one reservation references the id
     */
    public boolean hasReservationForCustomer(int customerId) {
        return reservations.stream().anyMatch(r -> r.customerId() == customerId);
    }

    /**
     * Removes a reservation from the queue.
     *
     * @param reservationId the id of the reservation to cancel
     * @return the removed reservation, or empty when no such id exists
     */
    public Optional<Reservation> cancel(long reservationId) {
        Optional<Reservation> found = reservations.stream()
                .filter(r -> r.id() == reservationId)
                .findFirst();
        found.ifPresent(reservations::remove);
        return found;
    }

    /**
     * Returns the FIFO queue for one title: earliest reservation first.
     *
     * @param bookIsbn the requested book ISBN
     * @return ordered list (may be empty)
     */
    public List<Reservation> queueFor(String bookIsbn) {
        return reservations.stream()
                .filter(r -> r.bookIsbn().equals(bookIsbn))
                .sorted(Comparator.comparing(Reservation::createdAt)
                        .thenComparingLong(Reservation::id))
                .toList();
    }

    /**
     * Returns every reservation, oldest first.
     *
     * @return immutable snapshot of the whole queue
     */
    public List<Reservation> all() {
        return reservations.stream()
                .sorted(Comparator.comparing(Reservation::createdAt)
                        .thenComparingLong(Reservation::id))
                .toList();
    }

    /**
     * Number of customers waiting for the given title.
     *
     * @param bookIsbn the requested book ISBN
     * @return queue size
     */
    public long queueSize(String bookIsbn) {
        return queueFor(bookIsbn).size();
    }

    /**
     * Restores the queue from a CSV source: either a filesystem path or a
     * classpath resource (see {@link CsvImporter#open}).
     *
     * <p>Columns: {@code id, bookIsbn, customerId, createdAt} (yyyy-MM-dd).
     * Restored ids are kept verbatim and {@code nextId} is advanced past the
     * largest one, so reservations created afterwards never collide with a
     * restored entry.</p>
     *
     * <p>Every row is checked against the book and customer collections: a
     * queue entry whose ISBN or customer no longer exists would otherwise be
     * reloaded silently as a dangling row.</p>
     *
     * @param resourcePath    the file path or classpath resource path
     * @param bookManager     the manager used to verify the ISBNs
     * @param customerManager the manager used to verify the customer ids
     * @throws RuntimeException         if the CSV cannot be read or parsed
     * @throws IllegalArgumentException if a row references an unknown book or
     *                                  customer, or has too few columns
     */
    public void importReservations(String resourcePath, BookManager bookManager,
                                   CustomerManager customerManager) {
        InputStream input = CsvImporter.open(getClass(), resourcePath);
        if (input == null) {
            System.out.println("Datei existiert nicht: " + resourcePath);
            return;
        }
        try {
            List<CsvImporter.Row> rows = CsvImporter.parseRows(input, true);
            for (CsvImporter.Row row : rows) {
                CsvImporter.requireColumns(row, resourcePath, 4);
                String[] parts = row.fields();
                long id = Long.parseLong(parts[0].trim());
                String bookIsbn = parts[1].trim();
                int customerId = Integer.parseInt(parts[2].trim());
                LocalDate createdAt = LocalDate.parse(parts[3].trim());
                if (!bookManager.exists(bookIsbn)) {
                    throw new IllegalArgumentException("Es existiert kein Buch mit ISBN "
                            + bookIsbn + " (Vormerkung " + id + " in " + resourcePath
                            + ", Zeile " + row.lineNumber() + ")");
                }
                if (!customerManager.exists(customerId)) {
                    throw new IllegalArgumentException("Es existiert kein Kunde mit ID "
                            + customerId + " (Vormerkung " + id + " in " + resourcePath
                            + ", Zeile " + row.lineNumber() + ")");
                }
                reservations.add(new Reservation(id, bookIsbn, customerId, createdAt));
                nextId = Math.max(nextId, id + 1);
            }
            System.out.println("Vormerkungen wurden erfolgreich importiert.");
        } catch (IOException e) {
            System.out.println("Fehler beim Importieren von Vormerkungen.");
            throw new RuntimeException(e);
        }
    }
}