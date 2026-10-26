package com.example.library.cli;

import com.example.library.csv.CsvExporter;
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
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Scanner;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Prüft den Speichervertrag der CLI: Ein Erfolg steht auf stdout, ein
 * Fehlschlag auf stderr — und {@link LibraryCLI#save(Path)} meldet ihn als
 * {@code false}, damit {@code main} mit einem Exit-Code ungleich 0 enden kann.
 */
@Timeout(5)
class LibraryCLISaveTest {

    private final PrintStream originalOut = System.out;
    private final PrintStream originalErr = System.err;
    private ByteArrayOutputStream out;
    private ByteArrayOutputStream err;
    private LibraryCLI cli;

    @BeforeEach
    void setUp() {
        out = new ByteArrayOutputStream();
        err = new ByteArrayOutputStream();
        System.setOut(new PrintStream(out, true, StandardCharsets.UTF_8));
        System.setErr(new PrintStream(err, true, StandardCharsets.UTF_8));

        BookManager books = new BookManager();
        CustomerManager customers = new CustomerManager();
        BookCopyManager copies = new BookCopyManager();
        books.importBooks("/bücher.csv");
        customers.importCustomers("/benutzer.csv");
        copies.importBookCopies("/buchkopien.csv", books, customers);
        cli = new LibraryCLI(books, copies, customers, new ReservationManager(),
                new LoanService(copies, customers),
                new ReportService(books, copies, customers), new Scanner(""));
    }

    @AfterEach
    void restoreStreams() {
        System.setOut(originalOut);
        System.setErr(originalErr);
    }

    @Test
    void saveWritesEveryFileAndReportsSuccessOnStdout(@TempDir Path dir) {
        assertThat(cli.save(dir)).isTrue();

        assertThat(stdout()).contains("Daten wurden gespeichert in: ");
        assertThat(stderr()).isEmpty();
        assertThat(Files.isRegularFile(dir.resolve(CsvExporter.BOOKS_FILE))).isTrue();
        assertThat(Files.isRegularFile(dir.resolve(CsvExporter.RESERVATIONS_FILE))).isTrue();
    }

    @Test
    void saveReportsFailureOnStderrAndReturnsFalse(@TempDir Path dir) throws IOException {
        // Eine Datei statt eines Verzeichnisses: createDirectories scheitert.
        Path blocked = Files.createFile(dir.resolve("kein-verzeichnis"));

        assertThat(cli.save(blocked)).isFalse();

        assertThat(stderr()).contains("Fehler beim Speichern:");
        assertThat(stderr()).contains("Änderungen dieser Sitzung gehen verloren.");
        assertThat(stdout()).doesNotContain("Daten wurden gespeichert");
    }

    private String stdout() {
        return out.toString(StandardCharsets.UTF_8);
    }

    private String stderr() {
        return err.toString(StandardCharsets.UTF_8);
    }
}
