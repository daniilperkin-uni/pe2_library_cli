package com.example.library.csv;

import com.example.library.manager.BookCopyManager;
import com.example.library.manager.BookManager;
import com.example.library.manager.CustomerManager;
import com.example.library.model.Book;
import com.example.library.model.BookCopy;
import com.example.library.model.Customer;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Writes the library state back to CSV files in exactly the format the
 * importers read, so a saved directory can be loaded again on the next start.
 */
public final class CsvExporter {

    public static final String BOOKS_FILE = "bücher.csv";
    public static final String CUSTOMERS_FILE = "benutzer.csv";
    public static final String COPIES_FILE = "buchkopien.csv";

    private CsvExporter() {
    }

    /**
     * Saves books, customers and book copies into {@code dir}.
     *
     * @param dir target directory (created if missing)
     * @param books the book manager
     * @param customers the customer manager
     * @param copies the book-copy manager
     * @throws IOException if writing fails
     */
    public static void save(Path dir, BookManager books, CustomerManager customers,
                            BookCopyManager copies) throws IOException {
        Files.createDirectories(dir);
        List<String> lines = new ArrayList<>();
        lines.add("isbn,title,authors,year,city,publisher,edition");
        books.getBooks().values().stream()
                .sorted(Comparator.comparing(Book::getIsbn))
                .forEach(bk -> lines.add(String.join(",", q(bk.getIsbn()), q(bk.getTitle()),
                        q(String.join(";", bk.getAuthors())), String.valueOf(bk.getYear()),
                        q(bk.getCity()), q(bk.getPublisher()), String.valueOf(bk.getEdition()))));
        write(dir.resolve(BOOKS_FILE), lines);

        lines.clear();
        lines.add("id,name,firstName,address,zipCode,city,feesPayed");
        customers.getCustomers().values().stream()
                .sorted(Comparator.comparingLong(Customer::getId))
                .forEach(c -> lines.add(String.join(",", String.valueOf(c.getId()),
                        q(c.getName()), q(c.getFirstName()), q(c.getAddress()),
                        q(c.getZipCode()), q(c.getCity()), c.isFeesPayed() ? "yes" : "no")));
        write(dir.resolve(CUSTOMERS_FILE), lines);

        lines.clear();
        lines.add("id,bookIsbn,shelfLocation,addedToLibrary,lent,lentDate,customerID");
        SimpleDateFormat fmt = new SimpleDateFormat("yyyy-MM-dd");
        copies.getBookCopies().values().stream()
                .sorted(Comparator.comparingLong(BookCopy::getId))
                .forEach(bc -> lines.add(String.join(",", String.valueOf(bc.getId()),
                        q(bc.getBook() == null ? "" : bc.getBook().getIsbn()),
                        q(bc.getShelfLocation()), fmt.format(bc.getAddedToLibrary()),
                        bc.isLent() ? "yes" : "no",
                        bc.isLent() && bc.getLoanDate() != null ? bc.getLoanDate().toString() : "",
                        bc.isLent() ? String.valueOf(bc.getCustomerID()) : "")));
        write(dir.resolve(COPIES_FILE), lines);
    }

    private static void write(Path file, List<String> lines) throws IOException {
        Files.write(file, lines, StandardCharsets.UTF_8);
    }

    /** Quotes a field when it contains a comma, quote or leading/trailing space. */
    static String q(String value) {
        if (value == null) {
            return "";
        }
        if (value.contains(",") || value.contains("\"") || !value.equals(value.trim())) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }
}
