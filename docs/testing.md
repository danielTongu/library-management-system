# Testing and verification

## Automated local suite

Use JDK 27 for the project and Maven runner. In IntelliJ, reload Maven and choose
**Maven > Lifecycle > test**, or run `mvn test` when Maven is on PATH. AllChecks can
also be run directly. The test-phase exec plugin launches the assertion-based
suite in a separate headless Java process; these are not JUnit test methods.

The current suite has thirteen check groups. A failing group makes the Maven build
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
| DesktopChecks | Loan identity/dates and foreign-key relationships, borrower/possession rows, checkout choices and prerequisites, same-name member identities, read-only tables, navigation page selection | FR-14 |
| UiPreferencesChecks | Window/column round trips, moved columns, smaller screens, malformed/unwritable settings | FR-16 |
| TableFilterChecks | Startup defaults, eligible selection actions and busy/refresh states, literal search, exact member scope, sort/filter selection, numeric fine sorting, exported row order | FR-10, FR-12 |
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
It runs separately from AllChecks and requires a live MySQL server.

## Native acceptance procedure (macOS and Windows)

1. Run Main with `--sample`. Verify all five sidebar destinations, explanation text,
   summary cards, tabular headers, status badges, and empty-filter messages. Verify
   Active is the initial Books filter and Current is the initial Loans filter. Select rows
   and check View details, including long borrower lists.
2. Exercise keyboard navigation and focus, including search, filter selection,
   sorting, action buttons, Cancel, and confirmation dialogs.
3. Add a book and copy, register a member, check out and return it. Test selection
   actions after sorting and filtering Copies and Loans, cancel each dialog, and
   reject borrowed/archived copies for checkout and returned history rows for returns. From Members,
   open current loans, verify the scope label, return a loan, and clear the scope.
   Verify checkout choices exclude borrowed/archived copies, distinguish same-name
   members, require explicit selections, and explain empty prerequisites. Check due dates,
   error messages, late-charge examples, and preserved history. From Books, select
   an active title and add a copy without entering ISBN. Verify disabled selection
   actions for no selection, borrowed/archived copies, and returned loans.
4. Edit a sorted/filtered selection and verify the intended record changes. Reject
   an invalid email and confirm the existing details remain.
5. Confirm Archive selected book is disabled during borrowing. Return all copies,
   archive the title, choose Archived, restore it, and verify availability.
6. Export a filtered/sorted view, cancel a save, reject an existing filename, and
   inspect a Unicode/quoted-value report with a CSV reader.
7. Repeat at the minimum window size and with platform display scaling. Check
   dialog placement, clipped labels, close behavior, and busy-state visibility. Resize
   the window and columns, close, reopen, and verify the saved layout; also test
   reopening on a smaller display.
8. With MySQL, run `--database --sample`, close, restart with `--database`, and
   verify saved edits, returns, archive state, and history survive.
9. Stop MySQL during a disposable test session and verify errors do not silently
   switch storage modes or claim a failed write succeeded.

## Documentation and structure checks

Generate Javadocs in IntelliJ with full doclint and warnings treated as errors.
Check local documentation links, package-to-folder alignment, duplicate source files,
and ignored build output. The [development guide](development.md) provides Javadoc
settings and the layout-review procedure.

## Verification scope

The local suite checks domain rules, repository failure handling, reporting,
Swing components, CSV output, and layout preferences. It runs without a database
or native application window.

Live database persistence requires JdbcPersistenceChecks and the restart procedure.
Native focus, dialogs, platform scaling, and window relaunch behavior require the
macOS/Windows acceptance procedure. Rendered reference images support visual review;
they do not establish native interaction or live database correctness.
