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
        assertFalse(template.contains("Original career tag"));
        assertFalse(template.contains("jobApplication.careerLane"));
    }
}
