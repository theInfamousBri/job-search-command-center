package com.brianna.jobsearch.model;

public record MaterialLibrarySummary(
        long materials,
        long resumes,
        long applicationLinks,
        long storedBytes,
        long avoidedDuplicateBytes) {

    public String storedSizeDisplay() {
        return sizeDisplay(storedBytes);
    }

    public String avoidedSizeDisplay() {
        return sizeDisplay(avoidedDuplicateBytes);
    }

    private String sizeDisplay(long bytes) {
        if (bytes < 1024) {
            return bytes + " B";
        }
        if (bytes < 1024L * 1024L) {
            return String.format("%.1f KB", bytes / 1024.0);
        }
        return String.format("%.1f MB", bytes / (1024.0 * 1024.0));
    }
}
