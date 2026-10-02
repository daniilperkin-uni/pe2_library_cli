package com.example.library.cli;

import com.example.library.manager.BookCopyManager;
import com.example.library.manager.BookManager;
import com.example.library.manager.CustomerManager;
import com.example.library.manager.ReservationManager;
import com.example.library.service.LoanService;
import com.example.library.service.ReportService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.Scanner;

import static org.assertj.core.api.Assertions.assertThat;

@Timeout(5)
class LibraryCLITest {

    private final PrintStream originalOut = System.out;
    private ByteArrayOutputStream out;
    private BookCopyManager copies;

    @BeforeEach
    void captureOut() {
        out = new ByteArrayOutputStream();
        System.setOut(new PrintStream(out, true, StandardCharsets.UTF_8));
    }

    @AfterEach
    void restoreOut() {
        System.setOut(originalOut);
    }

    private String run(String script) {
        BookManager books = new BookManager();
        CustomerManager customers = new CustomerManager();
        copies = new BookCopyManager();
        books.importBooks("/bücher.csv");
        customers.importCustomers("/benutzer.csv");
        copies.importBookCopies("/buchkopien.csv", books, customers);
        LoanService loans = new LoanService(copies, customers);
        ReportService reports = new ReportService(books, copies, customers);
        new LibraryCLI(books, copies, customers, new ReservationManager(), loans, reports,
                new Scanner(script)).start();
        return out.toString(StandardCharsets.UTF_8);
    }

    @Test
    void exitOptionEndsLoopWithoutSystemExit() {
        assertThat(run("9\n")).contains("Das Programm wird beendet.");
    }

    @Test
    void exitFromReportSubmenuReturnsFromStart() {
        String output = run("8\n8\n");
        assertThat(output).contains("==BERICHT ERSTELLEN==", "Das Programm wird beendet.");
        assertThat(output).doesNotContain("Option ist noch nicht implementiert");
    }

    @Test
    void exitFromPromptAfterLoanReturnsFromStart() {
        String output = run("4\n2\n1\n2\n");
        assertThat(output).contains("Das Programm wird beendet.");
        assertThat(copies.getBookCopy(1).isLent()).isTrue();
    }

    @Test
    void invalidInputIsRejectedAndEndOfInputStopsLoop() {
        String output = run("abc\n");
        assertThat(output).contains("Ungültige Eingabe!");
    }

    @Test
    void reservationForUnknownCustomerIsRejected() {
        String output = run("7\n1\n3036959548\n999\n2\n");
        assertThat(output).contains("Es existiert kein Kunde mit dieser ID: 999");
        assertThat(output).doesNotContain("angelegt.");
    }

    @Test
    void reservationForUnknownBookIsRejected() {
        String output = run("7\n1\n0000000000\n2\n");
        assertThat(output).contains("Es existiert kein Buch mit dieser ISBN: 0000000000");
        assertThat(output).doesNotContain("angelegt.");
    }

    @Test
    void reservationForExistingCustomerIsEnqueued() {
        String output = run("7\n1\n3036959548\n2\n2\n");
        assertThat(output).contains("Vormerkung 1 für ISBN 3036959548 von Kunde 2 angelegt.");
    }

    @Test
    void addBookViaSubmenuCreatesBook() {
        String output = run("1\n1\n9783442267744\nDie Verwandlung\nFranz Kafka\n2003\nMünchen\ndtv\n2\n1\n9\n");
        assertThat(output).contains("Das Buch mit ISBN: 9783442267744 wurde angelegt.");
    }

    @Test
    void addBookCopyViaSubmenuCreatesCopy() {
        String output = run("2\n1\n3036959548\nSF99\n1\n9\n");
        assertThat(output).contains("Die Buchkopie mit ID: 3 für ISBN 3036959548 wurde angelegt.");
        assertThat(copies.getBookCopy(3)).isNotNull();
        assertThat(copies.getBookCopy(3).isLent()).isFalse();
        assertThat(copies.getBookCopy(3).getBook().getIsbn()).isEqualTo("3036959548");
    }

    @Test
    void addCopyForUnknownIsbnListsExistingIsbns() {
        String output = run("2\n1\n0000000000\nSF99\n1\n9\n");
        assertThat(output).contains("Es existiert kein Buch mit dieser ISBN: 0000000000");
        assertThat(output).contains("Vorhandene ISBNs:", "3551551677");
    }

    @Test
    void returnBookShowsLateFee() {
        String output = run("5\n2\n2\n2\n");
        assertThat(output).contains("wurde erfolgreich vom Kunden 2 zurückgegeben.");
        assertThat(output).contains("Verspätungsgebühr:");
        assertThat(copies.getBookCopy(2).isLent()).isFalse();
    }
}
