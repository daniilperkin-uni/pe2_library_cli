package com.example.library.manager;

import com.example.library.model.BookCopy;
import com.example.library.model.Customer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.Date;
import java.util.Scanner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CustomerManagerTest {

    private CustomerManager customerManager;

    @BeforeEach
    void setUp() {
        customerManager = new CustomerManager();
        customerManager.importCustomers("/benutzer.csv");
    }

    @Test
    void importCustomers_loads_all_rows() {
        assertThat(customerManager.getCustomers()).hasSize(2);
    }

    @Test
    void importCustomers_populates_fields() {
        Customer c = customerManager.getCustomer(123);
        assertThat(c).isNotNull();
        assertThat(c.getName()).isEqualTo("Mustermann");
        assertThat(c.getFirstName()).isEqualTo("Max");
        assertThat(c.getAddress()).isEqualTo("Teststraße 1");
        assertThat(c.isFeesPayed()).isTrue();
    }

    @Test
    void exists_returns_correct_value() {
        assertThat(customerManager.exists(2)).isTrue();
        assertThat(customerManager.exists(999)).isFalse();
    }

    @Test
    void getCustomer_returns_null_for_unknown_id() {
        assertThat(customerManager.getCustomer(999)).isNull();
    }

    @Test
    void deleteCustomer_removes_customer_when_confirmed() {
        Scanner scanner = new Scanner("2\n1\n");
        customerManager.deleteCustomer(scanner);
        assertThat(customerManager.exists(2)).isFalse();
        assertThat(customerManager.getCustomers()).hasSize(1);
    }

    @Test
    void deleteCustomer_cancels_when_abort_chosen() {
        Scanner scanner = new Scanner("2\n2\n");
        customerManager.deleteCustomer(scanner);
        assertThat(customerManager.exists(2)).isTrue();
        assertThat(customerManager.getCustomers()).hasSize(2);
    }

    @Test
    void deleteCustomer_cancels_with_minus_one() {
        Scanner scanner = new Scanner("-1\n");
        customerManager.deleteCustomer(scanner);
        assertThat(customerManager.getCustomers()).hasSize(2);
    }

    @Test
    void deleteCustomer_refuses_when_customer_has_open_loans() {
        customerManager.getCustomer(2).addBookCopy(
                new BookCopy(99, null, "A1", new Date(), true, LocalDate.now(), 2));

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        PrintStream originalOut = System.out;
        try {
            System.setOut(new PrintStream(out, true, StandardCharsets.UTF_8));
            customerManager.deleteCustomer(new Scanner("2\n-1\n"));
        } finally {
            System.setOut(originalOut);
        }

        assertThat(out.toString(StandardCharsets.UTF_8))
                .contains("hat noch ausgeliehene Buchkopien und kann nicht gelöscht werden.");
        assertThat(customerManager.exists(2)).isTrue();
        assertThat(customerManager.getCustomers()).hasSize(2);
    }

    @Test
    void addCustomer_assigns_next_id_and_defaults() {
        Customer customer = customerManager.addCustomer("Beispiel", "Berta",
                "Beispielweg 7", "54321", "Musterstadt");
        assertThat(customer.getId()).isEqualTo(124);
        assertThat(customerManager.getCustomer(124)).isSameAs(customer);
        assertThat(customer.getName()).isEqualTo("Beispiel");
        assertThat(customer.getFirstName()).isEqualTo("Berta");
        assertThat(customer.isFeesPayed()).isFalse();
        assertThat(customer.getBookCopies()).isEmpty();
        assertThat(customerManager.getCustomers()).hasSize(3);
    }

    @Test
    void addCustomer_rejects_blank_names() {
        assertThatThrownBy(() -> customerManager.addCustomer("  ", "Berta",
                "Beispielweg 7", "54321", "Musterstadt"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Nachname darf nicht leer");
        assertThatThrownBy(() -> customerManager.addCustomer("Beispiel", "  ",
                "Beispielweg 7", "54321", "Musterstadt"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Vorname darf nicht leer");
    }
}
