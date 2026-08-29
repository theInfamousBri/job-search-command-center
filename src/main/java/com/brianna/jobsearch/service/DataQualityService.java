package com.brianna.jobsearch.service;

import com.brianna.jobsearch.model.DataQualityField;
import com.brianna.jobsearch.repository.DataQualityRepository;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class DataQualityService {

    private final DataQualityRepository repository;

    public DataQualityService(DataQualityRepository repository) {
        this.repository = repository;
    }

    public DataQualitySnapshot snapshot() {
        long total = repository.countApplications();
        Map<DataQualityField, Long> taggedCounts = repository.taggedCounts();
        List<DataQualityMetric> metrics = new ArrayList<>();
        long taggedAcrossMetrics = 0L;

        for (DataQualityField field : DataQualityField.values()) {
            long tagged = taggedCounts.getOrDefault(field, 0L);
            taggedAcrossMetrics += tagged;
            metrics.add(new DataQualityMetric(
                    field,
                    field.getDisplayName(),
                    field.getDescription(),
                    tagged,
                    Math.max(0L, total - tagged),
                    percentage(tagged, total)));
        }

        double overall = total == 0
                ? 0.0
                : percentage(taggedAcrossMetrics, total * (long) DataQualityField.values().length);

        return new DataQualitySnapshot(total, overall, List.copyOf(metrics));
    }

    private double percentage(long numerator, long denominator) {
        return denominator == 0 ? 0.0 : (numerator * 100.0) / denominator;
    }

    public record DataQualitySnapshot(long applications, double overallCoverage, List<DataQualityMetric> metrics) {
    }

    public record DataQualityMetric(
            DataQualityField field,
            String label,
            String description,
            long tagged,
            long missing,
            double coverageRate) {

        public String getStrength() {
            if (coverageRate >= 80.0) return "STRONG";
            if (coverageRate >= 50.0) return "PARTIAL";
            return "NEEDS_WORK";
        }

        public String getStrengthLabel() {
            return switch (getStrength()) {
                case "STRONG" -> "Strong coverage";
                case "PARTIAL" -> "Partial coverage";
                default -> "Needs attention";
            };
        }
    }
}
