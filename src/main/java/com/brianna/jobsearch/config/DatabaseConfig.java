package com.brianna.jobsearch.config;

import java.util.List;
import java.util.Map;
import javax.sql.DataSource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.sqlite.SQLiteDataSource;

@Configuration
public class DatabaseConfig {

    @Bean
    DataSource dataSource(@Value("${app.database.path:jobsearch.db}") String databasePath) {
        SQLiteDataSource dataSource = new SQLiteDataSource();
        dataSource.setUrl("jdbc:sqlite:" + databasePath);
        return dataSource;
    }

    /**
     * Lightweight, local SQLite migrations for columns added after the first version.
     * PRAGMA table_info lets this stay safe to run on every startup.
     */
    @Bean
    @Order(10)
    ApplicationRunner databaseMigrations(JdbcTemplate jdbcTemplate) {
        return args -> {
            List<Map<String, Object>> columns = jdbcTemplate.queryForList("PRAGMA table_info(job_applications)");
            boolean hasState = columns.stream()
                    .anyMatch(column -> "state".equalsIgnoreCase(String.valueOf(column.get("name"))));

            if (!hasState) {
                jdbcTemplate.execute("ALTER TABLE job_applications ADD COLUMN state TEXT NOT NULL DEFAULT 'ACTIVE'");
            }

            // Terminal outcomes are closed by definition. This also tidies data created before
            // the State field existed without changing active/interviewing applications.
            jdbcTemplate.update("""
                    UPDATE job_applications
                    SET state = 'CLOSED'
                    WHERE status IN ('REJECTED', 'WITHDRAWN', 'NO_RESPONSE')
                      AND state <> 'CLOSED'
                    """);

            // Older Thymeleaf date rendering could blank the applied date when editing an
            // application. Recover it from the application's preserved Applied timeline event.
            jdbcTemplate.update("""
                    UPDATE job_applications
                    SET applied_date = (
                        SELECT MIN(ae.event_date)
                        FROM application_events ae
                        WHERE ae.application_id = job_applications.id
                          AND ae.event_type = 'APPLIED'
                    )
                    WHERE (applied_date IS NULL OR TRIM(applied_date) = '')
                      AND EXISTS (
                          SELECT 1
                          FROM application_events ae
                          WHERE ae.application_id = job_applications.id
                            AND ae.event_type = 'APPLIED'
                      )
                    """);

            jdbcTemplate.execute("CREATE INDEX IF NOT EXISTS idx_job_applications_state ON job_applications(state)");

            List<Map<String, Object>> prepColumns = jdbcTemplate.queryForList("PRAGMA table_info(prep_items)");
            boolean hasLastReviewedAt = prepColumns.stream()
                    .anyMatch(column -> "last_reviewed_at".equalsIgnoreCase(String.valueOf(column.get("name"))));
            boolean hasReviewCount = prepColumns.stream()
                    .anyMatch(column -> "review_count".equalsIgnoreCase(String.valueOf(column.get("name"))));

            if (!hasLastReviewedAt) {
                jdbcTemplate.execute("ALTER TABLE prep_items ADD COLUMN last_reviewed_at TEXT");
            }
            if (!hasReviewCount) {
                jdbcTemplate.execute("ALTER TABLE prep_items ADD COLUMN review_count INTEGER NOT NULL DEFAULT 0");
            }

            jdbcTemplate.execute("CREATE INDEX IF NOT EXISTS idx_prep_items_last_reviewed_at ON prep_items(last_reviewed_at)");
        };
    }
}
