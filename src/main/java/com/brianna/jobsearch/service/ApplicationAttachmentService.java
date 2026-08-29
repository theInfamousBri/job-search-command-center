package com.brianna.jobsearch.service;

import com.brianna.jobsearch.model.ApplicationAttachment;
import com.brianna.jobsearch.model.ApplicationAttachmentType;
import com.brianna.jobsearch.repository.ApplicationAttachmentRepository;
import java.io.IOException;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class ApplicationAttachmentService {

    public static final long MAX_ATTACHMENT_BYTES = 10L * 1024L * 1024L;

    private final ApplicationAttachmentRepository repository;

    public ApplicationAttachmentService(ApplicationAttachmentRepository repository) {
        this.repository = repository;
    }

    public List<ApplicationAttachment> forApplication(long applicationId) {
        return repository.findByApplicationId(applicationId);
    }

    public ApplicationAttachment upload(long applicationId, String attachmentType, MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Choose a file to attach.");
        }
        if (file.getSize() > MAX_ATTACHMENT_BYTES) {
            throw new IllegalArgumentException("Attachments are limited to 10 MB each.");
        }

        String fileName = cleanFileName(file.getOriginalFilename());
        String mimeType = file.getContentType();
        if (mimeType == null || mimeType.isBlank()) {
            mimeType = "application/octet-stream";
        }

        byte[] data;
        try {
            data = file.getBytes();
        } catch (IOException ex) {
            throw new IllegalArgumentException("The selected file could not be read.", ex);
        }

        long id = repository.insert(
                applicationId,
                ApplicationAttachmentType.fromFormValue(attachmentType),
                fileName,
                mimeType,
                data);

        return repository.findMetadata(applicationId, id)
                .orElseThrow(() -> new IllegalStateException("Attachment was saved but could not be reloaded."));
    }

    public ApplicationAttachment metadata(long applicationId, long attachmentId) {
        return repository.findMetadata(applicationId, attachmentId)
                .orElseThrow(() -> new IllegalArgumentException("Attachment not found: " + attachmentId));
    }

    public ApplicationAttachmentRepository.AttachmentContent download(long applicationId, long attachmentId) {
        return repository.findContent(applicationId, attachmentId)
                .orElseThrow(() -> new IllegalArgumentException("Attachment not found: " + attachmentId));
    }

    public void delete(long applicationId, long attachmentId) {
        if (repository.delete(applicationId, attachmentId) == 0) {
            throw new IllegalArgumentException("Attachment not found: " + attachmentId);
        }
    }

    private String cleanFileName(String originalName) {
        String candidate = originalName == null ? "attachment" : originalName.replace('\\', '/');
        int lastSlash = candidate.lastIndexOf('/');
        if (lastSlash >= 0) {
            candidate = candidate.substring(lastSlash + 1);
        }
        candidate = candidate.replaceAll("[\\p{Cntrl}]", "").trim();
        if (candidate.isBlank()) {
            candidate = "attachment";
        }
        if (candidate.length() > 220) {
            candidate = candidate.substring(0, 220);
        }
        return candidate;
    }
}
