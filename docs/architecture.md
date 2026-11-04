# Architecture

## Component boundaries

```mermaid
flowchart TB
    Main --> DesktopApplication
    Main --> SampleData
    Main --> DatabaseSettings
    Main --> JDBC[JdbcLibraryRepository]
    DesktopApplication --> UI[LibraryWindow and workspace components]
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

## Packages

| Package | Responsibility |
| --- | --- |
| `com.daniel.library` | Entry point and launch flags |
| `demo` | Canonical sample fixture and repeatable population |
| `model` | Identity, local validation, and domain state |
| `service` | Workflow validation, date/fine policy, and reporting snapshots |
| `persistence` | Record storage contract, memory/JDBC implementations, configuration |
| `ui` | Swing controls, presentation data, styling, filters, and export |

All UI source lives directly in `ui`. SQL and database setup documentation live
in `database/`. There is one production class for each responsibility; generated
classes, previews, and API documentation are excluded from Git.

## Write ownership

Library is the application API. Repositories are storage APIs, not alternate
librarian interfaces. Repeated method names across these boundaries express
validation versus storage responsibilities, rather than duplicated workflows.
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
Read methods still load the full dataset; targeted queries and pagination are future work.

## Desktop composition

- DesktopApplication installs the theme and schedules Swing construction.
- LibraryWindow wires forms, record actions, worker lifecycle, and close handling.
- LibraryWorkspace owns sidebar navigation, page cards, storage badge, and status footer.
- SectionPanel supplies the title, explanation, and action area.
- OverviewDashboard highlights values already present in overview rows.
- RecordTable owns read-only tables, sorting, search, status filters, and export selection.
- RecordCellRenderer and DesktopTheme centralize visual presentation.

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
