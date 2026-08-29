package com.brianna.jobsearch.model;

public enum IndustryDomain {
    FINTECH_PAYMENTS("Fintech & Payments"),
    FINANCIAL_SERVICES("Financial Services"),
    HEALTHCARE("Healthcare"),
    SECURITY_IDENTITY("Security & Identity"),
    CLOUD_INFRASTRUCTURE("Cloud & Infrastructure"),
    DEVELOPER_TOOLS("Developer Tools"),
    ENTERPRISE_SAAS("Enterprise SaaS"),
    ECOMMERCE("E-commerce & Marketplaces"),
    CONSUMER("Consumer"),
    MEDIA_STREAMING("Media & Streaming"),
    EDUCATION("Education"),
    GOVERNMENT("Government / GovTech"),
    AEROSPACE_DEFENSE("Aerospace & Defense"),
    AUTOMOTIVE("Automotive"),
    MANUFACTURING_INDUSTRIAL("Manufacturing & Industrial"),
    ROBOTICS_AUTONOMY("Robotics & Autonomy"),
    LOGISTICS_SUPPLY_CHAIN("Logistics & Supply Chain"),
    TRAVEL_HOSPITALITY("Travel & Hospitality"),
    CLIMATE_SUSTAINABILITY("Climate & Sustainability"),
    LEGAL_COMPLIANCE("Legal & Compliance"),
    OTHER("Other");

    private final String displayName;

    IndustryDomain(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
