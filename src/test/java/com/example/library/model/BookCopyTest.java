package com.example.library.model;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;

class BookCopyTest {

    @Test
    void constructor_populates_all_fields() {
        Book book = new Book("isbn", "Title", java.util.List.of("A"), 2020, "C", "P", 1);
        Date added = new Date();
        LocalDate loan = LocalDate.of(2024, 1, 31);
        BookCopy copy = new BookCopy(42, book, "SF42", added, true, loan, 7);

        assertThat(copy.getId()).isEqualTo(42);
        assertThat(copy.getBook()).isSameAs(book);
        assertThat(copy.getShelfLocation()).isEqualTo("SF42");
        assertThat(copy.getAddedToLibrary()).isSameAs(added);
        assertThat(copy.isLent()).isTrue();
        assertThat(copy.getLoanDate()).isEqualTo(loan);
        assertThat(copy.getCustomerID()).isEqualTo(7);
    }

    @Test
    void setLent_toggles_lent_state() {
        BookCopy copy = new BookCopy(1, null, "A1", new Date(), false, null, -1);
        assertThat(copy.isLent()).isFalse();

        copy.setLent(true);
        assertThat(copy.isLent()).isTrue();

        copy.setLent(false);
        assertThat(copy.isLent()).isFalse();
    }

    @Test
    void setLoanDate_updates_and_clears() {
        BookCopy copy = new BookCopy(1, null, "A1", new Date(), false, null, -1);
        assertThat(copy.getLoanDate()).isNull();

        LocalDate today = LocalDate.now();
        copy.setLoanDate(today);
        assertThat(copy.getLoanDate()).isEqualTo(today);

        copy.setLoanDate(null);
        assertThat(copy.getLoanDate()).isNull();
    }

    @Test
    void setCustomerID_updates_borrower() {
        BookCopy copy = new BookCopy(1, null, "A1", new Date(), false, null, -1);
        assertThat(copy.getCustomerID()).isEqualTo(-1);

        copy.setCustomerID(99);
        assertThat(copy.getCustomerID()).isEqualTo(99);
    }

    @Test
    void toString_contains_id_and_lent_state() {
        BookCopy copy = new BookCopy(7, null, "A1", new Date(), true, null, 5);
        String s = copy.toString();
        assertThat(s).contains("id=7");
        assertThat(s).contains("lent=true");
    }
}
