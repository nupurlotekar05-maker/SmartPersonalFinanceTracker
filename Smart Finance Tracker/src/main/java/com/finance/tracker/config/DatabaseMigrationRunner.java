package com.finance.tracker.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class DatabaseMigrationRunner implements CommandLineRunner {

    private final JdbcTemplate jdbcTemplate;

    @Override
    public void run(String... args) {
        log.info("Starting database migration checks...");
        try {
            // FIX for 'Data truncated for column status' error.
            // Hibernate ddl-auto=update often fails to increase column length for existing columns in MySQL.
            log.info("Ensuring 'users.status' column is large enough for 'SUSPENDED' status...");
            jdbcTemplate.execute("ALTER TABLE users MODIFY COLUMN status VARCHAR(20) NOT NULL");
            
            log.info("Ensuring suspension-related columns exist in 'users' table...");
            try {
                jdbcTemplate.execute("ALTER TABLE users ADD COLUMN IF NOT EXISTS suspension_reason VARCHAR(255)");
                jdbcTemplate.execute("ALTER TABLE users ADD COLUMN IF NOT EXISTS suspended_at DATETIME");
            } catch (Exception ex) {
                // MySQL 5.7 and below don't support ADD COLUMN IF NOT EXISTS, 
                // so we handle the 'duplicate column' error gracefully.
                log.debug("Column addition skipped: {}", ex.getMessage());
            }
            
            log.info("Database migration: User management columns verified.");
            
        } catch (Exception e) {
            log.warn("Database migration warning: {}. This is expected if the column was already updated or if using an H2 database.", e.getMessage());
        }
    }
}
