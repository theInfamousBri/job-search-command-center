package com.brianna.jobsearch.model;

public enum Priority {
    UNSPECIFIED("Not set"),
    LOW("Low"),
    MEDIUM("Medium"),
    HIGH("High"),
    STRETCH("Stretch"),
    SKIP("Skip");

    private final String displayName;

    Priority(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
