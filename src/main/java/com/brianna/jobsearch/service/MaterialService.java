package com.brianna.jobsearch.service;

import com.brianna.jobsearch.exception.ResourceNotFoundException;
import com.brianna.jobsearch.model.MaterialApplicationReference;
import com.brianna.jobsearch.model.MaterialFile;
import com.brianna.jobsearch.model.MaterialLibrarySummary;
import com.brianna.jobsearch.model.MaterialType;
import com.brianna.jobsearch.repository.MaterialRepository;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class MaterialService {

    public static final long MAX_MATERIAL_BYTES = 10L * 1024L * 1024L;

    private final MaterialRepository repository;

    public MaterialService(MaterialRepository repository) {
        this.repository = repository;
    }

    public List<MaterialFile> all(String query) {
        return repository.findAll(query);
    }

    public MaterialLibrarySummary summary() {
        return repository.summary();
    }

    public List<MaterialFile> forApplication(long applicationId) {
        return repository.findByApplicationId(applicationId);
    }

    public List<MaterialFile> linkableForApplication(long applicationId) {
        return repository.findLinkableForApplication(applicationId);
    }

    public List<MaterialApplicationReference> applicationsForMaterial(long materialId) {
        get(materialId);
        return repository.findApplications(materialId);
    }

    public MaterialFile get(long materialId) {
        return repository.findMetadata(materialId)
                .orElseThrow(() -> new ResourceNotFoundException("Material not found: " + materialId));
    }

    public SaveResult upload(String materialType, String displayName, String notes, MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Choose a file to add to the library.");
        }
        if (file.getSize() > MAX_MATERIAL_BYTES) {
            throw new IllegalArgumentException("Materials are limited to 10 MB each.");
        }

        byte[] data;
        try {
            data = file.getBytes();
        } catch (IOException ex) {
            throw new IllegalArgumentException("The selected file could not be read.", ex);
        }

        String hash = sha256(data);
        var existing = repository.findBySha256(hash);
        if (existing.isPresent()) {
            return new SaveResult(existing.get(), false);
        }

        String fileName = cleanFileName(file.getOriginalFilename());
        String cleanDisplayName = cleanDisplayName(displayName, fileName);
        String cleanNotes = cleanNotes(notes);
        String mimeType = file.getContentType();
        if (mimeType == null || mimeType.isBlank()) {
            mimeType = "application/octet-stream";
        }

        long id = repository.insert(
                MaterialType.fromFormValue(materialType),
                cleanDisplayName,
                fileName,
                mimeType,
                hash,
                data,
                cleanNotes);
        return new SaveResult(get(id), true);
    }

    public SaveResult uploadAndLink(
            long applicationId,
            String materialType,
            String displayName,
            String notes,
            MultipartFile file) {
        SaveResult result = upload(materialType, displayName, notes, file);
        repository.link(applicationId, result.material().id());
        return new SaveResult(get(result.material().id()), result.created());
    }

    public boolean link(long applicationId, long materialId) {
        get(materialId);
        return repository.link(applicationId, materialId);
    }

    public void unlink(long applicationId, long materialId) {
        get(materialId);
        if (repository.unlink(applicationId, materialId) == 0) {
            throw new ResourceNotFoundException("Material link not found: " + materialId);
        }
    }

    public MaterialRepository.MaterialContent download(long materialId) {
        return repository.findContent(materialId)
                .orElseThrow(() -> new ResourceNotFoundException("Material not found: " + materialId));
    }

    public MaterialFile update(long materialId, String displayName, String notes) {
        MaterialFile current = get(materialId);
        repository.updateMetadata(
                materialId,
                cleanDisplayName(displayName, current.fileName()),
                cleanNotes(notes));
        return get(materialId);
    }

    public void delete(long materialId) {
        MaterialFile material = get(materialId);
        if (material.linkedApplicationCount() > 0) {
            String noun = material.linkedApplicationCount() == 1 ? "application" : "applications";
            throw new IllegalArgumentException(
                    "Unlink this material from " + material.linkedApplicationCount() + " " + noun + " before deleting it.");
        }
        repository.delete(materialId);
    }

    private String cleanDisplayName(String displayName, String fileName) {
        String candidate = displayName == null ? "" : displayName.trim();
        if (candidate.isBlank()) {
            candidate = fileName;
            int dot = candidate.lastIndexOf('.');
            if (dot > 0) {
                candidate = candidate.substring(0, dot);
            }
        }
        candidate = candidate.replaceAll("[\\p{Cntrl}]", "").trim();
        if (candidate.length() > 160) {
            candidate = candidate.substring(0, 160);
        }
        if (candidate.isBlank()) {
            return "Material";
        }
        return candidate;
    }

    private String cleanNotes(String notes) {
        if (notes == null || notes.isBlank()) {
            return null;
        }
        String candidate = notes.trim();
        return candidate.length() > 2000 ? candidate.substring(0, 2000) : candidate;
    }

    private String cleanFileName(String originalName) {
        String candidate = originalName == null ? "material" : originalName.replace('\\', '/');
        int lastSlash = candidate.lastIndexOf('/');
        if (lastSlash >= 0) {
            candidate = candidate.substring(lastSlash + 1);
        }
        candidate = candidate.replaceAll("[\\p{Cntrl}]", "").trim();
        if (candidate.isBlank()) {
            candidate = "material";
        }
        return candidate.length() > 220 ? candidate.substring(0, 220) : candidate;
    }

    private String sha256(byte[] data) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(data));
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 is not available.", ex);
        }
    }

    public record SaveResult(MaterialFile material, boolean created) {}
}
