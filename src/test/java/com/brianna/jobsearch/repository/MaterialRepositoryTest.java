package com.brianna.jobsearch.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.brianna.jobsearch.model.MaterialType;
import java.nio.file.Path;
import javax.sql.DataSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.sqlite.SQLiteDataSource;

class MaterialRepositoryTest {

    @TempDir
    Path tempDir;

    private JdbcTemplate jdbc;
    private MaterialRepository repository;

    @BeforeEach
    void setUp() {
        DataSource dataSource = sqlite(tempDir.resolve("materials.db"));
        new ResourceDatabasePopulator(new ClassPathResource("schema.sql")).execute(dataSource);
        jdbc = new JdbcTemplate(dataSource);
        repository = new MaterialRepository(jdbc);
        insertApplication(1L, "Northstar Labs", "Backend Engineer");
        insertApplication(2L, "Atlas Payments", "Software Engineer III");
    }

    @Test
    void metadataQueriesNeverNeedBlobContentAndLinksAreReusable() {
        byte[] bytes = "resume-v1".getBytes();
        long id = repository.insert(
                MaterialType.RESUME,
                "Backend Resume",
                "backend.pdf",
                "application/pdf",
                "hash-one",
                bytes,
                "Java-focused version");

        assertThat(repository.link(1L, id)).isTrue();
        assertThat(repository.link(2L, id)).isTrue();
        assertThat(repository.link(2L, id)).isFalse();

        var metadata = repository.findMetadata(id).orElseThrow();
        assertThat(metadata.displayName()).isEqualTo("Backend Resume");
        assertThat(metadata.linkedApplicationCount()).isEqualTo(2);
        assertThat(repository.findByApplicationId(1L)).extracting("id").containsExactly(id);
        assertThat(repository.findLinkableForApplication(1L)).isEmpty();
        assertThat(repository.findApplications(id)).extracting("applicationId").containsExactly(2L, 1L);
    }

    @Test
    void contentIsLoadedOnlyByExplicitContentQuery() {
        byte[] bytes = "resume-binary".getBytes();
        long id = repository.insert(
                MaterialType.RESUME, "Resume", "resume.pdf", "application/pdf", "hash-two", bytes, null);

        assertThat(repository.findContent(id))
                .hasValueSatisfying(content -> assertThat(content.data()).containsExactly(bytes));
    }

    @Test
    void summaryReportsPhysicalStorageAndAvoidedDuplicateStorage() {
        byte[] bytes = new byte[2048];
        long id = repository.insert(
                MaterialType.RESUME, "Shared Resume", "resume.pdf", "application/pdf", "hash-three", bytes, null);
        repository.link(1L, id);
        repository.link(2L, id);

        var summary = repository.summary();

        assertThat(summary.materials()).isEqualTo(1);
        assertThat(summary.resumes()).isEqualTo(1);
        assertThat(summary.applicationLinks()).isEqualTo(2);
        assertThat(summary.storedBytes()).isEqualTo(2048);
        assertThat(summary.avoidedDuplicateBytes()).isEqualTo(2048);
    }

    private void insertApplication(long id, String company, String role) {
        jdbc.update("""
                INSERT INTO job_applications (
                    id, company, role, status, state, priority, created_at, updated_at
                ) VALUES (?, ?, ?, 'APPLIED', 'ACTIVE', 'MEDIUM', '2026-08-01T12:00:00', ?)
                """, id, company, role, id == 1 ? "2026-08-02T12:00:00" : "2026-08-03T12:00:00");
    }

    private DataSource sqlite(Path path) {
        SQLiteDataSource dataSource = new SQLiteDataSource();
        dataSource.setUrl("jdbc:sqlite:" + path);
        return dataSource;
    }
}
