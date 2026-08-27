package com.brianna.jobsearch.repository;

import java.time.LocalDateTime;
import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class CompanyLogoRepository {

    private final JdbcTemplate jdbcTemplate;

    public CompanyLogoRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public Optional<CompanyLogo> findByDomain(String domain) {
        return jdbcTemplate.query("""
                SELECT domain, mime_type, image_data, source_url, updated_at
                FROM company_logos
                WHERE domain = ?
                """, (rs, rowNum) -> new CompanyLogo(
                rs.getString("domain"),
                rs.getString("mime_type"),
                rs.getBytes("image_data"),
                rs.getString("source_url"),
                LocalDateTime.parse(rs.getString("updated_at"))), domain).stream().findFirst();
    }

    public boolean exists(String domain) {
        Long count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM company_logos WHERE domain = ?",
                Long.class,
                domain);
        return count != null && count > 0;
    }

    public void upsert(String domain, String mimeType, byte[] data, String sourceUrl) {
        jdbcTemplate.update("""
                INSERT INTO company_logos (domain, mime_type, image_data, source_url, updated_at)
                VALUES (?, ?, ?, ?, ?)
                ON CONFLICT(domain) DO UPDATE SET
                    mime_type = excluded.mime_type,
                    image_data = excluded.image_data,
                    source_url = excluded.source_url,
                    updated_at = excluded.updated_at
                """,
                domain,
                mimeType,
                data,
                sourceUrl,
                LocalDateTime.now().toString());
    }

    public void delete(String domain) {
        jdbcTemplate.update("DELETE FROM company_logos WHERE domain = ?", domain);
    }

    public record CompanyLogo(
            String domain,
            String mimeType,
            byte[] data,
            String sourceUrl,
            LocalDateTime updatedAt) {
    }
}
