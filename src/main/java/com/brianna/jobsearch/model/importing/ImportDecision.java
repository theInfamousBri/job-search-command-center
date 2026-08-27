package com.brianna.jobsearch.model.importing;

public enum ImportDecision {
    IMPORT("Import separate"),
    MERGE("Merge with existing"),
    SKIP("Skip");

    private final String displayName;

    ImportDecision(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
