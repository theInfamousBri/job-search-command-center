package com.brianna.jobsearch.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import javax.sql.DataSource;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.sqlite.SQLiteDataSource;

class DatabaseMigrationTest {

    @TempDir
    Path tempDir;

    @Test
    void v12DatabaseMigratesToCurrentSchemaWithoutFakingApplicationActivity() throws Exception {
        DataSource dataSource = sqlite(tempDir.resolve("legacy-v12.db"));
        new ResourceDatabasePopulator(new ClassPathResource("db/v1.2-schema.sql")).execute(dataSource);
        JdbcTemplate jdbc = new JdbcTemplate(dataSource);
        String originalUpdatedAt = jdbc.queryForObject(
                "SELECT updated_at FROM job_applications WHERE id = 1", String.class);

        new DatabaseConfig().databaseMigrations(jdbc).run(null);

        Set<String> applicationColumns = columns(jdbc, "job_applications");
        assertThat(applicationColumns).contains("role_family", "industry_domain", "career_focus");
        assertThat(tableNames(jdbc)).contains(
                "company_notes", "company_contacts", "application_contact_links", "application_attachments",
                "material_files", "application_material_links");

        Map<String, Object> migrated = jdbc.queryForMap("""
                SELECT state, applied_date, updated_at
                FROM job_applications
                WHERE id = 1
                """);
        assertThat(migrated.get("state")).isEqualTo("CLOSED");
        assertThat(migrated.get("applied_date")).isEqualTo("2026-01-15");
        assertThat(migrated.get("updated_at")).isEqualTo(originalUpdatedAt);
    }

    @Test
    void v13ResumeAttachmentsMoveToSharedLibraryAndDeduplicateBytes() throws Exception {
        DataSource dataSource = sqlite(tempDir.resolve("legacy-v13-resumes.db"));
        new ResourceDatabasePopulator(new ClassPathResource("schema.sql")).execute(dataSource);
        JdbcTemplate jdbc = new JdbcTemplate(dataSource);

        String now = "2026-08-29T12:00:00";
        jdbc.update("""
                INSERT INTO job_applications (
                    id, company, role, status, state, priority, created_at, updated_at
                ) VALUES (1, 'Example Co', 'Backend Engineer', 'APPLIED', 'ACTIVE', 'MEDIUM', ?, ?)
                """, now, now);
        byte[] resumeBytes = "one physical resume".getBytes();
        jdbc.update("""
                INSERT INTO application_attachments (
                    application_id, attachment_type, file_name, mime_type, file_size, file_data, created_at
                ) VALUES (1, 'RESUME', 'backend-resume.pdf', 'application/pdf', ?, ?, ?)
                """, resumeBytes.length, resumeBytes, now);
        jdbc.update("""
                INSERT INTO application_attachments (
                    application_id, attachment_type, file_name, mime_type, file_size, file_data, created_at
                ) VALUES (1, 'RESUME', 'same-resume-copy.pdf', 'application/pdf', ?, ?, ?)
                """, resumeBytes.length, resumeBytes, now);
        String originalUpdatedAt = jdbc.queryForObject(
                "SELECT updated_at FROM job_applications WHERE id = 1", String.class);

        var migrations = new DatabaseConfig().databaseMigrations(jdbc);
        migrations.run(null);
        migrations.run(null);

        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM material_files", Long.class)).isEqualTo(1L);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM application_material_links", Long.class)).isEqualTo(1L);
        assertThat(jdbc.queryForObject(
                "SELECT COUNT(*) FROM application_attachments WHERE attachment_type = 'RESUME'", Long.class)).isZero();
        assertThat(jdbc.queryForObject(
                "SELECT file_data FROM material_files LIMIT 1", byte[].class)).containsExactly(resumeBytes);
        assertThat(jdbc.queryForObject(
                "SELECT updated_at FROM job_applications WHERE id = 1", String.class)).isEqualTo(originalUpdatedAt);
    }

    @Test
    void migrationsAreIdempotent() throws Exception {
        DataSource dataSource = sqlite(tempDir.resolve("idempotent.db"));
        new ResourceDatabasePopulator(new ClassPathResource("db/v1.2-schema.sql")).execute(dataSource);
        JdbcTemplate jdbc = new JdbcTemplate(dataSource);
        var migrations = new DatabaseConfig().databaseMigrations(jdbc);

        migrations.run(null);
        migrations.run(null);

        assertThat(columns(jdbc, "job_applications"))
                .contains("role_family", "industry_domain", "career_focus");
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM job_applications", Long.class)).isEqualTo(1L);
        assertThat(jdbc.queryForObject("SELECT updated_at FROM job_applications WHERE id = 1", String.class))
                .isEqualTo("2026-02-03T11:22:33");
    }

    private DataSource sqlite(Path path) {
        SQLiteDataSource dataSource = new SQLiteDataSource();
        dataSource.setUrl("jdbc:sqlite:" + path);
        return dataSource;
    }

    private Set<String> columns(JdbcTemplate jdbc, String table) {
        List<Map<String, Object>> rows = jdbc.queryForList("PRAGMA table_info(" + table + ")");
        return rows.stream()
                .map(row -> String.valueOf(row.get("name")).toLowerCase(Locale.ROOT))
                .collect(Collectors.toSet());
    }

    private Set<String> tableNames(JdbcTemplate jdbc) {
        return Set.copyOf(jdbc.queryForList(
                "SELECT name FROM sqlite_master WHERE type = 'table'", String.class));
    }
}
