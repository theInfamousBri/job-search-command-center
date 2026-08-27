package com.brianna.jobsearch.repository;

import com.brianna.jobsearch.model.ApplicationPage;
import com.brianna.jobsearch.model.ApplicationSearchCriteria;
import com.brianna.jobsearch.model.ApplicationSort;
import com.brianna.jobsearch.model.ApplicationState;
import com.brianna.jobsearch.model.ApplicationStatus;
import com.brianna.jobsearch.model.JobApplication;
import com.brianna.jobsearch.model.Priority;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
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
        application.setWorkArrangement(rs.getString("work_arrangement"));
        application.setYearsExperienceRequired(rs.getString("years_experience_required"));
        application.setCareerLane(rs.getString("career_lane"));
        application.setStatus(ApplicationStatus.valueOf(rs.getString("status")));
        String state = rs.getString("state");
        application.setState(state == null || state.isBlank() ? ApplicationState.ACTIVE : ApplicationState.valueOf(state));
        String priority = rs.getString("priority");
        application.setPriority(priority == null || priority.isBlank() ? Priority.UNSPECIFIED : Priority.valueOf(priority));
        application.setSource(rs.getString("source"));
        application.setJobUrl(rs.getString("job_url"));
        application.setSalary(rs.getString("salary"));

        String appliedDate = rs.getString("applied_date");
        application.setAppliedDate(appliedDate == null || appliedDate.isBlank() ? null : LocalDate.parse(appliedDate));

        application.setNextStep(rs.getString("next_step"));
        Object coverLetter = rs.getObject("cover_letter");
        application.setCoverLetter(coverLetter == null ? null : rs.getInt("cover_letter") != 0);
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
                   OR LOWER(COALESCE(work_arrangement, '')) LIKE LOWER(?)
                   OR LOWER(COALESCE(years_experience_required, '')) LIKE LOWER(?)
                   OR LOWER(COALESCE(career_lane, '')) LIKE LOWER(?)
                   OR LOWER(COALESCE(source, '')) LIKE LOWER(?)
                   OR LOWER(COALESCE(state, '')) LIKE LOWER(?)
                   OR LOWER(COALESCE(next_step, '')) LIKE LOWER(?)
                   OR LOWER(COALESCE(notes, '')) LIKE LOWER(?)
                   OR LOWER(COALESCE(job_description, '')) LIKE LOWER(?)
                ORDER BY updated_at DESC
                """, rowMapper, like, like, like, like, like, like, like, like, like, like, like);
    }

    public ApplicationPage findPage(ApplicationSearchCriteria criteria) {
        StringBuilder where = new StringBuilder(" WHERE 1 = 1");
        List<Object> params = new ArrayList<>();

        if (criteria.getQuery() != null) {
            String like = "%" + criteria.getQuery() + "%";
            where.append("""
                     AND (
                         LOWER(company) LIKE LOWER(?)
                         OR LOWER(role) LIKE LOWER(?)
                         OR LOWER(COALESCE(location, '')) LIKE LOWER(?)
                         OR LOWER(COALESCE(work_arrangement, '')) LIKE LOWER(?)
                         OR LOWER(COALESCE(years_experience_required, '')) LIKE LOWER(?)
                         OR LOWER(COALESCE(career_lane, '')) LIKE LOWER(?)
                         OR LOWER(COALESCE(source, '')) LIKE LOWER(?)
                         OR LOWER(COALESCE(next_step, '')) LIKE LOWER(?)
                         OR LOWER(COALESCE(notes, '')) LIKE LOWER(?)
                         OR LOWER(COALESCE(job_description, '')) LIKE LOWER(?)
                     )
                    """);
            for (int i = 0; i < 10; i++) {
                params.add(like);
            }
        }
        if (criteria.getStatus() != null) {
            where.append(" AND status = ?");
            params.add(criteria.getStatus().name());
        }
        if (criteria.getState() != null) {
            where.append(" AND state = ?");
            params.add(criteria.getState().name());
        }
        if (criteria.getPriority() != null) {
            where.append(" AND priority = ?");
            params.add(criteria.getPriority().name());
        }
        addExactTextFilter(where, params, "work_arrangement", criteria.getWorkArrangement());
        addExactTextFilter(where, params, "source", criteria.getSource());
        addExactTextFilter(where, params, "career_lane", criteria.getCareerLane());
        if (criteria.getAppliedFrom() != null) {
            where.append(" AND applied_date >= ?");
            params.add(criteria.getAppliedFrom().toString());
        }
        if (criteria.getAppliedTo() != null) {
            where.append(" AND applied_date <= ?");
            params.add(criteria.getAppliedTo().toString());
        }

        Long count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM job_applications" + where,
                Long.class,
                params.toArray());
        long total = valueOrZero(count);

        int page = criteria.getPage();
        int size = criteria.getSize();
        int totalPages = total == 0 ? 0 : (int) Math.ceil(total / (double) size);
        if (totalPages > 0 && page >= totalPages) {
            page = totalPages - 1;
        }

        List<Object> pageParams = new ArrayList<>(params);
        pageParams.add(size);
        pageParams.add(page * size);
        List<JobApplication> applications = jdbcTemplate.query(
                "SELECT * FROM job_applications" + where + " ORDER BY " + orderBy(criteria.getSort()) + " LIMIT ? OFFSET ?",
                rowMapper,
                pageParams.toArray());

        return new ApplicationPage(applications, total, page, size);
    }

    public List<String> findWorkArrangements() {
        return distinctValues("work_arrangement");
    }

    public List<String> findSources() {
        return distinctValues("source");
    }

    public List<String> findCareerLanes() {
        return distinctValues("career_lane");
    }

    private List<String> distinctValues(String column) {
        if (!List.of("work_arrangement", "source", "career_lane").contains(column)) {
            throw new IllegalArgumentException("Unsupported filter column: " + column);
        }
        return jdbcTemplate.queryForList(
                "SELECT DISTINCT TRIM(" + column + ") FROM job_applications " +
                        "WHERE " + column + " IS NOT NULL AND TRIM(" + column + ") <> '' " +
                        "ORDER BY LOWER(TRIM(" + column + ")) ASC",
                String.class);
    }

    private void addExactTextFilter(StringBuilder where, List<Object> params, String column, String value) {
        if (value == null || value.isBlank()) {
            return;
        }
        where.append(" AND LOWER(TRIM(COALESCE(").append(column).append(", ''))) = LOWER(?)");
        params.add(value.trim());
    }

    private String orderBy(ApplicationSort sort) {
        ApplicationSort safeSort = sort == null ? ApplicationSort.UPDATED_DESC : sort;
        return switch (safeSort) {
            case UPDATED_ASC -> "updated_at ASC, id ASC";
            case APPLIED_DESC -> "CASE WHEN applied_date IS NULL THEN 1 ELSE 0 END, applied_date DESC, updated_at DESC";
            case APPLIED_ASC -> "CASE WHEN applied_date IS NULL THEN 1 ELSE 0 END, applied_date ASC, updated_at ASC";
            case COMPANY_ASC -> "LOWER(company) ASC, LOWER(role) ASC, id ASC";
            case UPDATED_DESC -> "updated_at DESC, id DESC";
        };
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
        return insert(application, now, now, null);
    }

    public long saveImported(JobApplication application, LocalDateTime createdAt, LocalDateTime updatedAt, String importSource) {
        return insert(application, createdAt.toString(), updatedAt.toString(), importSource);
    }

    private long insert(JobApplication application, String createdAt, String updatedAt, String importSource) {
        KeyHolder keyHolder = new GeneratedKeyHolder();

        jdbcTemplate.update(connection -> {
            PreparedStatement statement = connection.prepareStatement("""
                    INSERT INTO job_applications (
                        company, role, location, work_arrangement, years_experience_required,
                        career_lane, status, state, priority, source, job_url, salary,
                        applied_date, next_step, cover_letter, notes, job_description,
                        import_source, created_at, updated_at
                    ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                    """, Statement.RETURN_GENERATED_KEYS);

            bindApplication(statement, application);
            statement.setString(18, blankToNull(importSource));
            statement.setString(19, createdAt);
            statement.setString(20, updatedAt);
            return statement;
        }, keyHolder);

        Number key = keyHolder.getKey();
        if (key == null) {
            throw new IllegalStateException("SQLite did not return the new application id.");
        }
        return key.longValue();
    }

    private void bindApplication(PreparedStatement statement, JobApplication application) throws java.sql.SQLException {
        statement.setString(1, application.getCompany().trim());
        statement.setString(2, application.getRole().trim());
        statement.setString(3, blankToNull(application.getLocation()));
        statement.setString(4, blankToNull(application.getWorkArrangement()));
        statement.setString(5, blankToNull(application.getYearsExperienceRequired()));
        statement.setString(6, blankToNull(application.getCareerLane()));
        statement.setString(7, application.getStatus().name());
        statement.setString(8, application.getState().name());
        statement.setString(9, application.getPriority().name());
        statement.setString(10, blankToNull(application.getSource()));
        statement.setString(11, blankToNull(application.getJobUrl()));
        statement.setString(12, blankToNull(application.getSalary()));
        statement.setString(13, application.getAppliedDate() == null ? null : application.getAppliedDate().toString());
        statement.setString(14, blankToNull(application.getNextStep()));
        if (application.getCoverLetter() == null) {
            statement.setObject(15, null);
        } else {
            statement.setInt(15, application.getCoverLetter() ? 1 : 0);
        }
        statement.setString(16, blankToNull(application.getNotes()));
        statement.setString(17, blankToNull(application.getJobDescription()));
    }

    public void update(JobApplication application) {
        updateWithTimestamp(application, LocalDateTime.now());
    }

    public void updatePreservingTimestamp(JobApplication application, LocalDateTime updatedAt) {
        updateWithTimestamp(application, updatedAt == null ? LocalDateTime.now() : updatedAt);
    }

    private void updateWithTimestamp(JobApplication application, LocalDateTime updatedAt) {
        jdbcTemplate.update("""
                UPDATE job_applications
                SET company = ?,
                    role = ?,
                    location = ?,
                    work_arrangement = ?,
                    years_experience_required = ?,
                    career_lane = ?,
                    status = ?,
                    state = ?,
                    priority = ?,
                    source = ?,
                    job_url = ?,
                    salary = ?,
                    applied_date = ?,
                    next_step = ?,
                    cover_letter = ?,
                    notes = ?,
                    job_description = ?,
                    updated_at = ?
                WHERE id = ?
                """,
                application.getCompany().trim(),
                application.getRole().trim(),
                blankToNull(application.getLocation()),
                blankToNull(application.getWorkArrangement()),
                blankToNull(application.getYearsExperienceRequired()),
                blankToNull(application.getCareerLane()),
                application.getStatus().name(),
                application.getState().name(),
                application.getPriority().name(),
                blankToNull(application.getSource()),
                blankToNull(application.getJobUrl()),
                blankToNull(application.getSalary()),
                application.getAppliedDate() == null ? null : application.getAppliedDate().toString(),
                blankToNull(application.getNextStep()),
                application.getCoverLetter() == null ? null : (application.getCoverLetter() ? 1 : 0),
                blankToNull(application.getNotes()),
                blankToNull(application.getJobDescription()),
                updatedAt.toString(),
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
                WHERE status IN ('APPLIED', 'RECRUITER_SCREEN', 'ASSESSMENT', 'HIRING_MANAGER',
                                 'TECHNICAL_INTERVIEW', 'FINAL_ROUND', 'OFFER')
                  AND state <> 'CLOSED'
                """, Long.class));
    }

    public long countInterviewing() {
        return valueOrZero(jdbcTemplate.queryForObject("""
                SELECT COUNT(*)
                FROM job_applications
                WHERE status IN ('RECRUITER_SCREEN', 'ASSESSMENT', 'HIRING_MANAGER',
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
                SELECT COUNT(DISTINCT application_id)
                FROM application_events
                WHERE event_type IN ('RECRUITER_CONTACT', 'CODING_ASSESSMENT', 'TAKE_HOME_ASSESSMENT',
                                     'RECRUITER_SCREEN', 'HIRING_MANAGER', 'TECHNICAL_INTERVIEW',
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
        return findNeedsAttention(limit, 21);
    }

    public List<JobApplication> findNeedsAttention(int limit, int staleDays) {
        return jdbcTemplate.query("""
                SELECT *
                FROM job_applications
                WHERE (
                        state = 'FOLLOW_UP_DUE'
                        OR (
                            state IN ('ACTIVE', 'AWAITING_FEEDBACK')
                            AND status IN ('APPLIED', 'RECRUITER_SCREEN', 'ASSESSMENT', 'HIRING_MANAGER',
                                           'TECHNICAL_INTERVIEW', 'FINAL_ROUND')
                            AND datetime(updated_at) <= datetime('now', ?)
                        )
                      )
                ORDER BY CASE WHEN state = 'FOLLOW_UP_DUE' THEN 0 ELSE 1 END,
                         updated_at ASC
                LIMIT ?
                """, rowMapper, "-" + Math.max(1, staleDays) + " days", limit);
    }

    public long countStale(int staleDays) {
        return valueOrZero(jdbcTemplate.queryForObject("""
                SELECT COUNT(*)
                FROM job_applications
                WHERE state IN ('ACTIVE', 'AWAITING_FEEDBACK')
                  AND status IN ('APPLIED', 'RECRUITER_SCREEN', 'ASSESSMENT', 'HIRING_MANAGER',
                                 'TECHNICAL_INTERVIEW', 'FINAL_ROUND')
                  AND datetime(updated_at) <= datetime('now', ?)
                """, Long.class, "-" + Math.max(1, staleDays) + " days"));
    }

    public List<JobApplication> findStale(int staleDays) {
        return jdbcTemplate.query("""
                SELECT *
                FROM job_applications
                WHERE state IN ('ACTIVE', 'AWAITING_FEEDBACK')
                  AND status IN ('APPLIED', 'RECRUITER_SCREEN', 'ASSESSMENT', 'HIRING_MANAGER',
                                 'TECHNICAL_INTERVIEW', 'FINAL_ROUND')
                  AND datetime(updated_at) <= datetime('now', ?)
                ORDER BY updated_at ASC, company ASC
                """, rowMapper, "-" + Math.max(1, staleDays) + " days");
    }

    private long valueOrZero(Long value) {
        return value == null ? 0L : value;
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
