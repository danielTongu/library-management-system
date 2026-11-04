# Requirements

## Purpose and operating scope

The application helps one librarian manage a small local library through a Java
Swing desktop interface. It runs on macOS or Windows with JDK 27. Temporary memory
supports learning and demonstrations; optional MySQL storage retains records
across restarts. MySQL 8.4 is the reference installation; live verification is pending.

A member is a borrower record, not an application login. There are no user accounts,
roles, or authentication inside the application. MySQL credentials belong to the
storage connection and are configured separately.

## Functional requirements

| ID | Requirement | Implemented behavior |
| --- | --- | --- |
| FR-01 | Launch a desktop library | Main opens the GUI with no arguments; invalid options produce a startup error |
| FR-02 | Select storage | Default and `--sample` use memory; `--database` uses MySQL; no silent fallback |
| FR-03 | Manage titles | Add titles, find them through search, and edit title/author while retaining ISBN |
| FR-04 | Manage physical copies | Give each copy a unique ID and associate it with an existing active title |
| FR-05 | Manage members | Register borrowers and edit names/emails while retaining member IDs |
| FR-06 | Check out books | Choose an available copy and registered member; preselect copies from the table and apply a fourteen-day due date |
| FR-07 | Record returns | Return a selected active or overdue loan in Loans; retain history and restore copy availability |
| FR-08 | Calculate late charges | Count overdue calendar days at the configured daily rate; stop at return |
| FR-09 | Archive and restore | Archive a title after all copies are returned; retain records and restore lending later |
| FR-10 | Search and sort | Literal search, status filters, numeric/date-aware sorting; Books starts Active and Loans starts Current |
| FR-11 | Show overview totals | Catalog, availability, members, borrowing, overdue counts, and separate current/returned late charges |
| FR-12 | Export reports | Export the displayed row/column order to a new UTF-8 CSV file without replacing an existing file |
| FR-13 | Populate examples | SampleData supplies memory examples or adds missing MySQL examples without resetting existing changes |
| FR-14 | Explain workflows and errors | Guidance, errors, and background activity are visible; selection actions disable for ineligible rows or busy state |
| FR-15 | Show current possession | Foreign keys are immediately followed by related details before the remaining fields continue; Books shows copy counts/current borrowers; Copies shows book and borrower details; Members shows current loan counts and links to filtered Loans; Loans includes its ID, checkout date, and book/member details; full record details and a member-to-current-loans shortcut are available |
| FR-16 | Remember local layout | Restore window size and column widths, bounded by screen/column limits; layout settings stay separate from records |

`--database --sample` explicitly adds examples to persistent storage before opening.

## Business rules

1. ISBN, Copy ID, and Member ID are required, stable identifiers. Application lookups
   compare them exactly and case-sensitively. GUI entries trim surrounding whitespace.
2. A title can have many physical copies. A copy has one title; a loan has one copy
   and one member. The application does not impose a maximum number of loans per member.
3. Only one active loan may refer to a copy. A null return date means active.
4. Due date is checkout date plus fourteen calendar days. There is no charge on the
   due date; the following day is one day overdue.
5. Default daily fine rate is 0.50 monetary units. Calculations use BigDecimal,
   rounded HALF_UP to two decimal places. Rates are application configuration,
   not stored per historical loan. Currency and payment balances are not tracked.
6. Return date must be on or after checkout. A completed loan cannot be returned again.
7. Active loans include overdue borrowing. Returned loans remain in history but
   are excluded from current and overdue counts.
8. Archive a title only when none of its copies is borrowed. Archived titles cannot
   accept new copies or checkouts. Editing details does not change archive state.
9. Invalid replacement fields and failed writes must leave existing model state unchanged.
10. Basic email syntax is checked. ISBN checksums, email delivery, and duplicate
    email-address detection are not implemented.

## Nonfunctional requirements

| ID | Requirement | Design |
| --- | --- | --- |
| NFR-01 | Separation of responsibilities | Models validate fields; service owns workflows; repositories store records; UI presents them |
| NFR-02 | Responsive desktop | Background workers perform storage and export operations; Swing components update on the event thread |
| NFR-03 | Consistent reports | One workspace refresh uses one repository read and one reporting date |
| NFR-04 | Controlled persistence failures | Errors are surfaced; individual writes commit before returned-object state changes |
| NFR-05 | Credential handling | Environment variables supply credentials; toString hides them; local settings are ignored by Git |
| NFR-06 | Maintainability | Focused classes, documented public APIs, package documentation, and one canonical sample source |
| NFR-07 | Repeatable verification | Maven test runs the local assertion-based suite; live MySQL is separate |
| NFR-08 | Platform clarity | Setup guide identifies macOS/Windows service steps and shared IntelliJ configuration |

## Boundaries and acceptance

Use one application instance at a time. Memory repositories and mutable domain
objects are not thread-safe. A snapshot has read-only lists but does not deep-copy
entities. The desktop serializes record-changing operations; a snapshot must not
be mutated while rendering.

MySQL field lengths are enforced by the schema; memory mode only checks model
validation. Reads load the full library. Sample population is resumable, not one atomic transaction.
The schema initializer creates missing tables; it is not a migration system.

Out of scope: reservations, renewals, payments, notifications, authentication,
network clients, CSV import, installers, and a standalone executable distribution.

Acceptance includes live MySQL restart verification and native GUI/dialog checks
on macOS and Windows. See [testing](testing.md) for coverage and procedures.
