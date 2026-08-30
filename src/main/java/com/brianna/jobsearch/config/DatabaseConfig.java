package com.brianna.jobsearch.config;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashSet;
import java.util.HexFormat;
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

            jdbcTemplate.execute("""
                    CREATE TABLE IF NOT EXISTS material_files (
                        id INTEGER PRIMARY KEY AUTOINCREMENT,
                        material_type TEXT NOT NULL DEFAULT 'RESUME',
                        display_name TEXT NOT NULL,
                        file_name TEXT NOT NULL,
                        mime_type TEXT NOT NULL,
                        file_size INTEGER NOT NULL,
                        sha256 TEXT NOT NULL UNIQUE,
                        file_data BLOB NOT NULL,
                        notes TEXT,
                        created_at TEXT NOT NULL,
                        updated_at TEXT NOT NULL
                    )
                    """);
            jdbcTemplate.execute("CREATE INDEX IF NOT EXISTS idx_material_files_type ON material_files(material_type)");
            jdbcTemplate.execute("CREATE INDEX IF NOT EXISTS idx_material_files_display_name ON material_files(display_name)");

            jdbcTemplate.execute("""
                    CREATE TABLE IF NOT EXISTS application_material_links (
                        application_id INTEGER NOT NULL,
                        material_id INTEGER NOT NULL,
                        created_at TEXT NOT NULL,
                        PRIMARY KEY (application_id, material_id)
                    )
                    """);
            jdbcTemplate.execute("CREATE INDEX IF NOT EXISTS idx_application_material_links_application ON application_material_links(application_id)");
            jdbcTemplate.execute("CREATE INDEX IF NOT EXISTS idx_application_material_links_material ON application_material_links(material_id)");

            jdbcTemplate.execute("""
                    CREATE TABLE IF NOT EXISTS application_attachments (
                        id INTEGER PRIMARY KEY AUTOINCREMENT,
                        application_id INTEGER NOT NULL,
                        attachment_type TEXT NOT NULL DEFAULT 'OTHER',
                        file_name TEXT NOT NULL,
                        mime_type TEXT NOT NULL,
                        file_size INTEGER NOT NULL,
                        file_data BLOB NOT NULL,
                        created_at TEXT NOT NULL
                    )
                    """);
            jdbcTemplate.execute("CREATE INDEX IF NOT EXISTS idx_application_attachments_application_id ON application_attachments(application_id)");
            migrateResumeAttachments(jdbcTemplate);

            List<Map<String, Object>> prepColumns = jdbcTemplate.queryForList("PRAGMA table_info(prep_items)");
            Set<String> prepColumnNames = new HashSet<>();
            prepColumns.forEach(column -> prepColumnNames.add(String.valueOf(column.get("name")).toLowerCase(Locale.ROOT)));

            addColumnIfMissing(jdbcTemplate, prepColumnNames, "last_reviewed_at", "TEXT", "prep_items");
            addColumnIfMissing(jdbcTemplate, prepColumnNames, "review_count", "INTEGER NOT NULL DEFAULT 0", "prep_items");
            jdbcTemplate.execute("CREATE INDEX IF NOT EXISTS idx_prep_items_last_reviewed_at ON prep_items(last_reviewed_at)");
        };
    }

    private void migrateResumeAttachments(JdbcTemplate jdbcTemplate) {
        List<Map<String, Object>> resumes = jdbcTemplate.queryForList("""
                SELECT id, application_id, file_name, mime_type, file_size, file_data, created_at
                FROM application_attachments
                WHERE attachment_type = 'RESUME'
                ORDER BY id
                """);

        for (Map<String, Object> resume : resumes) {
            byte[] data = (byte[]) resume.get("file_data");
            String hash = sha256(data);
            List<Long> existingIds = jdbcTemplate.queryForList(
                    "SELECT id FROM material_files WHERE sha256 = ?", Long.class, hash);

            long materialId;
            if (existingIds.isEmpty()) {
                String fileName = String.valueOf(resume.get("file_name"));
                String createdAt = String.valueOf(resume.get("created_at"));
                materialId = jdbcTemplate.queryForObject("""
                        INSERT INTO material_files (
                            material_type, display_name, file_name, mime_type, file_size,
                            sha256, file_data, notes, created_at, updated_at
                        ) VALUES ('RESUME', ?, ?, ?, ?, ?, ?, NULL, ?, ?)
                        RETURNING id
                        """, Long.class,
                        displayNameFromFileName(fileName),
                        fileName,
                        String.valueOf(resume.get("mime_type")),
                        ((Number) resume.get("file_size")).longValue(),
                        hash,
                        data,
                        createdAt,
                        createdAt);
            } else {
                materialId = existingIds.getFirst();
            }

            jdbcTemplate.update("""
                    INSERT OR IGNORE INTO application_material_links (application_id, material_id, created_at)
                    VALUES (?, ?, ?)
                    """,
                    ((Number) resume.get("application_id")).longValue(),
                    materialId,
                    String.valueOf(resume.get("created_at")));
        }

        if (!resumes.isEmpty()) {
            jdbcTemplate.update("DELETE FROM application_attachments WHERE attachment_type = 'RESUME'");
        }
    }

    private String displayNameFromFileName(String fileName) {
        if (fileName == null || fileName.isBlank()) {
            return "Resume";
        }
        int dot = fileName.lastIndexOf('.');
        String displayName = dot > 0 ? fileName.substring(0, dot) : fileName;
        return displayName.isBlank() ? "Resume" : displayName;
    }

    private String sha256(byte[] data) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(data));
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 is not available.", ex);
        }
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
