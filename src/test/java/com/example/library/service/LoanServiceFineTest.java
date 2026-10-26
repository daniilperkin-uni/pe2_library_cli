package com.example.library.service;

import com.example.library.manager.BookCopyManager;
import com.example.library.manager.CustomerManager;
import com.example.library.model.BookCopy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Tests for the late-return fine calculation in {@link LoanService}.
 * Uses the pure two-argument form of the method, so no book/customer
 * fixtures are needed beyond a lent {@link BookCopy}.
 *
 * <p>Jeder Test liest {@code LocalDate.now()} genau einmal in {@code today}
 * und benutzt diesen Wert für Fixture und Assertion. Zwei getrennte
 * Uhrzeit-Abrufe würden bei einem Mitternachtswechsel einen Fehler von einem
 * Tag (und damit 0,50 EUR) melden.</p>
 */
class LoanServiceFineTest {

    /** Loan period of the exercise: 21 days at 0,50 EUR per overdue day. */
    private static final int LOAN_PERIOD_DAYS = 21;
    private static final double FINE_PER_DAY_EURO = 0.50;

    private LoanService loanService;
    private BookCopyManager bookCopyManager;

    @BeforeEach
    void setUp() {
        bookCopyManager = new BookCopyManager();
        loanService = new LoanService(bookCopyManager, new CustomerManager());
    }

    private BookCopy lentCopy(long id, LocalDate loanDate) {
        return new BookCopy(id, null, "A1", new Date(), true, loanDate, -1);
    }

    @Test
    @DisplayName("No fine while the copy is still within the loan period")
    void noFineWithinPeriod() {
        LocalDate today = LocalDate.now();
        BookCopy copy = lentCopy(1L, today.minusDays(10));
        assertEquals(0.0, loanService.calculateFine(copy, today), 1e-9);
    }

    @Test
    @DisplayName("Exactly at the deadline still no fine")
    void noFineAtDeadline() {
        LocalDate today = LocalDate.now();
        BookCopy copy = lentCopy(2L, today.minusDays(LOAN_PERIOD_DAYS));
        assertEquals(0.0, loanService.calculateFine(copy, today), 1e-9);
    }

    @Test
    @DisplayName("One day overdue charges one day at the configured rate")
    void finePerDay() {
        LocalDate today = LocalDate.now();
        BookCopy copy = lentCopy(3L, today.minusDays(LOAN_PERIOD_DAYS + 1));
        assertEquals(FINE_PER_DAY_EURO, loanService.calculateFine(copy, today), 1e-9);
    }

    @Test
    @DisplayName("Ten days overdue charges ten days")
    void fineScalesWithDays() {
        LocalDate today = LocalDate.now();
        BookCopy copy = lentCopy(4L, today.minusDays(LOAN_PERIOD_DAYS + 10));
        assertEquals(10 * FINE_PER_DAY_EURO, loanService.calculateFine(copy, today), 1e-9);
    }

    @Test
    @DisplayName("A copy that is not lent accrues no fine")
    void notLentNoFine() {
        LocalDate today = LocalDate.now();
        BookCopy copy = new BookCopy(5L, null, "B2", new Date(), false,
                today.minusDays(100), -1);
        assertEquals(0.0, loanService.calculateFine(copy, today), 1e-9);
    }

    @Test
    @DisplayName("Null copy and null loan date accrue no fine")
    void nullSafe() {
        LocalDate today = LocalDate.now();
        assertEquals(0.0, loanService.calculateFine(null, today), 1e-9);
        BookCopy copy = new BookCopy(6L, null, "B3", new Date(), true, null, -1);
        assertEquals(0.0, loanService.calculateFine(copy, today), 1e-9);
    }

    @Test
    @DisplayName("Calculating by id throws when the copy does not exist")
    void unknownCopyThrows() {
        assertThrows(IllegalArgumentException.class, () -> loanService.calculateFine(9999));
    }

    @Test
    @DisplayName("Overdue days are reported directly and agree with the fine")
    void overdueDaysAgreeWithFine() {
        LocalDate today = LocalDate.now();
        BookCopy copy = lentCopy(7L, today.minusDays(LOAN_PERIOD_DAYS + 10));

        // Beide Werte stehen als Literal im Test. calculateFine ist genau
        // overdueDays * FINE_PER_DAY_EURO; eine Assertion gegen diese Formel
        // bliebe auch dann grün, wenn beide Zahlen falsch wären.
        assertEquals(10L, loanService.overdueDays(copy, today));
        assertEquals(5.00, loanService.calculateFine(copy, today), 1e-9);
        assertThrows(IllegalArgumentException.class, () -> loanService.overdueDays(9999));
    }
}