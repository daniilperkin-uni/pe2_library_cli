# AGENTS.md — pe2_library_cli

Guidance for AI agents (and humans) working on this repository.

## What this project is

A small Java 21 command-line library-management application, refactored from a
single 985-line `LibraryCLI.java` monolith into a layered
`model / manager / service / csv / cli` package structure. It also has
reservations, late-return fines and save-on-exit to a data dir (`./data`,
including the reservation queue in `reservations.csv`).

## Build commands

```bash
# full clean + test + coverage gate (the canonical verification command)
./mvnw clean verify
# NOTE: `clean test` skips the JaCoCo 0.70 coverage check - it only runs at verify.

# build the JAR
./mvnw clean package

# run the app
java -cp target/classes com.example.library.cli.LibraryCLI
```

The Maven Wrapper (`mvnw` / `mvnw.cmd`) is committed; it downloads Maven
3.10.0 automatically. Java 21 is required (matches `.github/workflows/ci.yml`).

CI: `.github/workflows/ci.yml` runs `./mvnw -B verify` on JDK 21 (Temurin)
for pushes/PRs to `master` (plus `merge_group` and manual
`workflow_dispatch`).

## Architecture rules

1. **Layering.** `cli` → {`service`, `csv`} → `manager` → `model`. The managers
   call `csv.CsvImporter` to read their fixtures, and `LibraryCLI` uses
   `CsvExporter` directly to save the state - so `csv` sits beside `service`,
   not under `manager`. The CLI layer owns
   all `Scanner` / `System.out` I/O. Service classes contain **only** pure
   business logic — no `System.out`, no `Scanner`, no `System.exit`.
   The manager layer holds **no** `Scanner` at all: the interactive
   delete/search flows were replaced by pure `deletionBlocker(...)` +
   `remove...()` pairs and `findByIsbn/Title/Author`, and the CLI drives the
   prompts. Remaining manager-side printing is the CSV import feedback
   ("… wurden erfolgreich importiert."), which is still on the to-do list.
2. **Errors as exceptions.** Services throw (`IllegalArgumentException` for
   missing entities, `IllegalStateException` for invalid state) instead of
   printing and returning silently. The CLI catches and prints them.
3. **CSV sources.** Fixtures live in `src/main/resources/` and are loaded with
   `getClass().getResourceAsStream("/<file>.csv")` — never from hardcoded `src/`
   filesystem paths. Saved state is the one exception: `LibraryCLI.loadState`
   reads `./data/*.csv` from the data dir and only falls back to the classpath
   fixtures on a first start.
4. **UTF-8 everywhere.** All source, resources, and the Maven
   `project.build.sourceEncoding` are UTF-8 (no BOM). German filenames like
   `bücher.csv` are intentional.
5. **No PII.** `benutzer.csv` must contain only synthetic placeholder data
   (Mustermann/Schmidt, Teststraße/Beispielweg). Never commit real personal
   data.

## Known-fixed bugs (do not regress)

*(Numbering follows the original audit; the list is not contiguous.)*

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
- **Deletion guards.** A customer with outstanding loans, a lent book copy,
  and a book that is still referenced by book copies must not be deletable
  from the menus — otherwise the saved state no longer reloads.
- **Startup load guard.** `LibraryCLI.main` aborts with a clear message and
  exit code 1 when the saved data cannot be loaded, instead of crashing with
  a stack trace.

## Test conventions

- JUnit 5 (`org.junit.jupiter`) + AssertJ (`assertThat`).
- Use `assertThatThrownBy(...).isInstanceOf(...)` for error paths.
- The old `LibraryTests.java` had **wrong assertions** (e.g. asserting
  `customerID == -1` after a successful loan, or `contains(copy) == false`).
  The current tests assert the correct post-conditions — do not "fix" them
  back.
- `deleteBook` / `deleteBookCopy` / `deleteCustomer` / `searchBookCopies` live
  in `LibraryCLI` now; `LibraryCLITest` drives them end-to-end by constructing a
  `Scanner` over a canned input string. The managers only answer
  `deletionBlocker(...)` and the `findBy...` lookups.

## File map (post-refactor)

| Path | Purpose |
|------|---------|
| `src/main/java/.../cli/LibraryCLI.java` | thin controller + `main` |
| `src/main/java/.../service/LoanService.java` | loan / return logic |
| `src/main/java/.../service/ReportService.java` | report logic |
| `src/main/java/.../manager/*.java` | CRUD + CSV import per entity |
| `src/main/java/.../model/*.java` | Book, BookCopy, Customer, Reservation |
| `src/main/java/.../csv/CsvImporter.java` | CSV parser |
| `src/main/java/.../csv/CsvExporter.java` | save state on exit |
| `src/main/resources/*.csv` | sanitized fixtures |
| `data/{bücher,benutzer,buchkopien,reservations}.csv` | state written on exit and reloaded on the next start |
| `src/test/java/.../*.java` | JUnit 5 + AssertJ tests |
