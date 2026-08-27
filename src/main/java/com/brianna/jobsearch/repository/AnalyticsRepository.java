package com.brianna.jobsearch.repository;

import java.time.LocalDate;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class AnalyticsRepository {

    private static final String RESPONSE_EVENT_TYPES = """
            'RECRUITER_CONTACT', 'CODING_ASSESSMENT', 'TAKE_HOME_ASSESSMENT',
            'RECRUITER_SCREEN', 'HIRING_MANAGER', 'TECHNICAL_INTERVIEW',
            'FINAL_ROUND', 'OFFER', 'REJECTED'
            """;

    private static final String INTERVIEW_EVENT_TYPES = """
            'RECRUITER_SCREEN', 'HIRING_MANAGER', 'TECHNICAL_INTERVIEW', 'FINAL_ROUND'
            """;

    private final JdbcTemplate jdbcTemplate;

    public AnalyticsRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public long countAppliedApplications() {
        return valueOrZero(jdbcTemplate.queryForObject("""
                SELECT COUNT(DISTINCT application_id)
                FROM application_events
                WHERE event_type = 'APPLIED'
                """, Long.class));
    }

    public long countRespondedApplications() {
        return valueOrZero(jdbcTemplate.queryForObject("""
                SELECT COUNT(DISTINCT ae.application_id)
                FROM application_events ae
                WHERE ae.event_type IN (""" + RESPONSE_EVENT_TYPES + ")", Long.class));
    }

    public long countInterviewedApplications() {
        return valueOrZero(jdbcTemplate.queryForObject("""
                SELECT COUNT(DISTINCT ae.application_id)
                FROM application_events ae
                WHERE ae.event_type IN (""" + INTERVIEW_EVENT_TYPES + ")", Long.class));
    }

    public long countApplicationsThatReached(String eventType) {
        return valueOrZero(jdbcTemplate.queryForObject("""
                SELECT COUNT(DISTINCT application_id)
                FROM application_events
                WHERE event_type = ?
                """, Long.class, eventType));
    }

    public Double averageDaysToFirstResponse() {
        return jdbcTemplate.queryForObject("""
                WITH applied AS (
                    SELECT application_id, MIN(event_date) AS applied_date
                    FROM application_events
                    WHERE event_type = 'APPLIED'
                    GROUP BY application_id
                ),
                response AS (
                    SELECT application_id, MIN(event_date) AS response_date
                    FROM application_events
                    WHERE event_type IN (""" + RESPONSE_EVENT_TYPES + """
                    )
                    GROUP BY application_id
                )
                SELECT AVG(julianday(response.response_date) - julianday(applied.applied_date))
                FROM applied
                JOIN response ON response.application_id = applied.application_id
                WHERE response.response_date >= applied.applied_date
                """, Double.class);
    }

    public StageTiming averageDaysToStage(String eventType) {
        return jdbcTemplate.queryForObject("""
                WITH applied AS (
                    SELECT application_id, MIN(event_date) AS applied_date
                    FROM application_events
                    WHERE event_type = 'APPLIED'
                    GROUP BY application_id
                ),
                stage AS (
                    SELECT application_id, MIN(event_date) AS stage_date
                    FROM application_events
                    WHERE event_type = ?
                    GROUP BY application_id
                )
                SELECT AVG(julianday(stage.stage_date) - julianday(applied.applied_date)) AS average_days,
                       COUNT(*) AS sample_size
                FROM applied
                JOIN stage ON stage.application_id = applied.application_id
                WHERE stage.stage_date >= applied.applied_date
                """, (rs, rowNum) -> new StageTiming(
                        nullableDouble(rs.getObject("average_days")),
                        rs.getLong("sample_size")),
                eventType);
    }

    public List<StateCount> stateCounts() {
        return jdbcTemplate.query("""
                SELECT state, COUNT(*) AS total
                FROM job_applications
                WHERE state <> 'CLOSED'
                GROUP BY state
                ORDER BY total DESC, state ASC
                """, (rs, rowNum) -> new StateCount(rs.getString("state"), rs.getLong("total")));
    }

    public List<OutcomeCount> outcomeCounts() {
        return jdbcTemplate.query("""
                SELECT outcome, COUNT(*) AS total
                FROM (
                    SELECT CASE
                        WHEN status = 'NO_RESPONSE' THEN 'NO_RESPONSE'
                        WHEN status = 'REJECTED' THEN 'REJECTED'
                        WHEN status = 'WITHDRAWN' THEN 'WITHDRAWN'
                        WHEN status = 'OFFER' THEN 'OFFER'
                        WHEN status IN ('RECRUITER_SCREEN', 'ASSESSMENT', 'HIRING_MANAGER', 'TECHNICAL_INTERVIEW', 'FINAL_ROUND')
                             AND state <> 'CLOSED' THEN 'INTERVIEWING'
                        WHEN state <> 'CLOSED' THEN 'ACTIVE'
                        ELSE 'OTHER_CLOSED'
                    END AS outcome
                    FROM job_applications ja
                    WHERE EXISTS (
                        SELECT 1 FROM application_events applied
                        WHERE applied.application_id = ja.id AND applied.event_type = 'APPLIED'
                    )
                )
                GROUP BY outcome
                ORDER BY total DESC, outcome ASC
                """, (rs, rowNum) -> new OutcomeCount(rs.getString("outcome"), rs.getLong("total")));
    }

    public List<MonthCount> applicationCountsByMonth(LocalDate start, LocalDate end) {
        return jdbcTemplate.query("""
                SELECT substr(applied_date, 1, 7) AS month_key,
                       COUNT(*) AS total
                FROM job_applications
                WHERE applied_date IS NOT NULL
                  AND applied_date BETWEEN ? AND ?
                GROUP BY substr(applied_date, 1, 7)
                ORDER BY month_key ASC
                """, (rs, rowNum) -> new MonthCount(rs.getString("month_key"), rs.getLong("total")),
                start.toString(), end.toString());
    }

    public List<MonthCount> interviewCountsByMonth(LocalDate start, LocalDate end) {
        return jdbcTemplate.query("""
                SELECT substr(event_date, 1, 7) AS month_key,
                       COUNT(*) AS total
                FROM application_events
                WHERE event_type IN (""" + INTERVIEW_EVENT_TYPES + """
                )
                  AND event_date BETWEEN ? AND ?
                GROUP BY substr(event_date, 1, 7)
                ORDER BY month_key ASC
                """, (rs, rowNum) -> new MonthCount(rs.getString("month_key"), rs.getLong("total")),
                start.toString(), end.toString());
    }

    public List<DimensionPerformance> sourcePerformance(int limit) {
        return dimensionPerformance("TRIM(ja.source)", "ja.source IS NOT NULL AND TRIM(ja.source) <> ''", limit);
    }

    public List<DimensionPerformance> priorityPerformance(int limit) {
        return dimensionPerformance("COALESCE(ja.priority, 'UNSPECIFIED')", "1 = 1", limit);
    }

    public List<DimensionPerformance> careerLanePerformance(int limit) {
        return dimensionPerformance("TRIM(ja.career_lane)", "ja.career_lane IS NOT NULL AND TRIM(ja.career_lane) <> ''", limit);
    }

    public List<DimensionPerformance> workArrangementPerformance(int limit) {
        return dimensionPerformance("TRIM(ja.work_arrangement)", "ja.work_arrangement IS NOT NULL AND TRIM(ja.work_arrangement) <> ''", limit);
    }

    private List<DimensionPerformance> dimensionPerformance(String expression, String whereClause, int limit) {
        String sql = """
                SELECT %s AS label,
                       COUNT(*) AS applications,
                       SUM(CASE WHEN EXISTS (
                           SELECT 1
                           FROM application_events ae
                           WHERE ae.application_id = ja.id
                             AND ae.event_type IN (%s)
                       ) THEN 1 ELSE 0 END) AS responses,
                       SUM(CASE WHEN EXISTS (
                           SELECT 1
                           FROM application_events ae
                           WHERE ae.application_id = ja.id
                             AND ae.event_type IN (%s)
                       ) THEN 1 ELSE 0 END) AS interviewed
                FROM job_applications ja
                WHERE EXISTS (
                    SELECT 1 FROM application_events applied
                    WHERE applied.application_id = ja.id AND applied.event_type = 'APPLIED'
                )
                  AND (%s)
                GROUP BY %s
                ORDER BY applications DESC, responses DESC, interviewed DESC, label ASC
                LIMIT ?
                """.formatted(expression, RESPONSE_EVENT_TYPES, INTERVIEW_EVENT_TYPES, whereClause, expression);

        return jdbcTemplate.query(sql, (rs, rowNum) -> new DimensionPerformance(
                rs.getString("label"),
                rs.getLong("applications"),
                rs.getLong("responses"),
                rs.getLong("interviewed")),
                Math.max(1, limit));
    }

    public PrepHealth prepHealth() {
        return jdbcTemplate.queryForObject("""
                SELECT COUNT(*) AS total_items,
                       AVG(confidence) AS average_confidence,
                       SUM(CASE WHEN last_reviewed_at IS NOT NULL THEN 1 ELSE 0 END) AS reviewed_items,
                       COALESCE(SUM(review_count), 0) AS completed_reviews
                FROM prep_items
                """, (rs, rowNum) -> new PrepHealth(
                        rs.getLong("total_items"),
                        nullableDouble(rs.getObject("average_confidence")),
                        rs.getLong("reviewed_items"),
                        rs.getLong("completed_reviews")));
    }

    public long countPrepNeedsReview() {
        return valueOrZero(jdbcTemplate.queryForObject("""
                SELECT COUNT(*)
                FROM prep_items p
                WHERE (
                    p.confidence <= 2
                    OR (p.confidence = 3 AND datetime(COALESCE(p.last_reviewed_at, p.updated_at)) <= datetime('now', '-14 days'))
                    OR (p.confidence = 4 AND datetime(COALESCE(p.last_reviewed_at, p.updated_at)) <= datetime('now', '-30 days'))
                    OR (p.confidence >= 5 AND datetime(COALESCE(p.last_reviewed_at, p.updated_at)) <= datetime('now', '-60 days'))
                )
                """, Long.class));
    }

    private long valueOrZero(Long value) {
        return value == null ? 0L : value;
    }

    private Double nullableDouble(Object value) {
        if (value == null) {
            return null;
        }
        return ((Number) value).doubleValue();
    }

    public record StageTiming(Double averageDays, long sampleSize) {
    }

    public record StateCount(String state, long total) {
    }

    public record OutcomeCount(String outcome, long total) {
    }

    public record MonthCount(String monthKey, long total) {
    }

    public record DimensionPerformance(String label, long applications, long responses, long interviewed) {
    }

    public record PrepHealth(long totalItems, Double averageConfidence, long reviewedItems, long completedReviews) {
    }
}
