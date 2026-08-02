package com.example.library.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Represents a book in the library system.
 *
 * <p>A book is identified by its ISBN and contains bibliographic metadata
 * such as title, authors, publication year, city, publisher and edition.</p>
 */
public class Book {

    private String isbn;
    private String title;
    private List<String> authors;
    private int year;
    private String city;
    private String publisher;
    private int edition;

    /**
     * Constructs a new Book with the given bibliographic data.
     *
     * @param isbn      the ISBN identifying this book
     * @param title     the title of the book
     * @param authors   the list of authors (may contain multiple entries)
     * @param year      the publication year
     * @param city      the city of publication
     * @param publisher the publisher name
     * @param edition   the edition number
     */
    public Book(String isbn, String title, List<String> authors, int year,
                String city, String publisher, int edition) {
        this.isbn = isbn;
        this.title = title;
        // defensive copy so callers cannot mutate the book's authors after construction
        this.authors = new ArrayList<>(authors);
        this.year = year;
        this.city = city;
        this.publisher = publisher;
        this.edition = edition;
    }

    /**
     * Returns the ISBN of this book.
     *
     * @return the ISBN string
     */
    public String getIsbn() {
        return isbn;
    }

    /**
     * Returns the title of this book.
     *
     * @return the title string
     */
    public String getTitle() {
        return title;
    }

    /**
     * Returns the list of authors of this book.
     *
     * @return an unmodifiable view of the authors list
     */
    public List<String> getAuthors() {
        return Collections.unmodifiableList(authors);
    }

    /**
     * Returns the publication year of this book.
     *
     * @return the year as an integer
     */
    public int getYear() {
        return year;
    }

    /**
     * Returns the city of publication.
     *
     * @return the city string
     */
    public String getCity() {
        return city;
    }

    /**
     * Returns the publisher of this book.
     *
     * @return the publisher name string
     */
    public String getPublisher() {
        return publisher;
    }

    /**
     * Returns the edition number of this book.
     *
     * @return the edition number as an integer
     */
    public int getEdition() {
        return edition;
    }

    /**
     * Returns a human-readable string representation of this book.
     *
     * @return a formatted string containing title, authors, year and ISBN
     */
    @Override
    public String toString() {
        return String.format("Titel: %s | Autor: %s | Jahr: %d | isbn: %s", title, authors, year, isbn);
    }
}
