package com.brianna.jobsearch.model;

public enum CareerRoleFamily {
    BACKEND_PLATFORM("Backend / Platform"),
    FULL_STACK("Full-Stack"),
    FRONTEND("Frontend"),
    CLOUD_INFRASTRUCTURE("Cloud / Infrastructure"),
    DEVOPS_SRE("DevOps / SRE"),
    DATA_ANALYTICS("Data / Analytics"),
    SECURITY("Security"),
    MOBILE("Mobile"),
    PRODUCT_ENGINEERING("Product Engineering"),
    FORWARD_DEPLOYED_CUSTOMER_ENGINEERING("Forward Deployed / Customer Engineering"),
    ROBOTICS_EMBEDDED_AUTONOMY("Robotics / Embedded / Autonomy"),
    ENGINEERING_LEADERSHIP("Engineering Leadership"),
    OTHER("Other");

    private final String displayName;

    CareerRoleFamily(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    public static String displayNameFor(String value) {
        if (value == null || value.isBlank()) {
            return value;
        }
        try {
            return valueOf(value).getDisplayName();
        } catch (IllegalArgumentException ignored) {
            return value;
        }
    }
}
