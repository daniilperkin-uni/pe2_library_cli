package com.example.library.manager;

import com.example.library.csv.CsvImporter;
import com.example.library.model.Book;
import com.example.library.model.BookCopy;
import com.example.library.model.Customer;

import java.io.IOException;
import java.io.InputStream;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.InputMismatchException;

/**
 * Manages the collection of {@link BookCopy} objects in the library.
 *
 * <p>Provides lookup, deletion, search and CSV import functionality.</p>
 */
public class BookCopyManager {

    final Map<Integer, BookCopy> bookCopies = new HashMap<>();

    /**
     * Returns the internal book-copy map.
     *
     * @return the map of copy ID to {@link BookCopy}
     */
    public Map<Integer, BookCopy> getBookCopies() {
        return bookCopies;
    }

    /**
     * Returns the book copy with the given ID.
     *
     * @param id the copy ID
     * @return the matching {@link BookCopy}, or {@code null} if not found
     */
    public BookCopy getBookCopy(int id) {
        return bookCopies.get(id);
    }

    /**
     * Checks whether a book copy with the given ID exists.
     *
     * @param id the copy ID
     * @return {@code true} if a copy with this ID exists
     */
    public boolean exists(int id) {
        return bookCopies.containsKey(id);
    }

    /**
     * Interactively deletes a book copy by ID.
     *
     * <p>The user is prompted for a copy ID and asked to confirm deletion.
     * Entering {@code -1} cancels the operation.</p>
     *
     * @param scanner the scanner used for user input
     */
    public void deleteBookCopy(Scanner scanner) {
        System.out.println("==EINE BUCHKOPIE LÖSCHEN==");
        int choice = -1;
        int id = -1;
        while (choice != 2) {
            System.out.println("Bitte geben Sie die Buchkopie-ID ein (oder -1 zum Abbrechen):");
            try {
                id = scanner.nextInt();
                scanner.nextLine();
            } catch (InputMismatchException e) {
                System.out.println("Ungültige Eingabe! Bitte geben Sie eine Zahl ein.");
                scanner.nextLine();
                continue;
            }
            if (id == -1) {
                break;
            }
            if (!bookCopies.containsKey(id)) {
                System.out.println("Es existiert keine Buchkopie mit dieser ID: " + id);
                continue;
            }

            System.out.println("Die Buchkopie mit ID: " + id + " wird gelöscht.");
            System.out.println("1. Bestätigen");
            System.out.println("2. Abbrechen");
            try {
                choice = scanner.nextInt();
                scanner.nextLine();
            } catch (InputMismatchException e) {
                System.out.println("Ungültige Eingabe! Bitte geben Sie eine Zahl ein.");
                scanner.nextLine();
                continue;
            }

            switch (choice) {
                case 1:
                    bookCopies.remove(id);
                    System.out.println("Die Buchkopie mit ID: " + id + " wurde gelöscht.");
                    choice = 2;
                    break;
                case 2:
                    break;
            }
        }
    }

    /**
     * Imports book copies from a CSV resource on the classpath.
     *
     * <p>The CSV must have a header row and columns: id, bookIsbn,
     * shelfLocation, addedToLibrary (yyyy-MM-dd), lent (yes/no),
     * lentDate (yyyy-MM-dd, only if lent), customerID (only if lent).</p>
     *
     * @param resourcePath   the classpath resource path
     * @param bookManager    the book manager for resolving ISBNs
     * @param customerManager the customer manager for resolving customer IDs
     * @throws RuntimeException if parsing fails or a referenced customer does not exist
     */
    public void importBookCopies(String resourcePath, BookManager bookManager,
                                 CustomerManager customerManager) {
        InputStream input = getClass().getResourceAsStream(resourcePath);
        if (input == null) {
            System.out.println("Datei existiert nicht: " + resourcePath);
            return;
        }
        try {
            List<String[]> rows = CsvImporter.parse(input, true);
            SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd");
            for (String[] parts : rows) {
                int id = Integer.parseInt(parts[0].trim());
                Book book = bookManager.getBook(parts[1].trim());
                String shelfLocation = parts[2].trim();
                Date addedToLibrary = formatter.parse(parts[3].trim());
                boolean lent = parts[4].trim().equalsIgnoreCase("yes");
                LocalDate loanDate = null;
                int customerID = -1;

                if (lent) {
                    loanDate = LocalDate.parse(parts[5].trim());
                    customerID = Integer.parseInt(parts[6].trim());
                    if (!customerManager.exists(customerID)) {
                        throw new IllegalArgumentException(
                                "Es existiert kein Kunde mit ID " + customerID);
                    }
                }

                BookCopy bookCopy = new BookCopy(id, book, shelfLocation, addedToLibrary,
                        lent, loanDate, customerID);
                bookCopies.put(id, bookCopy);

                if (lent) {
                    customerManager.getCustomer(customerID).addBookCopy(bookCopy);
                }
            }
            System.out.println("Buchkopien wurden erfolgreich importiert.");
        } catch (IOException | ParseException e) {
            System.out.println("Fehler beim Importieren von Buchkopien.");
            throw new RuntimeException(e);
        }
    }

    /**
     * Interactively searches for book copies by ISBN, title, or author.
     *
     * @param scanner the scanner used for user input
     */
    public void searchBookCopies(Scanner scanner) {
        System.out.println("==EINE BUCHKOPIE SUCHEN==");
        System.out.println("1. ISBN");
        System.out.println("2. Buchtitel");
        System.out.println("3. Autor");
        System.out.println("4. Zurück zum Haupmenü");
        int choice;
        try {
            choice = scanner.nextInt();
            scanner.nextLine();
        } catch (InputMismatchException e) {
            System.out.println("Ungültige Eingabe! Bitte geben Sie eine Zahl ein.");
            scanner.nextLine();
            return;
        }
        List<BookCopy> foundBookCopies = new ArrayList<>();
        switch (choice) {
            case -1:
                break;
            case 1:
                System.out.println("Bitte geben Sie die ISBN der Buchkopie ein:");
                String isbn = scanner.nextLine();
                for (BookCopy bookCopy : bookCopies.values()) {
                    if (bookCopy.getBook() != null && bookCopy.getBook().getIsbn().equals(isbn)) {
                        foundBookCopies.add(bookCopy);
                    }
                }
                if (foundBookCopies.isEmpty()) {
                    System.out.println("Es existiert keine Buchkopie mit dieser ISBN: " + isbn);
                }
                break;
            case 2:
                System.out.println("Bitte geben Sie den Buchtitel der Buchkopie ein:");
                String title = scanner.nextLine();
                for (BookCopy bookCopy : bookCopies.values()) {
                    if (bookCopy.getBook() != null
                            && bookCopy.getBook().getTitle().equalsIgnoreCase(title)) {
                        foundBookCopies.add(bookCopy);
                    }
                }
                if (foundBookCopies.isEmpty()) {
                    System.out.println("Es existiert keine Buchkopie mit diesem Buchtitel: " + title);
                }
                break;
            case 3:
                System.out.println("Bitte geben Sie den Autor der Buchkopie ein (oder -1 zum Abbrechen):");
                String author = scanner.nextLine();
                for (BookCopy bookCopy : bookCopies.values()) {
                    if (bookCopy.getBook() != null
                            && bookCopy.getBook().getAuthors().contains(author)) {
                        foundBookCopies.add(bookCopy);
                    }
                }
                if (foundBookCopies.isEmpty()) {
                    System.out.println("Es existiert keine Buchkopie mit diesem Autor: " + author);
                }
                break;
            case 4:
                break;
            default:
                System.out.println("Ungültige Option. Zurück zum Hauptmenu.");
        }
        if (!foundBookCopies.isEmpty()) {
            for (BookCopy bookCopy : foundBookCopies) {
                System.out.println(bookCopy.toString());
            }
        } else {
            System.out.println("Es wurden keine Buchkopien gefunden.");
        }
    }
}
