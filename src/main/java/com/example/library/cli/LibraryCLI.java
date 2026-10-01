package com.example.library.cli;

import com.example.library.manager.BookCopyManager;
import com.example.library.manager.BookManager;
import com.example.library.manager.CustomerManager;
import com.example.library.manager.ReservationManager;
import com.example.library.service.LoanService;
import com.example.library.service.ReportService;

import com.example.library.csv.CsvExporter;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.InputMismatchException;
import java.util.List;
import java.util.Scanner;

/**
 * Thin command-line controller for the library system.
 *
 * <p>This class owns only the interactive menu loop and the {@link Scanner}
 * based input/output. All business logic is delegated to
 * {@link LoanService} and {@link ReportService}; the managers handle their own
 * CRUD/search/import operations.</p>
 *
 * <p>CSV fixtures are loaded from the classpath (via
 * {@code getResourceAsStream}) so the application runs identically from the
 * unpacked classes directory or a packaged JAR.</p>
 */
public class LibraryCLI {

    private final BookManager bookManager;
    private final BookCopyManager bookCopyManager;
    private final CustomerManager customerManager;
    private final ReservationManager reservationManager;
    private final LoanService loanService;
    private final ReportService reportService;
    private final Scanner scanner;
    private boolean running;

    /**
     * Constructs a new LibraryCLI.
     *
     * @param bookManager     the book manager
     * @param bookCopyManager the book-copy manager
     * @param customerManager the customer manager
     * @param loanService     the loan service
     * @param reportService   the report service
     * @param scanner         the scanner used to read user input
     */
    public LibraryCLI(BookManager bookManager, BookCopyManager bookCopyManager,
                      CustomerManager customerManager, ReservationManager reservationManager,
                      LoanService loanService,
                      ReportService reportService, Scanner scanner) {
        this.bookManager = bookManager;
        this.bookCopyManager = bookCopyManager;
        this.customerManager = customerManager;
        this.reservationManager = reservationManager;
        this.loanService = loanService;
        this.reportService = reportService;
        this.scanner = scanner;
    }

    /**
     * Application entry point.
     *
     * <p>Wires up the managers and services, imports the CSV fixtures from the
     * classpath, and starts the interactive menu loop.</p>
     *
     * @param args optional data directory as the first argument (defaults to
     *             {@code ./data}); further arguments are ignored
     */
    public static void main(String[] args) {
        BookManager bookManager = new BookManager();
        BookCopyManager bookCopyManager = new BookCopyManager();
        CustomerManager customerManager = new CustomerManager();
        ReservationManager reservationManager = new ReservationManager();
        LoanService loanService = new LoanService(bookCopyManager, customerManager);
        ReportService reportService =
                new ReportService(bookManager, bookCopyManager, customerManager);
        Scanner scanner = new Scanner(System.in);
        Path dataDir = Path.of(args.length > 0 ? args[0] : "data");

        try {
            loadState(dataDir, bookManager, customerManager, bookCopyManager,
                    reservationManager);
        } catch (RuntimeException e) {
            // Refuse to start on unreadable or inconsistent saved data instead
            // of crashing with a stack trace. No user action has happened yet,
            // so there is nothing to save on this exit path.
            System.out.println("Fehler beim Laden der Daten: " + e.getMessage());
            System.out.println("Bitte das Datenverzeichnis prüfen oder entfernen: "
                    + dataDir.toAbsolutePath());
            System.exit(1);
        }

        LibraryCLI libraryCLI = new LibraryCLI(bookManager, bookCopyManager,
                customerManager, reservationManager, loanService, reportService, scanner);
        libraryCLI.start();
        libraryCLI.save(dataDir);
    }

    /**
     * Loads the library state from {@code dataDir} when a saved books file is
     * present, otherwise from the bundled classpath fixtures
     * ({@code CsvImporter.open} falls back to them).
     *
     * @param dataDir            the data directory (may be missing or empty)
     * @param bookManager        the book manager to populate
     * @param customerManager    the customer manager to populate
     * @param bookCopyManager    the book-copy manager to populate
     * @param reservationManager the reservation manager to populate
     * @throws RuntimeException when saved data exists but cannot be parsed,
     *                          e.g. a copy referencing a removed customer
     */
    static void loadState(Path dataDir, BookManager bookManager,
                          CustomerManager customerManager, BookCopyManager bookCopyManager,
                          ReservationManager reservationManager) {
        boolean saved = Files.isRegularFile(dataDir.resolve(CsvExporter.BOOKS_FILE));
        String prefix = saved ? dataDir.toString() + java.io.File.separator : "/";
        bookManager.importBooks(prefix + CsvExporter.BOOKS_FILE);
        customerManager.importCustomers(prefix + CsvExporter.CUSTOMERS_FILE);
        bookCopyManager.importBookCopies(prefix + CsvExporter.COPIES_FILE,
                bookManager, customerManager);
        // Reservations have no bundled fixture and only exist once a previous
        // run has saved them, so they are only imported when the file is there.
        if (Files.isRegularFile(dataDir.resolve(CsvExporter.RESERVATIONS_FILE))) {
            reservationManager.importReservations(prefix + CsvExporter.RESERVATIONS_FILE);
        }
    }

    /**
     * Persists the current state as CSV files into {@code dataDir}.
     *
     * @param dataDir the target directory
     */
    public void save(Path dataDir) {
        try {
            CsvExporter.save(dataDir, bookManager, customerManager, bookCopyManager,
                    reservationManager);
            System.out.println("Daten wurden gespeichert in: " + dataDir.toAbsolutePath());
        } catch (IOException e) {
            System.out.println("Fehler beim Speichern: " + e.getMessage());
        }
    }

    /**
     * Runs the main menu loop until the user chooses to exit.
     */
    public void start() {
        running = true;
        while (running) {
            System.out.println("==BIBLIOTHEKVERWALTUNGSSYSTEM==");
            System.out.println("1. Bücher verwalten");
            System.out.println("2. Buchkopien verwalten");
            System.out.println("3. Kunden verwalten");
            System.out.println("4. Eine Buchkopie ausleihen");
            System.out.println("5. Eine Buchkopie zurückgeben");
            System.out.println("6. Eine Buchkopie suchen");
            System.out.println("7. Vormerkungen verwalten");
            System.out.println("8. Bericht erstellen");
            System.out.println("9. Programm beenden");

            int choice = readInt("Wählen Sie eine Option: ");
            switch (choice) {
                case 1:
                    manageBooks();
                    break;
                case 2:
                    manageBookCopies();
                    break;
                case 3:
                    manageCustomers();
                    break;
                case 4:
                    loanBook();
                    break;
                case 5:
                    returnBook();
                    break;
                case 6:
                    bookCopyManager.searchBookCopies(scanner);
                    break;
                case 7:
                    manageReservations();
                    break;
                case 8:
                    createReport();
                    break;
                case 9:
                    stop();
                    break;
                default:
                    System.out.println("Ungültige Option. Zurück zum Hauptmenü.");
            }
        }
    }

    /**
     * Submenu for managing books.
     */
    public void manageBooks() {
        System.out.println("==BÜCHER VERWALTEN==");
        System.out.println("1. Ein Buch löschen");
        System.out.println("2. Zurück zum Hauptmenü");
        int choice = readInt("");
        switch (choice) {
            case 1:
                bookManager.deleteBook(scanner, bookCopyManager);
                break;
            case 2:
                break;
            default:
                System.out.println("Ungültige Option. Zurück zum Hauptmenü.");
        }
    }

    /**
     * Submenu for managing book copies.
     */
    public void manageBookCopies() {
        System.out.println("==BÜCHERKOPIEN VERWALTEN==");
        System.out.println("1. Eine Buchkopie löschen");
        System.out.println("2. Zurück zum Hauptmenü");
        int choice = readInt("");
        switch (choice) {
            case 1:
                bookCopyManager.deleteBookCopy(scanner);
                break;
            case 2:
                break;
            default:
                System.out.println("Ungültige Option. Zurück zum Hauptmenü.");
        }
    }

    /**
     * Submenu for managing customers.
     */
    public void manageCustomers() {
        System.out.println("==KUNDEN VERWALTEN==");
        System.out.println("1. Einen Kunden löschen");
        System.out.println("2. Zurück zum Hauptmenü");
        int choice = readInt("");
        switch (choice) {
            case 1:
                customerManager.deleteCustomer(scanner);
                break;
            case 2:
                break;
            default:
                System.out.println("Ungültige Option. Zurück zum Hauptmenü.");
        }
    }

    /**
     * Interactively loans a book copy to a customer.
     */
    public void loanBook() {
        System.out.println("==AUSLEIHE EINER BUCHKOPIE==");
        int customerId = readCustomerId();
        if (customerId == -1) {
            return;
        }
        int bookCopyId = readBookCopyId();
        if (bookCopyId == -1) {
            return;
        }
        try {
            loanService.loan(bookCopyId, customerId);
            System.out.println("Die Buchkopie " + bookCopyId
                    + " wurde erfolgreich an den Kunden " + customerId + " ausgeliehen.");
        } catch (IllegalStateException | IllegalArgumentException e) {
            System.out.println(e.getMessage());
        }
        promptReturnToMain();
    }

    /**
     * Interactively returns a book copy from a customer.
     */
    public void returnBook() {
        System.out.println("==RÜCKGABE EINER BUCHKOPIE==");
        int customerId = readCustomerId();
        if (customerId == -1) {
            return;
        }
        if (customerManager.getCustomer(customerId).getBookCopies().isEmpty()) {
            System.out.println("Der Kunde hat keine ausgeliehenen Buchkopien");
            return;
        }
        int bookCopyId = readBookCopyId();
        if (bookCopyId == -1) {
            return;
        }
        try {
            double fine = loanService.calculateFine(bookCopyId);
            long overdueDays = loanService.overdueDays(bookCopyId);
            loanService.returnBook(bookCopyId, customerId);
            System.out.println("Die Buchkopie " + bookCopyId
                    + " wurde erfolgreich vom Kunden " + customerId + " zurückgegeben.");
            if (fine > 0.0) {
                System.out.println(String.format(
                        java.util.Locale.GERMAN,
                        "Verspätungsgebühr: %.2f € (%d Tage überfällig)",
                        fine, overdueDays));
            }
        } catch (IllegalStateException | IllegalArgumentException e) {
            System.out.println(e.getMessage());
        }
        promptReturnToMain();
    }

    /**
     * Submenu for managing title reservations (the FIFO waiting list for
     * titles whose copies are all lent out).
     */
    public void manageReservations() {
        int choice = -1;
        while (choice != 4 && running) {
            System.out.println("==VORMERKUNGEN VERWALTEN==");
            System.out.println("1. Titel vormerken");
            System.out.println("2. Vormerkung stornieren");
            System.out.println("3. Warteschlange für einen Titel anzeigen");
            System.out.println("4. Zurück zum Hauptmenü");

            choice = readInt("");
            switch (choice) {
                case 1:
                    reserveTitle();
                    break;
                case 2:
                    cancelReservation();
                    break;
                case 3:
                    showReservationQueue();
                    break;
                case 4:
                    break;
                default:
                    System.out.println("Ungültige Option. Zurück zum Hauptmenü.");
            }
        }
    }

    /**
     * Places a reservation for a customer on a title that currently has no
     * available copy.
     *
     * <p>Both the book (by ISBN) and the waiting customer must exist; unknown
     * values are rejected before anything is enqueued.</p>
     */
    public void reserveTitle() {
        String isbn = readIsbnOrCancel("ISBN des gewünschten Titels: ");
        if (isbn == null) {
            return;
        }
        int customerId = readInt("Kunden-ID des Wartenden: ");
        if (customerId == -1) {
            return;
        }
        try {
            if (!bookManager.exists(isbn)) {
                throw new IllegalArgumentException(
                        "Es existiert kein Buch mit dieser ISBN: " + isbn);
            }
            if (!customerManager.exists(customerId)) {
                throw new IllegalArgumentException(
                        "Es existiert kein Kunde mit dieser ID: " + customerId);
            }
            var reservation = reservationManager.reserve(isbn, customerId);
            System.out.println("Vormerkung " + reservation.id() + " für ISBN "
                    + isbn + " von Kunde " + customerId + " angelegt.");
        } catch (IllegalArgumentException e) {
            System.out.println(e.getMessage());
        }
        promptReturnTo("Zurück zu den Vormerkungen");
    }

    /**
     * Removes a reservation from the queue by its id.
     */
    public void cancelReservation() {
        int reservationId = readInt("Vormerkungs-ID zum Stornieren: ");
        if (reservationId == -1) {
            return;
        }
        var removed = reservationManager.cancel(reservationId);
        if (removed.isPresent()) {
            System.out.println("Vormerkung " + reservationId + " wurde storniert.");
        } else {
            System.out.println("Keine Vormerkung mit dieser ID: " + reservationId);
        }
        promptReturnTo("Zurück zu den Vormerkungen");
    }

    /**
     * Prints the FIFO queue for one title, earliest reservation first.
     */
    public void showReservationQueue() {
        String isbn = readIsbnOrCancel("ISBN für die Warteschlange: ");
        if (isbn == null) {
            return;
        }
        var queue = reservationManager.queueFor(isbn);
        if (queue.isEmpty()) {
            System.out.println("Keine Vormerkungen für ISBN " + isbn);
        } else {
            System.out.println("Warteschlange für ISBN " + isbn + ":");
            for (var r : queue) {
                System.out.println("  Vormerkung " + r.id() + " – Kunde "
                        + r.customerId() + " (seit " + r.createdAt() + ")");
            }
        }
        promptReturnTo("Zurück zu den Vormerkungen");
    }

    /**
     * Submenu for generating reports.
     */
    public void createReport() {
        int choice = -1;
        while (choice != 7 && running) {
            System.out.println("==BERICHT ERSTELLEN==");
            System.out.println("1. Ausgabe aller Bücher");
            System.out.println("2. Ausgabe aller ausgeliehenen Buchkopien");
            System.out.println("3. Ausgabe aller nicht ausgeliehenen Buchkopien");
            System.out.println("4. Ausgabe aller Kunden");
            System.out.println("5. Ausgabe aller derzeit ausgeliehenen Buchkopien eines Kunden via Kunden-ID");
            System.out.println("6. Ausgabe der Anzahl an Buchkopien pro Verlag");
            System.out.println("7. Zurück zum Hauptmenü");
            System.out.println("8. Programm beenden");

            choice = readInt("");
            switch (choice) {
                case 1:
                    print(reportService.getAllBooks());
                    break;
                case 2:
                    print(reportService.getLentBookCopies());
                    break;
                case 3:
                    print(reportService.getAvailableBookCopies());
                    break;
                case 4:
                    print(reportService.getAllCustomers());
                    break;
                case 5:
                    printLentBookCopiesOfCustomer();
                    break;
                case 6:
                    print(reportService.getBookCopiesOfPublisher());
                    break;
                case 7:
                    break;
                case 8:
                    stop();
                    return;
                default:
                    System.out.println("Ungültige Eingabe.");
            }
        }
    }

    /**
     * Prompts for a customer ID and prints that customer's lent book copies.
     */
    private void printLentBookCopiesOfCustomer() {
        System.out.print("Bitte geben Sie die Kunden-ID ein (oder -1 zum Abbrechen): ");
        int customerId;
        try {
            customerId = Integer.parseInt(scanner.nextLine().trim());
        } catch (NumberFormatException e) {
            System.out.println("Ungültige Eingabe! Bitte eine ganze Zahl eingeben.");
            return;
        }
        if (customerId == -1) {
            return;
        }
        try {
            print(reportService.getLentBookCopiesOfCustomer(customerId));
        } catch (IllegalArgumentException e) {
            System.out.println(e.getMessage());
        }
    }

    /**
     * Prints each line of the given list, or a "no results" message.
     *
     * @param lines the lines to print
     */
    private void print(List<String> lines) {
        if (lines.isEmpty()) {
            System.out.println("Es wurden keine Einträge gefunden.");
            return;
        }
        for (String line : lines) {
            System.out.println(line);
        }
    }

    /**
     * Reads a non-empty text value, re-prompting on empty input and treating
     * {@code -1} or the end of the input as a cancel.
     *
     * @param prompt the prompt printed before reading
     * @return the trimmed input, or {@code null} to cancel
     */
    private String readIsbnOrCancel(String prompt) {
        while (true) {
            System.out.print(prompt);
            if (!scanner.hasNextLine()) {
                running = false;
                return null;
            }
            String input = scanner.nextLine().trim();
            if (input.isEmpty()) {
                System.out.println("Ungültige Eingabe! Bitte geben Sie eine ISBN ein.");
            } else if ("-1".equals(input)) {
                return null;
            } else {
                return input;
            }
        }
    }

    /**
     * Reads a customer ID, re-prompting until a valid, existing customer is
     * chosen or {@code -1} is entered to cancel.
     *
     * @return the chosen customer ID, or {@code -1} to cancel
     */
    private int readCustomerId() {
        while (true) {
            System.out.println("Bitte geben Sie die Kunden-ID ein (oder -1 zum Abbrechen):");
            int customerId = readInt("");
            if (customerId == -1) {
                return -1;
            }
            if (!customerManager.exists(customerId)) {
                System.out.println("Es existiert kein Kunde mit dieser ID: " + customerId);
            } else {
                return customerId;
            }
        }
    }

    /**
     * Reads a book-copy ID, re-prompting until a valid, existing copy is
     * chosen or {@code -1} is entered to cancel.
     *
     * @return the chosen book-copy ID, or {@code -1} to cancel
     */
    private int readBookCopyId() {
        while (true) {
            System.out.println("Bitte geben Sie die Buchkopie-ID ein:");
            int bookCopyId = readInt("");
            if (bookCopyId == -1) {
                return -1;
            }
            if (!bookCopyManager.exists(bookCopyId)) {
                System.out.println("Es existiert keine Buchkopie mit dieser ID: " + bookCopyId);
            } else {
                return bookCopyId;
            }
        }
    }

    /**
     * Reads an integer from the scanner, handling
     * {@link InputMismatchException} gracefully in every submenu.
     *
     * <p><b>Bug fix (BUG 7):</b> the original submenus called
     * {@code scanner.nextInt()} without catching
     * {@code InputMismatchException}, which crashed the app on non-numeric
     * input. This helper centralises the parse-and-retry logic.</p>
     *
     * @param prompt optional prompt printed before reading; may be empty
     * @return the parsed integer, or {@code -1} if the input could not be parsed
     */
    private int readInt(String prompt) {
        while (true) {
            if (prompt != null && !prompt.isEmpty()) {
                System.out.print(prompt);
            }
            if (!scanner.hasNextLine()) {
                running = false;
                return -1;
            }
            String input = scanner.nextLine();
            try {
                return Integer.parseInt(input.trim());
            } catch (NumberFormatException e) {
                System.out.println("Ungültige Eingabe! Bitte geben Sie eine Zahl ein.");
            }
        }
    }

    /** Ends the main loop; the caller then saves state and returns. */
    private void stop() {
        System.out.println("Das Programm wird beendet.");
        running = false;
    }

    /**
     * Offers the user a choice between returning to the main menu or exiting.
     */
    private void promptReturnToMain() {
        promptReturnTo("Zurück zum Hauptmenü");
    }

    /**
     * Offers the user a choice between returning to the caller's menu and
     * exiting the program.
     *
     * @param returnLabel label of the menu the user returns to, e.g.
     *                    {@code "Zurück zu den Vormerkungen"}
     */
    private void promptReturnTo(String returnLabel) {
        System.out.println("1. " + returnLabel);
        System.out.println("2. Das Programm beenden");
        int choice = readInt("");
        switch (choice) {
            case 1:
                break;
            case 2:
                stop();
                break;
            default:
                System.out.println("Ungültige Option.");
        }
    }
}
