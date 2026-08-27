package com.brianna.jobsearch.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class JobApplication {

    private Long id;

    @NotBlank(message = "Company is required.")
    @Size(max = 150, message = "Company must be 150 characters or fewer.")
    private String company;

    @NotBlank(message = "Role is required.")
    @Size(max = 200, message = "Role must be 200 characters or fewer.")
    private String role;

    private String location;
    private ApplicationStatus status = ApplicationStatus.APPLIED;
    private ApplicationState state = ApplicationState.ACTIVE;
    private Priority priority = Priority.MEDIUM;
    private String source;
    private String jobUrl;
    private String salary;
    private LocalDate appliedDate = LocalDate.now();
    private String notes;
    private String jobDescription;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public String getAppliedDateDisplay() {
        return appliedDate == null ? "—" : appliedDate.format(DateTimeFormatter.ofPattern("MMM d, yyyy"));
    }

    public String getAppliedDateInputValue() {
        return appliedDate == null ? "" : appliedDate.toString();
    }

    public String getUpdatedDateDisplay() {
        return updatedAt == null ? "—" : updatedAt.format(DateTimeFormatter.ofPattern("MMM d"));
    }

    public String getUpdatedDateTimeDisplay() {
        return updatedAt == null ? "—" : updatedAt.format(DateTimeFormatter.ofPattern("MMM d, yyyy 'at' h:mm a"));
    }

    public String getInitials() {
        if (company == null || company.isBlank()) {
            return "?";
        }

        String[] parts = company.trim().split("\\s+");
        if (parts.length == 1) {
            return parts[0].substring(0, Math.min(2, parts[0].length())).toUpperCase();
        }
        return (parts[0].substring(0, 1) + parts[1].substring(0, 1)).toUpperCase();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getCompany() { return company; }
    public void setCompany(String company) { this.company = company; }

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }

    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }

    public ApplicationStatus getStatus() { return status; }
    public void setStatus(ApplicationStatus status) { this.status = status; }

    public ApplicationState getState() { return state; }
    public void setState(ApplicationState state) { this.state = state; }

    public Priority getPriority() { return priority; }
    public void setPriority(Priority priority) { this.priority = priority; }

    public String getSource() { return source; }
    public void setSource(String source) { this.source = source; }

    public String getJobUrl() { return jobUrl; }
    public void setJobUrl(String jobUrl) { this.jobUrl = jobUrl; }

    public String getSalary() { return salary; }
    public void setSalary(String salary) { this.salary = salary; }

    public LocalDate getAppliedDate() { return appliedDate; }
    public void setAppliedDate(LocalDate appliedDate) { this.appliedDate = appliedDate; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }

    public String getJobDescription() { return jobDescription; }
    public void setJobDescription(String jobDescription) { this.jobDescription = jobDescription; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
