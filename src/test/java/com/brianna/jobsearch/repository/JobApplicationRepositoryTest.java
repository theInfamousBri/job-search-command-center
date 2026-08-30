package com.brianna.jobsearch.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.brianna.jobsearch.model.ApplicationState;
import com.brianna.jobsearch.model.ApplicationStatus;
import com.brianna.jobsearch.model.JobApplication;
import com.brianna.jobsearch.model.Priority;
import java.nio.file.Path;
import javax.sql.DataSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.sqlite.SQLiteDataSource;

class JobApplicationRepositoryTest {

    @TempDir Path tempDir;
    private JobApplicationRepository repository;

    @BeforeEach
    void setUp() {
        DataSource dataSource = sqlite(tempDir.resolve("applications.db"));
        new ResourceDatabasePopulator(new ClassPathResource("schema.sql")).execute(dataSource);
        repository = new JobApplicationRepository(new JdbcTemplate(dataSource));
    }

    @Test
    void requisitionIdRoundTripsAndParticipatesInApplicationSearch() {
        long id = repository.save(application("Mastercard", "Senior Software Engineer", "R-274666"));

        assertThat(repository.findById(id)).get()
                .extracting(JobApplication::getRequisitionId)
                .isEqualTo("R-274666");
        assertThat(repository.findAll("r-274666"))
                .extracting(JobApplication::getId)
                .containsExactly(id);
    }

    @Test
    void duplicateLookupMatchesCompanyAndRequisitionCaseInsensitivelyAndCanExcludeCurrentRecord() {
        long id = repository.save(application("Mastercard", "Senior Software Engineer", "R-274666"));
        repository.save(application("Other Company", "Engineer", "R-274666"));

        assertThat(repository.findDuplicateByCompanyAndRequisition("mastercard", "r-274666", null))
                .get().extracting(JobApplication::getId).isEqualTo(id);
        assertThat(repository.findDuplicateByCompanyAndRequisition("Mastercard", "R-274666", id)).isEmpty();
    }

    private JobApplication application(String company, String role, String requisitionId) {
        JobApplication application = new JobApplication();
        application.setCompany(company);
        application.setRole(role);
        application.setRequisitionId(requisitionId);
        application.setStatus(ApplicationStatus.APPLIED);
        application.setState(ApplicationState.ACTIVE);
        application.setPriority(Priority.HIGH);
        return application;
    }

    private DataSource sqlite(Path path) {
        SQLiteDataSource dataSource = new SQLiteDataSource();
        dataSource.setUrl("jdbc:sqlite:" + path);
        return dataSource;
    }
}
