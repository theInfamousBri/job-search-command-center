package com.brianna.jobsearch.repository;

import com.brianna.jobsearch.model.ApplicationAttachment;
import com.brianna.jobsearch.model.ApplicationAttachmentType;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

@Repository
public class ApplicationAttachmentRepository {

    private final JdbcTemplate jdbcTemplate;

    private final RowMapper<ApplicationAttachment> metadataMapper = (rs, rowNum) -> new ApplicationAttachment(
            rs.getLong("id"),
            rs.getLong("application_id"),
            ApplicationAttachmentType.valueOf(rs.getString("attachment_type")),
            rs.getString("file_name"),
            rs.getString("mime_type"),
            rs.getLong("file_size"),
            LocalDateTime.parse(rs.getString("created_at")));

    public ApplicationAttachmentRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<ApplicationAttachment> findByApplicationId(long applicationId) {
        return jdbcTemplate.query("""
                SELECT id, application_id, attachment_type, file_name, mime_type, file_size, created_at
                FROM application_attachments
                WHERE application_id = ?
                ORDER BY CASE attachment_type
                    WHEN 'RESUME' THEN 0
                    WHEN 'COVER_LETTER' THEN 1
                    ELSE 2
                END,
                created_at DESC,
                id DESC
                """, metadataMapper, applicationId);
    }

    public Optional<ApplicationAttachment> findMetadata(long applicationId, long attachmentId) {
        return jdbcTemplate.query("""
                SELECT id, application_id, attachment_type, file_name, mime_type, file_size, created_at
                FROM application_attachments
                WHERE application_id = ? AND id = ?
                """, metadataMapper, applicationId, attachmentId).stream().findFirst();
    }

    public Optional<AttachmentContent> findContent(long applicationId, long attachmentId) {
        return jdbcTemplate.query("""
                SELECT id, application_id, attachment_type, file_name, mime_type, file_size, file_data, created_at
                FROM application_attachments
                WHERE application_id = ? AND id = ?
                """, (rs, rowNum) -> new AttachmentContent(
                        new ApplicationAttachment(
                                rs.getLong("id"),
                                rs.getLong("application_id"),
                                ApplicationAttachmentType.valueOf(rs.getString("attachment_type")),
                                rs.getString("file_name"),
                                rs.getString("mime_type"),
                                rs.getLong("file_size"),
                                LocalDateTime.parse(rs.getString("created_at"))),
                        rs.getBytes("file_data")), applicationId, attachmentId).stream().findFirst();
    }

    public long insert(
            long applicationId,
            ApplicationAttachmentType attachmentType,
            String fileName,
            String mimeType,
            byte[] data) {
        return jdbcTemplate.queryForObject("""
                INSERT INTO application_attachments (
                    application_id, attachment_type, file_name, mime_type, file_size, file_data, created_at
                ) VALUES (?, ?, ?, ?, ?, ?, ?)
                RETURNING id
                """, Long.class,
                applicationId,
                attachmentType.name(),
                fileName,
                mimeType,
                data.length,
                data,
                LocalDateTime.now().toString());
    }

    public int delete(long applicationId, long attachmentId) {
        return jdbcTemplate.update(
                "DELETE FROM application_attachments WHERE application_id = ? AND id = ?",
                applicationId,
                attachmentId);
    }

    public void deleteByApplicationId(long applicationId) {
        jdbcTemplate.update("DELETE FROM application_attachments WHERE application_id = ?", applicationId);
    }

    public record AttachmentContent(ApplicationAttachment metadata, byte[] data) {}
}
