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
2. **Copies:** choose Add copy. Enter a unique Copy ID and the existing book ISBN.
3. **Members:** choose Register member. Enter a unique Member ID, name, and email.
4. **Loans:** choose Check out copy. Enter the Copy ID and Member ID.
5. Read the due date in the result message; the copy now shows Checked out.
6. Choose Return copy and enter its Copy ID when the item comes back.
7. The completed loan remains visible as Returned, and the copy becomes Available.

An ISBN identifies a title; a Copy ID identifies one physical item. Two physical
copies of one title share an ISBN but must have different Copy IDs. A Member ID
identifies the borrower. A loan is one borrowing record, not another kind of book.

## Find and edit records

Use Search records to match text in any displayed column. Status filters can
narrow books to Active/Archived, copies to Available/Checked out/Archived, or
loans to Active/Overdue/Returned. Search and status work together. Clear filters
restores the full table, and matching counts appear underneath.

Click a column heading to sort. Amounts sort numerically and dates chronologically.
Select a book or member row and choose Edit selected book/member. Existing details
are prefilled. ISBN and Member ID cannot change. Cancel leaves the record untouched.
If a replacement name, title, author, or email is invalid, the current details remain.

In Loans, search a Member ID or name to see that person's borrowing records. In
Copies, search an ISBN and select Available to find items ready for checkout.

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
