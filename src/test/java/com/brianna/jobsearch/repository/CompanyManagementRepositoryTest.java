package com.brianna.jobsearch.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.brianna.jobsearch.model.CompanyContact;
import java.nio.file.Path;
import javax.sql.DataSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.sqlite.SQLiteDataSource;

class CompanyManagementRepositoryTest {

    @TempDir
    Path tempDir;

    private JdbcTemplate jdbc;
    private CompanyManagementRepository repository;

    @BeforeEach
    void setUp() {
        DataSource dataSource = sqlite(tempDir.resolve("companies.db"));
        new ResourceDatabasePopulator(new ClassPathResource("schema.sql")).execute(dataSource);
        jdbc = new JdbcTemplate(dataSource);
        repository = new CompanyManagementRepository(jdbc);
        jdbc.update("""
                INSERT INTO job_applications (
                    id, company, role, status, state, priority, created_at, updated_at
                ) VALUES (1, 'Northstar Labs', 'Backend Engineer', 'APPLIED', 'ACTIVE', 'HIGH', ?, ?)
                """, "2026-08-01T12:00:00", "2026-08-02T12:00:00");
        jdbc.update("""
                INSERT INTO company_contacts (
                    id, company_key, name, role, relationship_type, created_at, updated_at
                ) VALUES (7, 'northstar labs', 'Alex Morgan', 'Staff Engineer', 'INTERVIEWER', ?, ?)
                """, "2026-08-01T12:00:00", "2026-08-01T12:00:00");
        jdbc.update("""
                INSERT INTO application_contact_links (application_id, contact_id, created_at)
                VALUES (1, 7, ?)
                """, "2026-08-02T12:00:00");
    }

    @Test
    void contactMetadataReportsHowManyApplicationsReferencePerson() {
        var contact = repository.findContact(7L);

        assertThat(contact).isNotNull();
        assertThat(contact.linkedApplicationCount()).isEqualTo(1L);
        assertThat(repository.findContacts("northstar labs"))
                .extracting(CompanyContact::linkedApplicationCount)
                .containsExactly(1L);
    }

    @Test
    void deletingPersonAlsoCleansApplicationLinksWithoutDeletingApplication() {
        assertThat(repository.deleteContact(7L)).isEqualTo(1);

        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM application_contact_links", Long.class)).isZero();
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM job_applications WHERE id = 1", Long.class)).isEqualTo(1L);
    }

    private DataSource sqlite(Path path) {
        SQLiteDataSource dataSource = new SQLiteDataSource();
        dataSource.setUrl("jdbc:sqlite:" + path);
        return dataSource;
    }
}
