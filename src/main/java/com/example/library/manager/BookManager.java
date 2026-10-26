package com.example.library.manager;

import com.example.library.csv.CsvImporter;
import com.example.library.model.Book;
import com.example.library.model.BookCopy;

import java.io.IOException;
import java.io.InputStream;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Manages the collection of {@link Book} objects in the library.
 *
 * <p>Provides lookup, creation, deletion and CSV import functionality.</p>
 */
public class BookManager {

    final Map<String, Book> books = new HashMap<>();

    /**
     * Returns the internal book map.
     *
     * @return the map of ISBN to {@link Book}
     */
    public Map<String, Book> getBooks() {
        return books;
    }

    /**
     * Returns the book with the given ISBN.
     *
     * @param isbn the ISBN to look up
     * @return the matching {@link Book}, or {@code null} if not found
     */
    public Book getBook(String isbn) {
        return books.get(isbn);
    }

    /**
     * Checks whether a book with the given ISBN exists.
     *
     * @param isbn the ISBN to check
     * @return {@code true} if a book with this ISBN exists
     */
    public boolean exists(String isbn) {
        return books.containsKey(isbn);
    }

        /**
     * Reports why a book cannot be deleted, or {@code null} when it can.
     *
     * <p>Pure check: the CLI asks for the ISBN, prints the reason and only then
     * calls {@link #removeBook(String)}. It stays in the manager because it
     * needs the book map, the copies that reference it and the reservation
     * queue — but it no longer reads from {@code System.in}.</p>
     *
     * @param isbn               the ISBN to check
     * @param bookCopyManager    the manager holding the copies
     * @param reservationManager the manager holding the waiting list
     * @return a German reason, or {@code null} when deletion is allowed
     */
    public String deletionBlocker(String isbn, BookCopyManager bookCopyManager,
                                  ReservationManager reservationManager) {
        if (!books.containsKey(isbn)) {
            return "Es existiert kein Buch mit dieser ID: " + isbn;
        }
        for (BookCopy copy : bookCopyManager.getBookCopies().values()) {
            if (copy.getBook() != null && isbn.equals(copy.getBook().getIsbn())) {
                return "Das Buch mit ISBN: " + isbn
                        + " hat noch Buchkopien und kann nicht gelöscht werden.";
            }
        }
        if (reservationManager.hasReservationForBook(isbn)) {
            // Sonst bliebe eine Vormerkung auf einen nicht mehr vorhandenen
            // Titel zurück; sie würde beim nächsten Start still wieder
            // eingelesen und die Warteschlange verfälschen.
            return "Das Buch mit ISBN: " + isbn
                    + " hat noch offene Vormerkungen in der Warteschlange"
                    + " und kann nicht gelöscht werden.";
        }
        return null;
    }

    /**
     * Removes a book without any checks; use {@link #deletionBlocker} first.
     *
     * @param isbn the ISBN to remove
     * @return {@code true} when a book was removed
     */
    public boolean removeBook(String isbn) {
        return books.remove(isbn) != null;
    }

    /**
     * Creates a new book and adds it to the collection.
     *
     * <p>Pure validation and storage — the interactive prompting lives in the
     * CLI layer. Invalid input is rejected with a German
     * {@link IllegalArgumentException}.</p>
     *
     * @param isbn      the ISBN (must not be blank or already present)
     * @param title     the title (must not be blank)
     * @param authors   the authors (at least one entry)
     * @param year      the publication year (must be positive)
     * @param city      the city of publication
     * @param publisher the publisher name
     * @param edition   the edition number
     * @return the created {@link Book}
     * @throws IllegalArgumentException if the ISBN is blank or already known,
     *                                  or title/authors/year are invalid
     */
    public Book addBook(String isbn, String title, List<String> authors, int year,
                        String city, String publisher, int edition) {
        String cleanIsbn = isbn == null ? "" : isbn.trim();
        if (cleanIsbn.isEmpty()) {
            throw new IllegalArgumentException("Die ISBN darf nicht leer sein.");
        }
        if (books.containsKey(cleanIsbn)) {
            throw new IllegalArgumentException(
                    "Es existiert bereits ein Buch mit dieser ISBN: " + cleanIsbn);
        }
        if (title == null || title.trim().isEmpty()) {
            throw new IllegalArgumentException("Der Titel darf nicht leer sein.");
        }
        if (authors == null || authors.isEmpty()) {
            throw new IllegalArgumentException("Es muss mindestens ein Autor angegeben werden.");
        }
        if (year <= 0) {
            throw new IllegalArgumentException("Das Erscheinungsjahr muss größer als 0 sein.");
        }
        Book book = new Book(cleanIsbn, title.trim(), authors, year, city, publisher, edition);
        books.put(cleanIsbn, book);
        return book;
    }

    /**
     * Imports books from a CSV resource on the classpath.
     *
     * <p>The CSV must have a header row and columns: isbn, title, authors
     * (semicolon-separated), year, city, publisher, edition.</p>
     *
     * @param resourcePath the classpath resource path (e.g. {@code "/bücher.csv"})
     * @throws IllegalArgumentException if a row has too few columns
     */
    public void importBooks(String resourcePath) {
        InputStream input = CsvImporter.open(getClass(), resourcePath);
        if (input == null) {
            System.out.println("Datei existiert nicht: " + resourcePath);
            return;
        }
        try {
            List<CsvImporter.Row> rows = CsvImporter.parseRows(input, true);
            for (CsvImporter.Row row : rows) {
                // Ein abgeschnittener Datensatz darf nicht als rohe
                // ArrayIndexOutOfBoundsException enden: sonst verweigert der
                // nächste Start das Laden, ohne die kaputte Zeile zu nennen.
                CsvImporter.requireColumns(row, resourcePath, 7);
                String[] parts = row.fields();
                String isbn = parts[0].trim();
                String title = parts[1].trim();
                List<String> authors = Arrays.asList(parts[2].trim().split(";"));
                int year = Integer.parseInt(parts[3].trim());
                String city = parts[4].trim();
                String publisher = parts[5].trim();
                int edition = Integer.parseInt(parts[6].trim());
                Book book = new Book(isbn, title, authors, year, city, publisher, edition);
                books.put(isbn, book);
            }
            System.out.println("Bücher wurden erfolgreich importiert.");
        } catch (IOException e) {
            System.out.println("Fehler beim Importieren von Büchern.");
            throw new RuntimeException(e);
        }
    }
}
