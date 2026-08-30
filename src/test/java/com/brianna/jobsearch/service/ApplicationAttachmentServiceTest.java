package com.brianna.jobsearch.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.brianna.jobsearch.exception.ResourceNotFoundException;
import com.brianna.jobsearch.model.ApplicationAttachment;
import com.brianna.jobsearch.model.ApplicationAttachmentType;
import com.brianna.jobsearch.repository.ApplicationAttachmentRepository;
import java.time.LocalDateTime;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

@ExtendWith(MockitoExtension.class)
class ApplicationAttachmentServiceTest {

    @Mock
    private ApplicationAttachmentRepository repository;

    @InjectMocks
    private ApplicationAttachmentService service;

    @Test
    void uploadSanitizesFilenameAndPersistsOriginalBytesOnce() {
        byte[] bytes = "resume-bytes".getBytes();
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "C:\\Users\\Brianna\\resume.pdf",
                "application/pdf",
                bytes);
        ApplicationAttachment saved = new ApplicationAttachment(
                91L, 12L, ApplicationAttachmentType.RESUME, "resume.pdf", "application/pdf",
                bytes.length, LocalDateTime.of(2026, 8, 29, 12, 0));
        when(repository.insert(eq(12L), eq(ApplicationAttachmentType.RESUME), eq("resume.pdf"),
                eq("application/pdf"), any(byte[].class))).thenReturn(91L);
        when(repository.findMetadata(12L, 91L)).thenReturn(Optional.of(saved));

        ApplicationAttachment result = service.upload(12L, "RESUME", file);

        assertThat(result).isEqualTo(saved);
        verify(repository).insert(12L, ApplicationAttachmentType.RESUME, "resume.pdf", "application/pdf", bytes);
    }

    @Test
    void uploadFallsBackToBinaryMimeTypeWhenBrowserDoesNotProvideOne() {
        byte[] bytes = {1, 2, 3};
        MockMultipartFile file = new MockMultipartFile("file", "sample.bin", null, bytes);
        ApplicationAttachment saved = new ApplicationAttachment(
                3L, 2L, ApplicationAttachmentType.OTHER, "sample.bin", "application/octet-stream",
                bytes.length, LocalDateTime.of(2026, 8, 29, 12, 0));
        when(repository.insert(2L, ApplicationAttachmentType.OTHER, "sample.bin", "application/octet-stream", bytes))
                .thenReturn(3L);
        when(repository.findMetadata(2L, 3L)).thenReturn(Optional.of(saved));

        service.upload(2L, "OTHER", file);

        verify(repository).insert(2L, ApplicationAttachmentType.OTHER, "sample.bin", "application/octet-stream", bytes);
    }

    @Test
    void uploadRejectsEmptyAndOversizedFilesBeforeWriting() {
        MockMultipartFile empty = new MockMultipartFile("file", "resume.pdf", "application/pdf", new byte[0]);
        MultipartFile oversized = org.mockito.Mockito.mock(MultipartFile.class);
        when(oversized.isEmpty()).thenReturn(false);
        when(oversized.getSize()).thenReturn(ApplicationAttachmentService.MAX_ATTACHMENT_BYTES + 1);

        assertThatThrownBy(() -> service.upload(1L, "RESUME", empty))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Choose a file to attach.");
        assertThatThrownBy(() -> service.upload(1L, "RESUME", oversized))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Attachments are limited to 10 MB each.");
    }

    @Test
    void attachmentLookupsRemainScopedToTheirApplication() {
        when(repository.findMetadata(22L, 99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.metadata(22L, 99L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Attachment not found: 99");
    }

    @Test
    void deletingMissingAttachmentReturnsResourceNotFound() {
        when(repository.delete(22L, 99L)).thenReturn(0);

        assertThatThrownBy(() -> service.delete(22L, 99L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Attachment not found: 99");
    }
}
