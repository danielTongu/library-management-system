# User guide

## Open the library

Run Main in IntelliJ. With no arguments the library is temporary and empty.
Use `--sample` for examples, `--database` for saved records, or
`--database --sample` to add missing examples to MySQL. Follow the [database guide](../database/README.md)
first if persistent storage is needed.

The header identifies Saved library or Temporary library. Temporary records are
lost when the window closes; the application asks before discarding them.

![Overview workspace](../images/overview.png)

## First borrowing workflow

1. **Books:** choose Add book. Enter ISBN, title, and author.
2. **Books:** select the active title and choose Add copy. Enter a unique Copy ID;
   the selected title and ISBN are fixed for this action. You can also add a copy
   in Copies by entering its Copy ID and book ISBN.
3. **Members:** choose Register member. Enter a unique Member ID, name, and email.
4. **Copies:** select an Available copy and choose Check out selected copy. The copy is preselected; choose a registered borrower.
5. Read the due date in the result message; Copies shows Checked out, the borrower, Member ID, and due date.
6. In Loans, select an Active or Overdue row and choose Return selected loan.
   Confirm the displayed copy and borrower.
7. The loan leaves the Current view. Choose Returned or All to see it in history;
   its copy is Available in Copies.

An ISBN identifies a title; a Copy ID identifies one physical item. Two physical
copies of one title share an ISBN but must have different Copy IDs. A Member ID
identifies the borrower. A loan records one borrowing event.

## Find and edit records

Books opens on **Active** and Loans opens on **Current** (active plus overdue).
Use Archived in Books to find archived titles, and Returned or All in Loans for history.
These are startup defaults; refreshes retain your current filters. Clear filters
removes search/member scope and selects All.

Use Search records to match text in any displayed column. Status filters can
narrow books to Active/Archived, copies to Available/Checked out/Archived, or
loans to Current/Active/Overdue/Returned. Current includes both Active and Overdue. Search and status work together. Clear filters
restores the full table, and matching counts appear underneath.

Selection actions are disabled until a suitable row is selected. Checkout requires
an available copy; return requires an active or overdue loan. Restore requires an
archived title. Archive requires an active title with no borrowed copies. Adding
a copy from Books requires an active title. Actions also pause during background work.

Click a column heading to sort. Amounts sort numerically and dates chronologically.
Select a book or member row and choose Edit selected book/member. Existing details
are prefilled. ISBN and Member ID cannot change. Cancel leaves the record untouched.
If a replacement name, title, author, or email is invalid, the current details remain.

In Loans, search a Member ID or name to see that person's borrowing records. In
Copies, search an ISBN and select Available to find items ready for checkout.

## Read the section columns

Each record section follows its fields in order. When a foreign key appears, its
related details follow immediately; the remaining record fields then continue:

| Section | Own fields and foreign keys | Relationships and related details |
| --- | --- | --- |
| Books | ISBN, Title, Author, Status | Copy count, Current borrowers |
| Copies | Copy ID, ISBN, Status | After ISBN: Title, Author; after Status: current loan Member ID, Borrower, Due |
| Members | Member ID, Name, Email | Current loan count; View current loans opens related records |
| Loans | Loan ID, Copy ID, Member ID, Checked out, Due, Returned, Status, Fine | After Copy ID: ISBN, Title, Author; after Member ID: Member |

Copy ISBN links to Books; Loan Copy ID and Member ID link to Copies and Members.
The borrower and due date on Copies come through its current loan. A loan's book
information comes through its linked copy. Status and fine are calculated displays.
Books shows copy counts and current borrowers; Members shows current loan counts.
Overview remains a summary rather than a record table.

Scroll horizontally for additional columns, or select **View details** to read the
complete record. Search and CSV export include both field and relationship columns.

## See who has a book

- **Books:** Copies shows the total physical copy count. Current borrowers lists each checked-out copy, borrower name and Member ID, and due date.
- **Copies:** each checked-out copy shows its current borrower and due date. Available copies have no borrower.
- **Members:** Current loans counts the copies still held. Use View current loans to see titles, Copy IDs, and due dates in Loans.
- **Loans:** includes titles as well as members, dates, status, and fines; returned borrowing stays in history.

In Members, select a person and choose **View current loans**. Loans opens with
only that exact Member ID's active and overdue borrowing. A label identifies the
member scope. You can search within it, select a loan, and return it. Switching
Status to Returned shows that member's history; **Clear filters** removes the
member scope and shows everyone. A member with no current loans shows an empty table.
The member scope survives refreshes and also limits CSV exports.

Current borrowing includes overdue loans and excludes returned loans. Returning or
checking out a copy refreshes these views. Search a borrower ID in Copies, or open a member’s current loans to review their books. Same-name borrowers are distinguished by Member ID.

Checkout choices show book titles and Copy IDs, plus borrower names and Member IDs.
Only available copies appear; borrowed and archived copies are excluded. A borrower
must be explicitly selected. If nothing is available, add/return a copy or restore
a title; if no borrowers are registered, register one in Members first. The choices
come from the latest refresh, and the service checks availability again when saving.
Cancel leaves borrowing unchanged.

![Checkout form](../images/checkout.png)

Selection actions use the selected record even after searching or sorting. Canceling
a checkout form or return confirmation leaves records unchanged. Returned history
rows cannot be returned again; choose the current Active or Overdue loan.

Select a row and choose **View details** to read the complete record without clipped
text. Wide tables scroll horizontally; hover over a cell to see its full text.
Borrowing details are included in CSV exports.

## Archive a title

Select the book, choose Archive selected book, and confirm. All copies of that title
must first be returned. The title and history remain, but its copies cannot be
borrowed or expanded with new copies. Find archived titles using the status filter,
select one, choose Restore selected book, and confirm to resume lending.

Editing an archived title's description does not restore it. The application has
no permanent-delete action for books, members, copies, or loan history.

## Understand Overview and fines

Overview shows quick cards plus detailed totals. Current loans include overdue
loans; archived copies are excluded from availability. Charges are calculated on
calendar days after the due date using the configured rate, default 0.50 per day.
Returning caps the charge at the return date. The Overview separates charges on
current borrowing from charges recorded on returned loans. It does not show
payments or unpaid balances, because payment collection is not implemented.

## Save a report

Set search, status, sorting, and column order, then choose Export CSV. Select a new
filename in an existing folder. The report follows the displayed records; empty
results export headers only. The application adds `.csv` if needed and will not
replace an existing file. CSV import is not available.

## Refresh, errors, and closing

Refresh all reloads every section and keeps filter choices. Record operations run
in the background; disabled actions and an activity indicator show that work is
in progress. A successful write can be followed by a failed refresh. If the message
says so, refresh before repeating the write. If a database connection was lost
during saving, reload or restart to determine what was saved before retrying.

Use one application instance at a time. For MySQL restart verification and common
connection errors, see the [database guide](../database/README.md). Report the exact
error message when asking for help, without sharing database passwords.

## Remembered layout

Closing saves each section's column widths. Window size is saved when the window
is closed in normal mode.
On reopening, the window is centered and its saved size is limited to the current
screen. Column widths follow their headers, even if you moved columns before closing.
Startup filters stay Active for Books and Current for Loans.

Settings live in `.library-management-system/ui.properties` inside your user home
folder (`/Users/your-name` on macOS, typically `C:\Users\your-name` on Windows).
They contain sizes only and are separate from library storage. Close the app and
delete this file to reset the layout. Missing or unreadable settings use defaults.
