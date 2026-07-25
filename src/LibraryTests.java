import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;

import java.io.*;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class LibraryCLICommandTests {

    BookManager bookManager;
    BookCopyManager bookCopyManager;
    CustomerManager customerManager;
    LoanService loanService;
    ReportService reportService;
    LibraryCLI libraryCLI;
    Scanner scanner;

    @BeforeEach
    void setup() {
        bookManager = new BookManager();
        bookCopyManager = new BookCopyManager();
        customerManager = new CustomerManager();
        loanService = new LoanService(bookCopyManager, customerManager);
        reportService = new ReportService(bookManager, bookCopyManager, customerManager);
        scanner = new Scanner(System.in);
        libraryCLI = new LibraryCLI(bookManager, bookCopyManager, customerManager, loanService, reportService, scanner);


        Book testBook = new Book("3036959548", "Testbuch", List.of("Max Mustermann"), 2022, "Berlin", "VerlagXY", 1);
        bookManager.books.put("3036959548", testBook);


        Customer c1 = new Customer(123, "Smith", "Bob", "Musterstraße 42", "12345", "Stuttgart", true, new ArrayList<>());
        Customer c2 = new Customer(2, "Miller", "Alice", "Universitätsweg 1", "54321", "Musterstadt", false, new ArrayList<>());
        customerManager.customers.put(123, c1);
        customerManager.customers.put(2, c2);


        BookCopy copy1 = new BookCopy(1, testBook, "SF42", new Date(), false, null, -1);
        BookCopy copy2 = new BookCopy(2, testBook, "SF42", new Date(), true, null, 2);
        bookCopyManager.bookCopies.put(1, copy1);
        bookCopyManager.bookCopies.put(2, copy2);
        c2.addBookCopy(copy2);
    }

    @Test
    void testBookImportAndRetrieval() {
        assertTrue(bookManager.exists("3036959548"));
        Book book = bookManager.getBook("3036959548");
        assertNotNull(book);
        assertEquals("3036959548", book.getIsbn());
    }

    @Test
    void testCustomerImport() {
        assertTrue(customerManager.exists(123));
        assertTrue(customerManager.exists(2));
        Customer customer = customerManager.getCustomer(123);
        assertEquals("Smith", customer.getName());
        assertEquals("Bob", customer.getFirstName());
    }

    @Test
    void testBookCopyImport() {
        assertTrue(bookCopyManager.exists(1));
        assertTrue(bookCopyManager.exists(2));
        BookCopy copy = bookCopyManager.getBookCopy(1);
        assertNotNull(copy);
        assertEquals("SF42", copy.getShelfLocation());
    }

    @Test
    void testLoanBook() {
        BookCopy copy = bookCopyManager.getBookCopy(1);
        Customer customer = customerManager.getCustomer(123);

        assertFalse(copy.isLent());
        loanService.loanBook(new Scanner("123\n1\n1\n"));

        assertTrue(copy.isLent());
        assertEquals(-1, copy.getCustomerID());
        assertFalse(customer.getBookCopies().contains(copy));
    }

    @Test
    void testReturnBook() {
        BookCopy copy = bookCopyManager.getBookCopy(2);
        Customer customer = customerManager.getCustomer(2);

        assertTrue(copy.isLent());
        assertTrue(customer.getBookCopies().contains(copy));
        loanService.returnBook(new Scanner("2\n2\n1\n"));

        assertFalse(copy.isLent());
        assertEquals(-1, copy.getCustomerID());
        assertFalse(customer.getBookCopies().contains(copy));
    }

    @Test
    void testSearchBookByIsbn() {
        Scanner scanner = new Scanner("1\n3036959548\n");
        bookCopyManager.searchBookCopies(scanner);
    }

    @Test
    void testSearchBookByTitle() {
        Book book = bookManager.getBook("3036959548");
        assertNotNull(book);
        Scanner scanner = new Scanner("2\n" + book.getTitle() + "\n");
        bookCopyManager.searchBookCopies(scanner);
    }

    @Test
    void testSearchBookByAuthor() {
        Book book = bookManager.getBook("3036959548");
        assertNotNull(book);
        String author = book.getAuthors().get(0);
        Scanner scanner = new Scanner("3\n" + author + "\n");
        bookCopyManager.searchBookCopies(scanner);
    }

    @Test
    void testDeleteBook() {
        assertTrue(bookManager.exists("3036959548"));
        Scanner scanner = new Scanner("3036959548\n1\n");
        bookManager.deleteBook(scanner);
        assertFalse(bookManager.exists("3036959548"));
    }

    @Test
    void testDeleteBookCopy() {
        assertTrue(bookCopyManager.exists(1));
        Scanner scanner = new Scanner("1\n1\n");
        bookCopyManager.deleteBookCopy(scanner);
        assertFalse(bookCopyManager.exists(1));
    }

    @Test
    void testDeleteCustomer() {
        assertTrue(customerManager.exists(2));
        Scanner scanner = new Scanner("2\n1\nj\n");
        customerManager.deleteCustomer(scanner);
        assertFalse(customerManager.exists(2));
    }

    @Test
    void testPrintAllBooksReport() {
        reportService.printAllBooks();
    }

    @Test
    void testPrintAllCustomersReport() {
        reportService.printAllCustomers();
    }

    @Test
    void testPrintAvailableBookCopies() {
        reportService.printAvailableBookCopies();
    }

    @Test
    void testPrintLentBookCopies() {
        reportService.printLentBookCopies();
    }

    @Test
    void testPrintBookCopiesOfPublisher() {
        reportService.printBookCopiesOfPublisher();
    }

    @Test
    void testPrintLentBookCopiesOfCustomer() {
        Scanner scanner = new Scanner("2\n");
        reportService.printLentBookCopiesOfCustomer(scanner);
    }
}



 class LibraryCLIFunctionalEdgeTests {

    BookManager bookManager;
    BookCopyManager bookCopyManager;
    CustomerManager customerManager;
    LoanService loanService;
    ReportService reportService;
    LibraryCLI libraryCLI;
    Scanner scanner;

    @BeforeEach
    void setup() {
        bookManager = new BookManager();
        bookCopyManager = new BookCopyManager();
        customerManager = new CustomerManager();
        loanService = new LoanService(bookCopyManager, customerManager);
        reportService = new ReportService(bookManager, bookCopyManager, customerManager);
        scanner = new Scanner(System.in);
        libraryCLI = new LibraryCLI(bookManager, bookCopyManager, customerManager, loanService, reportService, scanner);

        Book book = new Book("1111", "Grenzfall", List.of("Autor A"), 1999, "Leipzig", "Grenzverlag", 1);
        bookManager.books.put("1111", book);
        BookCopy copy = new BookCopy(10, book, "R10", new Date(), false, null, -1);
        bookCopyManager.bookCopies.put(10, copy);

        Customer customer = new Customer(999, "Edge", "Case", "Nowhere 0", "00000", "Void", false, new ArrayList<>());
        customerManager.customers.put(999, customer);
    }

    @Test
    void testLoanBookWithInvalidCustomerId() {
        Scanner invalidScanner = new Scanner("-999\n-1\n");
        loanService.loanBook(invalidScanner);
    }







    @Test
    void testPublisherReportWithNoCopies() {

        BookManager emptyBookManager = new BookManager();
        BookCopyManager emptyCopyManager = new BookCopyManager();
        ReportService rs = new ReportService(emptyBookManager, emptyCopyManager, customerManager);
        rs.printBookCopiesOfPublisher();
    }

}


class LibraryCLITests {

    @Nested
    class BookManagerTests {
        BookManager manager;

        @BeforeEach
        void setUp() {
            manager = new BookManager();
            manager.books.put("123", new Book("123", "Test Title", Arrays.asList("Author A"), 2020, "City", "Publisher", 1));
        }

        @Test
        void testExistsAndGetBook() {
            assertTrue(manager.exists("123"));
            Book book = manager.getBook("123");
            assertNotNull(book);
            assertEquals("Test Title", book.getTitle());
        }

        @Test
        void testDeleteBookConfirmed() {
            String input = "123\n1\n";
            Scanner scanner = new Scanner(input);
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            System.setOut(new PrintStream(out));

            manager.deleteBook(scanner);

            assertFalse(manager.exists("123"));
            String output = out.toString();
            assertTrue(output.contains("wird gel\u00F6scht"));
            assertTrue(output.contains("wurde gel\u00F6scht"));
        }

        @Test
        void testDeleteBookCanceled() {
            String input = "123\n2\n-1\n";
            Scanner scanner = new Scanner(input);
            manager.deleteBook(scanner);
            assertTrue(manager.exists("123"));
        }
    }

    @Nested
    class BookCopyManagerTests {
        BookCopyManager manager;
        BookManager bm;

        @BeforeEach
        void setUp() throws Exception {
            bm = new BookManager();
            bm.books.put("ISBN", new Book("ISBN", "Title", Collections.emptyList(), 2021, "City", "Pub", 1));
            manager = new BookCopyManager();
            manager.bookCopies.put(1, new BookCopy(1, bm.getBook("ISBN"), "A1", new Date(), false, null, 123));
        }

        @Test
        void testExistsAndGetBookCopy() {
            assertTrue(manager.exists(1));
            BookCopy copy = manager.getBookCopy(1);
            assertNotNull(copy);
            assertEquals(1, copy.getId());
        }

        @Test
        void testDeleteBookCopyConfirmed() {
            String input = "1\n1\n";
            Scanner scanner = new Scanner(input);
            manager.deleteBookCopy(scanner);
            assertFalse(manager.exists(1));
        }

        @Test
        void testDeleteBookCopyCanceled() {
            String input = "1\n2\n-1\n";
            Scanner scanner = new Scanner(input);
            manager.deleteBookCopy(scanner);
            assertTrue(manager.exists(1));
        }
    }

    @Nested
    class CustomerManagerTests {
        CustomerManager manager;

        @BeforeEach
        void setUp() {
            manager = new CustomerManager();
            manager.customers.put(1, new Customer(1, "Doe", "John", "Addr", "00000", "City", true, new ArrayList<>()));
        }

        @Test
        void testExistsAndGetCustomer() {
            assertTrue(manager.exists(1));
            Customer customer = manager.getCustomer(1);
            assertNotNull(customer);
            assertEquals("Doe", customer.getName());
        }

        @Test
        void testDeleteCustomerConfirmed() {
            String input = "1\n1\nj\n";
            Scanner scanner = new Scanner(input);
            manager.deleteCustomer(scanner);
            assertFalse(manager.exists(1));
        }

        @Test
        void testDeleteCustomerCanceled() {
            String input = "1\n2\n-1\n";
            Scanner scanner = new Scanner(input);
            manager.deleteCustomer(scanner);
            assertTrue(manager.exists(1));
        }
    }

    @Nested
    class LoanServiceTests {
        LoanService service;
        BookCopyManager bcm;
        CustomerManager cm;

        @BeforeEach
        void setUp() {
            bcm = new BookCopyManager();
            cm = new CustomerManager();
            bcm.bookCopies.put(1, new BookCopy(1, null, "A1", new Date(), false, null, 123));
            cm.customers.put(1, new Customer(1, "Doe", "Jane", "Addr", "00000", "City", true, new ArrayList<>()));
            service = new LoanService(bcm, cm);
        }

        @Test
        void testLoanBook() {
            String input = "1\n1\n1\n";
            Scanner scanner = new Scanner(input);
            service.loanBook(scanner);
            BookCopy copy = bcm.getBookCopy(1);
            assertTrue(copy.isLent());
            assertEquals(0, cm.getCustomer(1).getBookCopies().size());
        }

        @Test
        void testLoanBookCancelCustomer() {
            String input = "-1\n";
            Scanner scanner = new Scanner(input);
            service.loanBook(scanner);
            assertFalse(bcm.getBookCopy(1).isLent());
        }
    }

    @Nested
    class ReturnServiceTests {
        LoanService service;
        BookCopyManager bcm;
        CustomerManager cm;

        @BeforeEach
        void setUp() {
            bcm = new BookCopyManager();
            cm = new CustomerManager();
            BookCopy copy;
            copy = new BookCopy(1, null, "A1", new Date(), true, null, 123);
            bcm.bookCopies.put(1, copy);
            Customer cust = new Customer(1, "Doe", "Jane", "Addr", "00000", "City", true, new ArrayList<>());
            cust.addBookCopy(copy);
            cm.customers.put(1, cust);
            service = new LoanService(bcm, cm);
        }

        @Test
        void testReturnBook() {
            String input = "1\n1\n1\n";
            Scanner scanner = new Scanner(input);
            service.returnBook(scanner);
            BookCopy copy = bcm.getBookCopy(1);
            assertFalse(copy.isLent());
            assertTrue(cm.getCustomer(1).getBookCopies().isEmpty());
        }

        @Test
        void testReturnBookCancelCustomer() {
            String input = "-1\n";
            Scanner scanner = new Scanner(input);
            service.returnBook(scanner);
            assertTrue(bcm.getBookCopy(1).isLent());
        }
    }

    @Nested
    class ReportServiceTests {
        ReportService report;
        BookManager bm;
        BookCopyManager bcm;
        CustomerManager cm;
        ByteArrayOutputStream out;

        @BeforeEach
        void setUp() {
            bm = new BookManager();
            bcm = new BookCopyManager();
            cm = new CustomerManager();
            bm.books.put("1", new Book("1", "T1", Collections.emptyList(), 2000, "C", "P", 1));
            bcm.bookCopies.put(1, new BookCopy(1, bm.getBook("1"), "A", new Date(), false, null, 123));
            cm.customers.put(1, new Customer(1, "X", "Y", "Z", "000", "City", true, new ArrayList<>()));
            report = new ReportService(bm, bcm, cm);
            out = new ByteArrayOutputStream();
            System.setOut(new PrintStream(out));
        }

        @Test
        void testPrintAllBooks() {
            report.printAllBooks();
            String output = out.toString();
            assertTrue(output.contains("T1"));
        }

        @Test
        void testPrintAvailableBookCopies() {
            report.printAvailableBookCopies();
            String output = out.toString();
            assertTrue(output.contains("BookCopy"));
        }

        @Test
        void testPrintAllCustomers() {
            report.printAllCustomers();
            String output = out.toString();
            assertTrue(output.contains("Customer"));
        }
    }


    BookManager manager;

    @BeforeEach
    void setUp() {
        manager = new BookManager();
        manager.books.put("123", new Book("123", "Test Title", Arrays.asList("Author A"), 2020, "City", "Publisher", 1));
    }

    @Test
    void testExistsAndGetBook() {
        assertTrue(manager.exists("123"));
        Book book = manager.getBook("123");
        assertNotNull(book);
        assertEquals("Test Title", book.getTitle());
    }

    @Test
    void testDeleteBookConfirmed() {
        String input = "123\n1\n";
        Scanner scanner = new Scanner(input);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        System.setOut(new PrintStream(out));

        manager.deleteBook(scanner);

        assertFalse(manager.exists("123"));
        String output = out.toString();
        assertTrue(output.contains("wird gel\u00F6scht"));
        assertTrue(output.contains("wurde gel\u00F6scht"));
    }

    @Test
    void testDeleteBookCanceled() {
        String input = "123\n2\n-1\n";
        Scanner scanner = new Scanner(input);
        manager.deleteBook(scanner);
        assertTrue(manager.exists("123"));
    }


}

@Nested
class BookCopyManagerTests {
    BookCopyManager manager;
    BookManager bm;

    @BeforeEach
    void setUp() throws Exception {
        bm = new BookManager();
        bm.books.put("ISBN", new Book("ISBN", "Title", Collections.emptyList(), 2021, "City", "Pub", 1));
        manager = new BookCopyManager();
        manager.bookCopies.put(1, new BookCopy(1, bm.getBook("ISBN"), "A1", new Date(), false, null, 123));
    }

    @Test
    void testExistsAndGetBookCopy() {
        assertTrue(manager.exists(1));
        BookCopy copy = manager.getBookCopy(1);
        assertNotNull(copy);
        assertEquals(1, copy.getId());
    }

    @Test
    void testDeleteBookCopyConfirmed() {
        String input = "1\n1\n";
        Scanner scanner = new Scanner(input);
        manager.deleteBookCopy(scanner);
        assertFalse(manager.exists(1));
    }

    @Test
    void testDeleteBookCopyCanceled() {
        String input = "1\n2\n-1\n";
        Scanner scanner = new Scanner(input);
        manager.deleteBookCopy(scanner);
        assertTrue(manager.exists(1));
    }


}

@Nested
class CustomerManagerTests {
    CustomerManager manager;

    @BeforeEach
    void setUp() {
        manager = new CustomerManager();
        manager.customers.put(1, new Customer(1, "Doe", "John", "Addr", "00000", "City", true, new ArrayList<>()));
    }

    @Test
    void testExistsAndGetCustomer() {
        assertTrue(manager.exists(1));
        Customer customer = manager.getCustomer(1);
        assertNotNull(customer);
        assertEquals("Doe", customer.getName());
    }

    @Test
    void testDeleteCustomerConfirmed() {
        String input = "1\n1\nj\n";
        Scanner scanner = new Scanner(input);
        manager.deleteCustomer(scanner);
        assertFalse(manager.exists(1));
    }

    @Test
    void testDeleteCustomerCanceled() {
        String input = "1\n2\n-1\n";
        Scanner scanner = new Scanner(input);
        manager.deleteCustomer(scanner);
        assertTrue(manager.exists(1));
    }

}

@Nested
class LoanServiceTests {
    LoanService service;
    BookCopyManager bcm;
    CustomerManager cm;

    @BeforeEach
    void setUp() {
        bcm = new BookCopyManager();
        cm = new CustomerManager();
        bcm.bookCopies.put(1, new BookCopy(1, null, "A1", new Date(), false, null, 123));
        cm.customers.put(1, new Customer(1, "Doe", "Jane", "Addr", "00000", "City", true, new ArrayList<>()));
        service = new LoanService(bcm, cm);
    }

    @Test
    void testLoanBook() {
        String input = "1\n1\n1\n";
        Scanner scanner = new Scanner(input);
        service.loanBook(scanner);
        BookCopy copy = bcm.getBookCopy(1);
        assertTrue(copy.isLent());
        assertEquals(0, cm.getCustomer(1).getBookCopies().size());
    }

    @Test
    void testLoanBookCancelCustomer() {
        String input = "-1\n";
        Scanner scanner = new Scanner(input);
        service.loanBook(scanner);
        assertFalse(bcm.getBookCopy(1).isLent());
    }

}

@Nested
class ReturnServiceTests {
    LoanService service;
    BookCopyManager bcm;
    CustomerManager cm;

    @BeforeEach
    void setUp() {
        bcm = new BookCopyManager();
        cm = new CustomerManager();
        BookCopy copy = new BookCopy(1, null, "A1", new Date(), true, null, 123);
        bcm.bookCopies.put(1, copy);
        Customer cust = new Customer(1, "Doe", "Jane", "Addr", "00000", "City", true, new ArrayList<>());
        cust.addBookCopy(copy);
        cm.customers.put(1, cust);
        service = new LoanService(bcm, cm);
    }

    @Test
    void testReturnBook() {
        String input = "1\n1\n1\n";
        Scanner scanner = new Scanner(input);
        service.returnBook(scanner);
        BookCopy copy = bcm.getBookCopy(1);
        assertFalse(copy.isLent());
        assertTrue(cm.getCustomer(1).getBookCopies().isEmpty());
    }

    @Test
    void testReturnBookCancelCustomer() {
        String input = "-1\n";
        Scanner scanner = new Scanner(input);
        service.returnBook(scanner);
        assertTrue(bcm.getBookCopy(1).isLent());
    }

}

@Nested
class ReportServiceTests {
    ReportService report;
    BookManager bm;
    BookCopyManager bcm;
    CustomerManager cm;
    ByteArrayOutputStream out;

    @BeforeEach
    void setUp() {
        bm = new BookManager();
        bcm = new BookCopyManager();
        cm = new CustomerManager();
        bm.books.put("1", new Book("1", "T1", Collections.emptyList(), 2000, "C", "P", 1));
        bcm.bookCopies.put(1, new BookCopy(1, bm.getBook("1"), "A", new Date(), false, null, 123));
        cm.customers.put(1, new Customer(1, "X", "Y", "Z", "000", "City", true, new ArrayList<>()));
        report = new ReportService(bm, bcm, cm);
        out = new ByteArrayOutputStream();
        System.setOut(new PrintStream(out));
    }

    @Test
    void testPrintAllBooks() {
        report.printAllBooks();
        String output = out.toString();
        assertTrue(output.contains("T1"));
    }


    @Test
    void testPrintAllCustomers() {
        report.printAllCustomers();
        String output = out.toString();
        assertTrue(output.contains("Customer"));
    }




    private final InputStream systemIn = System.in;
    private final PrintStream systemOut = System.out;
    private ByteArrayInputStream testIn;
    private ByteArrayOutputStream testOut;



    @AfterEach
    void restoreSystemIO() {
        System.setIn(systemIn);
        System.setOut(systemOut);
    }

    private void provideInput(String data) {
        testIn = new ByteArrayInputStream(data.getBytes());
        System.setIn(testIn);
    }

    private final PrintStream originalOut = System.out;


    @BeforeEach
    void setUpOutput() {
        out = new ByteArrayOutputStream();
        System.setOut(new PrintStream(out));
    }

    @AfterEach
    void restoreOutput() {
        System.setOut(originalOut);
    }

    @Test
    void testBookManagerImportBooksFileNotFound() {
        BookManager manager = new BookManager();
        String missingPath = "nonexistent.csv";
        manager.importBooks(missingPath);
        String output = out.toString();
        assertTrue(output.contains("Datei existiert nicht: " + missingPath));
    }



    @Test
    void testCustomerManagerImportCustomersFileNotFound() {
        CustomerManager manager = new CustomerManager();
        manager.importCustomers("absent.csv");
        String output = out.toString();
        assertTrue(output.contains("Datei existiert nicht: absent.csv"));
    }

    @Test
    void testReportServicePrintLentBookCopies() {
        BookManager bm = new BookManager();
        BookCopyManager bcm = new BookCopyManager();
        CustomerManager cm = new CustomerManager();
        Book book = new Book("1", "Title", Collections.emptyList(), 2001, "City", "Pub", 1);
        bm.books.put("1", book);
        BookCopy lent = new BookCopy(1, book, "A1", new Date(), true, null, 123);
        BookCopy available = new BookCopy(2, book, "A2", new Date(), false, null, 123);
        bcm.bookCopies.put(1, lent);
        bcm.bookCopies.put(2, available);

        ReportService report = new ReportService(bm, bcm, cm);
        report.printLentBookCopies();
        String output = out.toString();
        assertTrue(output.contains("BookCopy{"));
        assertTrue(output.contains("id=1"));
        assertFalse(output.contains("id=2"));
    }
    @BeforeEach
    void initOutput() {
        out = new ByteArrayOutputStream();
        System.setOut(new PrintStream(out));
    }

    @TempDir
    static Path tempDir;

    @AfterEach
    void tearDown() {
        System.setOut(originalOut);
    }


    @Test
    void testSearchBookCopiesByISBNFound() {
        Book book = new Book("X", "T", Collections.emptyList(), 2020, "C", "P", 1);
        BookCopy copy = new BookCopy(1, book, "L1", new Date(), false, null, 123);
        BookCopyManager manager = new BookCopyManager();
        manager.bookCopies.put(1, copy);
        Scanner scanner = new Scanner("1\nX\n");

        manager.searchBookCopies(scanner);

        String output = out.toString();
        assertTrue(output.contains("BookCopy{"), "Expected to print found copy");
    }

    @Test
    void testSearchBookCopiesByTitleFound() {
        Book book = new Book("Y", "TitleY", Collections.emptyList(), 2020, "C", "P", 1);
        BookCopy copy = new BookCopy(2, book, "L2", new Date(), false, null, 123);
        BookCopyManager manager = new BookCopyManager();
        manager.bookCopies.put(2, copy);
        Scanner scanner = new Scanner("2\nTitleY\n");

        manager.searchBookCopies(scanner);

        String output = out.toString();
        assertTrue(output.contains("BookCopy{"));
    }

    @Test
    void testSearchBookCopiesByAuthorFound() {
        Book book = new Book("Z", "TZ", Arrays.asList("Auth"), 2020, "C", "P", 1);
        BookCopy copy = new BookCopy(3, book, "L3", new Date(), false, null, 123);
        BookCopyManager manager = new BookCopyManager();
        manager.bookCopies.put(3, copy);
        Scanner scanner = new Scanner("3\nAuth\n");

        manager.searchBookCopies(scanner);

        String output = out.toString();
        assertTrue(output.contains("BookCopy{"));
    }

    @Test
    void testSearchBookCopiesInvalidOption() {
        BookCopyManager manager = new BookCopyManager();
        Scanner scanner = new Scanner("5\n");

        manager.searchBookCopies(scanner);

        String output = out.toString();
        assertTrue(output.contains("Ungültige Option."));
        assertTrue(output.contains("Es wurden keine Buchkopien gefunden."));
    }

    @Test
    void testSearchBookCopiesBackToMenu() {
        BookCopyManager manager = new BookCopyManager();
        Scanner scanner = new Scanner("4\n");

        manager.searchBookCopies(scanner);

        String output = out.toString();
        assertTrue(output.contains("Es wurden keine Buchkopien gefunden."));
    }
}