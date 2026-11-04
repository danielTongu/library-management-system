# Library Management System

A Java Swing desktop application for a small library: catalog titles, physical
copies, members, borrowing/returns, late charges, editing, archiving, overview
totals, search, and CSV reports. It supports temporary memory or MySQL storage.

## Run

Open the project in IntelliJ, reload Maven, select JDK 27 for the project/module
and run configuration, then run `com.daniel.library.Main`.

| Program arguments | Behavior |
| --- | --- |
| None | Empty temporary desktop library |
| `--sample` | Temporary desktop with canonical example records |
| `--database` | Desktop using saved MySQL records |
| `--database --sample` | Add missing examples to MySQL, preserving existing records |

A graphical display is required. Temporary records are lost on close; the header identifies
storage mode. Configure MySQL with the [database setup guide](database/README.md)
on macOS or Windows. Credentials belong in environment variables, not source files.

## UI images

Reference screenshots are saved in [images](images/README.md).
See [layout review](docs/development.md#review-desktop-layout) for updating screenshots after UI changes.

## Documentation

Start with the [software documentation index](docs/README.md).

- [Requirements and scope](docs/requirements.md)
- [Application flow](docs/application-flow.md)
- [Architecture](docs/architecture.md) and [data model](docs/data-model.md)
- [User guide](docs/user-guide.md)
- [Testing and acceptance](docs/testing.md)
- [Development and Javadocs](docs/development.md)
- [Audit findings and remaining actions](docs/audit.md)

SampleData is the only example dataset. Database creation and the schema live in
`database/`; GUI classes live directly under `ui`, with reference screenshots in the project-root `images/` folder.

## Verify

In IntelliJ's Maven panel, choose **Lifecycle > test**. With Maven on PATH:

```text
mvn test
```

Use JDK 27 for Maven as well as the project. AllChecks runs twelve local check
groups without MySQL or native windows. Generate API documentation using
IntelliJ’s **Tools > Generate JavaDoc** on macOS or Windows; see the
[development guide](docs/development.md#generate-javadocs) for settings.

The local suite and Javadocs are verified. Live MySQL and native macOS/Windows
acceptance remain pending; see the testing guide. This is a source/Maven project,
not an installer or self-contained executable distribution.
# library-management-system
