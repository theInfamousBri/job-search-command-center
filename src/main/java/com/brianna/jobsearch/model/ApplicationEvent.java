package com.brianna.jobsearch.model;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

public class ApplicationEvent {

    private Long id;
    private Long applicationId;
    private ApplicationEventType eventType = ApplicationEventType.OTHER;
    private String title;
    private LocalDate eventDate = LocalDate.now();
    private LocalTime eventTime;
    private String contactName;
    private String notes;
    private LocalDateTime createdAt;

    public String getDisplayTitle() {
        if (title == null || title.isBlank()) {
            return eventType == null ? "Activity" : eventType.getDisplayName();
        }
        return title;
    }

    public String getEventDateDisplay() {
        return eventDate == null ? "—" : eventDate.format(DateTimeFormatter.ofPattern("MMM d, yyyy"));
    }

    public String getEventTimeDisplay() {
        return eventTime == null ? null : eventTime.format(DateTimeFormatter.ofPattern("h:mm a"));
    }

    public String getEventDateInputValue() {
        return eventDate == null ? "" : eventDate.toString();
    }

    public String getEventTimeInputValue() {
        return eventTime == null ? "" : eventTime.format(DateTimeFormatter.ofPattern("HH:mm"));
    }

    public boolean isInterview() {
        return eventType != null && eventType.isInterview();
    }

    public ApplicationEventCategory getCategory() {
        return eventType == null ? ApplicationEventCategory.ACTIVITY : eventType.getCategory();
    }

    public boolean isPipelineMilestone() {
        return eventType != null && eventType.isPipelineMilestone();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getApplicationId() { return applicationId; }
    public void setApplicationId(Long applicationId) { this.applicationId = applicationId; }

    public ApplicationEventType getEventType() { return eventType; }
    public void setEventType(ApplicationEventType eventType) { this.eventType = eventType; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public LocalDate getEventDate() { return eventDate; }
    public void setEventDate(LocalDate eventDate) { this.eventDate = eventDate; }

    public LocalTime getEventTime() { return eventTime; }
    public void setEventTime(LocalTime eventTime) { this.eventTime = eventTime; }

    public String getContactName() { return contactName; }
    public void setContactName(String contactName) { this.contactName = contactName; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
