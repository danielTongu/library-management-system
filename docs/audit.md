# Project audit

Audit scope: application source, check suite, build configuration, database scripts,
public API documentation, local scratch artifacts, documentation links, and Git
ignore/index state. Reference date: November 3, 2026.

## Findings and actions

| Finding | Action |
| --- | --- |
| Earlier console menu/demo, nested desktop package, Makefile, and sample SQL had been removed | Verified no obsolete production classes or references remain in source |
| A screen refresh repeatedly loaded the full repository for each row and summary | Added LibrarySnapshot; presentation now uses one repository read and reporting date |
| Monetary columns used text sorting | Added numeric/date comparisons and a regression check for 0.50, 2.00, 10.00 |
| Closing could interrupt a pending report export | Close now waits for record and export work to finish |
| Public APIs lacked consistent validation/result contracts | Added focused Javadocs, package documentation, and full doclint verification |
| Scratch folder retained an old JDBC download, crashed test-server logs, and old preview classes | Removed obsolete scratch artifacts; retained reference images and replaced the preview utility with manual screenshot instructions |
| IDE database/run configuration files were already in Git's index | Added complete IDE ignore rules; user untracked the local IDE files and the index is now clear of them |
| Documentation described retired console workflows and repeated old behavior | Consolidated current GUI-only documentation, requirements, flow, architecture, and verification |
| Generated classes and docs could retain moved/deleted artifacts | Rebuilt output from current source; generated files stay ignored |

No duplicate Java source files or duplicate fully qualified production class names
were found. Similar service/repository method names are intentional boundaries:
Library validates workflows, repositories store records. There is one canonical
sample source and one authoritative schema. UI classes use a flat ui package, with
matching test packages and screenshots retained in the project-root images folder.

## Verification record

- All local check groups pass, including the new single-read snapshot regression.
- Javadoc completes with full doclint and warnings-as-errors.
- Package names match source directories; documentation links and ignore behavior
  have been checked.
- Schema and sample population code compile; live MySQL behavior is not claimed
  as verified by the local suite.
- Rendered Swing panels support visual review, but are not proof of native focus
  or modal-dialog behavior.

Detailed coverage and remaining acceptance work are in [testing.md](testing.md).

## Remaining actions

1. Run the dedicated JdbcPersistenceChecks against library_management_test with
   local credentials, then perform the native macOS/Windows acceptance steps.
2. Keep single-instance operation and the documented full-dataset/read-model and
   per-write transaction limits in mind before expanding deployment scope.

This audit did not read out passwords, alter real database records, make a commit,
or treat unexecuted live/native checks as successful.
