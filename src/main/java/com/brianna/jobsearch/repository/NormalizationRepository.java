package com.brianna.jobsearch.repository;

import com.brianna.jobsearch.model.CareerRoleFamily;
import com.brianna.jobsearch.model.IndustryDomain;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.stream.Collectors;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class NormalizationRepository {

    private static final List<String> NORMALIZABLE_TEXT_COLUMNS = List.of("source", "work_arrangement");

    private final JdbcTemplate jdbcTemplate;

    public NormalizationRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public long countApplications() {
        Long value = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM job_applications", Long.class);
        return value == null ? 0L : value;
    }

    public long countLegacyCareerTaggedApplications() {
        Long value = jdbcTemplate.queryForObject("""
                SELECT COUNT(*)
                FROM job_applications
                WHERE career_lane IS NOT NULL AND TRIM(career_lane) <> ''
                """, Long.class);
        return value == null ? 0L : value;
    }

    public long countCareerApplicationsNeedingMapping() {
        Long value = jdbcTemplate.queryForObject("""
                SELECT COUNT(*)
                FROM job_applications
                WHERE career_lane IS NOT NULL
                  AND TRIM(career_lane) <> ''
                  AND (
                      role_family IS NULL OR TRIM(role_family) = ''
                      OR industry_domain IS NULL OR TRIM(industry_domain) = ''
                  )
                """, Long.class);
        return value == null ? 0L : value;
    }

    public List<CareerTagGroupRow> findCareerTagGroups(String query, String status) {
        String normalizedStatus = status == null ? "UNMAPPED" : status.trim().toUpperCase(Locale.ROOT);
        if (!List.of("UNMAPPED", "MAPPED", "ALL").contains(normalizedStatus)) {
            normalizedStatus = "UNMAPPED";
        }

        StringBuilder sql = new StringBuilder("""
                SELECT
                    MIN(TRIM(career_lane)) AS legacy_tag,
                    COUNT(*) AS application_count,
                    SUM(CASE
                        WHEN role_family IS NOT NULL AND TRIM(role_family) <> ''
                         AND industry_domain IS NOT NULL AND TRIM(industry_domain) <> ''
                        THEN 1 ELSE 0 END) AS mapped_count,
                    GROUP_CONCAT(DISTINCT role_family) AS role_families,
                    GROUP_CONCAT(DISTINCT industry_domain) AS industry_domains
                FROM job_applications
                WHERE career_lane IS NOT NULL
                  AND TRIM(career_lane) <> ''
                """);

        List<Object> params = new ArrayList<>();
        if (query != null && !query.isBlank()) {
            sql.append(" AND LOWER(career_lane) LIKE LOWER(?) ");
            params.add("%" + query.trim() + "%");
        }

        sql.append(" GROUP BY LOWER(TRIM(career_lane)) ");
        if ("UNMAPPED".equals(normalizedStatus)) {
            sql.append(" HAVING mapped_count < COUNT(*) ");
        } else if ("MAPPED".equals(normalizedStatus)) {
            sql.append(" HAVING mapped_count = COUNT(*) ");
        }
        sql.append(" ORDER BY application_count DESC, LOWER(legacy_tag) ASC ");

        return jdbcTemplate.query(
                sql.toString(),
                (rs, rowNum) -> new CareerTagGroupRow(
                        rs.getString("legacy_tag"),
                        rs.getLong("application_count"),
                        rs.getLong("mapped_count"),
                        rs.getString("role_families"),
                        rs.getString("industry_domains")),
                params.toArray());
    }

    public List<TextValueGroupRow> findSourceGroups() {
        return findTextValueGroups("source");
    }

    public List<TextValueGroupRow> findWorkArrangementGroups() {
        return findTextValueGroups("work_arrangement");
    }

    private List<TextValueGroupRow> findTextValueGroups(String column) {
        validateTextColumn(column);
        return jdbcTemplate.query(
                "SELECT MIN(TRIM(" + column + ")) AS value, COUNT(*) AS application_count " +
                        "FROM job_applications " +
                        "WHERE " + column + " IS NOT NULL AND TRIM(" + column + ") <> '' " +
                        "GROUP BY LOWER(TRIM(" + column + ")) " +
                        "ORDER BY application_count DESC, LOWER(value) ASC",
                (rs, rowNum) -> new TextValueGroupRow(rs.getString("value"), rs.getLong("application_count")));
    }

    public int applyCareerMapping(
            List<String> legacyTags,
            CareerRoleFamily roleFamily,
            IndustryDomain industryDomain,
            String focus,
            boolean overwriteExisting) {

        List<String> normalizedTags = normalizeSelections(legacyTags);
        if (normalizedTags.isEmpty()) {
            return 0;
        }

        String normalizedFocus = blankToNull(focus);
        if (roleFamily == null && industryDomain == null && normalizedFocus == null) {
            return 0;
        }

        List<String> assignments = new ArrayList<>();
        List<Object> params = new ArrayList<>();

        if (roleFamily != null) {
            if (overwriteExisting) {
                assignments.add("role_family = ?");
            } else {
                assignments.add("role_family = CASE WHEN role_family IS NULL OR TRIM(role_family) = '' THEN ? ELSE role_family END");
            }
            params.add(roleFamily.name());
        }

        if (industryDomain != null) {
            if (overwriteExisting) {
                assignments.add("industry_domain = ?");
            } else {
                assignments.add("industry_domain = CASE WHEN industry_domain IS NULL OR TRIM(industry_domain) = '' THEN ? ELSE industry_domain END");
            }
            params.add(industryDomain.name());
        }

        if (normalizedFocus != null) {
            if (overwriteExisting) {
                assignments.add("career_focus = ?");
            } else {
                assignments.add("career_focus = CASE WHEN career_focus IS NULL OR TRIM(career_focus) = '' THEN ? ELSE career_focus END");
            }
            params.add(normalizedFocus);
        }

        String placeholders = normalizedTags.stream().map(value -> "?").collect(Collectors.joining(","));
        String sql = "UPDATE job_applications SET " + String.join(", ", assignments) +
                " WHERE LOWER(TRIM(career_lane)) IN (" + placeholders + ")";
        normalizedTags.forEach(tag -> params.add(tag.toLowerCase(Locale.ROOT)));

        // Intentionally do not change updated_at. Data cleanup should not make an old
        // application look recently active or interfere with stale-review logic.
        return jdbcTemplate.update(sql, params.toArray());
    }

    public int normalizeSourceValues(List<String> values, String target) {
        return normalizeTextValues("source", values, target);
    }

    public int normalizeWorkArrangementValues(List<String> values, String target) {
        return normalizeTextValues("work_arrangement", values, target);
    }

    private int normalizeTextValues(String column, List<String> values, String target) {
        validateTextColumn(column);
        List<String> normalizedValues = normalizeSelections(values);
        String normalizedTarget = blankToNull(target);
        if (normalizedValues.isEmpty() || normalizedTarget == null) {
            return 0;
        }

        String placeholders = normalizedValues.stream().map(value -> "?").collect(Collectors.joining(","));
        List<Object> params = new ArrayList<>();
        params.add(normalizedTarget);
        normalizedValues.forEach(value -> params.add(value.toLowerCase(Locale.ROOT)));
        params.add(normalizedTarget);

        return jdbcTemplate.update(
                "UPDATE job_applications SET " + column + " = ? " +
                        "WHERE LOWER(TRIM(" + column + ")) IN (" + placeholders + ") " +
                        "AND TRIM(" + column + ") <> ?",
                params.toArray());
    }

    private List<String> normalizeSelections(List<String> values) {
        if (values == null) {
            return List.of();
        }
        return values.stream()
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(value -> !value.isBlank())
                .distinct()
                .toList();
    }

    private void validateTextColumn(String column) {
        if (!NORMALIZABLE_TEXT_COLUMNS.contains(column)) {
            throw new IllegalArgumentException("Unsupported normalization column: " + column);
        }
    }

    private String blankToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isBlank() ? null : trimmed;
    }

    public record CareerTagGroupRow(
            String legacyTag,
            long applicationCount,
            long mappedCount,
            String roleFamilies,
            String industryDomains) {
    }

    public record TextValueGroupRow(String value, long applicationCount) {
    }
}
