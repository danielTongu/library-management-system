# 1. Software Requirements Specification

**Document ID:** LMS-SRS

**Audience:** operators, implementers, and reviewers.

## 1.1. Introduction and operating scope

The application helps one librarian manage a small local library through a Java
Swing desktop interface. It runs on macOS or Windows with JDK 27. Temporary memory
supports learning and demonstrations; optional MySQL storage retains records
across restarts. MySQL 8.4 is the reference installation; live verification is pending.

A member is a borrower record, not an application login. There are no user accounts,
roles, or authentication inside the application. MySQL credentials belong to the
storage connection and are configured separately.

## 1.2. Terms and abbreviations

| Term | Meaning |
| --- | --- |
| Title / book | Catalog description identified by ISBN |
| Copy | One physical item identified by Copy ID |
| Member | Borrower identified by Member ID; not a login account |
| Loan | One borrowing event linking a copy and member |
| Current / active loan | Loan without a return date, including overdue borrowing |
| Overdue | Active borrowing whose due date precedes the reporting date |
| Returned | Completed borrowing retained as history |
| Archived | Title excluded from new copies and lending |
| JDBC | Java Database Connectivity |
| SRS / SDD | Software Requirements Specification / Software Design Description |

## 1.3. Functional requirements

| ID | Capability | Required behavior |
| --- | --- | --- |
| FR-01 | Launch a desktop library | The software shall open the desktop from Main and report invalid launch options as startup errors. |
| FR-02 | Select storage | The software shall select memory or MySQL as requested at launch and reject storage failure without silently changing modes. |
| FR-03 | Manage titles | The software shall add titles, find them through search, and edit title/author while retaining ISBN |
| FR-04 | Manage physical copies | The software shall give each copy a unique ID and associate it with an existing active title |
| FR-05 | Manage members | The software shall register borrowers and edit names/emails while retaining member IDs |
| FR-06 | Check out books | The software shall allow an available copy and registered member to be selected for checkout, preselect the table copy, and apply the due-date rule in [Section 1.4, rule 4](#14-business-rules). |
| FR-07 | Record returns | The software shall return a selected active or overdue loan in Loans; retain history and restore copy availability |
| FR-08 | Calculate late charges | The software shall count overdue calendar days at the configured daily rate; stop at return |
| FR-09 | Archive and restore | The software shall archive a title after all copies are returned; retain records and restore lending later |
| FR-10 | Search and sort | The software shall provide literal search, status filters, and numeric/date-aware sorting with Active as the initial Books filter and Current as the initial Loans filter. |
| FR-11 | Show overview totals | The software shall display catalog, availability, membership, borrowing, and overdue totals with separate current and returned late charges. |
| FR-12 | Export reports | The software shall export the displayed row/column order to a new UTF-8 CSV file without replacing an existing file |
| FR-13 | Populate examples | The software shall populate examples from SampleData without resetting existing records. |
| FR-14 | Explain workflows and errors | The software shall show guidance, errors, and background activity and disable selection actions for ineligible rows or busy state. |
| FR-15 | Show current possession | The software shall display each record’s fields with related details immediately after foreign keys, copy and current-loan counts, current borrower details, full-record inspection, and member-scoped loan navigation. |
| FR-16 | Remember local layout | The software shall restore window size and column widths, bounded by screen/column limits; layout settings stay separate from records |

Launch syntax and session selection are specified in [Section 5.1](user-guide.md#51-launch).

## 1.4. Business rules

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

## 1.5. Nonfunctional requirements

| ID | Quality attribute | Required behavior |
| --- | --- | --- |
| NFR-01 | Separation of responsibilities | The software shall separate model validation, service workflows, repository storage, and UI presentation. |
| NFR-02 | Responsive desktop | The software shall perform storage and export work outside the Swing event thread and update components on that thread. |
| NFR-03 | Consistent reports | The software shall use one repository read and one reporting date per workspace refresh. |
| NFR-04 | Controlled persistence failures | The software shall surface storage errors and complete durable writes before mutating returned objects. |
| NFR-05 | Credential handling | The software shall obtain database credentials from environment variables, hide them in settings text, and exclude local credentials from version control. |
| NFR-06 | Maintainability | The software shall provide focused classes, public API and package documentation, and one canonical sample source. |
| NFR-07 | Repeatable verification | The software shall provide a repeatable local verification suite through Maven test, separate from live MySQL verification. |
| NFR-08 | Platform clarity | The software shall provide labeled macOS and Windows database service procedures and shared IntelliJ setup instructions. |

## 1.6. Boundaries and acceptance

Use one application instance at a time. MySQL field-length limits are specified
in [Section 4.2](data-model.md#42-tables-and-limits). Reporting mutability and concurrency constraints are
specified in [Section 2.6](architecture.md#26-read-ownership-and-reporting); sample provisioning is specified in
[LMS-DB](../database/README.md). No response-time or capacity threshold has been
established; verification must not imply a measured performance guarantee.

Out of scope: reservations, renewals, payments, notifications, authentication,
network clients, CSV import, installers, and a standalone executable distribution.

Acceptance includes live MySQL restart verification and native GUI/dialog checks
on macOS and Windows. See [Section 6](testing.md#6-verification-plan) for coverage and procedures.
