package com.example.library.manager;

import com.example.library.model.Reservation;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for the FIFO reservation queue in {@link ReservationManager}.
 */
class ReservationManagerTest {

    @Test
    @DisplayName("Queue serves requests for one book in FIFO order")
    void fifoOrderForBook() {
        ReservationManager manager = new ReservationManager();
        Reservation first = manager.reserve(7, 100);
        Reservation second = manager.reserve(7, 101);
        Reservation third = manager.reserve(7, 102);

        var queue = manager.queueFor(7);
        assertEquals(java.util.List.of(first, second, third), queue);
        assertEquals(3, manager.queueSize(7));
    }

    @Test
    @DisplayName("Reservations for different books do not mix")
    void separateBooks() {
        ReservationManager manager = new ReservationManager();
        manager.reserve(1, 100);
        manager.reserve(2, 100);
        assertEquals(1, manager.queueFor(1).size());
        assertEquals(1, manager.queueFor(2).size());
    }

    @Test
    @DisplayName("Cancelling removes the entry and shrinks the queue")
    void cancelRemovesEntry() {
        ReservationManager manager = new ReservationManager();
        Reservation toCancel = manager.reserve(3, 100);
        manager.reserve(3, 101);

        var removed = manager.cancel(toCancel.id());
        assertTrue(removed.isPresent());
        assertEquals(toCancel, removed.get());
        assertEquals(1, manager.queueSize(3));
    }

    @Test
    @DisplayName("Cancelling an unknown id removes nothing")
    void cancelUnknownId() {
        ReservationManager manager = new ReservationManager();
        assertTrue(manager.cancel(999).isEmpty());
    }

    @Test
    @DisplayName("Non-positive ids are rejected")
    void invalidIdsRejected() {
        ReservationManager manager = new ReservationManager();
        assertThrows(IllegalArgumentException.class, () -> manager.reserve(0, 100));
        assertThrows(IllegalArgumentException.class, () -> manager.reserve(1, -5));
    }

    @Test
    @DisplayName("Validating a manually placed reservation with a fixed date")
    void manualDateReservation() {
        assertEquals(LocalDate.now(), Reservation.now(5, 2, 7).createdAt());
    }

    @Test
    @DisplayName("No reservations on a fresh manager")
    void freshManagerEmpty() {
        ReservationManager manager = new ReservationManager();
        assertTrue(manager.all().isEmpty());
    }
}