package com.brianna.jobsearch.repository;

import com.brianna.jobsearch.model.ApplicationState;
import com.brianna.jobsearch.model.ApplicationStatus;
import com.brianna.jobsearch.model.CompanyContact;
import com.brianna.jobsearch.model.CompanyContactRelationship;
import com.brianna.jobsearch.model.Priority;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class CompanyManagementRepository {

    private final JdbcTemplate jdbcTemplate;

    public CompanyManagementRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<CompanyNameRow> findCompanyNames() {
        return jdbcTemplate.query("""
                SELECT company,
                       company_domain,
                       COUNT(*) AS applications,
                       SUM(CASE WHEN state <> 'CLOSED' THEN 1 ELSE 0 END) AS open_applications,
                       MAX(applied_date) AS latest_applied_date
                FROM job_applications
                WHERE company IS NOT NULL AND TRIM(company) <> ''
                GROUP BY company, company_domain
                ORDER BY LOWER(company), company_domain
                """, (rs, rowNum) -> {
            String applied = rs.getString("latest_applied_date");
            return new CompanyNameRow(
                    rs.getString("company"),
                    rs.getString("company_domain"),
                    rs.getLong("applications"),
                    rs.getLong("open_applications"),
                    applied == null || applied.isBlank() ? null : LocalDate.parse(applied));
        });
    }

    public List<CompanyApplicationRow> findApplicationsForCompanyNames(List<String> companyNames) {
        if (companyNames == null || companyNames.isEmpty()) {
            return List.of();
        }
        return jdbcTemplate.query(
                """
                SELECT id, company, role, location, status, state, priority, applied_date, updated_at
                FROM job_applications
                WHERE company IN (%s)
                ORDER BY
                    CASE WHEN state = 'CLOSED' THEN 1 ELSE 0 END,
                    COALESCE(applied_date, '') DESC,
                    updated_at DESC,
                    id DESC
                """.formatted(placeholders(companyNames.size())),
                (rs, rowNum) -> {
                    String applied = rs.getString("applied_date");
                    String updated = rs.getString("updated_at");
                    String rawPriority = rs.getString("priority");
                    return new CompanyApplicationRow(
                            rs.getLong("id"),
                            rs.getString("company"),
                            rs.getString("role"),
                            rs.getString("location"),
                            ApplicationStatus.valueOf(rs.getString("status")),
                            ApplicationState.valueOf(rs.getString("state")),
                            rawPriority == null || rawPriority.isBlank()
                                    ? Priority.UNSPECIFIED
                                    : Priority.valueOf(rawPriority),
                            applied == null || applied.isBlank() ? null : LocalDate.parse(applied),
                            updated == null || updated.isBlank() ? null : LocalDateTime.parse(updated));
                },
                companyNames.toArray());
    }

    public String findCompanyNotes(String companyKey) {
        if (companyKey == null || companyKey.isBlank()) {
            return null;
        }
        return jdbcTemplate.query(
                "SELECT notes FROM company_notes WHERE company_key = ?",
                (rs, rowNum) -> rs.getString("notes"),
                companyKey).stream().findFirst().orElse(null);
    }

    public void saveCompanyNotes(String companyKey, String notes) {
        if (companyKey == null || companyKey.isBlank()) {
            throw new IllegalArgumentException("A company key is required to save notes.");
        }
        String clean = notes == null ? null : notes.trim();
        if (clean == null || clean.isBlank()) {
            jdbcTemplate.update("DELETE FROM company_notes WHERE company_key = ?", companyKey);
            return;
        }
        jdbcTemplate.update("""
                INSERT INTO company_notes (company_key, notes, updated_at)
                VALUES (?, ?, ?)
                ON CONFLICT(company_key) DO UPDATE SET
                    notes = excluded.notes,
                    updated_at = excluded.updated_at
                """, companyKey, clean, LocalDateTime.now().toString());
    }

    /**
     * Preserve company-level notes when aliases are renamed or merged. If more than
     * one source group has notes, keep each block rather than silently discarding one.
     */
    public void moveCompanyNotes(List<String> oldKeys, String newKey) {
        if (oldKeys == null || oldKeys.isEmpty() || newKey == null || newKey.isBlank()) {
            return;
        }

        List<String> uniqueKeys = oldKeys.stream()
                .filter(key -> key != null && !key.isBlank())
                .distinct()
                .toList();
        if (uniqueKeys.isEmpty()) return;

        List<String> noteBlocks = new ArrayList<>();
        for (String key : uniqueKeys) {
            String notes = findCompanyNotes(key);
            if (notes != null && !notes.isBlank() && !noteBlocks.contains(notes.trim())) {
                noteBlocks.add(notes.trim());
            }
        }

        String existingTarget = findCompanyNotes(newKey);
        if (existingTarget != null && !existingTarget.isBlank() && !noteBlocks.contains(existingTarget.trim())) {
            noteBlocks.add(existingTarget.trim());
        }

        if (!noteBlocks.isEmpty()) {
            saveCompanyNotes(newKey, String.join("\n\n---\n\n", noteBlocks));
        }

        for (String key : uniqueKeys) {
            if (!key.equals(newKey)) {
                jdbcTemplate.update("DELETE FROM company_notes WHERE company_key = ?", key);
            }
        }
    }


    public List<CompanyContact> findContacts(String companyKey) {
        if (companyKey == null || companyKey.isBlank()) {
            return List.of();
        }
        return jdbcTemplate.query("""
                SELECT id, company_key, name, role, relationship_type, email, linkedin_url, notes,
                       CASE WHEN photo_data IS NULL THEN 0 ELSE 1 END AS has_photo,
                       created_at, updated_at,
                       (SELECT COUNT(*) FROM application_contact_links acl WHERE acl.contact_id = company_contacts.id)
                           AS linked_application_count
                FROM company_contacts
                WHERE company_key = ?
                ORDER BY
                    CASE relationship_type
                        WHEN 'RECRUITER' THEN 0
                        WHEN 'HIRING_MANAGER' THEN 1
                        WHEN 'INTERVIEWER' THEN 2
                        WHEN 'REFERRAL' THEN 3
                        WHEN 'TEAM_MEMBER' THEN 4
                        WHEN 'NETWORKING_CONTACT' THEN 5
                        ELSE 6
                    END,
                    LOWER(name)
                """, (rs, rowNum) -> new CompanyContact(
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
                        rs.getLong("linked_application_count")),
                companyKey);
    }

    public CompanyContact findContact(long id) {
        return jdbcTemplate.query("""
                SELECT id, company_key, name, role, relationship_type, email, linkedin_url, notes,
                       CASE WHEN photo_data IS NULL THEN 0 ELSE 1 END AS has_photo,
                       created_at, updated_at,
                       (SELECT COUNT(*) FROM application_contact_links acl WHERE acl.contact_id = company_contacts.id)
                           AS linked_application_count
                FROM company_contacts
                WHERE id = ?
                """, (rs, rowNum) -> new CompanyContact(
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
                        rs.getLong("linked_application_count")),
                id).stream().findFirst().orElse(null);
    }

    public ContactPhoto findContactPhoto(long id) {
        return jdbcTemplate.query("""
                SELECT photo_mime_type, photo_data
                FROM company_contacts
                WHERE id = ? AND photo_data IS NOT NULL
                """, (rs, rowNum) -> new ContactPhoto(
                        rs.getString("photo_mime_type"),
                        rs.getBytes("photo_data")),
                id).stream().findFirst().orElse(null);
    }

    public long insertContact(
            String companyKey,
            String name,
            String role,
            String relationshipType,
            String email,
            String linkedinUrl,
            String notes,
            String photoMimeType,
            byte[] photoData) {
        LocalDateTime now = LocalDateTime.now();
        return jdbcTemplate.queryForObject("""
                INSERT INTO company_contacts (
                    company_key, name, role, relationship_type, email, linkedin_url, notes,
                    photo_mime_type, photo_data, created_at, updated_at
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                RETURNING id
                """, Long.class, companyKey, name, role, relationshipType, email, linkedinUrl, notes,
                photoMimeType, photoData, now.toString(), now.toString());
    }

    public int updateContact(
            long id,
            String name,
            String role,
            String relationshipType,
            String email,
            String linkedinUrl,
            String notes,
            boolean replacePhoto,
            String photoMimeType,
            byte[] photoData) {
        if (replacePhoto) {
            return jdbcTemplate.update("""
                    UPDATE company_contacts
                    SET name = ?, role = ?, relationship_type = ?, email = ?, linkedin_url = ?, notes = ?,
                        photo_mime_type = ?, photo_data = ?, updated_at = ?
                    WHERE id = ?
                    """, name, role, relationshipType, email, linkedinUrl, notes,
                    photoMimeType, photoData, LocalDateTime.now().toString(), id);
        }
        return jdbcTemplate.update("""
                UPDATE company_contacts
                SET name = ?, role = ?, relationship_type = ?, email = ?, linkedin_url = ?, notes = ?, updated_at = ?
                WHERE id = ?
                """, name, role, relationshipType, email, linkedinUrl, notes, LocalDateTime.now().toString(), id);
    }

    public int deleteContact(long id) {
        jdbcTemplate.update("DELETE FROM application_contact_links WHERE contact_id = ?", id);
        return jdbcTemplate.update("DELETE FROM company_contacts WHERE id = ?", id);
    }

    public void moveCompanyContacts(List<String> oldKeys, String newKey) {
        if (oldKeys == null || oldKeys.isEmpty() || newKey == null || newKey.isBlank()) {
            return;
        }
        List<String> keys = oldKeys.stream()
                .filter(key -> key != null && !key.isBlank() && !key.equals(newKey))
                .distinct()
                .toList();
        if (keys.isEmpty()) return;
        List<Object> params = new ArrayList<>();
        params.add(newKey);
        params.add(LocalDateTime.now().toString());
        params.addAll(keys);
        jdbcTemplate.update(
                "UPDATE company_contacts SET company_key = ?, updated_at = ? WHERE company_key IN ("
                        + placeholders(keys.size()) + ")",
                params.toArray());
    }

    /**
     * Company cleanup is metadata maintenance, not application activity. These
     * updates intentionally leave updated_at untouched so stale-review and
     * dashboard ordering do not change just because branding was cleaned up.
     */
    public int updateDomainForCompanyNames(List<String> companyNames, String domain) {
        if (companyNames == null || companyNames.isEmpty()) {
            return 0;
        }
        List<Object> params = new ArrayList<>();
        params.add(domain);
        params.addAll(companyNames);
        return jdbcTemplate.update(
                "UPDATE job_applications SET company_domain = ? WHERE company IN (" + placeholders(companyNames.size()) + ")",
                params.toArray());
    }

    public int renameCompanyNames(List<String> companyNames, String canonicalName) {
        if (companyNames == null || companyNames.isEmpty()) {
            return 0;
        }
        List<Object> params = new ArrayList<>();
        params.add(canonicalName);
        params.addAll(companyNames);
        return jdbcTemplate.update(
                "UPDATE job_applications SET company = ? WHERE company IN (" + placeholders(companyNames.size()) + ")",
                params.toArray());
    }

    private String placeholders(int count) {
        return String.join(",", java.util.Collections.nCopies(count, "?"));
    }

    public record ContactPhoto(String mimeType, byte[] data) {
    }

    public record CompanyNameRow(
            String companyName,
            String companyDomain,
            long applications,
            long openApplications,
            LocalDate latestAppliedDate) {
    }

    public record CompanyApplicationRow(
            long id,
            String companyName,
            String role,
            String location,
            ApplicationStatus status,
            ApplicationState state,
            Priority priority,
            LocalDate appliedDate,
            LocalDateTime updatedAt) {
    }
}
