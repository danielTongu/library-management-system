# Database Installation Procedure

**Document ID:** LMS-DB

**Audience:** local database administrator and application operator.

Use MySQL mode when library records should survive restarting the application.

## 1. Files

- `create-database.sql`: creates the library database and separate test database.
- `schema.sql`: creates the tables automatically when the application starts.

## 2. Requirements

On macOS (Apple) or Windows (Microsoft), install MySQL Server, IntelliJ IDEA, and
JDK 27 before setup. A database client alone does not install or start the server.
The SQL scripts and IntelliJ configuration below are the same on both systems.

## 3. Start MySQL

### 3.1. macOS

For MySQL installed with the official macOS package, run in Terminal:

```bash
sudo /usr/local/mysql/support-files/mysql.server start
```

Enter your Mac login password for sudo. If MySQL was installed another way, start
it using that installation's service controls. This path is specific to the
standard macOS package installation.

### 3.2. Windows

1. Press **Windows + R**, enter `services.msc`, and press Enter.
2. Find your MySQL service. Its name may be `MySQL84` or a custom name.
3. Right-click it and choose **Start**. If it is already running, leave it running.

If no MySQL service appears, check the MySQL Server installation and its service
configuration. Administrator access may be needed to start the service. See the
[MySQL Windows service guide](https://dev.mysql.com/doc/refman/8.4/en/windows-start-service.html).

## 4. Connect as the MySQL administrator

In IntelliJ's **Database** panel, add a **MySQL** data source with these values:

| Field | Value |
| --- | --- |
| Name | `MySQL administrator` |
| Host | `localhost` |
| Port | `3306` |
| User | `root` (or your existing MySQL administrator username) |
| Password | The MySQL administrator password set during MySQL installation |
| Database | Leave blank |

Click **Test Connection**, download the driver if requested, then click **OK**.
This password is not necessarily your Mac or Windows login password. The project
does not set it. Use this administrator connection for sections 5 and 6.

## 5. Create the databases

1. Right-click **MySQL administrator > New > Query Console**.
2. Open `database/create-database.sql` in the project, copy its contents into that
   console, select all the statements, and run them.
3. Refresh the connection. The databases `library_management` and
   `library_management_test` should exist. Select them in the schema selector
   if they are not visible.

The script can be rerun without deleting existing records. Tables are created
later, when the application starts in database mode.

## 6. Create the library account

In the same **MySQL administrator** query console, run the following SQL.
Replace `your-chosen-password` with a password you choose before running it:

```sql
CREATE USER 'library_user'@'localhost' IDENTIFIED BY 'your-chosen-password';
GRANT SELECT, INSERT, UPDATE, CREATE, REFERENCES ON library_management.* TO 'library_user'@'localhost';
GRANT SELECT, INSERT, UPDATE, DELETE, CREATE, REFERENCES ON library_management_test.* TO 'library_user'@'localhost';
```

If `library_user` already exists, skip CREATE USER, reuse its password, and run
only the GRANT statements. Use the username `library_user` consistently.
There is no built-in project password. Keep your chosen password for sections 7 and 8,
and keep actual passwords out of committed files.

## 7. Connect as the library account

Add a separate **MySQL** data source in IntelliJ's **Database** panel:

| Field | Value |
| --- | --- |
| Name | `Library database` |
| Host | `localhost` |
| Port | `3306` |
| User | `library_user` |
| Password | The password chosen in section 6 |
| Database | `library_management` |
| URL | `jdbc:mysql://localhost:3306/library_management` |

Click **Test Connection**, then **OK**. Use the account created in section 6.
The Name field is a display label.

## 8. Configure and run the application

1. Reload Maven to download the project's MySQL driver dependency.
2. Open **Run > Edit Configurations > Main**. Set **Program arguments** to
   `--database`.
3. Under **Modify options > Environment variables**, add these separate entries:

   | Variable | Value |
   | --- | --- |
   | `LIBRARY_DB_URL` | `jdbc:mysql://localhost:3306/library_management` |
   | `LIBRARY_DB_USER` | `library_user` |
   | `LIBRARY_DB_PASSWORD` | The same password chosen in section 6 |

4. Click **Apply > OK**, then run Main using JDK 27.
5. Confirm the window header shows **Saved library** and **Changes save automatically**.
   The application creates missing tables from `database/schema.sql`.

The application always opens the desktop GUI. `--database` selects MySQL storage. These settings work on both macOS and Windows.

The Database panel and the Java application use separate connections. Configuring
the Database panel does not automatically configure Main's environment variables.

## 9. Verify installation

Follow the saved-storage restart and outage acceptance cases in
[Section 6.3](../docs/testing.md#63-native-acceptance-procedure-macos-and-windows).

## 10. Optional: populate sample records

1. Complete the database and account setup above.
2. In Main's run configuration, set Program arguments to `--database --sample`.
3. Run Main. It creates missing tables, adds missing sample records, and opens
   the desktop.
4. For normal use afterward, change Program arguments back to `--database`.

This adds three books, four copies, three members, and three loans to a fresh
library. Sample checkout, due, and return dates are fixed in SampleData. Unreturned loans become overdue as time passes.
For examples without MySQL, use `--sample`; those changes are temporary.

SampleData supplies the sample records. Repeating
population keeps existing details, archives, returns, and active borrowing. It
skips conflicting copy IDs and does not reset existing records. If population
fails partway through, successful inserts remain; rerun after resolving the error
to finish adding missing examples. Existing conflicts can make a used database
look different from the fresh samples.

## 11. Each time you run

On either system, start MySQL if needed and run Main with the same IntelliJ
configuration. Setup scripts do not need to be rerun.

Use one running instance at a time. Records are read from the repository when
requested, but already displayed results are not refreshed automatically. Without `--database`,
the desktop uses temporary memory.

## 12. Optional database verification

Follow the live MySQL procedure in [Section 6.2](../docs/testing.md#62-live-mysql-check).

## 13. If connection fails

- **Connection refused:** start MySQL and check host/port.
- **Access denied:** check account password and grants.
- **Unknown database:** run `create-database.sql` on that server.
- **Missing driver:** reload Maven.
- **Records disappeared:** confirm you used `--database` on both runs.
- **Save failed:** restart to reload records before retrying if its outcome is uncertain.
