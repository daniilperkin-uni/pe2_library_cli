package com.example.library.service;

import com.example.library.manager.BookCopyManager;
import com.example.library.manager.BookManager;
import com.example.library.manager.CustomerManager;
import com.example.library.model.Book;
import com.example.library.model.BookCopy;
import com.example.library.model.Customer;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Produces the various reports offered by the library system.
 *
 * <p>Every method returns a {@link List} of pre-formatted {@link String} lines
 * (or throws on invalid input). The service never writes to
 * {@code System.out}; the CLI layer is responsible for printing the returned
 * lines. This keeps the service pure and unit-testable.</p>
 */
public class ReportService {

    private final BookManager bookManager;
    private final BookCopyManager bookCopyManager;
    private final CustomerManager customerManager;

    /**
     * Constructs a new ReportService bound to the given managers.
     *
     * @param bookManager     the manager that owns all books
     * @param bookCopyManager the manager that owns all book copies
     * @param customerManager the manager that owns all customers
     */
    public ReportService(BookManager bookManager, BookCopyManager bookCopyManager,
                         CustomerManager customerManager) {
        this.bookManager = bookManager;
        this.bookCopyManager = bookCopyManager;
        this.customerManager = customerManager;
    }

    /**
     * Returns a formatted line for every book in the catalogue.
     *
     * @return a list of book descriptions (one per book)
     */
    public List<String> getAllBooks() {
        List<String> lines = new ArrayList<>();
        for (Book book : bookManager.getBooks().values()) {
            lines.add(book.toString());
        }
        return lines;
    }

    /**
     * Returns a formatted line for every book copy that is currently lent out.
     *
     * @return a list of lent book-copy descriptions
     */
    public List<String> getLentBookCopies() {
        List<String> lines = new ArrayList<>();
        for (BookCopy bookCopy : bookCopyManager.getBookCopies().values()) {
            if (bookCopy.isLent()) {
                lines.add(bookCopy.toString());
            }
        }
        return lines;
    }

    /**
     * Returns a formatted line for every book copy that is currently available.
     *
     * @return a list of available book-copy descriptions
     */
    public List<String> getAvailableBookCopies() {
        List<String> lines = new ArrayList<>();
        for (BookCopy bookCopy : bookCopyManager.getBookCopies().values()) {
            if (!bookCopy.isLent()) {
                lines.add(bookCopy.toString());
            }
        }
        return lines;
    }

    /**
     * Returns a formatted line for every customer.
     *
     * @return a list of customer descriptions
     */
    public List<String> getAllCustomers() {
        List<String> lines = new ArrayList<>();
        for (Customer customer : customerManager.getCustomers().values()) {
            lines.add(customer.toString());
        }
        return lines;
    }

    /**
     * Returns a formatted line for every book copy currently lent to the
     * given customer.
     *
     * <p><b>Bug fix:</b> the original CLI implementation parsed the customer ID
     * twice (once via {@code nextLine()}, once via {@code nextInt()}), placed
     * the rendering loop outside the validation branch, and dereferenced the
     * customer without a null check. This method accepts an already-parsed ID,
     * rejects unknown customers with an exception, and only iterates over a
     * non-null borrowed-copies list.</p>
     *
     * @param customerId the ID of the customer whose lent copies are reported
     * @return a list of book-copy descriptions lent to the customer
     * @throws IllegalArgumentException if no customer with the given ID exists
     */
    public List<String> getLentBookCopiesOfCustomer(int customerId) {
        Customer customer = customerManager.getCustomer(customerId);
        if (customer == null) {
            throw new IllegalArgumentException(
                    "Es existiert kein Kunde mit dieser ID: " + customerId);
        }
        List<String> lines = new ArrayList<>();
        for (BookCopy bookCopy : customer.getBookCopies()) {
            lines.add(bookCopy.toString());
        }
        return lines;
    }

    /**
     * Returns a formatted line per publisher describing how many book copies
     * belong to it and the corresponding percentage of the total copies.
     *
     * <p>Publishers that appear in the catalogue but own no copies are reported
     * with a count of {@code 0} and {@code 0.0%}.</p>
     *
     * @return a list of formatted {@code "<publisher>: <n> Buchkopien (<pct>%)"}
     *         lines, sorted alphabetically by publisher
     */
    public List<String> getBookCopiesOfPublisher() {
        Map<Integer, BookCopy> bookCopies = bookCopyManager.getBookCopies();
        Map<String, Integer> publisherBookCopies = new TreeMap<>();

        for (Book book : bookManager.getBooks().values()) {
            publisherBookCopies.putIfAbsent(book.getPublisher(), 0);
        }

        for (BookCopy bookCopy : bookCopies.values()) {
            Book book = bookCopy.getBook();
            if (book == null) {
                continue;
            }
            publisherBookCopies.merge(book.getPublisher(), 1, Integer::sum);
        }

        List<String> lines = new ArrayList<>();
        if (publisherBookCopies.isEmpty()) {
            lines.add("Keine Buchkopien oder Verlage vorhanden.");
            return lines;
        }

        int numberOfBookCopies = bookCopies.size();
        for (Map.Entry<String, Integer> entry : publisherBookCopies.entrySet()) {
            int count = entry.getValue();
            double percent = count == 0 ? 0.0
                    : (double) count / numberOfBookCopies * 100;
            lines.add(String.format("%s: %d Buchkopien (%.1f%%)",
                    entry.getKey(), count, percent));
        }
        return lines;
    }
}
