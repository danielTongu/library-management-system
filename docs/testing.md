# Testing and verification

## Automated local suite

Use JDK 27 for the project and Maven runner. In IntelliJ, reload Maven and choose
**Maven > Lifecycle > test**, or run `mvn test` when Maven is on PATH. AllChecks can
also be run directly. The test-phase exec plugin launches the assertion-based
suite in a separate headless Java process; these are not JUnit test methods.

The current suite has twelve check groups. A failing group makes the Maven build
fail. The runner reports every group, then prints the passed/total summary.
It does not connect to MySQL or open native windows.

| Group | Main coverage | Requirements |
| --- | --- | --- |
| ValidationChecks | Required fields, email syntax, model dates, atomic local validation | FR-03–08 |
| FineChecks | Due-date boundary, overdue days, stopped charges, return/reborrow | FR-06–08 |
| AvailabilityChecks | Title filtering, copy availability, current member loans | FR-04, FR-06–07 |
| EditingChecks | Stable identities and preserved copy/member/loan references | FR-03, FR-05 |
| ArchivingChecks | Lending restrictions, restore, unchanged history | FR-09 |
| SummaryChecks | Totals after borrowing, returning, archiving, and restoring | FR-11 |
| SnapshotChecks | One repository read per refresh and one consistent reporting date | NFR-03 |
| PersistenceFailureChecks | Failed inserts/edits/returns/archive changes do not mutate successful-looking state | NFR-04 |
| DesktopChecks | Presentation rows, read-only tables, navigation page selection | FR-14 |
| TableFilterChecks | Literal search, statuses, sort/filter selection, numeric fine sorting, exported row order | FR-10, FR-12 |
| SampleDataChecks | Canonical fixture, repeat population, preserved edits/history, conflicts, retry after partial failure | FR-13 |
| CsvExportChecks | UTF-8, commas/quotes/newlines, nulls, empty reports, invalid destinations, existing-file preservation | FR-12 |

## Live MySQL check

The separate JdbcPersistenceChecks requires:

- A running MySQL server and the project JDBC dependency.
- The account and grants from the [database guide](../database/README.md).
- `LIBRARY_DB_USER` and `LIBRARY_DB_PASSWORD`.
- `LIBRARY_TEST_DB_URL=jdbc:mysql://localhost:3306/library_management_test`.

It only accepts that exact dedicated test URL. It creates uniquely named records,
checks reloads, editing, duplicate active-loan rejection, returns, archive/restore,
reborrowing, and retained historical return dates, then deletes its own records.
It is excluded from AllChecks and has not been run against a live server in this audit.

## Native acceptance procedure (macOS and Windows)

1. Run Main with `--sample`. Verify all five sidebar destinations, explanation text,
   summary cards, tabular headers, status badges, and empty-filter messages.
2. Exercise keyboard navigation and focus, including search, filter selection,
   sorting, action buttons, Cancel, and confirmation dialogs.
3. Add a book and copy, register a member, check out and return it. Check due dates,
   error messages, late-charge examples, and preserved history.
4. Edit a sorted/filtered selection and verify the intended record changes. Reject
   an invalid email and confirm the old details remain.
5. Confirm archive is rejected during borrowing. Return, archive, filter, restore,
   and verify availability.
6. Export a filtered/sorted view, cancel a save, reject an existing filename, and
   inspect a Unicode/quoted-value report with a CSV reader.
7. Repeat at the minimum window size and with platform display scaling. Check
   dialog placement, clipped labels, close behavior, and busy-state visibility.
8. With MySQL, run `--database --sample`, close, restart with `--database`, and
   verify saved edits, returns, archive state, and history survive.
9. Stop MySQL during a disposable test session and verify errors do not silently
   switch storage modes or claim a failed write succeeded.

## Documentation and structure checks

`tools/generate-javadoc.py` runs Javadoc with full doclint and warnings treated as
errors. Local documentation links, package-to-folder alignment, ignored outputs,
old production-class names, and duplicate source files are checked during audit.
See [development](development.md) for the API and visual-preview commands.

## Evidence recorded in this audit

- Fresh source compilation and all twelve local groups: passed.
- Public/package Javadocs with full doclint and warnings-as-errors: passed.
- Real Swing component previews: rendered at standard and compact sizes; visual
  review is distinct from native interaction testing.
- Live MySQL, native modal dialogs, and full macOS/Windows acceptance: pending.

The audit records implemented behavior and local evidence; it does not certify
untested deployment, database availability, or native operating-system interactions.
