package com.brianna.jobsearch.config;

import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
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
            Set<String> applicationColumns = new HashSet<>();
            columns.forEach(column -> applicationColumns.add(String.valueOf(column.get("name")).toLowerCase(Locale.ROOT)));

            addColumnIfMissing(jdbcTemplate, applicationColumns, "state", "TEXT NOT NULL DEFAULT 'ACTIVE'");
            addColumnIfMissing(jdbcTemplate, applicationColumns, "company_domain", "TEXT");
            addColumnIfMissing(jdbcTemplate, applicationColumns, "work_arrangement", "TEXT");
            addColumnIfMissing(jdbcTemplate, applicationColumns, "years_experience_required", "TEXT");
            addColumnIfMissing(jdbcTemplate, applicationColumns, "career_lane", "TEXT");
            addColumnIfMissing(jdbcTemplate, applicationColumns, "role_family", "TEXT");
            addColumnIfMissing(jdbcTemplate, applicationColumns, "industry_domain", "TEXT");
            addColumnIfMissing(jdbcTemplate, applicationColumns, "career_focus", "TEXT");
            addColumnIfMissing(jdbcTemplate, applicationColumns, "next_step", "TEXT");
            addColumnIfMissing(jdbcTemplate, applicationColumns, "cover_letter", "INTEGER");
            addColumnIfMissing(jdbcTemplate, applicationColumns, "cover_letter_text", "TEXT");
            addColumnIfMissing(jdbcTemplate, applicationColumns, "import_source", "TEXT");

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
            jdbcTemplate.execute("CREATE INDEX IF NOT EXISTS idx_job_applications_company_domain ON job_applications(company_domain)");
            jdbcTemplate.execute("CREATE INDEX IF NOT EXISTS idx_job_applications_career_lane ON job_applications(career_lane)");
            jdbcTemplate.execute("CREATE INDEX IF NOT EXISTS idx_job_applications_role_family ON job_applications(role_family)");
            jdbcTemplate.execute("CREATE INDEX IF NOT EXISTS idx_job_applications_industry_domain ON job_applications(industry_domain)");
            jdbcTemplate.execute("CREATE INDEX IF NOT EXISTS idx_job_applications_work_arrangement ON job_applications(work_arrangement)");

            jdbcTemplate.execute("""
                    CREATE TABLE IF NOT EXISTS company_logos (
                        domain TEXT PRIMARY KEY,
                        mime_type TEXT NOT NULL,
                        image_data BLOB NOT NULL,
                        source_url TEXT,
                        updated_at TEXT NOT NULL
                    )
                    """);

            jdbcTemplate.execute("""
                    CREATE TABLE IF NOT EXISTS company_notes (
                        company_key TEXT PRIMARY KEY,
                        notes TEXT NOT NULL,
                        updated_at TEXT NOT NULL
                    )
                    """);

            jdbcTemplate.execute("""
                    CREATE TABLE IF NOT EXISTS company_contacts (
                        id INTEGER PRIMARY KEY AUTOINCREMENT,
                        company_key TEXT NOT NULL,
                        name TEXT NOT NULL,
                        role TEXT,
                        relationship_type TEXT NOT NULL DEFAULT 'OTHER',
                        email TEXT,
                        linkedin_url TEXT,
                        notes TEXT,
                        photo_mime_type TEXT,
                        photo_data BLOB,
                        created_at TEXT NOT NULL,
                        updated_at TEXT NOT NULL
                    )
                    """);
            jdbcTemplate.execute("CREATE INDEX IF NOT EXISTS idx_company_contacts_company_key ON company_contacts(company_key)");
            jdbcTemplate.execute("CREATE INDEX IF NOT EXISTS idx_company_contacts_name ON company_contacts(name)");

            List<Map<String, Object>> prepColumns = jdbcTemplate.queryForList("PRAGMA table_info(prep_items)");
            Set<String> prepColumnNames = new HashSet<>();
            prepColumns.forEach(column -> prepColumnNames.add(String.valueOf(column.get("name")).toLowerCase(Locale.ROOT)));

            addColumnIfMissing(jdbcTemplate, prepColumnNames, "last_reviewed_at", "TEXT", "prep_items");
            addColumnIfMissing(jdbcTemplate, prepColumnNames, "review_count", "INTEGER NOT NULL DEFAULT 0", "prep_items");
            jdbcTemplate.execute("CREATE INDEX IF NOT EXISTS idx_prep_items_last_reviewed_at ON prep_items(last_reviewed_at)");
        };
    }

    private void addColumnIfMissing(JdbcTemplate jdbcTemplate, Set<String> names, String column, String definition) {
        addColumnIfMissing(jdbcTemplate, names, column, definition, "job_applications");
    }

    private void addColumnIfMissing(
            JdbcTemplate jdbcTemplate,
            Set<String> names,
            String column,
            String definition,
            String table) {
        if (names.add(column.toLowerCase(Locale.ROOT))) {
            jdbcTemplate.execute("ALTER TABLE " + table + " ADD COLUMN " + column + " " + definition);
        }
    }
}
