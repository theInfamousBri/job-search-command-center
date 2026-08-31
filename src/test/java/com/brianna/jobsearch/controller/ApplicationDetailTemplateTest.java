package com.brianna.jobsearch.controller;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import org.junit.jupiter.api.Test;

class ApplicationDetailTemplateTest {

    @Test
    void roleDetailsUsesNormalizedTaxonomyAndHidesLegacyCareerTag() throws IOException {
        String template;
        try (InputStream input = getClass().getClassLoader()
                .getResourceAsStream("templates/applications/detail.html")) {
            assertNotNull(input, "Application detail template should be available on the test classpath");
            template = new String(input.readAllBytes(), UTF_8);
        }

        assertTrue(template.contains("Role details"));
        assertTrue(template.contains("Role family"));
        assertTrue(template.contains("jobApplication.roleFamily"));
        assertTrue(template.contains("jobApplication.industryDomain"));
        assertTrue(template.contains("jobApplication.careerFocus"));
        assertTrue(template.contains("Requisition / Job ID"));
        assertTrue(template.contains("jobApplication.requisitionId"));
        assertTrue(template.contains("What you submitted"));
        assertTrue(template.contains("Manage materials"));
        assertTrue(template.contains("Salary context"));
        assertTrue(template.contains("compensationContext"));
        assertTrue(template.contains("Tracked middle 50%"));
        assertTrue(template.contains("Range midpoint"));
        assertTrue(template.contains("sampleStrength"));
        assertTrue(template.contains("targetRangeDisplay"));
        assertTrue(template.contains("timeline-inline-editor"));
        assertTrue(template.contains("event-editor-${event.id}"));
        assertTrue(template.contains("th:object=\"${newEventForm}\""));
        assertTrue(template.contains("data-event-editor-target"));
        assertTrue(template.contains("data-event-editor-close"));
        assertTrue(template.contains("data-event-return"));

        int overviewLeft = template.indexOf("application-overview-left");
        int timeline = template.indexOf("id=\"application-timeline\"");
        int overviewRight = template.indexOf("application-overview-right");
        assertTrue(overviewLeft >= 0 && timeline > overviewLeft && overviewRight > timeline,
                "Timeline should continue the primary overview column before the right-side role context");
        assertTrue(template.contains("id=\"manage-materials\""));
        assertTrue(template.contains("/js/application-detail.js"));
        assertFalse(template.contains("Store once, reference everywhere"));
        assertFalse(template.contains("Original career tag"));
        assertFalse(template.contains("jobApplication.careerLane"));
    }
}
