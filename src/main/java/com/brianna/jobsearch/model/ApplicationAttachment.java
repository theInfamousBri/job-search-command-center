package com.brianna.jobsearch.model;

import java.time.LocalDateTime;

public record ApplicationAttachment(
        long id,
        long applicationId,
        ApplicationAttachmentType attachmentType,
        String fileName,
        String mimeType,
        long fileSize,
        LocalDateTime createdAt) {

    public String fileSizeDisplay() {
        if (fileSize < 1024) {
            return fileSize + " B";
        }
        if (fileSize < 1024L * 1024L) {
            return String.format("%.1f KB", fileSize / 1024.0);
        }
        return String.format("%.1f MB", fileSize / (1024.0 * 1024.0));
    }
}
