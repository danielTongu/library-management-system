# Development guide

## Tools and build

Use JDK 27 and Maven, or IntelliJ's bundled Maven. Set the project SDK, module SDK,
Main JRE, and Maven runner JRE consistently. The project has no Makefile requirement.
Maven dependencies/plugins must be available locally or downloaded during setup.

- Compile: `mvn compile` or IntelliJ Build Project.
- Verify: `mvn test` or IntelliJ Maven Lifecycle > test.
- Launch: run Main with no arguments, `--sample`, `--database`, or `--database --sample`.
- Native UI work: use an actual graphical desktop, not headless CI.

The application is not packaged as a standalone executable JAR. Maven package can
produce the normal project artifact, but its dependencies/launcher are not bundled.

## Documentation ownership

- README is the quick-start and navigation entry point.
- `docs/` holds requirements, flow, architecture, data model, operation, and verification.
- `database/README.md` is the only detailed database setup guide.
- `database/schema.sql` is the authoritative DDL; Maven bundles it unchanged.
- SampleData is the only sample fixture. Do not add another SQL/CSV example dataset.
- Package-info files explain Java package boundaries; Javadocs describe public API contracts.

Update the relevant documents when a workflow, schema, launch flag, or requirement
changes. Avoid copying long setup instructions into multiple guides.

## Generate Javadocs

In IntelliJ, choose **Tools > Generate JavaDoc** (or search for Generate JavaDoc
with Find Action). Select the production source scope and use the project's JDK 27.
Set the output directory to `target/site/apidocs` and add these options:

```text
-Xdoclint:all -Werror -encoding UTF-8 -docencoding UTF-8 -charset UTF-8
```

Open `target/site/apidocs/index.html` afterward. These steps apply on macOS and
Windows. Generated HTML stays under the ignored Maven output directory.

Javadocs document parameters, results, validation, identity, and storage/threading
boundaries. Comments explain decisions such as commit-before-mutation and
save-success/refresh-failure; avoid narrating obvious assignments or loops.

## Review desktop layout

Run Main with `--sample` in IntelliJ and review all five sections at standard
and minimum window sizes. Check focus, forms, confirmations, and save dialogs.
Capture updated screenshots into `images/` when the layout changes. Existing
images are references from an earlier rendered session, not automatically updated.

## Change workflow

1. Put record validation in models, business workflows in Library/service reporting,
   storage in repositories, and presentation directly in `ui`.
2. Preserve fixed identifiers and existing history. Repository methods are not a
   second librarian API; call Library from UI actions.
3. Use one reporting snapshot per screen refresh. Do not query the repository once
   per row or duplicate fine/availability rules in Swing renderers.
4. Keep record I/O off Swing's event thread and component updates on it.
5. Add meaningful regression checks under matching test packages and register new
   local groups in AllChecks. Keep live database checks separate.
6. Run Maven test, generate Javadocs, check documentation links and the diff, and
   review the running UI when layout changes.
7. Keep statements on one line when comfortably within the 120-column guide;
   wrap for length or clarity rather than decorative formatting.

## Git and local files

The .gitignore excludes build output, scratch work, root-level CSV reports,
exports, credentials/local configuration, IDE metadata, logs, JVM diagnostics,
and operating-system metadata. Commit source, tests, documentation, DDL, and pom.xml.
Use `exports/` for local reports when choosing a save destination.

Ignoring a file does not remove an already indexed file. If IDE settings are
already staged/tracked, run this from the project folder to untrack them while
keeping local files:

```text
git rm -r --cached -- .idea
```

Review `git status` before committing. The local IDE files have been
untracked and their absence from the index verified. No automatic commits or
broad restaging were performed.

## UI layout

All UI Java classes live directly in `com.daniel.library.ui`, and checks use the
same package under src/test/java. Components keep focused class responsibilities
without additional UI subpackages. Helpers stay package-private when only the UI
uses them.

```text
project/
├── images/                             # Versioned screenshots
└── src/main/java/com/daniel/library/
    └── ui/                             # All UI Java classes
```

Save reference screenshots in `images/`.
