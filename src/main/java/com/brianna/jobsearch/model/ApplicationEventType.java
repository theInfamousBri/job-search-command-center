package com.brianna.jobsearch.model;

public enum ApplicationEventType {
    SAVED("Saved", ApplicationEventCategory.PIPELINE),
    APPLIED("Applied", ApplicationEventCategory.PIPELINE),

    RECRUITER_CONTACT("Recruiter Outreach", ApplicationEventCategory.COMMUNICATION),
    INTERVIEW_SCHEDULED("Interview Scheduled", ApplicationEventCategory.COMMUNICATION),
    FOLLOW_UP("Follow Up", ApplicationEventCategory.COMMUNICATION),

    CODING_ASSESSMENT("Coding Assessment", ApplicationEventCategory.ASSESSMENT),
    TAKE_HOME_ASSESSMENT("Take-home Assessment", ApplicationEventCategory.ASSESSMENT),

    RECRUITER_SCREEN("Recruiter Screen", ApplicationEventCategory.PIPELINE),
    HIRING_MANAGER("Hiring Manager Interview", ApplicationEventCategory.PIPELINE),
    TECHNICAL_INTERVIEW("Technical Interview", ApplicationEventCategory.PIPELINE),
    FINAL_ROUND("Final Round", ApplicationEventCategory.PIPELINE),
    OFFER("Offer", ApplicationEventCategory.PIPELINE),
    REJECTED("Rejected", ApplicationEventCategory.PIPELINE),
    WITHDRAWN("Withdrawn", ApplicationEventCategory.PIPELINE),
    NO_RESPONSE("No Response", ApplicationEventCategory.PIPELINE),

    OTHER("Other", ApplicationEventCategory.ACTIVITY);

    private final String displayName;
    private final ApplicationEventCategory category;

    ApplicationEventType(String displayName, ApplicationEventCategory category) {
        this.displayName = displayName;
        this.category = category;
    }

    public String getDisplayName() {
        return displayName;
    }

    public ApplicationEventCategory getCategory() {
        return category;
    }

    public boolean isPipelineMilestone() {
        return category == ApplicationEventCategory.PIPELINE;
    }

    public boolean isInterview() {
        return this == RECRUITER_SCREEN
                || this == HIRING_MANAGER
                || this == TECHNICAL_INTERVIEW
                || this == FINAL_ROUND;
    }

    public static ApplicationEventType fromStatus(ApplicationStatus status) {
        if (status == null) {
            return OTHER;
        }

        return switch (status) {
            case SAVED -> SAVED;
            case APPLIED -> APPLIED;
            case RECRUITER_SCREEN -> RECRUITER_SCREEN;
            case HIRING_MANAGER -> HIRING_MANAGER;
            case TECHNICAL_INTERVIEW -> TECHNICAL_INTERVIEW;
            case FINAL_ROUND -> FINAL_ROUND;
            case OFFER -> OFFER;
            case REJECTED -> REJECTED;
            case WITHDRAWN -> WITHDRAWN;
            case NO_RESPONSE -> NO_RESPONSE;
        };
    }
}
