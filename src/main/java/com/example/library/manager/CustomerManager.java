package com.example.library.manager;

import com.example.library.csv.CsvImporter;
import com.example.library.model.Customer;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Manages the collection of {@link Customer} objects in the library.
 *
 * <p>Provides lookup, creation, deletion and CSV import functionality.</p>
 */
public class CustomerManager {

    final Map<Integer, Customer> customers = new HashMap<>();

    /**
     * Returns the internal customer map.
     *
     * @return the map of customer ID to {@link Customer}
     */
    public Map<Integer, Customer> getCustomers() {
        return customers;
    }

    /**
     * Returns the customer with the given ID.
     *
     * @param id the customer ID
     * @return the matching {@link Customer}, or {@code null} if not found
     */
    public Customer getCustomer(int id) {
        return customers.get(id);
    }

    /**
     * Checks whether a customer with the given ID exists.
     *
     * @param id the customer ID
     * @return {@code true} if a customer with this ID exists
     */
    public boolean exists(int id) {
        return customers.containsKey(id);
    }

        /**
     * Reports why a customer cannot be deleted, or {@code null} when it can.
     *
     * <p>Pure check: the CLI asks for the ID, prints the reason and only then
     * calls {@link #removeCustomer(int)}. It stays in the manager because it
     * needs the customer map and their loans — but it no longer reads from
     * {@code System.in}.</p>
     *
     * @param id the customer ID to check
     * @return a German reason, or {@code null} when deletion is allowed
     */
    public String deletionBlocker(int id) {
        if (!customers.containsKey(id)) {
            return "Es existiert kein Kunde mit dieser ID: " + id;
        }
        if (!customers.get(id).getBookCopies().isEmpty()) {
            return "Der Kunde mit ID: " + id
                    + " hat noch ausgeliehene Buchkopien und kann nicht gelöscht werden.";
        }
        return null;
    }

    /**
     * Removes a customer without any checks; use {@link #deletionBlocker} first.
     *
     * @param id the customer ID to remove
     * @return {@code true} when a customer was removed
     */
    public boolean removeCustomer(int id) {
        return customers.remove(id) != null;
    }

    /**
     * Creates a new customer. The customer ID is assigned automatically as
     * the next free number; new customers start without paid fees.
     *
     * <p>Pure validation and storage — the interactive prompting lives in the
     * CLI layer.</p>
     *
     * @param name      the last name (must not be blank)
     * @param firstName the first name (must not be blank)
     * @param address   the street address
     * @param zipCode   the postal code
     * @param city      the city of residence
     * @return the created {@link Customer}
     * @throws IllegalArgumentException if the last or first name is blank
     */
    public Customer addCustomer(String name, String firstName, String address,
                                String zipCode, String city) {
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("Der Nachname darf nicht leer sein.");
        }
        if (firstName == null || firstName.trim().isEmpty()) {
            throw new IllegalArgumentException("Der Vorname darf nicht leer sein.");
        }
        int nextId = customers.keySet().stream().mapToInt(Integer::intValue).max().orElse(0) + 1;
        Customer customer = new Customer(nextId, name.trim(), firstName.trim(), address,
                zipCode, city, false, new ArrayList<>());
        customers.put(nextId, customer);
        return customer;
    }

    /**
     * Imports customers from a CSV resource on the classpath.
     *
     * <p>The CSV must have a header row and columns: id, name, firstName,
     * address, zipCode, city, feesPayed (yes/no).</p>
     *
     * @param resourcePath the classpath resource path
     */
    public void importCustomers(String resourcePath) {
        InputStream input = CsvImporter.open(getClass(), resourcePath);
        if (input == null) {
            System.out.println("Datei existiert nicht: " + resourcePath);
            return;
        }
        try {
            List<String[]> rows = CsvImporter.parse(input, true);
            for (String[] parts : rows) {
                int id = Integer.parseInt(parts[0].trim());
                String name = parts[1].trim();
                String firstName = parts[2].trim();
                String address = parts[3].trim();
                String zipCode = parts[4].trim();
                String city = parts[5].trim();
                boolean feesPayed = parts[6].trim().equalsIgnoreCase("yes");
                Customer customer = new Customer(id, name, firstName, address, zipCode,
                        city, feesPayed, new ArrayList<>());
                customers.put(id, customer);
            }
            System.out.println("Benutzer wurden erfolgreich importiert.");
        } catch (IOException e) {
            System.out.println("Fehler beim Importieren von Benutzern.");
            throw new RuntimeException(e);
        }
    }
}
