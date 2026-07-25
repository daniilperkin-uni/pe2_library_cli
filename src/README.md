LibraryCLI

LibraryCLI is a command-line application for managing a small library system. It provides functionality to import, list, search and delete books and book copies, manage customers, perform loan and return operations, and generate various reports.

Requirements

Java version 21 or later and Maven version 3.x or later are required.

Installation

Clone the repository:

git clone <repository-url>

Change into the project directory and build with Maven:

cd pe2_library_cli
mvn clean install

After a successful build, the application JAR file will be generated in the target directory under the name library-cli-1.0-SNAPSHOT.jar.

Project Configuration

All build and dependency settings are defined in the pom.xml file. The project uses the Maven Surefire Plugin to execute tests. An excerpt from pom.xml:

<project xmlns="http://maven.apache.org/POM/4.0.0" ...>
  <modelVersion>4.0.0</modelVersion>
  <groupId>com.example</groupId>
  <artifactId>pe2_library_cli</artifactId>
  <version>1.0-SNAPSHOT</version>

  <dependencies>
    <dependency>
      <groupId>junit</groupId>
      <artifactId>junit</artifactId>
      <version>4.13.2</version>
      <scope>test</scope>
    </dependency>
    <dependency>
      <groupId>org.junit.jupiter</groupId>
      <artifactId>junit-jupiter</artifactId>
      <version>5.8.1</version>
      <scope>test</scope>
    </dependency>
    <dependency>
      <groupId>org.testng</groupId>
      <artifactId>testng</artifactId>
      <version>7.9.0</version>
      <scope>test</scope>
    </dependency>
  </dependencies>

  <build>
    <plugins>
      <plugin>
        <groupId>org.apache.maven.plugins</groupId>
        <artifactId>maven-surefire-plugin</artifactId>
        <version>3.1.2</version>
      </plugin>
    </plugins>
  </build>
</project>

Usage

Start the application by running the JAR file:

java -jar target/library-cli-1.0-SNAPSHOT.jar

Upon launch, a text-based menu is displayed. Select options by entering the corresponding number and pressing Enter.

Menu Options:

Bücher verwalten

Buchkopien verwalten

Kunden verwalten

Eine Buchkopie ausleihen

Eine Buchkopie zurückgeben

Eine Buchkopie suchen

Bericht erstellen

Programm beenden

Command-Line Interface Details

The application consists of the following modules:

BookManager

Import books from resources/bücher.csv.

Delete books by ISBN.

Lookup a book by ISBN.

BookCopyManager

Import book copies from resources/buchkopien.csv.

Delete book copies by ID.

Search copies by ISBN, title, or author.

CustomerManager

Import customers from resources/benutzer.csv.

Delete customers by ID.

LoanService

Loan a book copy to a customer with input validation.

Return a book copy with validation.

ReportService

Generate reports listing all books.

Generate reports listing all lent book copies.

Generate reports listing all available book copies.

Generate reports listing all customers.

Generate reports listing all copies lent to a specific customer.

Tests

Automated tests are provided using JUnit 4, JUnit 5, and TestNG. Execute all tests with Maven:

mvn test

Code Coverage

Current coverage metrics are approximately 95 percent of classes and 80 percent of lines. To generate detailed coverage reports, add a coverage plugin such as JaCoCo to the Maven configuration.

Project Structure

pe2_library_cli/

.github/workflows/ci.yml (CI pipeline configuration)

src/ (Java source files and CSV data files in the resources directory)

target/ (compiled artifacts and reports)

pom.xml (project build and dependency configuration)

README.md (this document)

Dependencies

The project depends on the following libraries for testing:

junit version 4.13.2

junit-jupiter version 5.8.1

testng version 7.9.0

The Maven Surefire Plugin version 3.1.2 is used for test execution.

Contributing

Contributions are welcome. Suggested process:

Fork the repository.

Create a new branch for your feature.

Commit and push your changes.

Open a pull request describing the changes.

Authors

- [Author 1]
- [Author 2]
- [Author 3]

