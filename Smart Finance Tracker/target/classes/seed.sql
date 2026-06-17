-- =====================================================
-- SEED FILE: Run this ONCE after creating the schema
-- Hibernate (ddl-auto=update) creates the tables.
-- Run this file AFTER the application has started once
-- so that all tables exist.
--
-- Command:  mysql -u root -p finance_tracker1 < seed.sql
-- =====================================================

-- 1. ROLES (idempotent)
INSERT IGNORE INTO roles (role_name) VALUES ('USER'), ('ADMIN');

-- 2. DEFAULT ADMIN USER
-- Email:    admin@finai.com
-- Password: Admin@123   (BCrypt hash below — DO NOT change the hash manually)
-- FIX: Use IGNORE so re-running is safe; role_id looked up by name, not hardcoded.
INSERT IGNORE INTO users
    (name, email, password_hash, role_id, status, is_verified, failed_attempts, account_locked)
SELECT
    'Super Admin',
    'admin@finai.com',
    '$2a$10$7QfJpf1JUKRqc/QHVTdwWus6u7LcAhC6FrFBFpmwQDHdlSTkx0cRG',
    r.id,
    'ACTIVE',
    TRUE,
    0,
    FALSE
FROM roles r
WHERE r.role_name = 'ADMIN'
LIMIT 1;

-- VERIFICATION: Run this to confirm the admin row exists
-- SELECT u.id, u.email, r.role_name, u.status FROM users u JOIN roles r ON u.role_id = r.id;

-- 3. DEFAULT CATEGORIES (user_id IS NULL = global / system default)
INSERT IGNORE INTO categories (user_id, name, type, is_default) VALUES
-- EXPENSE
(NULL, 'Food & Dining',    'EXPENSE', TRUE),
(NULL, 'Transport',        'EXPENSE', TRUE),
(NULL, 'Shopping',         'EXPENSE', TRUE),
(NULL, 'Entertainment',    'EXPENSE', TRUE),
(NULL, 'Health & Fitness', 'EXPENSE', TRUE),
(NULL, 'Utilities',        'EXPENSE', TRUE),
(NULL, 'Housing',          'EXPENSE', TRUE),
(NULL, 'Loan & EMI',       'EXPENSE', TRUE),
(NULL, 'Education',        'EXPENSE', TRUE),
(NULL, 'Travel',           'EXPENSE', TRUE),
(NULL, 'Bills',            'EXPENSE', TRUE),
(NULL, 'Others',           'EXPENSE', TRUE),
-- INCOME
(NULL, 'Salary',           'INCOME',  TRUE),
(NULL, 'Freelance',        'INCOME',  TRUE),
(NULL, 'Investment',       'INCOME',  TRUE),
(NULL, 'Other Income',     'INCOME',  TRUE);

-- 4. EXTRA TABLES (Hibernate creates them via ddl-auto=update,
--    but included here for manual migrations / fresh DB setup)
CREATE TABLE IF NOT EXISTS password_reset_tokens (
    id          BIGINT       PRIMARY KEY AUTO_INCREMENT,
    user_id     BIGINT       NOT NULL,
    token       VARCHAR(255) NOT NULL,
    expiry_date DATETIME     NOT NULL,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS activity_logs (
    id          BIGINT       PRIMARY KEY AUTO_INCREMENT,
    user_id     BIGINT,
    action      VARCHAR(100) NOT NULL,
    description VARCHAR(500),
    ip_address  VARCHAR(50),
    log_type    VARCHAR(50)  NOT NULL,
    created_at  DATETIME     DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE SET NULL
);
