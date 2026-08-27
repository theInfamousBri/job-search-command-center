package com.brianna.jobsearch.model;

public enum ApplicationEventCategory {
    PIPELINE("Pipeline"),
    COMMUNICATION("Communication"),
    ASSESSMENT("Assessment"),
    ACTIVITY("Activity");

    private final String displayName;

    ApplicationEventCategory(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
