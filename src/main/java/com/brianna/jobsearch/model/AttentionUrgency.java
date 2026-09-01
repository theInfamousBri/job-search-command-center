package com.brianna.jobsearch.model;

public enum AttentionUrgency {
    NOW("Now"),
    SOON("Soon"),
    KEEP_WARM("Keep warm");

    private final String displayName;

    AttentionUrgency(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getCssName() {
        return name().toLowerCase().replace('_', '-');
    }
}
