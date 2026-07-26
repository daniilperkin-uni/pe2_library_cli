package com.example.library.manager;

import com.example.library.csv.CsvImporter;
import com.example.library.model.Customer;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.InputMismatchException;

/**
 * Manages the collection of {@link Customer} objects in the library.
 *
 * <p>Provides lookup, deletion and CSV import functionality.</p>
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
     * Interactively deletes a customer by ID.
     *
     * <p>The user is prompted for a customer ID, asked to confirm deletion,
     * and then asked to confirm again with {@code j}/{@code n}/{@code x}.
     * Entering {@code -1} at the ID prompt cancels the operation.</p>
     *
     * @param scanner the scanner used for user input
     */
    public void deleteCustomer(Scanner scanner) {
        System.out.println("==EINEN KUNDEN LÖSCHEN==");
        int choice = -1;
        int id = -1;
        while (choice != 2) {
            System.out.println("Bitte geben Sie die ID des Kundes ein (oder -1 zum Abbrechen):");
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
            if (!customers.containsKey(id)) {
                System.out.println("Es existiert kein Kunde mit dieser ID: " + id);
                continue;
            }

            System.out.println("Der Kunde mit ID: " + id + " wird gelöscht.");
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
                    boolean confirmed = false;
                    while (!confirmed) {
                        System.out.print("Kunde wirklich löschen? (j/n) oder 'x' zum Abbrechen: ");
                        String resp = scanner.nextLine().trim().toLowerCase();
                        if ("j".equals(resp)) {
                            confirmed = true;
                            customers.remove(id);
                            System.out.println("Der Kunde mit ID: " + id + " wurde gelöscht.");
                            choice = 2;
                        } else if ("n".equals(resp) || "x".equals(resp)) {
                            System.out.println("Löschvorgang abgebrochen.");
                            return;
                        } else {
                            System.out.println("Bitte 'j', 'n' oder 'x' eingeben.");
                        }
                    }
                    break;
                case 2:
                    break;
            }
        }
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
        InputStream input = getClass().getResourceAsStream(resourcePath);
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
