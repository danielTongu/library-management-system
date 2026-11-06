# Library Management System

A Java Swing application for managing books, physical copies, members, loans,
returns, late charges, and archived titles. Records can use temporary memory or MySQL.

## Run

Open the project in IntelliJ, reload Maven, select JDK 27 for the project, module,
and run configuration, then run `com.daniel.library.Main`.

| Program arguments | Behavior |
| --- | --- |
| None | Empty temporary library |
| `--sample` | Temporary library with sample records |
| `--database` | Library using MySQL storage |
| `--database --sample` | Add missing sample records to MySQL and open the library |

A graphical display is required. Temporary records are lost when the window closes.
For MySQL on macOS or Windows, follow the [database setup guide](database/README.md).
Credentials are supplied through environment variables.

## Use the library

- **Overview:** totals, availability, overdue borrowing, and late charges.
- **Books:** title records, copy counts, current borrowers, and archive/restore actions.
  The initial filter is Active. Add copy uses the selected title's ISBN.
- **Copies:** physical items, book details, availability, and checkout.
- **Members:** contact records, current loan counts, and View current loans.
- **Loans:** borrowing details and returns. The initial filter is Current;
  Returned and All include completed borrowing.

Related details follow their foreign-key IDs in tables. Search, sorting, View details,
and CSV exports use the displayed information. Selection actions require eligible
records. Window size and column widths are saved locally.

## Documentation

- [Documentation index](docs/README.md)
- [Requirements](docs/requirements.md)
- [Application flow](docs/application-flow.md)
- [Architecture and UML class diagrams](docs/architecture.md#uml-class-diagrams)
  and [data model](docs/data-model.md)
- [User guide](docs/user-guide.md)
- [Testing](docs/testing.md)
- [Development and Javadocs](docs/development.md)
- [UI reference images](images/README.md)

Business workflows, the repository contract, and its record data live in `service/`.
Memory and JDBC storage implementations live in `persistence/`.

## Verify

Use JDK 27. In IntelliJ's Maven panel, choose **Lifecycle > test**, or run:

```text
mvn test
```

AllChecks runs thirteen local check groups. Live MySQL checks and native GUI
acceptance are separate procedures in the testing guide. Generate API documentation
with IntelliJ's **Tools > Generate JavaDoc** using the [development settings](docs/development.md#generate-javadocs).
