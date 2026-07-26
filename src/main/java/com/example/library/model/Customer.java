package com.example.library.model;

import java.util.List;

/**
 * Represents a library customer (member) who can borrow book copies.
 *
 * <p>A customer has an ID, name, address, and a list of currently borrowed
 * {@link BookCopy} objects.</p>
 */
public class Customer {

    private long id;
    private String name;
    private String firstName;
    private String address;
    private String zipCode;
    private String city;
    private boolean feesPayed;
    private List<BookCopy> bookCopies;

    /**
     * Constructs a new Customer.
     *
     * @param id         the unique customer ID
     * @param name       the last name
     * @param firstName  the first name
     * @param address    the street address
     * @param zipCode    the postal code
     * @param city       the city
     * @param feesPayed  whether library fees are paid up to date
     * @param bookCopies the list of currently borrowed book copies
     */
    public Customer(long id, String name, String firstName, String address, String zipCode,
                    String city, boolean feesPayed, List<BookCopy> bookCopies) {
        this.id = id;
        this.name = name;
        this.firstName = firstName;
        this.address = address;
        this.zipCode = zipCode;
        this.city = city;
        this.feesPayed = feesPayed;
        this.bookCopies = bookCopies;
    }

    /**
     * Sets whether this customer's fees are paid.
     *
     * @param feesPayed {@code true} if fees are paid, {@code false} otherwise
     */
    public void setFeesPayed(boolean feesPayed) {
        this.feesPayed = feesPayed;
    }

    /**
     * Adds a book copy to this customer's borrowed list.
     *
     * @param bookCopy the copy to add
     */
    public void addBookCopy(BookCopy bookCopy) {
        bookCopies.add(bookCopy);
    }

    /**
     * Removes a book copy from this customer's borrowed list.
     *
     * @param bookCopy the copy to remove
     */
    public void removeBookCopy(BookCopy bookCopy) {
        bookCopies.remove(bookCopy);
    }

    /**
     * Returns the unique customer ID.
     *
     * @return the customer ID
     */
    public long getId() {
        return id;
    }

    /**
     * Returns the last name of this customer.
     *
     * @return the last name string
     */
    public String getName() {
        return name;
    }

    /**
     * Returns the first name of this customer.
     *
     * @return the first name string
     */
    public String getFirstName() {
        return firstName;
    }

    /**
     * Returns the street address of this customer.
     *
     * @return the address string
     */
    public String getAddress() {
        return address;
    }

    /**
     * Returns the postal code of this customer.
     *
     * @return the ZIP code string
     */
    public String getZipCode() {
        return zipCode;
    }

    /**
     * Returns the city of this customer.
     *
     * @return the city string
     */
    public String getCity() {
        return city;
    }

    /**
     * Returns whether this customer's fees are paid.
     *
     * @return {@code true} if fees are paid, {@code false} otherwise
     */
    public boolean isFeesPayed() {
        return feesPayed;
    }

    /**
     * Returns the list of book copies currently borrowed by this customer.
     *
     * @return the list of borrowed copies
     */
    public List<BookCopy> getBookCopies() {
        return bookCopies;
    }

    /**
     * Returns a string representation of this customer.
     *
     * @return a formatted string with customer fields
     */
    @Override
    public String toString() {
        return "Customer{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", firstName='" + firstName + '\'' +
                ", feesPayed=" + feesPayed +
                ", bookCopiesLent=" + bookCopies.size() +
                '}';
    }
}
