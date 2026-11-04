# Software documentation

This documentation describes the implemented GUI-only library application.

| Document | Purpose |
| --- | --- |
| [Requirements](requirements.md) | Scope, functional requirements, business rules, and limits |
| [Application flow](application-flow.md) | Startup, navigation, borrowing, returns, archiving, and export |
| [Architecture](architecture.md) | Package responsibilities, dependencies, threading, and storage boundaries |
| [Data model](data-model.md) | Entities, MySQL tables, keys, constraints, and persistence behavior |
| [User guide](user-guide.md) | Everyday desktop workflows |
| [Testing and verification](testing.md) | Automated groups, live database check, and manual acceptance checks |
| [Development guide](development.md) | Build, Javadocs, layout review, and change conventions |
| [Project audit](audit.md) | Cleanup findings, corrections, and remaining verification tasks |
| [Database setup](../database/README.md) | Canonical macOS/Windows MySQL setup instructions |

Start with requirements and application flow. Developers should then read the
architecture and data model. Follow the user guide to operate the application.
Generated API documentation is written to `target/site/apidocs/index.html`; see
the development guide to create it. Generated files are not committed.
