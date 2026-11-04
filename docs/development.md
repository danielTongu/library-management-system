# Development guide

## Tools and build

Use JDK 27 and Maven, or IntelliJ's bundled Maven. Set the project SDK, module SDK,
Main JRE, and Maven runner JRE consistently. The project has no Makefile requirement.
Maven dependencies/plugins must be available locally or downloaded during setup.
No Python dependency is needed to run the application; Python 3 is optional for
the documentation helper below.

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

With JAVA_HOME pointing to JDK 27, run from the project folder:

macOS:

```bash
python3 tools/generate-javadoc.py
```

Windows:

```powershell
py -3 tools/generate-javadoc.py
```

Open `target/site/apidocs/index.html`. The helper uses only Python's standard
library and the JDK Javadoc executable. It enables full doclint and treats warnings
as errors. Generated HTML stays under the ignored Maven output directory.
You can also use IntelliJ's Generate JavaDoc action for interactive browsing.

Javadocs document parameters, results, validation, identity, and storage/threading
boundaries. Comments explain decisions such as commit-before-mutation and
save-success/refresh-failure; avoid narrating obvious assignments or loops.

## Review desktop layout

Run `WorkspacePreview.main()` under src/test/java in IntelliJ. It renders actual
workspace components to ignored `work/previews/` PNG files without a native JFrame.
The preview has inert action buttons and does not connect to MySQL. It covers
Overview, Books, Loans, and a compact layout. Review those images, then manually
check native focus, forms, confirmations, and save dialogs.

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
   review previews when layout changes.
7. Keep statements on one line when comfortably within the 120-column guide;
   wrap for length or clarity rather than decorative formatting.

## Git and local files

The .gitignore excludes build output, work/previews, root-level CSV reports,
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
