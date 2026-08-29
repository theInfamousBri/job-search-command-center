package com.brianna.jobsearch.repository;

import com.brianna.jobsearch.model.DataQualityField;
import java.util.EnumMap;
import java.util.Map;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class DataQualityRepository {

    private final JdbcTemplate jdbcTemplate;

    public DataQualityRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public long countApplications() {
        Long value = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM job_applications", Long.class);
        return value == null ? 0L : value;
    }

    public Map<DataQualityField, Long> taggedCounts() {
        Map<DataQualityField, Long> counts = new EnumMap<>(DataQualityField.class);
        for (DataQualityField field : DataQualityField.values()) {
            Long value = jdbcTemplate.queryForObject(taggedCountSql(field), Long.class);
            counts.put(field, value == null ? 0L : value);
        }
        return counts;
    }

    private String taggedCountSql(DataQualityField field) {
        return switch (field) {
            case ROLE_FAMILY -> "SELECT COUNT(*) FROM job_applications WHERE role_family IS NOT NULL AND TRIM(role_family) <> ''";
            case INDUSTRY_DOMAIN -> "SELECT COUNT(*) FROM job_applications WHERE industry_domain IS NOT NULL AND TRIM(industry_domain) <> ''";
            case SOURCE -> "SELECT COUNT(*) FROM job_applications WHERE source IS NOT NULL AND TRIM(source) <> ''";
            case WORK_ARRANGEMENT -> "SELECT COUNT(*) FROM job_applications WHERE work_arrangement IS NOT NULL AND TRIM(work_arrangement) <> ''";
            case PRIORITY -> "SELECT COUNT(*) FROM job_applications WHERE priority IS NOT NULL AND TRIM(priority) <> '' AND priority <> 'UNSPECIFIED'";
            case COMPANY_DOMAIN -> "SELECT COUNT(*) FROM job_applications WHERE company_domain IS NOT NULL AND TRIM(company_domain) <> ''";
        };
    }
}
