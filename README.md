# pe2_library_cli

**Author:** Daniil Perkin

---

pe2_library_cli is a command-line application for managing a small library
system. It supports importing, listing, searching and deleting books and book
copies, managing customers, performing loan and return operations, and
generating various reports.

## Requirements

- Java 21 or later
- Maven 3.9+ (the included Maven Wrapper (`./mvnw`) downloads it automatically)

## Project structure

The codebase is split into focused packages under
`src/main/java/com/example/library/`:

```
src/
├── main/
│   ├── java/com/example/library/
│   │   ├── cli/         # LibraryCLI — thin menu loop + Scanner I/O
│   │   ├── csv/         # CsvImporter — quoted-field-aware UTF-8 CSV parser
│   │   ├── manager/     # BookManager, BookCopyManager, CustomerManager
│   │   ├── model/       # Book, BookCopy, Customer
│   │   └── service/     # LoanService, ReportService (pure business logic)
│   └── resources/       # bücher.csv, buchkopien.csv, benutzer.csv (classpath)
└── test/java/com/example/library/
    ├── csv/             # CsvImporterTest
    ├── manager/         # BookManagerTest, BookCopyManagerTest, CustomerManagerTest
    ├── model/           # BookTest, BookCopyTest, CustomerTest
    └── service/         # LoanServiceTest, ReportServiceTest
```

CSV fixtures live in `src/main/resources/` and are loaded from the classpath
via `getResourceAsStream`, so the application runs identically from the
unpacked classes directory or a packaged JAR. The `benutzer.csv` file contains
only synthetic placeholder data (no real PII).

## Build & test

```bash
./mvnw clean test
```

To build the JAR without running tests:

```bash
./mvnw clean package
```

The runnable JAR is produced in `target/`.

## Usage

Run the application:

```bash
java -cp target/classes com.example.library.cli.LibraryCLI
```

A text-based menu is displayed. Select options by entering the corresponding
number and pressing Enter.

### Menu options

1. Bücher verwalten — manage books (delete by ISBN)
2. Buchkopien verwalten — manage book copies (delete by ID)
3. Kunden verwalten — manage customers (delete by ID)
4. Eine Buchkopie ausleihen — loan a book copy to a customer
5. Eine Buchkopie zurückgeben — return a book copy
6. Eine Buchkopie suchen — search book copies by ISBN, title, or author
7. Bericht erstellen — generate reports
8. Programm beenden — exit

## Modules

- **BookManager** — import books from `bücher.csv`; delete/lookup books by ISBN.
- **BookCopyManager** — import book copies from `buchkopien.csv`; delete/search
  copies by ISBN, title, or author.
- **CustomerManager** — import customers from `benutzer.csv`; delete/lookup
  customers by ID.
- **LoanService** — pure business logic for loaning and returning book copies;
  throws on invalid operations (no console I/O).
- **ReportService** — pure business logic that returns report lines as lists
  for the CLI to print (no console I/O).

## Tests

Automated tests use **JUnit 5** (Jupiter) and AssertJ. Run them with:

```bash
./mvnw clean test
```
