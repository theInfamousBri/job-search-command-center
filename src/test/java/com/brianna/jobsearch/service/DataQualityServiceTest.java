package com.brianna.jobsearch.service;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.brianna.jobsearch.model.DataQualityField;
import com.brianna.jobsearch.repository.DataQualityRepository;
import java.util.EnumMap;
import java.util.Map;
import org.junit.jupiter.api.Test;

class DataQualityServiceTest {

    @Test
    void completenessSnapshotCalculatesCoverageAndMissingCounts() {
        FakeDataQualityRepository repository = new FakeDataQualityRepository();
        repository.total = 10;
        repository.counts.put(DataQualityField.ROLE_FAMILY, 2L);
        repository.counts.put(DataQualityField.INDUSTRY_DOMAIN, 4L);
        repository.counts.put(DataQualityField.SOURCE, 8L);
        repository.counts.put(DataQualityField.WORK_ARRANGEMENT, 9L);
        repository.counts.put(DataQualityField.PRIORITY, 10L);
        repository.counts.put(DataQualityField.COMPANY_DOMAIN, 3L);

        var snapshot = new DataQualityService(repository).snapshot();

        assertEquals(10L, snapshot.applications());
        assertEquals(60.0, snapshot.overallCoverage(), 0.0001);

        var roleFamily = snapshot.metrics().get(0);
        assertEquals(2L, roleFamily.tagged());
        assertEquals(8L, roleFamily.missing());
        assertEquals(20.0, roleFamily.coverageRate(), 0.0001);
        assertEquals("NEEDS_WORK", roleFamily.getStrength());

        var priority = snapshot.metrics().get(4);
        assertEquals("STRONG", priority.getStrength());
    }

    private static final class FakeDataQualityRepository extends DataQualityRepository {
        long total;
        Map<DataQualityField, Long> counts = new EnumMap<>(DataQualityField.class);

        FakeDataQualityRepository() {
            super(null);
        }

        @Override
        public long countApplications() {
            return total;
        }

        @Override
        public Map<DataQualityField, Long> taggedCounts() {
            return counts;
        }
    }
}
