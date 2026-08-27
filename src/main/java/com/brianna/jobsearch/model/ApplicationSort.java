package com.brianna.jobsearch.model;

public enum ApplicationSort {
    UPDATED_DESC("Recently updated"),
    UPDATED_ASC("Oldest updated"),
    APPLIED_DESC("Applied newest"),
    APPLIED_ASC("Applied oldest"),
    COMPANY_ASC("Company A–Z");

    private final String displayName;

    ApplicationSort(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
