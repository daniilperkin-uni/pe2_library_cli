package com.example.library.manager;

import com.example.library.model.Customer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Scanner;

import static org.assertj.core.api.Assertions.assertThat;

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
    void deleteCustomer_removes_customer_when_confirmed_with_j() {
        Scanner scanner = new Scanner("2\n1\nj\n");
        customerManager.deleteCustomer(scanner);
        assertThat(customerManager.exists(2)).isFalse();
        assertThat(customerManager.getCustomers()).hasSize(1);
    }

    @Test
    void deleteCustomer_cancels_with_minus_one() {
        Scanner scanner = new Scanner("-1\n");
        customerManager.deleteCustomer(scanner);
        assertThat(customerManager.getCustomers()).hasSize(2);
    }
}
