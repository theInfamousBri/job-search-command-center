package com.brianna.jobsearch.model;

import java.time.LocalDateTime;

public record MaterialFile(
        long id,
        MaterialType materialType,
        String displayName,
        String fileName,
        String mimeType,
        long fileSize,
        String sha256,
        String notes,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        long linkedApplicationCount) {

    public String fileSizeDisplay() {
        if (fileSize < 1024) {
            return fileSize + " B";
        }
        if (fileSize < 1024L * 1024L) {
            return String.format("%.1f KB", fileSize / 1024.0);
        }
        return String.format("%.1f MB", fileSize / (1024.0 * 1024.0));
    }

    public String usageDisplay() {
        if (linkedApplicationCount == 0) {
            return "Not linked yet";
        }
        return linkedApplicationCount == 1
                ? "Used by 1 application"
                : "Used by " + linkedApplicationCount + " applications";
    }
}
