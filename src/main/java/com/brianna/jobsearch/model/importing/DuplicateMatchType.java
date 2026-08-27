package com.brianna.jobsearch.model.importing;

public enum DuplicateMatchType {
    NONE("No duplicate found"),
    EXACT("Likely existing application"),
    POSSIBLE("Same company / role found");

    private final String displayName;

    DuplicateMatchType(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
