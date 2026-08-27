package com.brianna.jobsearch.model;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

public class CalendarEntry {

    private Long eventId;
    private Long applicationId;
    private String company;
    private String role;
    private ApplicationEventType eventType;
    private String title;
    private LocalDate eventDate;
    private LocalTime eventTime;
    private String contactName;

    public String getDisplayTitle() {
        if (title == null || title.isBlank()) {
            return eventType == null ? "Activity" : eventType.getDisplayName();
        }
        return title;
    }

    public ApplicationEventCategory getCategory() {
        return eventType == null ? ApplicationEventCategory.ACTIVITY : eventType.getCategory();
    }

    public String getEventTimeDisplay() {
        return eventTime == null ? null : eventTime.format(DateTimeFormatter.ofPattern("h:mm a"));
    }

    public String getEventDateDisplay() {
        return eventDate == null ? "—" : eventDate.format(DateTimeFormatter.ofPattern("EEE, MMM d"));
    }

    public String getCompanyInitials() {
        if (company == null || company.isBlank()) {
            return "?";
        }
        String[] parts = company.trim().split("\\s+");
        if (parts.length == 1) {
            return parts[0].substring(0, Math.min(2, parts[0].length())).toUpperCase();
        }
        return (parts[0].substring(0, 1) + parts[1].substring(0, 1)).toUpperCase();
    }

    public Long getEventId() { return eventId; }
    public void setEventId(Long eventId) { this.eventId = eventId; }

    public Long getApplicationId() { return applicationId; }
    public void setApplicationId(Long applicationId) { this.applicationId = applicationId; }

    public String getCompany() { return company; }
    public void setCompany(String company) { this.company = company; }

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }

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
}
