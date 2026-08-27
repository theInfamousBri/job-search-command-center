package com.brianna.jobsearch.model;

import jakarta.validation.constraints.NotBlank;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;

public class PrepItem {

    private long id;
    private PrepItemType type = PrepItemType.TECHNICAL_TOPIC;

    @NotBlank(message = "Title is required")
    private String title;

    private String category;
    private String tags;
    private String content;
    private Integer confidence = 3;
    private Long applicationId;
    private String applicationCompany;
    private String applicationRole;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime lastReviewedAt;
    private int reviewCount;

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public PrepItemType getType() {
        return type;
    }

    public void setType(PrepItemType type) {
        this.type = type;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getTags() {
        return tags;
    }

    public void setTags(String tags) {
        this.tags = tags;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public Integer getConfidence() {
        return confidence;
    }

    public void setConfidence(Integer confidence) {
        this.confidence = confidence;
    }

    public Long getApplicationId() {
        return applicationId;
    }

    public void setApplicationId(Long applicationId) {
        this.applicationId = applicationId;
    }

    public String getApplicationCompany() {
        return applicationCompany;
    }

    public void setApplicationCompany(String applicationCompany) {
        this.applicationCompany = applicationCompany;
    }

    public String getApplicationRole() {
        return applicationRole;
    }

    public void setApplicationRole(String applicationRole) {
        this.applicationRole = applicationRole;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public LocalDateTime getLastReviewedAt() {
        return lastReviewedAt;
    }

    public void setLastReviewedAt(LocalDateTime lastReviewedAt) {
        this.lastReviewedAt = lastReviewedAt;
    }

    public int getReviewCount() {
        return reviewCount;
    }

    public void setReviewCount(int reviewCount) {
        this.reviewCount = Math.max(0, reviewCount);
    }

    public boolean isLinkedToApplication() {
        return applicationId != null;
    }

    public String getApplicationLabel() {
        if (applicationCompany == null || applicationCompany.isBlank()) {
            return null;
        }
        if (applicationRole == null || applicationRole.isBlank()) {
            return applicationCompany;
        }
        return applicationCompany + " · " + applicationRole;
    }

    public String getContentPreview() {
        if (content == null || content.isBlank()) {
            return "No notes yet.";
        }
        String normalized = content.replaceAll("\\s+", " ").trim();
        return normalized.length() <= 220 ? normalized : normalized.substring(0, 217) + "…";
    }

    public List<String> getTagList() {
        if (tags == null || tags.isBlank()) {
            return List.of();
        }
        return Arrays.stream(tags.split(","))
                .map(String::trim)
                .filter(tag -> !tag.isBlank())
                .toList();
    }

    public String getUpdatedDisplay() {
        if (updatedAt == null) {
            return "";
        }
        return updatedAt.format(DateTimeFormatter.ofPattern("MMM d, yyyy"));
    }

    public String getLastReviewedDisplay() {
        if (lastReviewedAt == null) {
            return "Never reviewed";
        }
        return lastReviewedAt.format(DateTimeFormatter.ofPattern("MMM d, yyyy"));
    }

    public String getReviewAgeLabel() {
        if (lastReviewedAt == null) {
            return "Never reviewed";
        }
        long days = Math.max(0, Duration.between(lastReviewedAt, LocalDateTime.now()).toDays());
        if (days == 0) {
            return "Reviewed today";
        }
        if (days == 1) {
            return "Reviewed yesterday";
        }
        return "Reviewed " + days + " days ago";
    }

    public boolean isReviewDue() {
        int confidence = getSafeConfidence();
        if (confidence <= 2) {
            return true;
        }

        LocalDateTime reference = lastReviewedAt != null ? lastReviewedAt : updatedAt;
        if (reference == null) {
            return false;
        }

        return !reference.plusDays(reviewIntervalDays()).isAfter(LocalDateTime.now());
    }

    public String getReviewDueReason() {
        if (getSafeConfidence() <= 2) {
            return lastReviewedAt == null ? "Low confidence · never reviewed" : "Low confidence · " + getReviewAgeLabel().toLowerCase();
        }
        if (lastReviewedAt == null) {
            return "Ready for a first review";
        }
        return getReviewAgeLabel();
    }

    public String getReviewCadenceLabel() {
        int confidence = getSafeConfidence();
        if (confidence <= 2) {
            return "Review again until this feels solid";
        }
        return "Suggested refresh every " + reviewIntervalDays() + " days";
    }

    public String getNextReviewDisplay() {
        if (getSafeConfidence() <= 2) {
            return "Now";
        }
        LocalDateTime reference = lastReviewedAt != null ? lastReviewedAt : updatedAt;
        if (reference == null) {
            return "Not scheduled";
        }
        LocalDate next = reference.plusDays(reviewIntervalDays()).toLocalDate();
        return next.format(DateTimeFormatter.ofPattern("MMM d, yyyy"));
    }

    private int reviewIntervalDays() {
        return switch (getSafeConfidence()) {
            case 3 -> 14;
            case 4 -> 30;
            case 5 -> 60;
            default -> 0;
        };
    }

    public int getSafeConfidence() {
        if (confidence == null) {
            return 3;
        }
        return Math.max(1, Math.min(5, confidence));
    }
}
