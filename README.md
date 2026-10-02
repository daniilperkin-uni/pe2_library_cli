# pe2_library_cli

[![CI](https://github.com/daniilperkin-uni/pe2_library_cli/actions/workflows/ci.yml/badge.svg?branch=master)](https://github.com/daniilperkin-uni/pe2_library_cli/actions/workflows/ci.yml)

**Author:** Daniil Perkin

---

pe2_library_cli is a command-line application for managing a small library
system. It supports creating, importing, listing, searching and deleting books
and book copies, managing customers, performing loan and return operations,
and generating various reports.

Features:

- Creating books, book copies and customers directly in the menus
- CSV import of books, copies and customers (quoted-field aware, UTF-8)
- Loans, returns and late-return fines
- Reservations (Vormerkungen) with a per-title (ISBN) queue
- Reports on the current stock and loans
- **State is saved on exit** to a data directory (default `./data`) in the
  same CSV format and reloaded on the next start — books, customers, book
  copies **and the reservation queue** (`reservations.csv`)

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
│   │   ├── csv/         # CsvImporter (parser), CsvExporter (save on exit)
│   │   ├── manager/     # BookManager, BookCopyManager, CustomerManager,
│   │   │                # ReservationManager
│   │   ├── model/       # Book, BookCopy, Customer, Reservation
│   │   └── service/     # LoanService, ReportService (pure business logic)
│   └── resources/       # bücher.csv, buchkopien.csv, benutzer.csv (classpath)
└── test/java/com/example/library/
    ├── cli/             # LibraryCLITest (scripted Scanner)
    ├── csv/             # CsvImporterTest, CsvExporterTest
    ├── manager/         # BookManagerTest, BookCopyManagerTest,
    │                    # CustomerManagerTest, ReservationManagerTest
    ├── model/           # BookTest, BookCopyTest, CustomerTest
    └── service/         # LoanServiceTest, LoanServiceFineTest, ReportServiceTest
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
./mvnw clean package -DskipTests
```

The runnable JAR is produced in `target/`.

## Usage

Run the application:

```bash
./mvnw clean package
java -jar target/pe2-library-cli-1.0-SNAPSHOT.jar [data-dir]
```

`data-dir` defaults to `./data`. If it already contains saved CSV files they
are loaded; otherwise the bundled fixtures are used. On exit the current state
is written back there as `bücher.csv`, `benutzer.csv`, `buchkopien.csv` and
`reservations.csv`. Deletion is guarded: a customer with open loans, a lent
book copy and a book that still has copies cannot be deleted, and the app
refuses to start on saved data that cannot be loaded (clear message, exit
code 1) instead of crashing.

Sample session:

```
==BIBLIOTHEKVERWALTUNGSSYSTEM==
...
8. Bericht erstellen
9. Programm beenden
Wählen Sie eine Option: 9
Das Programm wird beendet.
Daten wurden gespeichert in: /home/me/data
```

A text-based menu is displayed. Select options by entering the corresponding
number and pressing Enter.

### Menu options

1. Bücher verwalten — manage books (create / delete by ISBN)
2. Buchkopien verwalten — manage book copies (create / delete by ID)
3. Kunden verwalten — manage customers (create / delete by ID)
4. Eine Buchkopie ausleihen — loan a book copy to a customer
5. Eine Buchkopie zurückgeben — return a book copy
6. Eine Buchkopie suchen — search book copies by ISBN, title, or author
7. Vormerkungen verwalten — manage reservations
8. Bericht erstellen — generate reports
9. Programm beenden — save and exit

## Modules

- **BookManager** — import books from `bücher.csv`; create/delete/lookup
  books by ISBN.
- **BookCopyManager** — import book copies from `buchkopien.csv`;
  create/delete/search copies by ISBN, title, or author.
- **CustomerManager** — import customers from `benutzer.csv`;
  create/delete/lookup customers by ID.
- **LoanService** — pure business logic for loaning and returning book copies;
  throws on invalid operations (no console I/O).
- **ReservationManager** — per-title (ISBN-keyed) reservation queues;
  `importReservations` restores the queue saved on the previous run.
- **CsvExporter** — writes state back to CSV on exit.
- **ReportService** — pure business logic that returns report lines as lists
  for the CLI to print (no console I/O).

## Tests

Automated tests use **JUnit 5** (Jupiter) and AssertJ. Run them with:

```bash
./mvnw clean test
```
