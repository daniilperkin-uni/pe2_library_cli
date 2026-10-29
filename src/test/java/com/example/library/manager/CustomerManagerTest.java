package com.example.library.manager;

import com.example.library.model.BookCopy;
import com.example.library.model.Customer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.Date;

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
    void removeCustomer_removes_the_customer() {
        assertThat(customerManager.removeCustomer(2)).isTrue();
        assertThat(customerManager.exists(2)).isFalse();
        assertThat(customerManager.getCustomers()).hasSize(1);
    }

    @Test
    void removeCustomer_reports_false_for_an_unknown_id() {
        assertThat(customerManager.removeCustomer(999)).isFalse();
        assertThat(customerManager.getCustomers()).hasSize(2);
    }

    @Test
    void deletionBlocker_allows_a_customer_without_loans() {
        assertThat(customerManager.deletionBlocker(2, new ReservationManager())).isNull();
    }

    @Test
    void deletionBlocker_reports_an_unknown_id() {
        assertThat(customerManager.deletionBlocker(999, new ReservationManager()))
                .contains("Es existiert kein Kunde mit dieser ID: 999");
    }

    @Test
    void deletionBlocker_refuses_a_customer_with_open_loans() {
        customerManager.getCustomer(2).addBookCopy(
                new BookCopy(99, null, "A1", new Date(), true, LocalDate.now(), 2));

        assertThat(customerManager.deletionBlocker(2, new ReservationManager()))
                .contains("hat noch ausgeliehene Buchkopien und kann nicht gelöscht werden.");
        assertThat(customerManager.exists(2)).isTrue();
    }

    @Test
    void deletionBlocker_refuses_a_customer_who_is_still_queued() {
        ReservationManager reservationManager = new ReservationManager();
        BookManager bookManager = new BookManager();
        bookManager.importBooks("/bücher.csv");
        reservationManager.reserve("3551551677", 123, bookManager, customerManager);

        assertThat(customerManager.deletionBlocker(123, reservationManager))
                .contains("steht noch in der Vormerkungs-Warteschlange")
                .contains("kann nicht gelöscht werden.");
        // Ein Kunde ohne Vormerkung bleibt löschbar.
        assertThat(customerManager.deletionBlocker(2, reservationManager)).isNull();
        assertThat(customerManager.exists(123)).isTrue();
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
    void addCustomer_does_not_recycle_the_id_of_a_deleted_customer() {
        assertThat(customerManager.removeCustomer(123)).isTrue();

        Customer customer = customerManager.addCustomer("Neu", "Nina",
                "Neustraße 1", "11111", "Musterstadt");

        // Eine freigewordene ID darf nicht neu vergeben werden, sonst erbt der
        // neue Kunde die Vormerkungen des gelöschten.
        assertThat(customer.getId()).isEqualTo(124);
        assertThat(customerManager.getCustomer(123)).isNull();
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

    @Test
    void importCustomers_reports_a_truncated_row_with_file_and_line(@TempDir Path dir)
            throws IOException {
        Path csv = dir.resolve("benutzer.csv");
        Files.writeString(csv, "id,name,firstName,address,zipCode,city,feesPayed\n"
                + "7,Beispiel,Berta,Beispielweg 7,54321\n", StandardCharsets.UTF_8);

        assertThatThrownBy(() -> customerManager.importCustomers(csv.toString()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining(csv.toString())
                .hasMessageContaining("Zeile 2")
                .hasMessageContaining("erwartet mindestens 7 Spalten, gefunden 5");
        assertThat(customerManager.getCustomers()).hasSize(2);
    }
}
