import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.util.*;

public class LibraryCLI {
    private final BookManager bookManager;
    private final BookCopyManager bookCopyManager;
    private final CustomerManager customerManager;
    private final LoanService loanService;
    private final ReportService reportService;
    private final Scanner scanner;

    public LibraryCLI(BookManager bookManager, BookCopyManager bookCopyManager,
                      CustomerManager customerManager, LoanService loanService, ReportService reportService, Scanner scanner) {
        this.bookManager = bookManager;
        this.bookCopyManager = bookCopyManager;
        this.customerManager = customerManager;
        this.loanService = loanService;
        this.reportService = reportService;
        this.scanner = scanner;
    }
    public static void main(String[] args) {
        BookManager bookManager = new BookManager();
        BookCopyManager bookCopyManager = new BookCopyManager();
        CustomerManager customerManager = new CustomerManager();
        LoanService loanService = new LoanService(bookCopyManager, customerManager);
        ReportService reportService = new ReportService(bookManager, bookCopyManager ,customerManager);
        Scanner scanner = new Scanner(System.in);

        bookManager.importBooks("src/resources/bücher.csv");
        customerManager.importCustomers("src/resources/benutzer.csv");
        bookCopyManager.importBookCopies("src/resources/buchkopien.csv", bookManager, customerManager);

        LibraryCLI libraryCLI = new LibraryCLI(bookManager, bookCopyManager,
                customerManager, loanService, reportService, scanner);
        libraryCLI.start();
    }
    public void start() {
        boolean running = true;

        while (running) {
            System.out.println("==BIBLIOTHEKVERWALTUNGSSYSTEM==");
            System.out.println("1. Bücher verwalten");
            System.out.println("2. Buchkopien verwalten");
            System.out.println("3. Kunden verwalten");
            System.out.println("4. Eine Buchkopie ausleihen");
            System.out.println("5. Eine Buchkopie zurückgeben");
            System.out.println("6. Eine Buchkopie suchen");
            System.out.println("7. Bericht erstellen");
            System.out.println("8. Programm beenden");

            //Fix Issue 1
            int choice = 0;
            boolean valid = false;
            do {
                System.out.print("Wählen Sie eine Option: ");
                String input = scanner.nextLine();
                try {
                    choice = Integer.parseInt(input.trim());
                    valid = true;
                } catch (NumberFormatException e) {
                    System.out.println("Ungültige Eingabe! Bitte geben Sie eine Zahl ein.");
                }
            } while (!valid);
            //End

            switch (choice) {
                case 1:
                    manageBooks(scanner);
                    break;
                case 2:
                    manageBookCopies(scanner);
                    break;
                case 3:
                    manageCustomers(scanner);
                    break;
                case 4:
                    loanService.loanBook(scanner);
                    break;
                case 5:
                    loanService.returnBook(scanner);
                    break;
                case 6:
                    bookCopyManager.searchBookCopies(scanner);
                    break;
                case 7:
                    reportService.createReport(scanner);
                    break;
                case 8:
                    System.out.println("Das Programm wird beendet.");
                    System.exit(0);
                default:
                    System.out.println("Option ist noch nicht implementiert.");
            }

        }
    }

    public void manageBooks(Scanner scanner) {
        System.out.println("==BÜCHER VERWALTEN==");
        System.out.println("1. Ein Buch löschen");
        System.out.println("2. Zurück zum Hauptmenü");
        int choice = scanner.nextInt();
        scanner.nextLine();
        switch (choice) {
            case 1:
                bookManager.deleteBook(scanner);
                break;
            case 2:
                break;
        }
    }


    public void manageBookCopies(Scanner scanner) {
        System.out.println("==BÜCHERKOPIEN VERWALTEN==");
        System.out.println("1. Eine Buchkopie löschen");
        System.out.println("2. Zurück zum Hauptmenü");
        int choice = scanner.nextInt();
        scanner.nextLine();
        switch (choice) {
            case 1:
                bookCopyManager.deleteBookCopy(scanner);
                break;
            case 2:
                break;
        }
    }


    public void manageCustomers(Scanner scanner){
        System.out.println("==KUNDEN VERWALTEN==");
        System.out.println("1. Einen Kunden löschen");
        System.out.println("2. Zurück zum Hauptmenü");
        int choice = scanner.nextInt();
        scanner.nextLine();

        switch (choice) {
            case 1:
                customerManager.deleteCustomer(scanner);
                break;
            case 2:
                break;
        }
    }
}

class BookManager{
    final Map<String, Book> books = new HashMap<>();

    public void deleteBook(Scanner scanner) {
        System.out.println("==EIN BUCH LÖSCHEN==");
        int choice = -1;
        String isbn;
        while (choice != 2) {
            System.out.println("Bitte geben Sie die ISBN des Buches ein (oder -1 zum Abbrechen):");
            isbn = scanner.nextLine();
            if(isbn.equals("-1")){
                break;
            }
            if(!books.containsKey(isbn)) {
                System.out.println("Es existiert kein Buch mit dieser ID: " + isbn);
                continue;
            }
            System.out.println("Das Buch mit ISBN: " + isbn + " wird gelöscht.");
            System.out.println("1. Bestätigen");
            System.out.println("2. Abbrechen");
            choice = scanner.nextInt();
            scanner.nextLine();
            switch (choice) {
                case 1:
                    books.remove(isbn);
                    System.out.println("Das Buch mit ISBN: " + isbn + " wurde gelöscht.");
                    choice = 2;
                    break;
                case 2:
                    break;
            }
        }
    }

    public Book getBook(String isbn){
        return books.get(isbn);
    }

    public boolean exists(String isbn){
        return books.containsKey(isbn);
    }

    public void importBooks(String path) {

        try (BufferedReader reader = new BufferedReader(new FileReader(path))) {
            String line = reader.readLine();
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split(",");
                String isbn = parts[0];
                String title = parts[1];
                List<String> authors = Arrays.asList(parts[2].split(";"));
                int year = Integer.parseInt(parts[3]);
                String city = parts[4];
                String publisher = parts[5];
                int edition = Integer.parseInt(parts[6]);
                Book book = new Book(isbn, title, authors, year, city, publisher, edition);
                books.put(isbn, book);
            }
            System.out.println("Bücher wurden erfolgreich importiert.");

        } catch (FileNotFoundException e) {
            System.out.println("Datei existiert nicht: " + path);
        } catch (IOException e) {
            System.out.println("Fehler beim Importieren von Büchern.");
            throw new RuntimeException(e);
        }

    }

}
class BookCopyManager{
    final Map<Integer, BookCopy> bookCopies = new HashMap<>();

    public void deleteBookCopy(Scanner scanner) {
        System.out.println("==EINE BUCHKOPIE LÖSCHEN==");
        int choice = -1;
        int id = -1;
        while (choice != 2){
            System.out.println("Bitte geben Sie die Buchkopie-ID ein (oder -1 zum Abbrechen):");
            id  = scanner.nextInt();
            scanner.nextLine();
            if(id == -1){
                break;
            }
            if(!bookCopies.containsKey(id)) {
                System.out.println("Es existiert keine Buchkopie mit dieser ID: " + id);
                continue;
            }

            System.out.println("Die Buchkopie mit ID: " + id + " wird gelöscht.");
            System.out.println("1. Bestätigen");
            System.out.println("2. Abbrechen");
            choice = scanner.nextInt();
            scanner.nextLine();

            switch (choice) {
                case 1:
                    bookCopies.remove(id);
                    System.out.println("Die Buchkopie mit ID: " + id + " wurde gelöscht.");
                    choice = 2;
                    break;
                case 2:
                    break;
            }
        }

    }
    public void importBookCopies(String path, BookManager bookManager, CustomerManager customerManager){
        Map<String, Book> books = bookManager.books;

        try (BufferedReader reader = new BufferedReader(new FileReader(path))) {
            String line = reader.readLine();

            while ((line = reader.readLine()) != null) {
                String[] parts = line.split(",");
                int id = Integer.parseInt(parts[0]);
                Book book = books.get(parts[1]);
                String shelfLocation = parts[2];
                String addedToLibraryString = parts[3];
                SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd");
                Date addedToLibrary = formatter.parse(addedToLibraryString);
                boolean lent = parts[4].equalsIgnoreCase("yes");
                Date loanDate = null;

                //Fix Issue 3
                int customerID = -1;
                if(lent){
                    loanDate = formatter.parse(parts[5]);
                    Integer customerId1 = null;
                    if ("yes".equalsIgnoreCase(parts[4].trim())) {
                        String idStr = parts.length > 6 ? parts[6].trim() : "";
                        try {
                            customerId1 = idStr.isEmpty() ? null : Integer.valueOf(idStr);
                        } catch (NumberFormatException ex) {
                            System.err.println("Ungültige Kunden-ID in Zeile: " + line);
                            // ggf. überspringen oder auf null setzen
                            customerId1 = null;
                        }
                    }
                    customerID = customerId1;
                    //end

                    if(!customerManager.exists(customerID)){
                        throw new IllegalArgumentException("Es existiert kein Kunde mit ID " + customerID);
                    }
                }

                LocalDate LocalDate = java.time.LocalDate.now();
                BookCopy bookCopy = new BookCopy(id, book, shelfLocation, addedToLibrary, lent, LocalDate, customerID);
                bookCopies.put(id, bookCopy);
            }

            System.out.println("Buchkopien wurden erfolgreich importiert.");

        } catch (FileNotFoundException e) {
            System.out.println("Datei existiert nicht: " + path);
        } catch (IOException e) {
            System.out.println("Fehler beim Importieren von Buchkopien.");
            throw new RuntimeException(e);
        } catch (ParseException e) {
            throw new RuntimeException(e);
        }

    }
    public void searchBookCopies(Scanner scanner) {
        System.out.println("==EINE BUCHKOPIE SUCHEN==");
        System.out.println("1. ISBN");
        System.out.println("2. Buchtitel");
        System.out.println("3. Autor");
        System.out.println("4. Zurück zum Haupmenü");
        int choice = scanner.nextInt();
        scanner.nextLine();
        List<BookCopy> foundBookCopies = new ArrayList<>();
        switch (choice) {
            case -1:
                break;
            case 1:

                System.out.println("Bitte geben Sie die ISBN der Buchkopie ein:");

                String isbn = scanner.nextLine();

                for (BookCopy bookCopy : bookCopies.values()) {
                    if (bookCopy.getBook().getIsbn().equals(isbn)) {
                        foundBookCopies.add(bookCopy);
                    }
                }
                if(foundBookCopies.isEmpty()) {
                    System.out.println("Es existiert keine Buchkopie mit dieser ISBN: " + isbn);
                }

                break;
            case 2:

                System.out.println("Bitte geben Sie den Buchtitel der Buchkopie ein:");

                String title = scanner.nextLine();
                for (BookCopy bookCopy : bookCopies.values()) {
                    if (bookCopy.getBook().getTitle().equalsIgnoreCase(title)) {
                        foundBookCopies.add(bookCopy);
                    }
                }
                if(foundBookCopies.isEmpty()) {
                    System.out.println("Es existiert keine Buchkopie mit diesem Buchtitel: " + title);
                }
                break;
            case 3:

                System.out.println("Bitte geben Sie den Autor der Buchkopie ein (oder -1 zum Abbrechen):");

                String author = scanner.nextLine();

                for (BookCopy bookCopy : bookCopies.values()) {
                    if(bookCopy.getBook().getAuthors().contains(author)){
                        foundBookCopies.add(bookCopy);
                    }
                }
                if(foundBookCopies.isEmpty()) {
                    System.out.println("Es existiert keine Buchkopie mit diesem Autor: " + author);
                }

                break;
            case 4:
                break;
            default:
                System.out.println("Ungültige Option. Zurück zum Hauptmenu.");
        }
        if(!foundBookCopies.isEmpty()){
            for(BookCopy bookCopy : foundBookCopies){
                System.out.println(bookCopy.toString());
            }
        }else {
            System.out.println("Es wurden keine Buchkopien gefunden.");
        }

    }
    public BookCopy getBookCopy(int id){
        return bookCopies.get(id);
    }

    public boolean exists(int id){
        return bookCopies.containsKey(id);
    }

}
class CustomerManager{
    final Map<Integer, Customer> customers = new HashMap<>();

    public void deleteCustomer(Scanner scanner) {
        System.out.println("==EINEN KUNDEN LÖSCHEN==");
        int choice = -1;
        int id = -1;
        while (choice != 2){
            System.out.println("Bitte geben Sie die ID des Kundes ein (oder -1 zum Abbrechen):");
            id  = scanner.nextInt();
            scanner.nextLine();
            if(id == -1){
                break;
            }
            if(!customers.containsKey(id)) {
                System.out.println("Es existiert kein Kunde mit dieser ID: " + id);
                continue;
            }

            System.out.println("Der Kunde mit ID: " + id + " wird gelöscht.");
            System.out.println("1. Bestätigen");
            System.out.println("2. Abbrechen");
            choice = scanner.nextInt();
            scanner.nextLine();

            switch (choice) {
                case 1:
                    //Fix Issue 5

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
                    //end

                case 2:
                    break;
            }
        }
    }
    public void importCustomers(String path){

        try (BufferedReader reader = new BufferedReader(new FileReader(path))) {
            String line = reader.readLine();
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split(",");
                int id = Integer.parseInt(parts[0]);
                String name = parts[1];
                String firstName = parts[2];
                String address = parts[3];
                String zipCode = parts[4];
                String city = parts[5];
                boolean feesPayed = parts[6].equalsIgnoreCase("yes");
                Customer customer = new Customer(id, name, firstName, address, zipCode, city, feesPayed, new ArrayList<>());
                customers.put(id, customer);
            }
            System.out.println("Benutzer wurden erfolgreich importiert.");
        } catch (FileNotFoundException e) {
            System.out.println("Datei existiert nicht: " + path);
        } catch (IOException e) {
            System.out.println("Fehler beim Importieren von Benutzern.");
            throw new RuntimeException(e);
        }

    }
    public Customer getCustomer(int id){
        return customers.get(id);
    }

    public boolean exists(int id){
        return customers.containsKey(id);
    }
}
class LoanService{
    final BookCopyManager bookCopyManager;
    final CustomerManager customerManager;

    private final Map<Integer, Integer> loans = new HashMap<>();

    public LoanService(BookCopyManager bookCopyManager, CustomerManager customerManager) {
        this.bookCopyManager = bookCopyManager;
        this.customerManager = customerManager;
    }

    public void loanBook(Scanner scanner){
        System.out.println("==AUSLEIHE EINER BUCHKOPIE==");
        int customerID;

        while(true) {
            System.out.println("Bitte geben Sie die Kunden-ID ein (oder -1 zum Abbrechen):");
            customerID = scanner.nextInt();
            scanner.nextLine();

            if(customerID == -1){
                return;
            }

            if(!customerManager.exists(customerID)) {
                System.out.println("Es existiert kein Kunde mit dieser ID: " + customerID);
            }else {
                break;
            }
        }

        int bookCopyID;
        while(true) {
            System.out.println("Bitte geben Sie die Buchkopie-ID ein:");
            bookCopyID = scanner.nextInt();
            scanner.nextLine();
            if(bookCopyID == -1){
                return;
            }

            if(!bookCopyManager.exists(bookCopyID)) {
                System.out.println("Es existiert keine Buchkopie mit dieser ID: " + bookCopyID);
            }else {
                break;
            }
        }
        BookCopy bookCopy = bookCopyManager.bookCopies.get(bookCopyID);
        bookCopy.setLent(true);

        //Fix Issue 4
        LocalDate today = LocalDate.now();
        if (bookCopy.isLent()) {
            System.out.println("Diese Kopie ist bereits verliehen.");
            return;
        }
        bookCopy.setLent(true);
        bookCopy.setLoanDate(today);
        //end

        bookCopy.setCustomerID(customerID);
        Customer customer = customerManager.getCustomer(customerID);
        customer.addBookCopy(bookCopy);
        System.out.println("Die Buchkopie " + bookCopyID + " wurde erfolgreich an den Kunden " + customerID + " ausgeliehen.");
        System.out.println("1. Zurück zum Hauptmenü");
        System.out.println("2. Das Programm beenden");
        int choice = scanner.nextInt();
        scanner.nextLine();
        switch(choice){
            case 1:
                break;
            case 2:
                System.out.println("Das Programm wird beendet");
                System.exit(0);
            default:
                System.out.println("Ungültige Option. Zurück zum Hauptmenu.");

        }
    }
    public void returnBook(Scanner scanner){
        System.out.println("==RÜCKGABE EINER BUCHKOPIE==");
        int customerID;

        while(true) {
            System.out.println("Bitte geben Sie die Kunden-ID ein (oder -1 zum Abbrechen):");
            customerID = scanner.nextInt();
            scanner.nextLine();

            if(customerID == -1){
                return;
            }

            if(!customerManager.exists(customerID)) {
                System.out.println("Es existiert kein Kunde mit dieser ID: " + customerID);
            }else if(customerManager.getCustomer(customerID).getBookCopies().isEmpty()) {
                System.out.println("Der Kunde hat keine ausgeliehenen Buchkopien");
            }else{
                break;
            }
        }

        int bookCopyID;
        while(true) {
            System.out.println("Bitte geben Sie die Buchkopie-ID ein:");
            bookCopyID = scanner.nextInt();
            scanner.nextLine();
            if(bookCopyID == -1){
                return;
            }

            if(!bookCopyManager.exists(bookCopyID)) {
                System.out.println("Es existiert keine Buchkopie mit dieser ID: " + bookCopyID);
            }else {
                break;
            }
        }
        BookCopy bookCopy = bookCopyManager.bookCopies.get(bookCopyID);
        bookCopy.setLent(false);
        bookCopy.setLoanDate(null);
        bookCopy.setCustomerID(-1);
        Customer customer = customerManager.getCustomer(customerID);
        customer.removeBookCopy(bookCopy);
        System.out.println("Die Buchkopie " + bookCopyID + " wurde erfolgreich vom Kunden " + customerID + " zurückgegeben.");
        System.out.println("1. Zurück zum Hauptmenü");
        System.out.println("2. Das Programm beenden");
        int choice = scanner.nextInt();
        scanner.nextLine();
        switch(choice){
            case 1:
                break;
            case 2:
                System.out.println("Das Programm wird beendet");
                System.exit(0);
            default:
                System.out.println("Ungültige Option. Zurück zum Hauptmenu.");

        }
    }
}
class ReportService{
    final BookManager bookManager;
    final BookCopyManager bookCopyManager;
    final CustomerManager customerManager;
    public ReportService(BookManager bookManager,BookCopyManager bookCopyManager, CustomerManager customerManager) {
        this.bookManager = bookManager;
        this.bookCopyManager = bookCopyManager;
        this.customerManager = customerManager;
    }
    public void createReport(Scanner scanner){
        int choice = -1;
        while (choice != 7) {
            System.out.println("==BERICHT ERSTELLEN==");

            System.out.println("1. Ausgabe aller Bücher");
            System.out.println("2. Ausgabe aller ausgeliehenen Buchkopien");
            System.out.println("3. Ausgabe aller nicht ausgeliehenen Buchkopien");
            System.out.println("4. Ausgabe aller Kunden");
            System.out.println("5. Ausgabe aller derzeit ausgeliehenen Buchkopien eines Kunden via Kunden-ID");
            System.out.println("6. Ausgabe der Anzahl an Buchkopien pro Verlag");
            System.out.println("7. Zurück zum Hauptmenü");
            System.out.println("8. Programm beenden");

            choice = scanner.nextInt();
            scanner.nextLine();

            switch (choice) {
                case 1:
                    printAllBooks();
                    break;
                case 2:
                    printLentBookCopies();
                    break;
                case 3:
                    printAvailableBookCopies();
                    break;
                case 4:
                    printAllCustomers();
                    break;
                case 5:
                    printLentBookCopiesOfCustomer(scanner);
                    break;
                case 6:
                    printBookCopiesOfPublisher();
                    break;
                case 7:
                    break;
                case 8:
                    System.out.println("Das Programm wird beendet.");
                    System.exit(0);
                default:
                    System.out.println("Ungültige Eingabe.");
            }
        }
    }
    public void printAllBooks(){
        for(Book book : bookManager.books.values()){
            System.out.println(book);
        }
    }
    public void printLentBookCopies(){
        List<BookCopy> lentBookCopies = new ArrayList<>();

        for(BookCopy bookCopy : bookCopyManager.bookCopies.values()){
            if(bookCopy.isLent()){
                lentBookCopies.add(bookCopy);
            }
        }
        for (BookCopy bookCopy : lentBookCopies) {
            System.out.println(bookCopy);
        }
    }
    public void printAvailableBookCopies(){
        List<BookCopy> availableBookCopies = new ArrayList<>();
        for(BookCopy bookCopy : bookCopyManager.bookCopies.values()){
            if(!bookCopy.isLent()){
                availableBookCopies.add(bookCopy);
            }
        }
        for (BookCopy bookCopy : availableBookCopies) {
            System.out.println(bookCopy);
        }
    }
    public void printAllCustomers(){
        for(Customer customer: customerManager.customers.values()){
            System.out.println(customer);
        }
    }
    public void printLentBookCopiesOfCustomer(Scanner scanner){


        while (true) {

            int customerID = -1;

            //Fix Issue 6

            System.out.print("Bitte geben Sie die Kunden-ID ein (oder -1 zum Abbrechen): ");
            try {
                customerID = Integer.parseInt(scanner.nextLine().trim());
                break;
            } catch (NumberFormatException e) {
                System.out.println("Ungültige Eingabe! Bitte eine ganze Zahl eingeben.");
            }

            customerID = scanner.nextInt();
            scanner.nextLine();

            //end

            if(customerID == -1){
                return;
            }

            if(!customerManager.exists(customerID)) {
                System.out.println("Es existiert kein Kunde mit dieser ID: " + customerID);
            }
            else if(customerManager.getCustomer(customerID).getBookCopies().isEmpty()) {
                System.out.println("Der Kunde hat keine ausgeliehenen Buchkopien");
            }
            else{
                break;
            }
            for(BookCopy bookCopy: customerManager.getCustomer(customerID).getBookCopies()){
                System.out.println(bookCopy);
            }

        }
    }
    public void printBookCopiesOfPublisher(){
        Map<Integer, BookCopy> bookCopies = bookCopyManager.bookCopies;
        Map<String, Integer> publisherBookCopies = new TreeMap<>();



        for(Book book : bookManager.books.values()){
            String publisher = book.getPublisher();
            publisherBookCopies.putIfAbsent(publisher, 0);
        }

        for(BookCopy bookCopy : bookCopies.values()){
            String publisher = bookCopy.getBook().getPublisher();
            publisherBookCopies.merge(publisher, 1, Integer::sum);
        }
        if (publisherBookCopies.isEmpty()) {
            System.out.println("Keine Buchkopien oder Verlage vorhanden.");
            return;
        }
        int numberOfBookCopies = bookCopies.size();
        String report;
        for(String publisher : publisherBookCopies.keySet()){
            if(publisherBookCopies.get(publisher) == 0){
                report = String.format("%s: %d Buchkopien (%.1f%%)", publisher, publisherBookCopies.get(publisher), 0.0);
                System.out.println(report);
            }
            else if(publisherBookCopies.get(publisher) > 0){
                double percent = (double) publisherBookCopies.get(publisher)/ numberOfBookCopies * 100;
                report = String.format("%s: %d Buchkopien (%.1f%%)", publisher, publisherBookCopies.get(publisher), percent);
                System.out.println(report);
            }
        }

    }
}
class Book {
    private String isbn;
    private String title;
    private List<String> authors;
    private int year;
    private String city;
    private String publisher;
    private int edition;

    public Book(String isbn, String title, List<String> authors, int year,
                String city, String publisher, int edition) {
        this.isbn = isbn;
        this.title = title;
        this.authors = authors;
        this.year = year;
        this.city = city;
        this.publisher = publisher;
        this.edition = edition;
    }

    public String getIsbn() {
        return isbn;
    }

    public String getTitle() {
        return title;
    }

    public List<String> getAuthors() {
        return authors;
    }

    public int getYear() {
        return year;
    }

    public String getCity() {
        return city;
    }

    public String getPublisher() {
        return publisher;
    }

    public int getEdition() {
        return edition;
    }

    @Override
    public String toString() {

        //Fix Issue 8
        return String.format("Titel: %s | Autor: %s | Year: %d | isbn: %s", title, authors, year, isbn);
        //end
    }
}
class BookCopy {

    private long id;
    private Book book;
    private String shelfLocation;
    private Date addedToLibrary;
    private boolean lent;
    private LocalDate loanDate;
    private int customerID;

    public BookCopy(long id, Book book, String shelfLocation, Date addedToLibrary, boolean lent, LocalDate loanDate, int customerID) {
        this.id = id;
        this.book = book;
        this.shelfLocation = shelfLocation;
        this.addedToLibrary = addedToLibrary;
        this.lent = lent;
        this.loanDate = loanDate;
        this.customerID = customerID;
    }

    public void setLent(boolean lent) {
        this.lent = lent;
    }

    public void setLoanDate(LocalDate loanDate) {
        this.loanDate = loanDate;
    }

    public void setCustomerID(int customerID){
        this.customerID = customerID;
    }

    public long getId() {
        return id;
    }

    public Book getBook() {
        return book;
    }

    public String getShelfLocation() {
        return shelfLocation;
    }

    public Date getAddedToLibrary() {
        return addedToLibrary;
    }

    public boolean isLent() {
        return lent;
    }

    public LocalDate getLoanDate() {
        return loanDate;
    }

    public int getCustomerID(){return customerID;}

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
class Customer {
    private long id;
    private String name;
    private String firstName;
    private String address;
    private String zipCode;
    private String city;
    private boolean feesPayed;
    private List<BookCopy> bookCopies;

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

    public void setFeesPayed(boolean feesPayed) {
        this.feesPayed = feesPayed;
    }

    public void addBookCopy(BookCopy bookCopy) {
        bookCopies.add(bookCopy);
    }
    public void removeBookCopy(BookCopy bookCopy){
        bookCopies.remove(bookCopy);
    }

    public long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getFirstName() {
        return firstName;
    }

    public String getAddress() {
        return address;
    }

    public String getZipCode() {
        return zipCode;
    }

    public String getCity() {
        return city;
    }

    public boolean isFeesPayed() {
        return feesPayed;
    }

    public List<BookCopy> getBookCopies() {
        return bookCopies;
    }

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