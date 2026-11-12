# Software Documentation Index

**System:** Library Management System

**Document ID:** LMS-DOC

**Audience:** librarians, developers, and reviewers

## Purpose and organization

This set uses numbered sections, identified requirements, design views, and verification
traceability. The requirements specification is organized with ISO/IEC/IEEE 29148:2018
as a reference; the design description uses IEEE 1016-2009 as a reference. This is a
project-tailored IEEE-style set, without a claim of full standards conformance.

Each document in this directory is a chapter numbered in document-register order,
starting with 1. Software Requirements Specification. Its sections use the chapter
prefix (1.1, 1.2, and so on). This index is unnumbered. Database and image guides
are separate supporting documents with their own numbering.

Cross-references use “Section” followed by the complete heading number and a
clickable link. References into separate supporting guides include their document
ID to distinguish their local numbering (for example, LMS-DB, Section 6).
Ordered lists use local item numbers, referenced with their containing section
(for example, Section 5.2, step 5). Requirement references retain their FR/NFR identifiers.

Each topic has one authoritative location. Other documents link to that location
rather than reproducing its rules, procedures, or definitions.

## Document register

| ID | Chapter | Document | Authoritative content |
| --- | --- | --- | --- |
| LMS-SRS | 1 | [Software requirements specification](requirements.md) | Scope, terms, functional and quality requirements, business rules, constraints |
| LMS-SDD | 2 | [Software design description](architecture.md) | Java UML, components, interfaces, ownership, concurrency, error boundaries |
| LMS-BHV | 3 | [Behavior design](application-flow.md) | Internal startup and operation sequences |
| LMS-DAT | 4 | [Data design](data-model.md) | Stored structure, keys, mappings, database constraints |
| LMS-USR | 5 | [User documentation](user-guide.md) | Launch options and everyday operating procedures |
| LMS-DB | Separate guide | [Database installation procedure](../database/README.md) | macOS/Windows server, accounts, credentials, persistent startup and troubleshooting |
| LMS-VV | 6 | [Verification plan](testing.md) | Requirement-to-check traceability, test execution, acceptance and verification limits |
| LMS-DEV | 7 | [Development procedure](development.md) | Build, Javadocs, source maintenance, version control |
| LMS-IMG | Separate guide | [UI reference catalogue](../images/README.md) | Screenshot inventory and capture procedure |

AGENTS.md supplies repository editing instructions; it is not a product specification.
Generated Java API contracts are available at `target/site/apidocs/index.html` after
following LMS-DEV. Generated output is excluded from version control.

## Reading paths

Operators start with LMS-USR and use LMS-DB when saved storage is required.
Developers read LMS-SRS, LMS-SDD, LMS-BHV, and LMS-DAT before LMS-DEV.
Reviewers use LMS-VV to connect requirements with evidence and outstanding acceptance work.

## References

[1] ISO/IEC/IEEE, [29148:2018, Systems and software engineering—Life cycle processes—Requirements engineering](https://standards.ieee.org/ieee/29148/6937/).

[2] IEEE, [1016-2009, IEEE Standard for Information Technology—Systems Design—Software Design Descriptions](https://standards.ieee.org/ieee/1016/4502/).
