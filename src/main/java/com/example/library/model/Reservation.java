package com.example.library.model;

import java.time.LocalDate;

/**
 * A queue entry for a title that is currently lent out and cannot be
 * borrowed yet.
 *
 * <p>Kept intentionally a plain data record without JPA or framework
 * dependencies, mirroring how {@link BookCopy} and {@link Customer} are
 * modeled in this exercise.</p>
 *
 * @param id         unique reservation id
 * @param bookId     the id of the book (not a specific copy) being requested
 * @param customerId the id of the customer waiting in line
 * @param createdAt  when the reservation was placed (drives FIFO ordering)
 */
public record Reservation(
        long id,
        int bookId,
        int customerId,
        LocalDate createdAt) {

    /**
     * Creates a reservation with the given id and the current date.
     *
     * @param id         unique reservation id
     * @param bookId     requested book id
     * @param customerId waiting customer id
     * @return new reservation dated today
     */
    public static Reservation now(long id, int bookId, int customerId) {
        return new Reservation(id, bookId, customerId, LocalDate.now());
    }
}