package com.brianna.jobsearch.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.brianna.jobsearch.exception.ResourceNotFoundException;
import com.brianna.jobsearch.model.MaterialFile;
import com.brianna.jobsearch.model.MaterialType;
import com.brianna.jobsearch.repository.MaterialRepository;
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
class MaterialServiceTest {

    @Mock
    private MaterialRepository repository;

    @InjectMocks
    private MaterialService service;

    @Test
    void uploadStoresUniqueBytesOnceAndUsesFriendlyName() {
        byte[] bytes = "resume-v1".getBytes();
        MockMultipartFile file = new MockMultipartFile(
                "file", "C:\\Users\\Brianna\\backend-resume.pdf", "application/pdf", bytes);
        MaterialFile saved = material(7L, "Backend / Platform · Aug 2026", 0);

        when(repository.findBySha256(any())).thenReturn(Optional.empty());
        when(repository.insert(
                eq(MaterialType.RESUME), eq("Backend / Platform · Aug 2026"), eq("backend-resume.pdf"),
                eq("application/pdf"), any(), eq(bytes), eq(null)))
                .thenReturn(7L);
        when(repository.findMetadata(7L)).thenReturn(Optional.of(saved));

        MaterialService.SaveResult result = service.upload(
                "RESUME", "Backend / Platform · Aug 2026", null, file);

        assertThat(result.created()).isTrue();
        assertThat(result.material()).isEqualTo(saved);
        verify(repository).insert(
                eq(MaterialType.RESUME), eq("Backend / Platform · Aug 2026"), eq("backend-resume.pdf"),
                eq("application/pdf"), any(), eq(bytes), eq(null));
    }

    @Test
    void duplicateUploadReusesExistingBlobInsteadOfInsertingAgain() {
        byte[] bytes = "same-resume".getBytes();
        MockMultipartFile file = new MockMultipartFile("file", "duplicate.pdf", "application/pdf", bytes);
        MaterialFile existing = material(3L, "Backend Resume", 8);
        when(repository.findBySha256(any())).thenReturn(Optional.of(existing));

        MaterialService.SaveResult result = service.upload("RESUME", "Different label", null, file);

        assertThat(result.created()).isFalse();
        assertThat(result.material()).isEqualTo(existing);
        verify(repository, never()).insert(any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    void uploadAndLinkReusesDuplicateAndCreatesOnlyTheApplicationReference() {
        byte[] bytes = "same-resume".getBytes();
        MockMultipartFile file = new MockMultipartFile("file", "duplicate.pdf", "application/pdf", bytes);
        MaterialFile existing = material(3L, "Backend Resume", 4);
        MaterialFile afterLink = material(3L, "Backend Resume", 5);
        when(repository.findBySha256(any())).thenReturn(Optional.of(existing));
        when(repository.findMetadata(3L)).thenReturn(Optional.of(afterLink));
        when(repository.link(44L, 3L)).thenReturn(true);

        MaterialService.SaveResult result = service.uploadAndLink(44L, "RESUME", null, null, file);

        assertThat(result.created()).isFalse();
        assertThat(result.material().linkedApplicationCount()).isEqualTo(5);
        verify(repository).link(44L, 3L);
        verify(repository, never()).insert(any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    void linkedMaterialCannotBeDeletedUntilApplicationsAreUnlinked() {
        when(repository.findMetadata(5L)).thenReturn(Optional.of(material(5L, "Backend Resume", 3)));

        assertThatThrownBy(() -> service.delete(5L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Unlink this material from 3 applications before deleting it.");
        verify(repository, never()).delete(5L);
    }

    @Test
    void materialLookupsUseResourceNotFoundForMissingIds() {
        when(repository.findMetadata(404L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.get(404L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Material not found: 404");
    }

    @Test
    void uploadRejectsEmptyAndOversizedFilesBeforeHashingOrWriting() {
        MockMultipartFile empty = new MockMultipartFile("file", "resume.pdf", "application/pdf", new byte[0]);
        MultipartFile oversized = org.mockito.Mockito.mock(MultipartFile.class);
        when(oversized.isEmpty()).thenReturn(false);
        when(oversized.getSize()).thenReturn(MaterialService.MAX_MATERIAL_BYTES + 1);

        assertThatThrownBy(() -> service.upload("RESUME", null, null, empty))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Choose a file to add to the library.");
        assertThatThrownBy(() -> service.upload("RESUME", null, null, oversized))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Materials are limited to 10 MB each.");
    }

    private MaterialFile material(long id, String name, long linkCount) {
        return new MaterialFile(
                id,
                MaterialType.RESUME,
                name,
                "backend-resume.pdf",
                "application/pdf",
                1234,
                "abc123",
                null,
                LocalDateTime.of(2026, 8, 29, 12, 0),
                LocalDateTime.of(2026, 8, 29, 12, 0),
                linkCount);
    }
}
