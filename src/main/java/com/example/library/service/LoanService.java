package com.example.library.service;

import com.example.library.manager.BookCopyManager;
import com.example.library.manager.CustomerManager;
import com.example.library.model.BookCopy;
import com.example.library.model.Customer;

import java.time.LocalDate;

/**
 * Encapsulates the loan and return business logic of the library.
 *
 * <p>This service is intentionally free of any console I/O. It validates
 * preconditions, mutates the affected {@link BookCopy} and {@link Customer}
 * objects, and throws an exception whenever an operation cannot be performed.
 * The calling CLI layer is responsible for catching the exceptions and
 * presenting them to the user.</p>
 */
public class LoanService {

    /** Borrow period in days before a fine accrues. */
    public static final int LOAN_PERIOD_DAYS = 21;

    /** Fine charged per day a copy is overdue. */
    public static final double FINE_PER_DAY_EURO = 0.50;

    private final BookCopyManager bookCopyManager;
    private final CustomerManager customerManager;

    /**
     * Constructs a new LoanService bound to the given managers.
     *
     * @param bookCopyManager the manager that owns all book copies
     * @param customerManager the manager that owns all customers
     */
    public LoanService(BookCopyManager bookCopyManager, CustomerManager customerManager) {
        this.bookCopyManager = bookCopyManager;
        this.customerManager = customerManager;
    }

    /**
     * Loans the given book copy to the given customer.
     *
     * <p>Preconditions (validated in order):</p>
     * <ol>
     *   <li>the customer must exist,</li>
     *   <li>the book copy must exist,</li>
     *   <li>the book copy must not already be lent out.</li>
     * </ol>
     *
     * <p><b>Bug fix:</b> the {@code isLent()} guard runs <em>before</em>
     * {@code setLent(true)}. The original implementation mutated the copy first
     * and only then checked the flag, so the guard could never trigger.</p>
     *
     * @param bookCopyId the ID of the book copy to lend
     * @param customerId the ID of the customer borrowing the copy
     * @throws IllegalArgumentException if the customer or book copy does not exist
     * @throws IllegalStateException    if the book copy is already lent out
     */
    public void loan(int bookCopyId, int customerId) {
        Customer customer = customerManager.getCustomer(customerId);
        if (customer == null) {
            throw new IllegalArgumentException(
                    "Es existiert kein Kunde mit dieser ID: " + customerId);
        }
        BookCopy bookCopy = bookCopyManager.getBookCopy(bookCopyId);
        if (bookCopy == null) {
            throw new IllegalArgumentException(
                    "Es existiert keine Buchkopie mit dieser ID: " + bookCopyId);
        }
        if (bookCopy.isLent()) {
            throw new IllegalStateException(
                    "Diese Buchkopie ist bereits verliehen: " + bookCopyId);
        }
        bookCopy.setLent(true);
        bookCopy.setLoanDate(LocalDate.now());
        bookCopy.setCustomerID(customerId);
        customer.addBookCopy(bookCopy);
    }

    /**
     * Returns a previously loaned book copy from the given customer.
     *
     * <p>Preconditions (validated in order):</p>
     * <ol>
     *   <li>the customer must exist,</li>
     *   <li>the book copy must exist,</li>
     *   <li>the book copy must currently be lent,</li>
     *   <li>the book copy must belong to the given customer's borrowed list.</li>
     * </ol>
     *
     * <p><b>Bug fix:</b> the original implementation cleared the loan state
     * unconditionally and then removed the copy from whatever customer was
     * passed in, even if that customer had never borrowed it. This version
     * validates {@code customer.getBookCopies().contains(bookCopy)} before
     * mutating any state.</p>
     *
     * @param bookCopyId the ID of the book copy to return
     * @param customerId the ID of the customer returning the copy
     * @throws IllegalArgumentException if the customer or book copy does not exist
     * @throws IllegalStateException    if the copy is not lent or does not belong to the customer
     */
    public void returnBook(int bookCopyId, int customerId) {
        Customer customer = customerManager.getCustomer(customerId);
        if (customer == null) {
            throw new IllegalArgumentException(
                    "Es existiert kein Kunde mit dieser ID: " + customerId);
        }
        BookCopy bookCopy = bookCopyManager.getBookCopy(bookCopyId);
        if (bookCopy == null) {
            throw new IllegalArgumentException(
                    "Es existiert keine Buchkopie mit dieser ID: " + bookCopyId);
        }
        if (!bookCopy.isLent()) {
            throw new IllegalStateException(
                    "Diese Buchkopie ist nicht verliehen: " + bookCopyId);
        }
        if (!customer.getBookCopies().contains(bookCopy)) {
            throw new IllegalStateException(
                    "Diese Buchkopie wurde nicht vom Kunden " + customerId
                            + " ausgeliehen: " + bookCopyId);
        }
        bookCopy.setLent(false);
        bookCopy.setLoanDate(null);
        bookCopy.setCustomerID(-1);
        customer.removeBookCopy(bookCopy);
    }

    /**
     * Computes the late-return fine for a lent book copy.
     *
     * <p>Pure function with no side effects: uses the copy's recorded loan
     * date plus the {@link #LOAN_PERIOD_DAYS} grace period, charged at
     * {@link #FINE_PER_DAY_EURO} per overdue day. No fine when the copy is
     * not lent or has no loan date.</p>
     *
     * <p>The {@code today} parameter exists so tests can exercise overdue
     * and within-period cases deterministically; production callers use
     * {@link #calculateFine(int)}.</p>
     *
     * @param copy the book copy to evaluate
     * @param today reference date for the overdue calculation
     * @return fine in euro, 0.0 when nothing is due
     */
    public double calculateFine(BookCopy copy, LocalDate today) {
        return overdueDays(copy, today) * FINE_PER_DAY_EURO;
    }

    /**
     * Number of days the copy is past its loan period.
     *
     * <p>The register print used to re-derive the day count from the fine
     * ({@code (int) (fine / FINE_PER_DAY_EURO)}), which couples the message to
     * the euro constant; this method exposes the day count directly.</p>
     *
     * @param copy the book copy to evaluate
     * @param today reference date for the overdue calculation
     * @return overdue days, 0 when the copy is not lent, has no loan date, or
     *         is still within the loan period
     */
    public long overdueDays(BookCopy copy, LocalDate today) {
        if (copy == null || !copy.isLent() || copy.getLoanDate() == null) {
            return 0L;
        }
        long overdueDays = today.toEpochDay()
                - copy.getLoanDate().plusDays(LOAN_PERIOD_DAYS).toEpochDay();
        return Math.max(0L, overdueDays);
    }

    /**
     * Convenience wrapper for the CLI: overdue days of the copy with the given
     * id, measured against the current date.
     *
     * @param bookCopyId the ID of the book copy to evaluate
     * @return overdue days, 0 when the copy is not lent or not overdue
     * @throws IllegalArgumentException if no copy exists for the id
     */
    public long overdueDays(int bookCopyId) {
        BookCopy bookCopy = bookCopyManager.getBookCopy(bookCopyId);
        if (bookCopy == null) {
            throw new IllegalArgumentException(
                    "Es existiert keine Buchkopie mit dieser ID: " + bookCopyId);
        }
        return overdueDays(bookCopy, LocalDate.now());
    }

    /**
     * Convenience wrapper for the CLI: computes the fine for the copy with
     * the given id against the current date.
     *
     * @param bookCopyId the ID of the book copy to evaluate
     * @return fine in euro, 0.0 when the copy is not lent or not overdue
     * @throws IllegalArgumentException if no copy exists for the id
     */
    public double calculateFine(int bookCopyId) {
        BookCopy bookCopy = bookCopyManager.getBookCopy(bookCopyId);
        if (bookCopy == null) {
            throw new IllegalArgumentException(
                    "Es existiert keine Buchkopie mit dieser ID: " + bookCopyId);
        }
        return calculateFine(bookCopy, LocalDate.now());
    }
}
