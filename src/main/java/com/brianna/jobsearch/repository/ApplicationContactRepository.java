package com.brianna.jobsearch.repository;

import com.brianna.jobsearch.model.CompanyContact;
import com.brianna.jobsearch.model.CompanyContactRelationship;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

@Repository
public class ApplicationContactRepository {

    private final JdbcTemplate jdbcTemplate;

    private final RowMapper<CompanyContact> contactMapper = (rs, rowNum) -> new CompanyContact(
            rs.getLong("id"),
            rs.getString("company_key"),
            rs.getString("name"),
            rs.getString("role"),
            CompanyContactRelationship.valueOf(rs.getString("relationship_type")),
            rs.getString("email"),
            rs.getString("linkedin_url"),
            rs.getString("notes"),
            rs.getInt("has_photo") == 1,
            LocalDateTime.parse(rs.getString("created_at")),
            LocalDateTime.parse(rs.getString("updated_at")),
            rs.getLong("linked_application_count"));

    public ApplicationContactRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<CompanyContact> findByApplicationId(long applicationId) {
        return jdbcTemplate.query("""
                SELECT cc.id, cc.company_key, cc.name, cc.role, cc.relationship_type,
                       cc.email, cc.linkedin_url, cc.notes,
                       CASE WHEN cc.photo_data IS NULL THEN 0 ELSE 1 END AS has_photo,
                       cc.created_at, cc.updated_at,
                       (SELECT COUNT(*) FROM application_contact_links all_links WHERE all_links.contact_id = cc.id)
                           AS linked_application_count
                FROM company_contacts cc
                JOIN application_contact_links acl ON acl.contact_id = cc.id
                WHERE acl.application_id = ?
                ORDER BY
                    CASE cc.relationship_type
                        WHEN 'RECRUITER' THEN 0
                        WHEN 'HIRING_MANAGER' THEN 1
                        WHEN 'INTERVIEWER' THEN 2
                        WHEN 'REFERRAL' THEN 3
                        WHEN 'TEAM_MEMBER' THEN 4
                        WHEN 'NETWORKING_CONTACT' THEN 5
                        ELSE 6
                    END,
                    LOWER(cc.name)
                """, contactMapper, applicationId);
    }

    public List<CompanyContact> findLinkableForApplication(long applicationId, String companyKey) {
        return jdbcTemplate.query("""
                SELECT cc.id, cc.company_key, cc.name, cc.role, cc.relationship_type,
                       cc.email, cc.linkedin_url, cc.notes,
                       CASE WHEN cc.photo_data IS NULL THEN 0 ELSE 1 END AS has_photo,
                       cc.created_at, cc.updated_at,
                       (SELECT COUNT(*) FROM application_contact_links all_links WHERE all_links.contact_id = cc.id)
                           AS linked_application_count
                FROM company_contacts cc
                WHERE cc.company_key = ?
                  AND NOT EXISTS (
                      SELECT 1 FROM application_contact_links acl
                      WHERE acl.application_id = ? AND acl.contact_id = cc.id
                  )
                ORDER BY
                    CASE cc.relationship_type
                        WHEN 'RECRUITER' THEN 0
                        WHEN 'HIRING_MANAGER' THEN 1
                        WHEN 'INTERVIEWER' THEN 2
                        WHEN 'REFERRAL' THEN 3
                        WHEN 'TEAM_MEMBER' THEN 4
                        WHEN 'NETWORKING_CONTACT' THEN 5
                        ELSE 6
                    END,
                    LOWER(cc.name)
                """, contactMapper, companyKey, applicationId);
    }

    public boolean link(long applicationId, long contactId) {
        return jdbcTemplate.update("""
                INSERT OR IGNORE INTO application_contact_links (application_id, contact_id, created_at)
                VALUES (?, ?, ?)
                """, applicationId, contactId, LocalDateTime.now().toString()) > 0;
    }

    public int unlink(long applicationId, long contactId) {
        return jdbcTemplate.update("""
                DELETE FROM application_contact_links
                WHERE application_id = ? AND contact_id = ?
                """, applicationId, contactId);
    }

    public void deleteLinksByApplicationId(long applicationId) {
        jdbcTemplate.update("DELETE FROM application_contact_links WHERE application_id = ?", applicationId);
    }

}
