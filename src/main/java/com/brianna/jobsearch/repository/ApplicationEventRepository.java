package com.brianna.jobsearch.repository;

import com.brianna.jobsearch.model.ApplicationEvent;
import com.brianna.jobsearch.model.ApplicationEventType;
import com.brianna.jobsearch.model.CalendarEntry;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

@Repository
public class ApplicationEventRepository {

    private final JdbcTemplate jdbcTemplate;

    private final RowMapper<ApplicationEvent> rowMapper = (rs, rowNum) -> {
        ApplicationEvent event = new ApplicationEvent();
        event.setId(rs.getLong("id"));
        event.setApplicationId(rs.getLong("application_id"));
        event.setEventType(ApplicationEventType.valueOf(rs.getString("event_type")));
        event.setTitle(rs.getString("title"));

        String eventDate = rs.getString("event_date");
        event.setEventDate(eventDate == null ? null : LocalDate.parse(eventDate));

        String eventTime = rs.getString("event_time");
        event.setEventTime(eventTime == null || eventTime.isBlank() ? null : LocalTime.parse(eventTime));

        event.setContactName(rs.getString("contact_name"));
        event.setNotes(rs.getString("notes"));

        String createdAt = rs.getString("created_at");
        event.setCreatedAt(createdAt == null ? null : LocalDateTime.parse(createdAt));
        return event;
    };


    private final RowMapper<CalendarEntry> calendarRowMapper = (rs, rowNum) -> {
        CalendarEntry entry = new CalendarEntry();
        entry.setEventId(rs.getLong("event_id"));
        entry.setApplicationId(rs.getLong("application_id"));
        entry.setCompany(rs.getString("company"));
        entry.setRole(rs.getString("role"));
        entry.setEventType(ApplicationEventType.valueOf(rs.getString("event_type")));
        entry.setTitle(rs.getString("title"));

        String eventDate = rs.getString("event_date");
        entry.setEventDate(eventDate == null ? null : LocalDate.parse(eventDate));

        String eventTime = rs.getString("event_time");
        entry.setEventTime(eventTime == null || eventTime.isBlank() ? null : LocalTime.parse(eventTime));

        entry.setContactName(rs.getString("contact_name"));
        return entry;
    };

    public ApplicationEventRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<ApplicationEvent> findByApplicationId(long applicationId) {
        return jdbcTemplate.query("""
                SELECT *
                FROM application_events
                WHERE application_id = ?
                ORDER BY event_date ASC,
                         CASE WHEN event_time IS NULL THEN 1 ELSE 0 END ASC,
                         event_time ASC,
                         id ASC
                """, rowMapper, applicationId);
    }


    public List<CalendarEntry> findBetween(LocalDate startDate, LocalDate endDate) {
        return jdbcTemplate.query("""
                SELECT ae.id AS event_id,
                       ae.application_id,
                       ae.event_type,
                       ae.title,
                       ae.event_date,
                       ae.event_time,
                       ae.contact_name,
                       ja.company,
                       ja.role
                FROM application_events ae
                JOIN job_applications ja ON ja.id = ae.application_id
                WHERE ae.event_date BETWEEN ? AND ?
                ORDER BY ae.event_date ASC,
                         CASE WHEN ae.event_time IS NULL THEN 1 ELSE 0 END ASC,
                         ae.event_time ASC,
                         ae.id ASC
                """, calendarRowMapper, startDate.toString(), endDate.toString());
    }

    public Optional<ApplicationEvent> findById(long eventId, long applicationId) {
        return jdbcTemplate.query("""
                SELECT *
                FROM application_events
                WHERE id = ? AND application_id = ?
                """, rowMapper, eventId, applicationId).stream().findFirst();
    }

    public void save(ApplicationEvent event) {
        jdbcTemplate.update("""
                INSERT INTO application_events (
                    application_id, event_type, title, event_date, event_time,
                    contact_name, notes, created_at
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                """,
                event.getApplicationId(),
                event.getEventType().name(),
                blankToNull(event.getTitle()),
                event.getEventDate().toString(),
                event.getEventTime() == null ? null : event.getEventTime().toString(),
                blankToNull(event.getContactName()),
                blankToNull(event.getNotes()),
                LocalDateTime.now().toString());
    }

    public void update(ApplicationEvent event) {
        jdbcTemplate.update("""
                UPDATE application_events
                SET event_type = ?,
                    title = ?,
                    event_date = ?,
                    event_time = ?,
                    contact_name = ?,
                    notes = ?
                WHERE id = ? AND application_id = ?
                """,
                event.getEventType().name(),
                blankToNull(event.getTitle()),
                event.getEventDate().toString(),
                event.getEventTime() == null ? null : event.getEventTime().toString(),
                blankToNull(event.getContactName()),
                blankToNull(event.getNotes()),
                event.getId(),
                event.getApplicationId());
    }

    public void delete(long eventId, long applicationId) {
        jdbcTemplate.update("""
                DELETE FROM application_events
                WHERE id = ? AND application_id = ?
                """, eventId, applicationId);
    }

    public boolean exists(long applicationId, ApplicationEventType eventType, LocalDate eventDate) {
        Long count = jdbcTemplate.queryForObject("""
                SELECT COUNT(*)
                FROM application_events
                WHERE application_id = ?
                  AND event_type = ?
                  AND event_date = ?
                """, Long.class, applicationId, eventType.name(), eventDate.toString());
        return count != null && count > 0;
    }

    public void deleteByApplicationId(long applicationId) {
        jdbcTemplate.update("DELETE FROM application_events WHERE application_id = ?", applicationId);
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
