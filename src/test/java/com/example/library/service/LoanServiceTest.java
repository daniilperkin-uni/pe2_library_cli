package com.example.library.service;

import com.example.library.manager.BookCopyManager;
import com.example.library.manager.CustomerManager;
import com.example.library.model.BookCopy;
import com.example.library.model.Customer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class LoanServiceTest {

    private BookCopyManager bookCopyManager;
    private CustomerManager customerManager;
    private LoanService loanService;
    private Customer customer;
    private BookCopy availableCopy;

    @BeforeEach
    void setUp() {
        bookCopyManager = new BookCopyManager();
        customerManager = new CustomerManager();
        loanService = new LoanService(bookCopyManager, customerManager);

        customer = new Customer(1, "Mustermann", "Max", "Teststraße 1",
                "12345", "Musterstadt", true, new ArrayList<>());
        customerManager.getCustomers().put(1, customer);

        Customer other = new Customer(2, "Schmidt", "Anna", "Beispielweg 42",
                "54321", "Musterstadt", false, new ArrayList<>());
        customerManager.getCustomers().put(2, other);

        availableCopy = new BookCopy(10, null, "A1", new Date(), false, null, -1);
        bookCopyManager.getBookCopies().put(10, availableCopy);
    }

    @Test
    void loan_happy_path_marks_copy_and_links_customer() {
        loanService.loan(10, 1);

        assertThat(availableCopy.isLent()).isTrue();
        // BUG 1 fix: customerID must be set to the borrowing customer (NOT -1)
        assertThat(availableCopy.getCustomerID()).isEqualTo(1);
        assertThat(availableCopy.getLoanDate()).isNotNull();
        // the customer must now hold the copy (NOT false / empty)
        assertThat(customer.getBookCopies()).contains(availableCopy);
    }

    @Test
    void loan_already_lent_copy_throws_illegal_state() {
        BookCopy lentCopy = new BookCopy(11, null, "A2", new Date(),
                true, LocalDate.now(), 2);
        bookCopyManager.getBookCopies().put(11, lentCopy);

        assertThatThrownBy(() -> loanService.loan(11, 1))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("bereits verliehen");

        // state must remain unchanged
        assertThat(lentCopy.getCustomerID()).isEqualTo(2);
    }

    @Test
    void loan_unknown_customer_throws_illegal_argument() {
        assertThatThrownBy(() -> loanService.loan(10, 999))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Kunde");
    }

    @Test
    void loan_unknown_copy_throws_illegal_argument() {
        assertThatThrownBy(() -> loanService.loan(999, 1))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Buchkopie");
    }

    @Test
    void returnBook_happy_path_clears_loan_state() {
        loanService.loan(10, 1);
        loanService.returnBook(10, 1);

        assertThat(availableCopy.isLent()).isFalse();
        assertThat(availableCopy.getCustomerID()).isEqualTo(-1);
        assertThat(availableCopy.getLoanDate()).isNull();
        assertThat(customer.getBookCopies()).doesNotContain(availableCopy);
    }

    @Test
    void returnBook_wrong_copy_for_customer_throws_illegal_state() {
        loanService.loan(10, 1);

        // customer 2 never borrowed copy 10
        assertThatThrownBy(() -> loanService.returnBook(10, 2))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("nicht vom Kunden");

        // BUG 2 fix: loan state must NOT have been cleared on the failure path
        assertThat(availableCopy.isLent()).isTrue();
        assertThat(availableCopy.getCustomerID()).isEqualTo(1);
        assertThat(customer.getBookCopies()).contains(availableCopy);
    }

    @Test
    void returnBook_not_lent_copy_throws_illegal_state() {
        assertThatThrownBy(() -> loanService.returnBook(10, 1))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("nicht verliehen");
    }

    @Test
    void loan_then_return_then_reloan_works() {
        loanService.loan(10, 1);
        loanService.returnBook(10, 1);
        // re-loan to a different customer should succeed now
        loanService.loan(10, 2);

        assertThat(availableCopy.isLent()).isTrue();
        assertThat(availableCopy.getCustomerID()).isEqualTo(2);
        assertThat(customerManager.getCustomer(2).getBookCopies()).contains(availableCopy);
        assertThat(customer.getBookCopies()).doesNotContain(availableCopy);
    }
}
