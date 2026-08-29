package com.brianna.jobsearch.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDateTime;

import com.brianna.jobsearch.model.ApplicationAttachment;
import com.brianna.jobsearch.model.ApplicationAttachmentType;
import org.junit.jupiter.api.Test;

class ApplicationAttachmentTest {

    @Test
    void formatsFileSizesForDisplay() {
        assertThat(attachment(900).fileSizeDisplay()).isEqualTo("900 B");
        assertThat(attachment(1536).fileSizeDisplay()).isEqualTo("1.5 KB");
        assertThat(attachment(2L * 1024L * 1024L).fileSizeDisplay()).isEqualTo("2.0 MB");
    }

    @Test
    void parsesAttachmentTypesFromFormValues() {
        assertThat(ApplicationAttachmentType.fromFormValue("resume")).isEqualTo(ApplicationAttachmentType.RESUME);
        assertThat(ApplicationAttachmentType.fromFormValue("COVER_LETTER")).isEqualTo(ApplicationAttachmentType.COVER_LETTER);
        assertThat(ApplicationAttachmentType.fromFormValue(null)).isEqualTo(ApplicationAttachmentType.OTHER);
        assertThatThrownBy(() -> ApplicationAttachmentType.fromFormValue("something-else"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Choose a valid attachment type.");
    }

    private ApplicationAttachment attachment(long size) {
        return new ApplicationAttachment(
                1,
                7,
                ApplicationAttachmentType.RESUME,
                "resume.pdf",
                "application/pdf",
                size,
                LocalDateTime.of(2026, 8, 29, 1, 30));
    }
}
