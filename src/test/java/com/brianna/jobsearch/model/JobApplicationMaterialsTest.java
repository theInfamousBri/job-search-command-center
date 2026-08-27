package com.brianna.jobsearch.model;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class JobApplicationMaterialsTest {

    @Test
    void coverLetterDisplayDistinguishesUsageFromArchivedContent() {
        JobApplication application = new JobApplication();
        assertThat(application.getCoverLetterDisplay()).isEqualTo("Not tracked");

        application.setCoverLetter(false);
        assertThat(application.getCoverLetterDisplay()).isEqualTo("Not used");

        application.setCoverLetter(true);
        assertThat(application.getCoverLetterDisplay()).isEqualTo("Used · content not archived");

        application.setCoverLetterText("Dear Hiring Team...");
        assertThat(application.getCoverLetterDisplay()).isEqualTo("Used · archived");
        assertThat(application.hasCoverLetterText()).isTrue();
    }
}
