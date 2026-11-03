-- Run as a MySQL administrator before starting Main with --database.
-- Setup instructions: database/README.md.
-- Safe to rerun: creates missing databases without deleting existing records.
-- This script does not create accounts or tables; the application creates tables at startup.
CREATE DATABASE IF NOT EXISTS library_management
    CHARACTER SET utf8mb4 COLLATE utf8mb4_bin;

-- Optional separate database for JdbcPersistenceChecks; never run checks against your real records.
CREATE DATABASE IF NOT EXISTS library_management_test
    CHARACTER SET utf8mb4 COLLATE utf8mb4_bin;
