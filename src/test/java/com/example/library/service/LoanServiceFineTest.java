package com.example.library.service;

import com.example.library.manager.BookCopyManager;
import com.example.library.manager.CustomerManager;
import com.example.library.model.BookCopy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.Date;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Tests for the late-return fine calculation in {@link LoanService}.
 * Uses the pure two-argument form of the method, so no book/customer
 * fixtures are needed beyond a lent {@link BookCopy}.
 */
class LoanServiceFineTest {

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
        BookCopy copy = lentCopy(1L, LocalDate.now().minusDays(10));
        assertEquals(0.0, loanService.calculateFine(copy, LocalDate.now()), 1e-9);
    }

    @Test
    @DisplayName("Exactly at the deadline still no fine")
    void noFineAtDeadline() {
        BookCopy copy = lentCopy(2L, LocalDate.now().minusDays(LoanService.LOAN_PERIOD_DAYS));
        assertEquals(0.0, loanService.calculateFine(copy, LocalDate.now()), 1e-9);
    }

    @Test
    @DisplayName("One day overdue charges one day at the configured rate")
    void finePerDay() {
        BookCopy copy = lentCopy(3L, LocalDate.now().minusDays(LoanService.LOAN_PERIOD_DAYS + 1));
        assertEquals(LoanService.FINE_PER_DAY_EURO, loanService.calculateFine(copy, LocalDate.now()), 1e-9);
    }

    @Test
    @DisplayName("Ten days overdue charges ten days")
    void fineScalesWithDays() {
        BookCopy copy = lentCopy(4L, LocalDate.now().minusDays(LoanService.LOAN_PERIOD_DAYS + 10));
        assertEquals(10 * LoanService.FINE_PER_DAY_EURO, loanService.calculateFine(copy, LocalDate.now()), 1e-9);
    }

    @Test
    @DisplayName("A copy that is not lent accrues no fine")
    void notLentNoFine() {
        BookCopy copy = new BookCopy(5L, null, "B2", new Date(), false, LocalDate.now().minusDays(100), -1);
        assertEquals(0.0, loanService.calculateFine(copy, LocalDate.now()), 1e-9);
    }

    @Test
    @DisplayName("Null copy and null loan date accrue no fine")
    void nullSafe() {
        assertEquals(0.0, loanService.calculateFine(null, LocalDate.now()), 1e-9);
        BookCopy copy = new BookCopy(6L, null, "B3", new Date(), true, null, -1);
        assertEquals(0.0, loanService.calculateFine(copy, LocalDate.now()), 1e-9);
    }

    @Test
    @DisplayName("Calculating by id throws when the copy does not exist")
    void unknownCopyThrows() {
        assertThrows(IllegalArgumentException.class, () -> loanService.calculateFine(9999));
    }
}