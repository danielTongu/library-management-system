CREATE TABLE IF NOT EXISTS books (
    isbn VARCHAR(64) PRIMARY KEY,
    title VARCHAR(500) NOT NULL,
    author VARCHAR(300) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_bin;

CREATE TABLE IF NOT EXISTS book_archive (
    isbn VARCHAR(64) PRIMARY KEY,
    archived BOOLEAN NOT NULL,
    FOREIGN KEY (isbn) REFERENCES books(isbn)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_bin;

CREATE TABLE IF NOT EXISTS members (
    member_id VARCHAR(64) PRIMARY KEY,
    name VARCHAR(300) NOT NULL,
    email VARCHAR(320) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_bin;

CREATE TABLE IF NOT EXISTS book_copies (
    copy_id VARCHAR(64) PRIMARY KEY,
    isbn VARCHAR(64) NOT NULL,
    FOREIGN KEY (isbn) REFERENCES books(isbn)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_bin;

CREATE TABLE IF NOT EXISTS loans (
    loan_id VARCHAR(36) PRIMARY KEY,
    copy_id VARCHAR(64) NOT NULL,
    member_id VARCHAR(64) NOT NULL,
    checkout_date DATE NOT NULL,
    due_date DATE NOT NULL,
    return_date DATE,
    active_copy_id VARCHAR(64) GENERATED ALWAYS AS (IF(return_date IS NULL, copy_id, NULL)) STORED,
    UNIQUE KEY one_active_loan_per_copy (active_copy_id),
    FOREIGN KEY (copy_id) REFERENCES book_copies(copy_id),
    FOREIGN KEY (member_id) REFERENCES members(member_id),
    CHECK (due_date >= checkout_date),
    CHECK (return_date IS NULL OR return_date >= checkout_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_bin;
