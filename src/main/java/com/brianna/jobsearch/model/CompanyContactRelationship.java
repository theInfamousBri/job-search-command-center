package com.brianna.jobsearch.model;

public enum CompanyContactRelationship {
    RECRUITER("Recruiter"),
    HIRING_MANAGER("Hiring Manager"),
    INTERVIEWER("Interviewer"),
    REFERRAL("Referral"),
    TEAM_MEMBER("Team Member"),
    NETWORKING_CONTACT("Networking Contact"),
    OTHER("Other");

    private final String displayName;

    CompanyContactRelationship(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
