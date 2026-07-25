# LibraryCLI (team92)

**Team Members:**
- Daniil Perkin (st194422@stud.uni-stuttgart.de)
- Vladyslav Handzha (st192994@stud.uni-stuttgart.de)
- Boyang Wang (st182103@stud.uni-stuttgart.de)

---

LibraryCLI is a command-line application for managing a small library system. It provides functionality to import, list, search and delete books and book copies, manage customers, perform loan and return operations, and generate various reports.

## Requirements

Java version 21 or later and Maven version 3.x or later are required.

## Installation

Clone the repository:

```bash
git clone <repository-url>
```

Change into the project directory and build with Maven:

```bash
cd team92
mvn clean install
```

After a successful build, the application JAR file will be generated in the target directory under the name `team92-1.0-SNAPSHOT.jar`.

## Usage

Start the application by running the JAR file:

```bash
java -jar target/team92-1.0-SNAPSHOT.jar
```

Upon launch, a text-based menu is displayed. Select options by entering the corresponding number and pressing Enter.

### Menu Options:
1. Bücher verwalten
2. Buchkopien verwalten
3. Kunden verwalten
4. Eine Buchkopie ausleihen
5. Eine Buchkopie zurückgeben
6. Eine Buchkopie suchen
7. Bericht erstellen
8. Programm beenden

## Command-Line Interface Details

The application consists of the following modules:

- **BookManager**: Import books from `resources/bücher.csv`. Delete books by ISBN. Lookup a book by ISBN.
- **BookCopyManager**: Import book copies from `resources/buchkopien.csv`. Delete book copies by ID. Search copies by ISBN, title, or author.
- **CustomerManager**: Import customers from `resources/benutzer.csv`. Delete customers by ID.
- **LoanService**: Loan a book copy to a customer with input validation. Return a book copy with validation.
- **ReportService**: Generate reports listing all books, all lent book copies, all available book copies, all customers, and all copies lent to a specific customer.

## Tests

Automated tests are provided using JUnit 4, JUnit 5, and TestNG. Execute all tests with Maven:

```bash
mvn test
```

## Code Coverage

Current coverage metrics are approximately 95 percent of classes and 80 percent of lines. To generate detailed coverage reports, add a coverage plugin such as JaCoCo to the Maven configuration.
