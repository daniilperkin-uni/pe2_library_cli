# AGENTS.md — pe2_library_cli

Guidance for AI agents (and humans) working on this repository.

## What this project is

A small Java 21 command-line library-management application, refactored from a
single 985-line `LibraryCLI.java` monolith into a layered
`model / manager / service / csv / cli` package structure.

## Build commands

```bash
# full clean + test (the canonical verification command)
./mvnw clean test

# build the JAR
./mvnw clean package

# run the app
java -cp target/classes com.example.library.cli.LibraryCLI
```

The Maven Wrapper (`mvnw` / `mvnw.cmd`) is committed; it downloads Maven
3.9.9 automatically. Java 21 is required (matches `.github/workflows/ci.yml`).

CI: `.github/workflows/ci.yml` runs `mvn clean install` on JDK 21 (Temurin)
for pushes/PRs to `main` and `uebungsblatt3`.

## Architecture rules

1. **Layering.** `cli` → `service` → `manager` → `model`. The CLI layer owns
   all `Scanner` / `System.out` I/O. Service classes contain **only** pure
   business logic — no `System.out`, no `Scanner`, no `System.exit`.
2. **Errors as exceptions.** Services throw (`IllegalArgumentException` for
   missing entities, `IllegalStateException` for invalid state) instead of
   printing and returning silently. The CLI catches and prints them.
3. **Classpath resources.** CSV fixtures live in `src/main/resources/` and are
   loaded with `getClass().getResourceAsStream("/<file>.csv")` — never from
   hardcoded `src/` filesystem paths.
4. **UTF-8 everywhere.** All source, resources, and the Maven
   `project.build.sourceEncoding` are UTF-8 (no BOM). German filenames like
   `bücher.csv` are intentional.
5. **No PII.** `benutzer.csv` must contain only synthetic placeholder data
   (Mustermann/Schmidt, Teststraße/Beispielweg). Never commit real personal
   data.

## Known-fixed bugs (do not regress)

- **BUG 1 — loanBook guard order.** `isLent()` must be checked **before**
  `setLent(true)`. The original mutated first, so the guard never fired.
  Fixed in `LoanService.loan`.
- **BUG 2 — returnBook authorization.** `returnBook` must validate
  `customer.getBookCopies().contains(bookCopy)` **before** clearing loan
  state. The original cleared state unconditionally and removed the copy from
  whichever customer was passed in. Fixed in `LoanService.returnBook`.
- **BUG 3 — printLentBookCopiesOfCustomer double parse.** The original parsed
  the customer ID twice (`nextLine()` then `nextInt()`), placed the render
  loop outside the validation branch, and dereferenced the customer without a
  null check. The refactored `ReportService.getLentBookCopiesOfCustomer`
  accepts an already-parsed ID and rejects unknown customers with an
  exception before iterating.
- **BUG 4 — CSV parser.** The original used `String.split(",")` which broke on
  quoted fields containing commas and escaped quotes. `CsvImporter` is a
  proper RFC-4180-subset parser (UTF-8, quoted fields, `""` escapes).
- **BUG 6 — hardcoded CSV paths.** Replaced `src/resources/*.csv` filesystem
  paths with classpath `getResourceAsStream` loads (see rule 3).
- **BUG 7 — uncaught InputMismatchException.** All submenus now read through
  the CLI's `readInt` helper, which catches non-numeric input and re-prompts
  instead of crashing.

## Test conventions

- JUnit 5 (`org.junit.jupiter`) + AssertJ (`assertThat`).
- Use `assertThatThrownBy(...).isInstanceOf(...)` for error paths.
- The old `LibraryTests.java` had **wrong assertions** (e.g. asserting
  `customerID == -1` after a successful loan, or `contains(copy) == false`).
  The current tests assert the correct post-conditions — do not "fix" them
  back.
- `deleteBook` / `deleteBookCopy` / `deleteCustomer` are interactive; tests
  drive them by constructing a `Scanner` over a canned input string.

## File map (post-refactor)

| Path | Purpose |
|------|---------|
| `src/main/java/.../cli/LibraryCLI.java` | thin controller + `main` |
| `src/main/java/.../service/LoanService.java` | loan / return logic |
| `src/main/java/.../service/ReportService.java` | report logic |
| `src/main/java/.../manager/*.java` | CRUD + CSV import per entity |
| `src/main/java/.../model/*.java` | Book, BookCopy, Customer |
| `src/main/java/.../csv/CsvImporter.java` | CSV parser |
| `src/main/resources/*.csv` | sanitized fixtures |
| `src/test/java/.../*.java` | JUnit 5 + AssertJ tests |
