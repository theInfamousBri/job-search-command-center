package com.brianna.jobsearch.model;

import java.util.Locale;

public enum ApplicationAttachmentType {
    RESUME("Resume", "CV"),
    COVER_LETTER("Cover letter", "CL"),
    OTHER("Other", "FILE");

    private final String displayName;
    private final String iconLabel;

    ApplicationAttachmentType(String displayName, String iconLabel) {
        this.displayName = displayName;
        this.iconLabel = iconLabel;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getIconLabel() {
        return iconLabel;
    }

    public static ApplicationAttachmentType fromFormValue(String value) {
        if (value == null || value.isBlank()) {
            return OTHER;
        }
        try {
            return valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("Choose a valid attachment type.");
        }
    }
}
