package com.brianna.jobsearch.model;

public enum CalendarFilter {
    ACTIONABLE("Actionable"),
    INTERVIEWS("Interviews"),
    ASSESSMENTS("Assessments"),
    FOLLOW_UPS("Follow-ups"),
    ALL("All history");

    private final String displayName;

    CalendarFilter(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    public boolean includes(ApplicationEventType type) {
        if (type == null) {
            return false;
        }
        return switch (this) {
            case ALL -> true;
            case INTERVIEWS -> type.isInterview() || type == ApplicationEventType.INTERVIEW_SCHEDULED;
            case ASSESSMENTS -> type.getCategory() == ApplicationEventCategory.ASSESSMENT;
            case FOLLOW_UPS -> type == ApplicationEventType.FOLLOW_UP;
            case ACTIONABLE -> type != ApplicationEventType.SAVED
                    && type != ApplicationEventType.APPLIED
                    && type != ApplicationEventType.REJECTED
                    && type != ApplicationEventType.WITHDRAWN
                    && type != ApplicationEventType.NO_RESPONSE;
        };
    }
}
