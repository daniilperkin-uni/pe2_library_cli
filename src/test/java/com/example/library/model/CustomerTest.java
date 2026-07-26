package com.example.library.model;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;

class CustomerTest {

    private Customer newCustomer() {
        return new Customer(1, "Mustermann", "Max", "Teststraße 1",
                "12345", "Musterstadt", true, new ArrayList<>());
    }

    @Test
    void constructor_populates_all_fields() {
        Customer c = newCustomer();
        assertThat(c.getId()).isEqualTo(1);
        assertThat(c.getName()).isEqualTo("Mustermann");
        assertThat(c.getFirstName()).isEqualTo("Max");
        assertThat(c.getAddress()).isEqualTo("Teststraße 1");
        assertThat(c.getZipCode()).isEqualTo("12345");
        assertThat(c.getCity()).isEqualTo("Musterstadt");
        assertThat(c.isFeesPayed()).isTrue();
        assertThat(c.getBookCopies()).isEmpty();
    }

    @Test
    void addBookCopy_appends_to_borrowed_list() {
        Customer c = newCustomer();
        BookCopy copy = new BookCopy(10, null, "A1", new Date(), false, null, -1);

        c.addBookCopy(copy);

        assertThat(c.getBookCopies()).containsExactly(copy);
        assertThat(c.getBookCopies()).hasSize(1);
    }

    @Test
    void removeBookCopy_removes_from_borrowed_list() {
        Customer c = newCustomer();
        BookCopy copy = new BookCopy(10, null, "A1", new Date(), false, null, -1);
        c.addBookCopy(copy);
        assertThat(c.getBookCopies()).contains(copy);

        c.removeBookCopy(copy);

        assertThat(c.getBookCopies()).doesNotContain(copy);
        assertThat(c.getBookCopies()).isEmpty();
    }

    @Test
    void setFeesPayed_toggles_state() {
        Customer c = newCustomer();
        assertThat(c.isFeesPayed()).isTrue();

        c.setFeesPayed(false);
        assertThat(c.isFeesPayed()).isFalse();
    }

    @Test
    void toString_contains_id_and_bookCopiesLent_count() {
        Customer c = newCustomer();
        c.addBookCopy(new BookCopy(1, null, "A", new Date(), true, null, 1));
        String s = c.toString();
        assertThat(s).contains("id=1");
        assertThat(s).contains("bookCopiesLent=1");
    }
}
