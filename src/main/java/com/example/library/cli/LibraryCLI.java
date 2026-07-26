package com.example.library.cli;

import com.example.library.manager.BookCopyManager;
import com.example.library.manager.BookManager;
import com.example.library.manager.CustomerManager;
import com.example.library.service.LoanService;
import com.example.library.service.ReportService;

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
    private final LoanService loanService;
    private final ReportService reportService;
    private final Scanner scanner;

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
                      CustomerManager customerManager, LoanService loanService,
                      ReportService reportService, Scanner scanner) {
        this.bookManager = bookManager;
        this.bookCopyManager = bookCopyManager;
        this.customerManager = customerManager;
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
     * @param args command-line arguments (ignored)
     */
    public static void main(String[] args) {
        BookManager bookManager = new BookManager();
        BookCopyManager bookCopyManager = new BookCopyManager();
        CustomerManager customerManager = new CustomerManager();
        LoanService loanService = new LoanService(bookCopyManager, customerManager);
        ReportService reportService =
                new ReportService(bookManager, bookCopyManager, customerManager);
        Scanner scanner = new Scanner(System.in);

        bookManager.importBooks("/bücher.csv");
        customerManager.importCustomers("/benutzer.csv");
        bookCopyManager.importBookCopies("/buchkopien.csv", bookManager, customerManager);

        LibraryCLI libraryCLI = new LibraryCLI(bookManager, bookCopyManager,
                customerManager, loanService, reportService, scanner);
        libraryCLI.start();
    }

    /**
     * Runs the main menu loop until the user chooses to exit.
     */
    public void start() {
        boolean running = true;
        while (running) {
            System.out.println("==BIBLIOTHEKVERWALTUNGSSYSTEM==");
            System.out.println("1. Bücher verwalten");
            System.out.println("2. Buchkopien verwalten");
            System.out.println("3. Kunden verwalten");
            System.out.println("4. Eine Buchkopie ausleihen");
            System.out.println("5. Eine Buchkopie zurückgeben");
            System.out.println("6. Eine Buchkopie suchen");
            System.out.println("7. Bericht erstellen");
            System.out.println("8. Programm beenden");

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
                    createReport();
                    break;
                case 8:
                    System.out.println("Das Programm wird beendet.");
                    running = false;
                    break;
                default:
                    System.out.println("Option ist noch nicht implementiert.");
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
                bookManager.deleteBook(scanner);
                break;
            case 2:
                break;
            default:
                System.out.println("Ungültige Option. Zurück zum Hauptmenu.");
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
                System.out.println("Ungültige Option. Zurück zum Hauptmenu.");
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
                System.out.println("Ungültige Option. Zurück zum Hauptmenu.");
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
            loanService.returnBook(bookCopyId, customerId);
            System.out.println("Die Buchkopie " + bookCopyId
                    + " wurde erfolgreich vom Kunden " + customerId + " zurückgegeben.");
        } catch (IllegalStateException | IllegalArgumentException e) {
            System.out.println(e.getMessage());
        }
        promptReturnToMain();
    }

    /**
     * Submenu for generating reports.
     */
    public void createReport() {
        int choice = -1;
        while (choice != 7) {
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
                    System.out.println("Das Programm wird beendet.");
                    System.exit(0);
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
            String input = scanner.nextLine();
            try {
                return Integer.parseInt(input.trim());
            } catch (NumberFormatException e) {
                System.out.println("Ungültige Eingabe! Bitte geben Sie eine Zahl ein.");
            }
        }
    }

    /**
     * Offers the user a choice between returning to the main menu or exiting.
     */
    private void promptReturnToMain() {
        System.out.println("1. Zurück zum Hauptmenü");
        System.out.println("2. Das Programm beenden");
        int choice = readInt("");
        switch (choice) {
            case 1:
                break;
            case 2:
                System.out.println("Das Programm wird beendet");
                System.exit(0);
            default:
                System.out.println("Ungültige Option. Zurück zum Hauptmenu.");
        }
    }
}
