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
    private JdbcTemplate jdbc;

    @BeforeEach
    void setUp() {
        DataSource dataSource = sqlite(tempDir.resolve("applications.db"));
        new ResourceDatabasePopulator(new ClassPathResource("schema.sql")).execute(dataSource);
        jdbc = new JdbcTemplate(dataSource);
        repository = new JobApplicationRepository(jdbc);
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
    void globalSearchRanksExactRequisitionAheadOfOtherTextMatches() {
        long exactId = repository.save(application("Mastercard", "Senior Software Engineer", "R-274666"));
        repository.save(application("R-274666 Labs", "Backend Engineer", "OTHER-1"));

        assertThat(repository.searchGlobal("r-274666", 5))
                .extracting(JobApplication::getId)
                .startsWith(exactId);
    }

    @Test
    void duplicateLookupMatchesCompanyAndRequisitionCaseInsensitivelyAndCanExcludeCurrentRecord() {
        long id = repository.save(application("Mastercard", "Senior Software Engineer", "R-274666"));
        repository.save(application("Other Company", "Engineer", "R-274666"));

        assertThat(repository.findDuplicateByCompanyAndRequisition("mastercard", "r-274666", null))
                .get().extracting(JobApplication::getId).isEqualTo(id);
        assertThat(repository.findDuplicateByCompanyAndRequisition("Mastercard", "R-274666", id)).isEmpty();
    }

    @Test
    void staleQueueOnlyIncludesOldAppliedActiveApplicationsWithoutLifecycleProgress() {
        long ghostedId = repository.save(application("Ghosted Co", "Engineer", "GHOST-1"));
        long progressedId = repository.save(application("Progressed Co", "Engineer", "PROG-1"));

        JobApplication interviewing = application("Interview Co", "Engineer", "INT-STALE");
        interviewing.setStatus(ApplicationStatus.TECHNICAL_INTERVIEW);
        long interviewingId = repository.save(interviewing);

        String oldDate = java.time.LocalDate.now().minusDays(60).toString();
        String oldTimestamp = java.time.LocalDateTime.now().minusDays(60).toString();
        jdbc.update("UPDATE job_applications SET applied_date = ?, updated_at = ? WHERE id IN (?, ?, ?)",
                oldDate, oldTimestamp, ghostedId, progressedId, interviewingId);
        jdbc.update("""
                INSERT INTO application_events
                    (application_id, event_type, title, event_date, event_time, contact_name, notes, created_at)
                VALUES (?, 'RECRUITER_CONTACT', 'Recruiter reached out', ?, NULL, NULL, NULL, ?)
                """, progressedId, java.time.LocalDate.now().minusDays(55).toString(), oldTimestamp);

        assertThat(repository.findStale(45))
                .extracting(JobApplication::getId)
                .containsExactly(ghostedId);
        assertThat(repository.countStale(45)).isEqualTo(1);
    }

    @Test
    void metadataEditDoesNotResetStaleClosureClock() {
        long id = repository.save(application("Edited Ghost Co", "Engineer", "EDITED-OLD"));
        jdbc.update("UPDATE job_applications SET applied_date = ?, updated_at = ? WHERE id = ?",
                java.time.LocalDate.now().minusDays(70).toString(),
                java.time.LocalDateTime.now().toString(), id);

        assertThat(repository.findStale(45))
                .extracting(JobApplication::getId)
                .contains(id);
    }

    @Test
    void explicitStillActiveReviewDefersStaleClosureClock() {
        long id = repository.save(application("Still Active Co", "Engineer", "ACTIVE-OLD"));
        String appliedDate = java.time.LocalDate.now().minusDays(70).toString();
        String now = java.time.LocalDateTime.now().toString();
        jdbc.update("UPDATE job_applications SET applied_date = ? WHERE id = ?", appliedDate, id);
        jdbc.update("""
                INSERT INTO application_events
                    (application_id, event_type, title, event_date, event_time, contact_name, notes, created_at)
                VALUES (?, 'STILL_ACTIVE', 'Still active', ?, NULL, NULL, NULL, ?)
                """, id, java.time.LocalDate.now().toString(), now);

        assertThat(repository.findStale(45))
                .extracting(JobApplication::getId)
                .doesNotContain(id);
        assertThat(repository.countStale(45)).isZero();
    }

    @Test
    void attentionCandidatesIncludeInterviewFollowUpAndActiveAppliedButExcludeClosed() {
        JobApplication interviewing = application("Interview Co", "Engineer", "INT-1");
        interviewing.setStatus(ApplicationStatus.TECHNICAL_INTERVIEW);
        long interviewingId = repository.save(interviewing);

        JobApplication followUp = application("Follow Up Co", "Engineer", "FU-1");
        followUp.setState(ApplicationState.FOLLOW_UP_DUE);
        long followUpId = repository.save(followUp);

        JobApplication closed = application("Closed Co", "Engineer", "CLOSED-1");
        closed.setStatus(ApplicationStatus.FINAL_ROUND);
        closed.setState(ApplicationState.CLOSED);
        repository.save(closed);

        long ordinaryAppliedId = repository.save(application("Ordinary Co", "Engineer", "APPLIED-1"));

        JobApplication savedOnly = application("Saved Co", "Engineer", "SAVED-1");
        savedOnly.setStatus(ApplicationStatus.SAVED);
        repository.save(savedOnly);

        assertThat(repository.findAttentionCandidates())
                .extracting(JobApplication::getId)
                .containsExactlyInAnyOrder(interviewingId, followUpId, ordinaryAppliedId);
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
