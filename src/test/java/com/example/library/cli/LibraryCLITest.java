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

    @Test
    void deleteBookViaSubmenuRemovesTheBookAfterConfirmation() {
        String output = run("1\n2\n3551551677\n1\n9\n");
        assertThat(output).contains("Das Buch mit ISBN: 3551551677 wurde gelöscht.");
    }

    @Test
    void deleteBookViaSubmenuCancelsOnMinusOne() {
        String output = run("1\n2\n-1\n9\n");
        assertThat(output).doesNotContain("wurde gelöscht.");
    }

    @Test
    void deleteBookCopyViaSubmenuRefusesALentCopyAndReprompts() {
        String output = run("2\n2\n2\n-1\n9\n");
        assertThat(output).contains("ist verliehen und kann nicht gelöscht werden.");
        assertThat(output).doesNotContain("wurde gelöscht.");
    }

    @Test
    void deleteCustomerViaSubmenuRemovesTheCustomerAfterConfirmation() {
        // Customer 2 still holds copy 2 in the fixture, so 123 is the deletable one.
        String output = run("3\n2\n123\n1\n9\n");
        assertThat(output).contains("Der Kunde mit ID: 123 wurde gelöscht.");
    }

    @Test
    void deleteCustomerViaSubmenuRefusesACustomerWithOpenLoans() {
        String output = run("3\n2\n2\n-1\n9\n");
        assertThat(output)
                .contains("hat noch ausgeliehene Buchkopien und kann nicht gelöscht werden.");
        assertThat(output).doesNotContain("wurde gelöscht.");
    }

    @Test
    void deleteCustomerViaSubmenuRefusesACustomerWhoIsStillQueued() {
        // 7 -> Vormerkungen, 1 -> vormerken (ISBN 3551551677, Kunde 123),
        // 1 -> zurück, 4 -> Hauptmenü, 3 -> Kunden, 2 -> löschen,
        // 123 -> wird abgelehnt, -1 -> abbrechen, 9 -> Ende
        String output = run("7\n1\n3551551677\n123\n1\n4\n3\n2\n123\n-1\n9\n");

        assertThat(output).contains("Vormerkung 1 für ISBN 3551551677 von Kunde 123 angelegt.");
        assertThat(output).contains("steht noch in der Vormerkungs-Warteschlange");
        assertThat(output).doesNotContain("Der Kunde mit ID: 123 wurde gelöscht.");
    }

    @Test
    void deleteBookViaSubmenuRefusesABookThatIsStillReserved() {
        String output = run("7\n1\n3551551677\n123\n1\n4\n1\n2\n3551551677\n-1\n9\n");

        assertThat(output).contains("hat noch offene Vormerkungen in der Warteschlange");
        assertThat(output).doesNotContain("Das Buch mit ISBN: 3551551677 wurde gelöscht.");
    }

    @Test
    void searchViaSubmenuPrintsTheMatchingCopies() {
        String output = run("6\n1\n3036959548\n9\n");
        assertThat(output).contains("==EINE BUCHKOPIE SUCHEN==");
        assertThat(output).doesNotContain("Es existiert keine Buchkopie");
    }

    @Test
    void searchViaSubmenuReportsAnUnknownIsbn() {
        String output = run("6\n1\n0000000000\n9\n");
        assertThat(output).contains("Es existiert keine Buchkopie mit dieser ISBN: 0000000000");
    }
}
