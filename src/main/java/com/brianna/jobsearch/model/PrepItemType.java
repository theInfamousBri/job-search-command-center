package com.brianna.jobsearch.model;

public enum PrepItemType {
    TECHNICAL_TOPIC("Technical Topic", "Technical"),
    STAR_STORY("STAR Story", "Behavioral"),
    INTERVIEW_QUESTION("Interview Question", "Questions"),
    COMPANY_RESEARCH("Company Research", "Company"),
    GENERAL_NOTE("General Note", "Notes");

    private final String displayName;
    private final String shortLabel;

    PrepItemType(String displayName, String shortLabel) {
        this.displayName = displayName;
        this.shortLabel = shortLabel;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getShortLabel() {
        return shortLabel;
    }
}
