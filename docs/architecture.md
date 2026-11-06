# Architecture

## Design reference

Use the [schema and its foreign keys](data-model.md#schema-first-design-reference)
as the starting point for stored fields and relationships. Models, repository
mapping, service workflows, and GUI record sections build on that definition.
The GUI presents readable relationships through Library rather than running SQL.

## Component boundaries

```mermaid
flowchart TB
    Main --> LibraryApp
    Main --> SampleData
    Main --> JdbcSettings
    Main --> JDBC[JdbcLibraryRepository]
    LibraryApp --> UI[LibraryWindow and workspace components]
    UI --> Library
    UI --> Rows[LibraryRecords / LibrarySection / RecordFilter]
    UI --> CSV[CsvExport]
    Rows --> Library
    Library --> Snapshot[LibrarySnapshot / LibrarySummary]
    Library --> Contract[LibraryRepository]
    Contract --> Memory[InMemoryLibraryRepository]
    Contract --> JDBC
    JDBC --> DB[(MySQL)]
    Library --> Model[Book / BookCopy / Member / Loan]
    Snapshot --> Model
    SampleData --> Contract
```

Main coordinates startup only. A repository instance is selected once and passed
to Library. Temporary mode uses an empty or example-populated memory repository;
MySQL mode uses JDBC and explicitly initializes missing tables before the GUI starts.

## UML class diagrams

The diagrams show the main classes with selected fields and methods. Types without
package labels are project classes; the desktop view identifies its Swing base classes.
`<<record>>` marks Java records and `<<interface>>` marks the repository contract.

| Notation | Meaning |
| --- | --- |
| `A --> B` | A holds or navigates a reference to B |
| `A ..> B` | A uses, creates, or returns B without a retained field |
| `A <|-- B` | B extends A |
| `A <|.. B` | B implements interface A |
| `1`, `0..*` | One associated object, or zero or more associated objects |
| `+`, `-` | Public member, private member |

### Domain classes

These classes belong to `com.daniel.library.model`. A copy references one title;
each loan references one copy and one member. A copy can have many historical loans,
with at most one active loan. Associations represent shared objects, so changing a
title or member's details is visible through related copies and loans.

```mermaid
classDiagram
    direction LR
    class Book {
        -String isbn
        -String title
        -String author
        -boolean archived
        +updateDetails(String title, String author) void
        +setArchived(boolean archived) void
    }
    class BookCopy {
        <<record>>
        -String copyId
        -Book book
        +copyId() String
        +book() Book
    }
    class Member {
        -String memberId
        -String name
        -String email
        +updateDetails(String name, String email) void
    }
    class Loan {
        -String loanId
        -BookCopy copy
        -Member member
        -LocalDate checkoutDate
        -LocalDate dueDate
        -LocalDate returnDate
        +isActive() boolean
        +isOverdue(LocalDate asOfDate) boolean
        +calculateFine(LocalDate asOfDate, BigDecimal dailyRate) BigDecimal
        +markReturned(LocalDate date) void
    }
    BookCopy "0..*" --> "1" Book : book
    Loan "0..*" --> "1" BookCopy : copy
    Loan "0..*" --> "1" Member : member
```

### Service and storage classes

Library, LibraryRepository, LibraryData, LibrarySnapshot, and LibrarySummary belong
to `service`. InMemoryLibraryRepository, JdbcLibraryRepository, and JdbcSettings
belong to `persistence`. The implementations realize the same interface; they do
not inherit from each other. Library's convenience constructors create memory
storage, while its repository-taking constructors accept either implementation.

```mermaid
classDiagram
    class Library {
        -LibraryRepository repository
        -Clock clock
        -BigDecimal dailyFineRate
        +checkoutBook(String copyId, String memberId) Loan
        +returnBook(String copyId) Loan
        +getSnapshot() LibrarySnapshot
        +getSummary() LibrarySummary
    }
    class LibraryRepository {
        <<interface>>
        +load() LibraryData
        +addBook(Book book) void
        +updateBook(Book book) void
        +setBookArchived(String isbn, boolean archived) void
        +addCopy(BookCopy copy) void
        +addMember(Member member) void
        +updateMember(Member member) void
        +addLoan(Loan loan) void
        +returnLoan(Loan loan, LocalDate returnDate) void
    }
    class InMemoryLibraryRepository {
        -List~Book~ books
        -List~BookCopy~ copies
        -List~Member~ members
        -List~Loan~ loans
    }
    class JdbcLibraryRepository {
        -JdbcSettings settings
        +initializeSchema() void
    }
    class JdbcSettings {
        <<record>>
        -String url
        -String username
        -String password
        +openConnection() Connection
    }
    class LibraryData {
        <<record>>
        -List~Book~ books
        -List~BookCopy~ copies
        -List~Member~ members
        -List~Loan~ loans
    }
    class LibrarySnapshot {
        <<record>>
        -LibraryData records
        -LocalDate asOfDate
        -BigDecimal dailyFineRate
        +isAvailable(BookCopy copy) boolean
        +isOverdue(Loan loan) boolean
        +calculateFine(Loan loan) BigDecimal
        +summary() LibrarySummary
    }
    class LibrarySummary {
        <<record>>
        -LocalDate asOfDate
        -int currentLoans
        -int overdueLoans
        -int returnedLoans
        -BigDecimal currentLateCharges
        -BigDecimal returnedLateCharges
    }
    Library --> "1" LibraryRepository : repository
    LibraryRepository <|.. InMemoryLibraryRepository
    LibraryRepository <|.. JdbcLibraryRepository
    Library ..> InMemoryLibraryRepository : default storage
    LibraryRepository ..> LibraryData : returns
    JdbcLibraryRepository --> "1" JdbcSettings : settings
    Library ..> LibrarySnapshot : creates
    Library ..> LibrarySummary : returns
    LibrarySnapshot --> "1" LibraryData : records
    LibrarySnapshot ..> LibrarySummary : calculates
    LibraryData --> "0..*" Book : books
    LibraryData --> "0..*" BookCopy : copies
    LibraryData --> "0..*" Member : members
    LibraryData --> "0..*" Loan : loans
```

LibraryData and memory storage hold references to the same domain object types.
The read-only lists returned by a repository do not make their entities immutable.
JdbcSettings opens JDBC connections; JdbcLibraryRepository controls SQL operations
and connection lifetimes.

### Desktop classes and inheritance

Main belongs to `com.daniel.library`; SampleData belongs to `demo`. Desktop classes
belong to `ui`. Each view shows a different relationship type; repeated class names
refer to the same Java classes.

#### Swing inheritance

JFrame and JPanel belong to `javax.swing`. These arrows represent Java `extends`
declarations only.

```mermaid
classDiagram
    direction TB
    class JFrame {
        <<Swing>>
    }
    class JPanel {
        <<Swing>>
    }
    JFrame <|-- LibraryWindow
    JPanel <|-- LibraryWorkspace
    JPanel <|-- SectionPanel
    JPanel <|-- RecordTable
    JPanel <|-- OverviewDashboard
    JPanel <|-- CheckoutForm
```

#### Startup dependencies

Main selects the service and sample data; LibraryApp constructs the window.

```mermaid
classDiagram
    direction LR
    Main ..> SampleData : sample records
    Main ..> Library : creates
    Main ..> LibraryApp : starts
    LibraryApp ..> LibraryWindow : creates
```

#### Window associations

LibraryWindow retains the service, workspace components, layout settings, and
presentation rows. All arrows originate from the window; multiplicities describe
how many objects it references.

```mermaid
classDiagram
    direction LR
    class LibraryWindow {
        -Library library
        -LibraryWorkspace workspace
        -LibraryRecords currentRecords
        -UiPreferences preferences
    }
    class LibraryRecords {
        <<record>>
    }
    LibraryWindow --> "1" Library : library
    LibraryWindow --> "1" LibraryWorkspace : workspace
    LibraryWindow --> "5" RecordTable : section tables
    LibraryWindow --> "1" OverviewDashboard : dashboard
    LibraryWindow --> "1" UiPreferences : preferences
    LibraryWindow --> "0..1" LibraryRecords : currentRecords
```

The following links describe collaboration between the components:

| Class | Related class | Relationship |
| --- | --- | --- |
| LibraryWindow | SectionPanel | Creates section pages |
| LibraryWindow | CheckoutForm | Creates the checkout form |
| LibraryRecords | Library | Loads one reporting snapshot to prepare display values |
| RecordTable | LibrarySection | Uses column definitions and status choices |
| RecordTable | UiPreferences | Reads and saves column widths |
| OverviewDashboard | RecordTable | Contains the detailed overview table |
| CheckoutForm | LibraryRecords | Reads available-copy and borrower choices |

LibraryRecords contains display values derived from one LibrarySnapshot. The
window's record actions call Library; repository implementations remain behind
the service contract. Styling and utility classes are described in
[Desktop composition](#desktop-composition).

## Packages

| Package | Responsibility |
| --- | --- |
| `com.daniel.library` | Entry point and launch flags |
| `demo` | Canonical sample fixture and repeatable population |
| `model` | Identity, local validation, and domain state |
| `service` | Workflow validation, date/fine policy, reporting snapshots, LibraryRepository, and LibraryData |
| `persistence` | Memory/JDBC storage implementations and connection configuration |
| `ui` | Desktop startup, window, workspace, components, styling, display data, and CSV export |
| `images/` | Versioned UI reference screenshots at the project root |

UI source lives directly in ui; tests use the same package. SQL and database setup documentation live
in `database/`. Generated classes and API documentation are excluded from Git. UI reference screenshots
are versioned in `images/`.

## Write ownership

Library validates application workflows. The service package defines LibraryRepository
and LibraryData: the storage contract and its related record result. The persistence
package supplies the memory and JDBC implementations.
Models do not perform I/O. Repository inserts assume service-validated records;
SampleData intentionally uses that low-level contract to add historical examples.

Memory storage retains shared entities so editing a Book or Member updates the
references held by copies and loans. JDBC hydrates related objects together using
identifier maps. Each SQL write commits independently through a short-lived
connection. The return operation saves its date before changing the caller's loan.

## Read ownership and reporting

Library.getSnapshot captures one repository load, one clock date, and the configured
fine rate. LibrarySnapshot owns availability, late-charge, overdue, and overview
calculations for that report. LibraryRecords maps them to display rows without
opening additional connections per copy or loan.

JDBC load uses a repeatable-read transaction for a related data view. Lists are
read-only; entities inside them remain mutable, especially in memory mode. This
is an operation-scoped reporting view, not an immutable historical snapshot.
Read methods load the full dataset.

## Desktop composition

- LibraryApp installs the theme and schedules Swing construction.
- LibraryWindow wires forms, record actions, worker lifecycle, and close handling.
- LibraryWorkspace owns sidebar navigation, page cards, storage badge, and status footer.
- SectionPanel supplies the title, explanation, and action area.
- UiPreferences stores optional local layout sizes independently of library records.
- CheckoutForm selects copy/member identities from the latest display snapshot; Library revalidates checkout before saving.
- OverviewDashboard highlights values already present in overview rows.
- RecordTable owns read-only tables, sorting, search, status filters, and export selection.
- RecordCellRenderer and LibraryTheme centralize visual presentation.

A SwingWorker runs record operations and refresh mapping outside the event thread.
Its completion handler updates components on the event thread. Export captures a
view snapshot there, then writes files in a separate worker. Assertions can inspect
these components headlessly; native focus and modal behavior require a real display.

## Error and operational boundaries

SQLExceptions are wrapped in user-readable IllegalStateExceptions with original
causes retained. Main shows startup errors and LibraryWindow shows workflow errors.
Credentials are read from environment variables and hidden from settings.toString().

There is no connection pool, application login, background scheduler, schema
migration framework, or multi-writer conflict manager. Use one running instance.
A schema initializer creates missing tables only. Database outage never switches
the application to memory silently. Packaging is an IntelliJ/Maven project, not
an installer or self-contained executable JAR.
