package com.example.library.model;

import java.time.LocalDate;
import java.util.Date;

/**
 * Represents a physical copy of a {@link Book} in the library.
 *
 * <p>Each copy has a unique ID, a shelf location, the date it was added to the
 * library, and loan state information (whether it is currently lent, the loan
 * date, and the ID of the customer who borrowed it).</p>
 */
public class BookCopy {

    private long id;
    private Book book;
    private String shelfLocation;
    private Date addedToLibrary;
    private boolean lent;
    private LocalDate loanDate;
    private int customerID;

    /**
     * Constructs a new BookCopy.
     *
     * @param id            the unique copy ID
     * @param book          the {@link Book} this copy belongs to (may be {@code null})
     * @param shelfLocation the shelf location code
     * @param addedToLibrary the date this copy was added to the library
     * @param lent          whether this copy is currently lent out
     * @param loanDate      the date of the current loan, or {@code null} if not lent
     * @param customerID    the ID of the customer who borrowed this copy, or {@code -1} if not lent
     */
    public BookCopy(long id, Book book, String shelfLocation, Date addedToLibrary,
                    boolean lent, LocalDate loanDate, int customerID) {
        this.id = id;
        this.book = book;
        this.shelfLocation = shelfLocation;
        this.addedToLibrary = addedToLibrary;
        this.lent = lent;
        this.loanDate = loanDate;
        this.customerID = customerID;
    }

    /**
     * Sets whether this copy is currently lent out.
     *
     * @param lent {@code true} if lent, {@code false} otherwise
     */
    public void setLent(boolean lent) {
        this.lent = lent;
    }

    /**
     * Sets the loan date for this copy.
     *
     * @param loanDate the loan date, or {@code null} to clear
     */
    public void setLoanDate(LocalDate loanDate) {
        this.loanDate = loanDate;
    }

    /**
     * Sets the customer ID associated with the current loan.
     *
     * @param customerID the customer ID, or {@code -1} if not lent
     */
    public void setCustomerID(int customerID) {
        this.customerID = customerID;
    }

    /**
     * Returns the unique ID of this copy.
     *
     * @return the copy ID
     */
    public long getId() {
        return id;
    }

    /**
     * Returns the book this copy belongs to.
     *
     * @return the {@link Book}, or {@code null} if not linked
     */
    public Book getBook() {
        return book;
    }

    /**
     * Returns the shelf location code.
     *
     * @return the shelf location string
     */
    public String getShelfLocation() {
        return shelfLocation;
    }

    /**
     * Returns the date this copy was added to the library.
     *
     * @return the addition date
     */
    public Date getAddedToLibrary() {
        return addedToLibrary;
    }

    /**
     * Returns whether this copy is currently lent out.
     *
     * @return {@code true} if lent, {@code false} otherwise
     */
    public boolean isLent() {
        return lent;
    }

    /**
     * Returns the loan date of this copy.
     *
     * @return the loan date, or {@code null} if not currently lent
     */
    public LocalDate getLoanDate() {
        return loanDate;
    }

    /**
     * Returns the customer ID of the current borrower.
     *
     * @return the customer ID, or {@code -1} if not lent
     */
    public int getCustomerID() {
        return customerID;
    }

    /**
     * Returns a string representation of this copy.
     *
     * @return a formatted string with all copy fields
     */
    @Override
    public String toString() {
        return "BookCopy{" +
                "id=" + id +
                ", book=" + book +
                ", shelfLocation='" + shelfLocation + '\'' +
                ", addedToLibrary=" + addedToLibrary +
                ", lent=" + lent +
                ", loanDate=" + loanDate +
                '}';
    }
}
