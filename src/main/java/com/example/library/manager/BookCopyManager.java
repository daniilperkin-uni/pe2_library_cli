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

/**
 * Manages the collection of {@link BookCopy} objects in the library.
 *
 * <p>Provides lookup, creation, deletion, search and CSV import functionality.</p>
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
     * Reports why a book copy cannot be deleted, or {@code null} when it can.
     *
     * <p>Pure check: the CLI asks for the ID, prints the reason and only then
     * calls {@link #removeBookCopy(int)}. It stays in the manager because it
     * needs the copy map — but it no longer reads from {@code System.in}.</p>
     *
     * @param id the copy ID to check
     * @return a German reason, or {@code null} when deletion is allowed
     */
    public String deletionBlocker(int id) {
        if (!bookCopies.containsKey(id)) {
            return "Es existiert keine Buchkopie mit dieser ID: " + id;
        }
        if (bookCopies.get(id).isLent()) {
            return "Die Buchkopie mit ID: " + id
                    + " ist verliehen und kann nicht gelöscht werden.";
        }
        return null;
    }

    /**
     * Removes a book copy without any checks; use {@link #deletionBlocker}
     * first.
     *
     * @param id the copy ID to remove
     * @return {@code true} when a copy was removed
     */
    public boolean removeBookCopy(int id) {
        return bookCopies.remove(id) != null;
    }

    /**
     * Creates a new copy for an existing book. The copy ID is assigned
     * automatically as the next free number; new copies start out available
     * (not lent) and with today's date as the date they were added.
     *
     * <p>Pure validation and storage — the interactive prompting lives in the
     * CLI layer.</p>
     *
     * @param isbn          the ISBN of the book the copy belongs to
     * @param shelfLocation the shelf location (must not be blank)
     * @param bookManager   the book manager used to resolve the ISBN
     * @return the created {@link BookCopy}
     * @throws IllegalArgumentException if no book exists for the ISBN or the
     *                                  shelf location is blank
     */
    public BookCopy addBookCopy(String isbn, String shelfLocation, BookManager bookManager) {
        String cleanIsbn = isbn == null ? "" : isbn.trim();
        if (!bookManager.exists(cleanIsbn)) {
            throw new IllegalArgumentException(
                    "Es existiert kein Buch mit dieser ISBN: " + cleanIsbn);
        }
        if (shelfLocation == null || shelfLocation.trim().isEmpty()) {
            throw new IllegalArgumentException("Der Standort darf nicht leer sein.");
        }
        int nextId = bookCopies.keySet().stream().mapToInt(Integer::intValue).max().orElse(0) + 1;
        BookCopy bookCopy = new BookCopy(nextId, bookManager.getBook(cleanIsbn),
                shelfLocation.trim(), new Date(), false, null, -1);
        bookCopies.put(nextId, bookCopy);
        return bookCopy;
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
        InputStream input = CsvImporter.open(getClass(), resourcePath);
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
     * Returns every copy of the book with the given ISBN.
     *
     * <p>Pure lookup — the CLI owns the prompt, the menu and the printing.</p>
     *
     * @param isbn the ISBN to match exactly
     * @return the matching copies, empty when none match
     */
    public List<BookCopy> findByIsbn(String isbn) {
        List<BookCopy> found = new ArrayList<>();
        for (BookCopy bookCopy : bookCopies.values()) {
            if (bookCopy.getBook() != null && bookCopy.getBook().getIsbn().equals(isbn)) {
                found.add(bookCopy);
            }
        }
        return found;
    }

    /**
     * Returns every copy whose book title matches, ignoring case.
     *
     * @param title the title to match
     * @return the matching copies, empty when none match
     */
    public List<BookCopy> findByTitle(String title) {
        List<BookCopy> found = new ArrayList<>();
        for (BookCopy bookCopy : bookCopies.values()) {
            if (bookCopy.getBook() != null
                    && bookCopy.getBook().getTitle().equalsIgnoreCase(title)) {
                found.add(bookCopy);
            }
        }
        return found;
    }

    /**
     * Returns every copy whose book lists the given author.
     *
     * @param author the author to match exactly
     * @return the matching copies, empty when none match
     */
    public List<BookCopy> findByAuthor(String author) {
        List<BookCopy> found = new ArrayList<>();
        for (BookCopy bookCopy : bookCopies.values()) {
            if (bookCopy.getBook() != null
                    && bookCopy.getBook().getAuthors().contains(author)) {
                found.add(bookCopy);
            }
        }
        return found;
    }
}
