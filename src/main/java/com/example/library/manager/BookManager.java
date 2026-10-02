package com.example.library.manager;

import com.example.library.csv.CsvImporter;
import com.example.library.model.Book;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.InputMismatchException;

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
     * Interactively deletes a book by ISBN.
     *
     * <p>The user is prompted for an ISBN and asked to confirm deletion.
     * Entering {@code -1} cancels the operation. A book that still has book
     * copies cannot be deleted.</p>
     *
     * @param scanner         the scanner used for user input
     * @param bookCopyManager the manager used to check for remaining copies
     */
    public void deleteBook(Scanner scanner, BookCopyManager bookCopyManager) {
        System.out.println("==EIN BUCH LÖSCHEN==");
        int choice = -1;
        String isbn;
        while (choice != 2) {
            System.out.println("Bitte geben Sie die ISBN des Buches ein (oder -1 zum Abbrechen):");
            isbn = scanner.nextLine();
            if (isbn.equals("-1")) {
                break;
            }
            if (!books.containsKey(isbn)) {
                System.out.println("Es existiert kein Buch mit dieser ID: " + isbn);
                continue;
            }
            boolean referencedByCopies = false;
            for (var copy : bookCopyManager.getBookCopies().values()) {
                if (copy.getBook() != null && isbn.equals(copy.getBook().getIsbn())) {
                    referencedByCopies = true;
                    break;
                }
            }
            if (referencedByCopies) {
                System.out.println("Das Buch mit ISBN: " + isbn
                        + " hat noch Buchkopien und kann nicht gelöscht werden.");
                continue;
            }
            System.out.println("Das Buch mit ISBN: " + isbn + " wird gelöscht.");
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
                    books.remove(isbn);
                    System.out.println("Das Buch mit ISBN: " + isbn + " wurde gelöscht.");
                    choice = 2;
                    break;
                case 2:
                    break;
                default:
                    System.out.println("Ungültige Option. Bitte erneut wählen.");
            }
        }
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
     */
    public void importBooks(String resourcePath) {
        InputStream input = CsvImporter.open(getClass(), resourcePath);
        if (input == null) {
            System.out.println("Datei existiert nicht: " + resourcePath);
            return;
        }
        try {
            List<String[]> rows = CsvImporter.parse(input, true);
            for (String[] parts : rows) {
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
