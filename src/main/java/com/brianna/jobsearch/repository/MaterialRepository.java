package com.brianna.jobsearch.repository;

import com.brianna.jobsearch.model.MaterialApplicationReference;
import com.brianna.jobsearch.model.MaterialFile;
import com.brianna.jobsearch.model.MaterialLibrarySummary;
import com.brianna.jobsearch.model.MaterialType;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

@Repository
public class MaterialRepository {

    private final JdbcTemplate jdbcTemplate;

    private final RowMapper<MaterialFile> metadataMapper = (rs, rowNum) -> new MaterialFile(
            rs.getLong("id"),
            MaterialType.valueOf(rs.getString("material_type")),
            rs.getString("display_name"),
            rs.getString("file_name"),
            rs.getString("mime_type"),
            rs.getLong("file_size"),
            rs.getString("sha256"),
            rs.getString("notes"),
            LocalDateTime.parse(rs.getString("created_at")),
            LocalDateTime.parse(rs.getString("updated_at")),
            rs.getLong("linked_application_count"));

    public MaterialRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<MaterialFile> findAll(String query) {
        String normalized = query == null ? "" : query.trim().toLowerCase();
        String like = "%" + normalized + "%";
        return jdbcTemplate.query("""
                SELECT mf.id, mf.material_type, mf.display_name, mf.file_name, mf.mime_type,
                       mf.file_size, mf.sha256, mf.notes, mf.created_at, mf.updated_at,
                       COUNT(aml.application_id) AS linked_application_count
                FROM material_files mf
                LEFT JOIN application_material_links aml ON aml.material_id = mf.id
                WHERE ? = ''
                   OR LOWER(mf.display_name) LIKE ?
                   OR LOWER(mf.file_name) LIKE ?
                   OR LOWER(COALESCE(mf.notes, '')) LIKE ?
                GROUP BY mf.id, mf.material_type, mf.display_name, mf.file_name, mf.mime_type,
                         mf.file_size, mf.sha256, mf.notes, mf.created_at, mf.updated_at
                ORDER BY CASE mf.material_type WHEN 'RESUME' THEN 0 WHEN 'COVER_LETTER' THEN 1 ELSE 2 END,
                         mf.updated_at DESC, mf.id DESC
                """, metadataMapper, normalized, like, like, like);
    }

    public List<MaterialFile> findByApplicationId(long applicationId) {
        return jdbcTemplate.query("""
                SELECT mf.id, mf.material_type, mf.display_name, mf.file_name, mf.mime_type,
                       mf.file_size, mf.sha256, mf.notes, mf.created_at, mf.updated_at,
                       (SELECT COUNT(*) FROM application_material_links all_links WHERE all_links.material_id = mf.id)
                           AS linked_application_count
                FROM material_files mf
                JOIN application_material_links aml ON aml.material_id = mf.id
                WHERE aml.application_id = ?
                ORDER BY CASE mf.material_type WHEN 'RESUME' THEN 0 WHEN 'COVER_LETTER' THEN 1 ELSE 2 END,
                         mf.updated_at DESC, mf.id DESC
                """, metadataMapper, applicationId);
    }

    public List<MaterialFile> findLinkableForApplication(long applicationId) {
        return jdbcTemplate.query("""
                SELECT mf.id, mf.material_type, mf.display_name, mf.file_name, mf.mime_type,
                       mf.file_size, mf.sha256, mf.notes, mf.created_at, mf.updated_at,
                       (SELECT COUNT(*) FROM application_material_links all_links WHERE all_links.material_id = mf.id)
                           AS linked_application_count
                FROM material_files mf
                WHERE NOT EXISTS (
                    SELECT 1 FROM application_material_links aml
                    WHERE aml.application_id = ? AND aml.material_id = mf.id
                )
                ORDER BY CASE mf.material_type WHEN 'RESUME' THEN 0 WHEN 'COVER_LETTER' THEN 1 ELSE 2 END,
                         mf.updated_at DESC, mf.id DESC
                """, metadataMapper, applicationId);
    }

    public Optional<MaterialFile> findMetadata(long materialId) {
        return jdbcTemplate.query("""
                SELECT mf.id, mf.material_type, mf.display_name, mf.file_name, mf.mime_type,
                       mf.file_size, mf.sha256, mf.notes, mf.created_at, mf.updated_at,
                       (SELECT COUNT(*) FROM application_material_links aml WHERE aml.material_id = mf.id)
                           AS linked_application_count
                FROM material_files mf
                WHERE mf.id = ?
                """, metadataMapper, materialId).stream().findFirst();
    }

    public Optional<MaterialFile> findBySha256(String sha256) {
        return jdbcTemplate.query("""
                SELECT mf.id, mf.material_type, mf.display_name, mf.file_name, mf.mime_type,
                       mf.file_size, mf.sha256, mf.notes, mf.created_at, mf.updated_at,
                       (SELECT COUNT(*) FROM application_material_links aml WHERE aml.material_id = mf.id)
                           AS linked_application_count
                FROM material_files mf
                WHERE mf.sha256 = ?
                """, metadataMapper, sha256).stream().findFirst();
    }

    public Optional<MaterialContent> findContent(long materialId) {
        return jdbcTemplate.query("""
                SELECT mf.id, mf.material_type, mf.display_name, mf.file_name, mf.mime_type,
                       mf.file_size, mf.sha256, mf.notes, mf.file_data, mf.created_at, mf.updated_at,
                       (SELECT COUNT(*) FROM application_material_links aml WHERE aml.material_id = mf.id)
                           AS linked_application_count
                FROM material_files mf
                WHERE mf.id = ?
                """, (rs, rowNum) -> new MaterialContent(metadataMapper.mapRow(rs, rowNum), rs.getBytes("file_data")),
                materialId).stream().findFirst();
    }

    public long insert(
            MaterialType materialType,
            String displayName,
            String fileName,
            String mimeType,
            String sha256,
            byte[] data,
            String notes) {
        String now = LocalDateTime.now().toString();
        return jdbcTemplate.queryForObject("""
                INSERT INTO material_files (
                    material_type, display_name, file_name, mime_type, file_size,
                    sha256, file_data, notes, created_at, updated_at
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                RETURNING id
                """, Long.class,
                materialType.name(), displayName, fileName, mimeType, data.length,
                sha256, data, notes, now, now);
    }

    public void updateMetadata(long materialId, String displayName, String notes) {
        jdbcTemplate.update("""
                UPDATE material_files
                SET display_name = ?, notes = ?, updated_at = ?
                WHERE id = ?
                """, displayName, notes, LocalDateTime.now().toString(), materialId);
    }

    public int delete(long materialId) {
        return jdbcTemplate.update("DELETE FROM material_files WHERE id = ?", materialId);
    }

    public boolean link(long applicationId, long materialId) {
        return jdbcTemplate.update("""
                INSERT OR IGNORE INTO application_material_links (application_id, material_id, created_at)
                VALUES (?, ?, ?)
                """, applicationId, materialId, LocalDateTime.now().toString()) > 0;
    }

    public int unlink(long applicationId, long materialId) {
        return jdbcTemplate.update("""
                DELETE FROM application_material_links
                WHERE application_id = ? AND material_id = ?
                """, applicationId, materialId);
    }

    public void deleteLinksByApplicationId(long applicationId) {
        jdbcTemplate.update("DELETE FROM application_material_links WHERE application_id = ?", applicationId);
    }

    public List<MaterialApplicationReference> findApplications(long materialId) {
        return jdbcTemplate.query("""
                SELECT ja.id, ja.company, ja.role
                FROM application_material_links aml
                JOIN job_applications ja ON ja.id = aml.application_id
                WHERE aml.material_id = ?
                ORDER BY ja.updated_at DESC, ja.id DESC
                """, (rs, rowNum) -> new MaterialApplicationReference(
                        rs.getLong("id"), rs.getString("company"), rs.getString("role")), materialId);
    }

    public MaterialLibrarySummary summary() {
        return jdbcTemplate.queryForObject("""
                SELECT
                    COUNT(*) AS materials,
                    COALESCE(SUM(CASE WHEN material_type = 'RESUME' THEN 1 ELSE 0 END), 0) AS resumes,
                    COALESCE(SUM(link_count), 0) AS application_links,
                    COALESCE(SUM(file_size), 0) AS stored_bytes,
                    COALESCE(SUM(file_size * CASE WHEN link_count > 1 THEN link_count - 1 ELSE 0 END), 0)
                        AS avoided_duplicate_bytes
                FROM (
                    SELECT mf.id, mf.material_type, mf.file_size, COUNT(aml.application_id) AS link_count
                    FROM material_files mf
                    LEFT JOIN application_material_links aml ON aml.material_id = mf.id
                    GROUP BY mf.id, mf.material_type, mf.file_size
                ) material_usage
                """, (rs, rowNum) -> new MaterialLibrarySummary(
                rs.getLong("materials"),
                rs.getLong("resumes"),
                rs.getLong("application_links"),
                rs.getLong("stored_bytes"),
                rs.getLong("avoided_duplicate_bytes")));
    }

    public record MaterialContent(MaterialFile metadata, byte[] data) {}
}
