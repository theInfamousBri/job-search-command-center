package com.brianna.jobsearch.repository;

import com.brianna.jobsearch.model.ApplicationState;
import com.brianna.jobsearch.model.ApplicationStatus;
import com.brianna.jobsearch.model.JobApplication;
import com.brianna.jobsearch.model.Priority;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

@Repository
public class JobApplicationRepository {

    private final JdbcTemplate jdbcTemplate;

    private final RowMapper<JobApplication> rowMapper = (rs, rowNum) -> {
        JobApplication application = new JobApplication();
        application.setId(rs.getLong("id"));
        application.setCompany(rs.getString("company"));
        application.setRole(rs.getString("role"));
        application.setLocation(rs.getString("location"));
        application.setStatus(ApplicationStatus.valueOf(rs.getString("status")));
        String state = rs.getString("state");
        application.setState(state == null || state.isBlank() ? ApplicationState.ACTIVE : ApplicationState.valueOf(state));
        application.setPriority(Priority.valueOf(rs.getString("priority")));
        application.setSource(rs.getString("source"));
        application.setJobUrl(rs.getString("job_url"));
        application.setSalary(rs.getString("salary"));

        String appliedDate = rs.getString("applied_date");
        application.setAppliedDate(appliedDate == null || appliedDate.isBlank() ? null : LocalDate.parse(appliedDate));

        application.setNotes(rs.getString("notes"));
        application.setJobDescription(rs.getString("job_description"));

        String createdAt = rs.getString("created_at");
        String updatedAt = rs.getString("updated_at");
        application.setCreatedAt(createdAt == null ? null : LocalDateTime.parse(createdAt));
        application.setUpdatedAt(updatedAt == null ? null : LocalDateTime.parse(updatedAt));
        return application;
    };

    public JobApplicationRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<JobApplication> findAll(String query) {
        if (query == null || query.isBlank()) {
            return jdbcTemplate.query("""
                    SELECT *
                    FROM job_applications
                    ORDER BY updated_at DESC
                    """, rowMapper);
        }

        String like = "%" + query.trim() + "%";
        return jdbcTemplate.query("""
                SELECT *
                FROM job_applications
                WHERE LOWER(company) LIKE LOWER(?)
                   OR LOWER(role) LIKE LOWER(?)
                   OR LOWER(COALESCE(location, '')) LIKE LOWER(?)
                   OR LOWER(COALESCE(source, '')) LIKE LOWER(?)
                   OR LOWER(COALESCE(state, '')) LIKE LOWER(?)
                   OR LOWER(COALESCE(notes, '')) LIKE LOWER(?)
                   OR LOWER(COALESCE(job_description, '')) LIKE LOWER(?)
                ORDER BY updated_at DESC
                """, rowMapper, like, like, like, like, like, like, like);
    }

    public Optional<JobApplication> findById(long id) {
        return jdbcTemplate.query("""
                SELECT *
                FROM job_applications
                WHERE id = ?
                """, rowMapper, id).stream().findFirst();
    }

    public long save(JobApplication application) {
        String now = LocalDateTime.now().toString();
        KeyHolder keyHolder = new GeneratedKeyHolder();

        jdbcTemplate.update(connection -> {
            PreparedStatement statement = connection.prepareStatement("""
                    INSERT INTO job_applications (
                        company, role, location, status, state, priority, source, job_url,
                        salary, applied_date, notes, job_description, created_at, updated_at
                    ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                    """, Statement.RETURN_GENERATED_KEYS);

            statement.setString(1, application.getCompany().trim());
            statement.setString(2, application.getRole().trim());
            statement.setString(3, blankToNull(application.getLocation()));
            statement.setString(4, application.getStatus().name());
            statement.setString(5, application.getState().name());
            statement.setString(6, application.getPriority().name());
            statement.setString(7, blankToNull(application.getSource()));
            statement.setString(8, blankToNull(application.getJobUrl()));
            statement.setString(9, blankToNull(application.getSalary()));
            statement.setString(10, application.getAppliedDate() == null ? null : application.getAppliedDate().toString());
            statement.setString(11, blankToNull(application.getNotes()));
            statement.setString(12, blankToNull(application.getJobDescription()));
            statement.setString(13, now);
            statement.setString(14, now);
            return statement;
        }, keyHolder);

        Number key = keyHolder.getKey();
        if (key == null) {
            throw new IllegalStateException("SQLite did not return the new application id.");
        }
        return key.longValue();
    }

    public void update(JobApplication application) {
        jdbcTemplate.update("""
                UPDATE job_applications
                SET company = ?,
                    role = ?,
                    location = ?,
                    status = ?,
                    state = ?,
                    priority = ?,
                    source = ?,
                    job_url = ?,
                    salary = ?,
                    applied_date = ?,
                    notes = ?,
                    job_description = ?,
                    updated_at = ?
                WHERE id = ?
                """,
                application.getCompany().trim(),
                application.getRole().trim(),
                blankToNull(application.getLocation()),
                application.getStatus().name(),
                application.getState().name(),
                application.getPriority().name(),
                blankToNull(application.getSource()),
                blankToNull(application.getJobUrl()),
                blankToNull(application.getSalary()),
                application.getAppliedDate() == null ? null : application.getAppliedDate().toString(),
                blankToNull(application.getNotes()),
                blankToNull(application.getJobDescription()),
                LocalDateTime.now().toString(),
                application.getId());
    }

    public void touch(long id) {
        jdbcTemplate.update(
                "UPDATE job_applications SET updated_at = ? WHERE id = ?",
                LocalDateTime.now().toString(),
                id);
    }

    public void delete(long id) {
        jdbcTemplate.update("DELETE FROM job_applications WHERE id = ?", id);
    }

    public long countAll() {
        return valueOrZero(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM job_applications", Long.class));
    }

    public long countActive() {
        return valueOrZero(jdbcTemplate.queryForObject("""
                SELECT COUNT(*)
                FROM job_applications
                WHERE status IN ('APPLIED', 'RECRUITER_SCREEN', 'HIRING_MANAGER',
                                 'TECHNICAL_INTERVIEW', 'FINAL_ROUND', 'OFFER')
                  AND state <> 'CLOSED'
                """, Long.class));
    }

    public long countInterviewing() {
        return valueOrZero(jdbcTemplate.queryForObject("""
                SELECT COUNT(*)
                FROM job_applications
                WHERE status IN ('RECRUITER_SCREEN', 'HIRING_MANAGER',
                                 'TECHNICAL_INTERVIEW', 'FINAL_ROUND')
                  AND state <> 'CLOSED'
                """, Long.class));
    }

    public long countOffers() {
        return valueOrZero(jdbcTemplate.queryForObject("""
                SELECT COUNT(*)
                FROM job_applications
                WHERE status = 'OFFER'
                """, Long.class));
    }

    public long countResponded() {
        return valueOrZero(jdbcTemplate.queryForObject("""
                SELECT COUNT(*)
                FROM job_applications
                WHERE status IN ('RECRUITER_SCREEN', 'HIRING_MANAGER', 'TECHNICAL_INTERVIEW',
                                 'FINAL_ROUND', 'OFFER', 'REJECTED')
                """, Long.class));
    }

    public List<JobApplication> findRecent(int limit) {
        return jdbcTemplate.query("""
                SELECT *
                FROM job_applications
                ORDER BY updated_at DESC
                LIMIT ?
                """, rowMapper, limit);
    }

    public List<JobApplication> findNeedsAttention(int limit) {
        return jdbcTemplate.query("""
                SELECT *
                FROM job_applications
                WHERE (
                        state = 'FOLLOW_UP_DUE'
                        OR (
                            state IN ('ACTIVE', 'AWAITING_FEEDBACK')
                            AND status IN ('APPLIED', 'RECRUITER_SCREEN', 'HIRING_MANAGER',
                                           'TECHNICAL_INTERVIEW', 'FINAL_ROUND')
                            AND datetime(updated_at) <= datetime('now', '-7 days')
                        )
                      )
                ORDER BY CASE WHEN state = 'FOLLOW_UP_DUE' THEN 0 ELSE 1 END,
                         updated_at ASC
                LIMIT ?
                """, rowMapper, limit);
    }

    private long valueOrZero(Long value) {
        return value == null ? 0L : value;
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
