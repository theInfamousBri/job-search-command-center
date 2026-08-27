package com.brianna.jobsearch.model;

public enum ApplicationState {
    ACTIVE("Active"),
    INTERVIEW_SCHEDULED("Interview Scheduled"),
    AWAITING_FEEDBACK("Awaiting Feedback"),
    FOLLOW_UP_DUE("Follow-up Due"),
    ON_HOLD("On Hold"),
    CLOSED("Closed");

    private final String displayName;

    ApplicationState(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
