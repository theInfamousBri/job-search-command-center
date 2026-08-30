package com.brianna.jobsearch.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Path;
import javax.sql.DataSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.sqlite.SQLiteDataSource;

class ApplicationContactRepositoryTest {

    @TempDir
    Path tempDir;

    private JdbcTemplate jdbc;
    private ApplicationContactRepository repository;

    @BeforeEach
    void setUp() {
        DataSource dataSource = sqlite(tempDir.resolve("application-contacts.db"));
        new ResourceDatabasePopulator(new ClassPathResource("schema.sql")).execute(dataSource);
        jdbc = new JdbcTemplate(dataSource);
        repository = new ApplicationContactRepository(jdbc);

        jdbc.update("""
                INSERT INTO job_applications (
                    id, company, role, status, state, priority, created_at, updated_at
                ) VALUES (1, 'Northstar Labs', 'Backend Engineer', 'APPLIED', 'ACTIVE', 'HIGH', ?, ?)
                """, "2026-08-01T12:00:00", "2026-08-02T12:00:00");
        jdbc.update("""
                INSERT INTO company_contacts (
                    id, company_key, name, role, relationship_type, created_at, updated_at
                ) VALUES
                    (10, 'northstar labs', 'Alex Morgan', 'Staff Engineer', 'INTERVIEWER', ?, ?),
                    (11, 'northstar labs', 'Taylor Reed', 'Technical Recruiter', 'RECRUITER', ?, ?),
                    (12, 'other company', 'Wrong Company', 'Recruiter', 'RECRUITER', ?, ?)
                """,
                "2026-08-01T12:00:00", "2026-08-01T12:00:00",
                "2026-08-01T12:00:00", "2026-08-01T12:00:00",
                "2026-08-01T12:00:00", "2026-08-01T12:00:00");
    }

    @Test
    void linksAreIdempotentAndApplicationQueriesStayCompanyScoped() {
        assertThat(repository.link(1L, 10L)).isTrue();
        assertThat(repository.link(1L, 10L)).isFalse();

        assertThat(repository.findByApplicationId(1L))
                .extracting("name")
                .containsExactly("Alex Morgan");
        assertThat(repository.findLinkableForApplication(1L, "northstar labs"))
                .extracting("name")
                .containsExactly("Taylor Reed");
    }

    @Test
    void unlinkAndCleanupRemoveOnlyRelationshipRows() {
        repository.link(1L, 10L);
        repository.link(1L, 11L);

        assertThat(repository.unlink(1L, 10L)).isEqualTo(1);
        assertThat(repository.findByApplicationId(1L)).extracting("id").containsExactly(11L);

        repository.deleteLinksByApplicationId(1L);
        assertThat(repository.findByApplicationId(1L)).isEmpty();
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM company_contacts", Long.class)).isEqualTo(3L);
    }

    private DataSource sqlite(Path path) {
        SQLiteDataSource dataSource = new SQLiteDataSource();
        dataSource.setUrl("jdbc:sqlite:" + path);
        return dataSource;
    }
}
