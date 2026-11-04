# Data model and persistence

The authoritative DDL is [database/schema.sql](../database/schema.sql). It is bundled
as `/database/schema.sql` by Maven. Database creation remains separate in
[create-database.sql](../database/create-database.sql).

```mermaid
erDiagram
    books ||--o{ book_copies : has
    books ||--o| book_archive : has_state
    book_copies ||--o{ loans : appears_in
    members ||--o{ loans : borrows
    books {
        varchar isbn PK
        varchar title
        varchar author
    }
    book_archive {
        varchar isbn PK,FK
        boolean archived
    }
    book_copies {
        varchar copy_id PK
        varchar isbn FK
    }
    members {
        varchar member_id PK
        varchar name
        varchar email
    }
    loans {
        varchar loan_id PK
        varchar copy_id FK
        varchar member_id FK
        date checkout_date
        date due_date
        date return_date
        varchar active_copy_id UK
    }
```

## Tables and limits

| Table | Stored fields and constraints |
| --- | --- |
| books | ISBN VARCHAR(64) primary key; title VARCHAR(500), author VARCHAR(300), both required |
| book_archive | ISBN VARCHAR(64) primary/foreign key; required archived boolean; missing row means active |
| book_copies | Copy ID VARCHAR(64) primary key; required ISBN foreign key |
| members | Member ID VARCHAR(64) primary key; required name VARCHAR(300) and email VARCHAR(320) |
| loans | Loan ID VARCHAR(36) primary key; required copy/member foreign keys and checkout/due dates; nullable return date |

The schema uses InnoDB and utf8mb4 with utf8mb4_bin collation. SQL field lengths
are storage limits, not matching model length validators. Avoid identifiers with
surrounding whitespace: GUI forms trim entries, and database string comparison
semantics can differ from Java equality for trailing spaces.

## Active borrowing constraint

`active_copy_id` is a stored generated column: copy_id while return_date is null,
otherwise null. Its unique index allows only one active loan per copy while
allowing multiple returned records. It is not entered or exported as a separate
application identifier.

CHECK constraints require due_date on/after checkout_date and a return date that
is null or on/after checkout. The service additionally checks participants,
availability, and archive policy before saving.

## Identity and history

BookCopy is a record with `copyId()` and `book()` accessors. Its components cannot
be reassigned, but the referenced Book can still be edited. Record equality compares
both components; Book currently uses object identity. BookCopy references Book; Loan references BookCopy and Member. Application loans
receive UUIDs, while canonical sample loans have fixed IDs. Updating title/contact
fields retains identity. Archiving is stored separately so existing tables do not
need a destructive rebuild. Restore updates archive state without deleting history.

Overdue status, availability, fine amount, and overview totals are calculated;
they are not redundant status columns. The daily fine rate and currency are not
stored per loan, and no payments table exists. Current configuration must remain
consistent when interpreting historical charges.

## Persistence semantics

- One load reconstructs a connected entity graph inside a read transaction.
- Individual writes use prepared statements and commit independently.
- Active and returned loans can be inserted; sample/historical return dates are retained.
- Memory lists are copied on read, but contained entities are shared and mutable.
- Sample population uses one Java fixture. It preserves existing IDs, edits,
  archive state, and borrowing; skips incompatible copy identities or active-loan
  conflicts; and can resume after partial failure.
- The schema initializer creates absent tables; it does not upgrade existing table definitions.
- There is no hard-delete workflow in the application. The separate live test removes
  only the uniquely named records it created in the dedicated test database.

See the [database guide](../database/README.md) for account privileges, credentials,
setup, and optional example population on macOS and Windows.
