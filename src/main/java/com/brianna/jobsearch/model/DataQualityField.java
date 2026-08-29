package com.brianna.jobsearch.model;

public enum DataQualityField {
    ROLE_FAMILY("Career Lane", "Broad role family used for strategy analytics."),
    INDUSTRY_DOMAIN("Industry / Domain", "The business domain the role belongs to."),
    SOURCE("Source", "Where the application originated."),
    WORK_ARRANGEMENT("Work Arrangement", "Remote, hybrid, on-site, or another arrangement."),
    PRIORITY("Priority", "Your intentional priority for the application."),
    COMPANY_DOMAIN("Company Domain", "Company website domain used for branding and grouping.");

    private final String displayName;
    private final String description;

    DataQualityField(String displayName, String description) {
        this.displayName = displayName;
        this.description = description;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getDescription() {
        return description;
    }
}
