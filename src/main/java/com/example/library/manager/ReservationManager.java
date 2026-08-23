package com.example.library.manager;

import com.example.library.model.Reservation;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/**
 * Manages the reservation queue for titles that are currently all lent out.
 *
 * <p>Semantics are deliberately thin: a reservation is placed against a book
 * id (not a specific copy), the queue is FIFO by {@code createdAt}, and
 * cancelling removes an entry. Auto-notifying the next customer when a copy
 * is returned is deliberately out of scope for this exercise - the queue is
 * a waiting list the library staff consults when a copy comes back.</p>
 */
public class ReservationManager {

    private final List<Reservation> reservations = new ArrayList<>();
    private long nextId = 1;

    /**
     * Adds a reservation to the end of the queue.
     *
     * @param bookId     the id of the requested book
     * @param customerId the id of the waiting customer
     * @return the created reservation
     * @throws IllegalArgumentException when ids are not positive
     */
    public Reservation reserve(int bookId, int customerId) {
        if (bookId <= 0) {
            throw new IllegalArgumentException("Buch-ID muss positiv sein: " + bookId);
        }
        if (customerId <= 0) {
            throw new IllegalArgumentException("Kunden-ID muss positiv sein: " + customerId);
        }
        Reservation r = Reservation.now(nextId++, bookId, customerId);
        reservations.add(r);
        return r;
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
     * Returns the FIFO queue for one book: earliest reservation first.
     *
     * @param bookId the requested book id
     * @return ordered list (may be empty)
     */
    public List<Reservation> queueFor(int bookId) {
        return reservations.stream()
                .filter(r -> r.bookId() == bookId)
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
     * Number of customers waiting for the given book.
     *
     * @param bookId the requested book id
     * @return queue size
     */
    public long queueSize(int bookId) {
        return queueFor(bookId).size();
    }
}