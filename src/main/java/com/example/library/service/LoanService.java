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
}
