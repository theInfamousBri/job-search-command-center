package com.brianna.jobsearch.model;

public enum ApplicationStatus {
    SAVED("Saved"),
    APPLIED("Applied"),
    RECRUITER_SCREEN("Recruiter Screen"),
    ASSESSMENT("Assessment"),
    HIRING_MANAGER("Hiring Manager"),
    TECHNICAL_INTERVIEW("Technical Interview"),
    FINAL_ROUND("Final Round"),
    OFFER("Offer"),
    REJECTED("Rejected"),
    WITHDRAWN("Withdrawn"),
    NO_RESPONSE("No Response");

    private final String displayName;

    ApplicationStatus(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
