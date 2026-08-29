package com.brianna.jobsearch.model;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class CareerTaxonomyTest {

    @Test
    void roleFamilyProvidesStableDisplayLabelsForAnalytics() {
        assertEquals("Backend / Platform", CareerRoleFamily.BACKEND_PLATFORM.getDisplayName());
        assertEquals("Backend / Platform", CareerRoleFamily.displayNameFor("BACKEND_PLATFORM"));
        assertEquals("Forward Deployed / Customer Engineering",
                CareerRoleFamily.FORWARD_DEPLOYED_CUSTOMER_ENGINEERING.getDisplayName());
        assertEquals("Robotics / Embedded / Autonomy",
                CareerRoleFamily.ROBOTICS_EMBEDDED_AUTONOMY.getDisplayName());
        assertEquals("Legacy label", CareerRoleFamily.displayNameFor("Legacy label"));
    }

    @Test
    void industryDomainSeparatesBusinessContextFromRoleFamily() {
        assertEquals("Fintech & Payments", IndustryDomain.FINTECH_PAYMENTS.getDisplayName());
        assertEquals("Developer Tools", IndustryDomain.DEVELOPER_TOOLS.getDisplayName());
        assertEquals("Aerospace & Defense", IndustryDomain.AEROSPACE_DEFENSE.getDisplayName());
        assertEquals("Manufacturing & Industrial", IndustryDomain.MANUFACTURING_INDUSTRIAL.getDisplayName());
        assertEquals("Logistics & Supply Chain", IndustryDomain.LOGISTICS_SUPPLY_CHAIN.getDisplayName());
        assertEquals("Travel & Hospitality", IndustryDomain.TRAVEL_HOSPITALITY.getDisplayName());
        assertEquals("Climate & Sustainability", IndustryDomain.CLIMATE_SUSTAINABILITY.getDisplayName());
        assertEquals("Legal & Compliance", IndustryDomain.LEGAL_COMPLIANCE.getDisplayName());
    }

    @Test
    void existingEnumNamesRemainStableForPersistedRows() {
        assertEquals("Security & Identity", IndustryDomain.SECURITY_IDENTITY.getDisplayName());
        assertEquals("E-commerce & Marketplaces", IndustryDomain.ECOMMERCE.getDisplayName());
        assertEquals("Government / GovTech", IndustryDomain.GOVERNMENT.getDisplayName());
    }
}
