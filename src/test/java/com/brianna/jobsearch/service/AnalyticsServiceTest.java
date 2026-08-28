package com.brianna.jobsearch.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.brianna.jobsearch.repository.AnalyticsRepository;
import com.brianna.jobsearch.repository.AnalyticsRepository.DimensionPerformance;

import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;

class AnalyticsServiceTest {

    @Test
    void strategyLeaderIgnoresOneApplicationOutlierWhenUsableSampleExists() {
        FakeAnalyticsRepository repository = new FakeAnalyticsRepository();
        repository.applied = 20;
        repository.responded = 8;
        repository.interviewed = 2;
        repository.careerLanes = List.of(
                new DimensionPerformance("One-off experiment", 1, 1, 1),
                new DimensionPerformance("Backend / Platform", 10, 6, 2),
                new DimensionPerformance("Product Backend", 9, 2, 0));

        var snapshot = new AnalyticsService(repository).snapshot();
        var insight = snapshot.strategyInsights().get(1);

        assertEquals("Backend / Platform", insight.leader());
        assertEquals("STRONG", insight.confidence());
        assertEquals("20.0% interview", insight.metric());
        assertTrue(insight.detail().contains("+10.0 pp vs overall interview"));

        var rows = snapshot.careerLanePerformance().rows();
        assertEquals("SMALL", rows.get(0).sampleStrength());
        assertEquals("STRONG", rows.get(1).sampleStrength());
        assertEquals(33.333333333333336, rows.get(1).responseToInterviewRate(), 0.0001);
    }

    @Test
    void careerLaneRowsSeparateUsableSignalsFromTinySamples() {
        FakeAnalyticsRepository repository = new FakeAnalyticsRepository();
        repository.applied = 20;
        repository.responded = 8;
        repository.interviewed = 2;
        repository.careerLanes = List.of(
                new DimensionPerformance("One-off experiment", 1, 1, 1),
                new DimensionPerformance("Backend / Platform", 10, 6, 2),
                new DimensionPerformance("Product Backend", 4, 1, 0));

        var snapshot = new AnalyticsService(repository).snapshot();
        var dimension = snapshot.careerLanePerformance();

        assertEquals(List.of("Backend / Platform", "Product Backend"),
                dimension.signalRows().stream().map(AnalyticsService.PerformanceRow::label).toList());
        assertEquals(List.of("One-off experiment"),
                dimension.smallSampleRows().stream().map(AnalyticsService.PerformanceRow::label).toList());
    }

    @Test
    void allPerformanceDimensionsSeparateTinySamplesFromUsableSignals() {
        FakeAnalyticsRepository repository = new FakeAnalyticsRepository();
        repository.applied = 30;
        repository.priorities = List.of(
                new DimensionPerformance("HIGH", 12, 5, 2),
                new DimensionPerformance("LOW", 2, 1, 0));
        repository.workArrangements = List.of(
                new DimensionPerformance("Hybrid", 10, 6, 2),
                new DimensionPerformance("Hybrid / On-Site", 1, 0, 0));
        repository.sources = List.of(
                new DimensionPerformance("LinkedIn", 11, 7, 2),
                new DimensionPerformance("Ashby", 1, 1, 1));

        var snapshot = new AnalyticsService(repository).snapshot();

        assertEquals(List.of("High"),
                snapshot.priorityPerformance().signalRows().stream().map(AnalyticsService.PerformanceRow::label).toList());
        assertEquals(List.of("Low"),
                snapshot.priorityPerformance().smallSampleRows().stream().map(AnalyticsService.PerformanceRow::label).toList());
        assertEquals(List.of("Hybrid"),
                snapshot.workArrangementPerformance().signalRows().stream().map(AnalyticsService.PerformanceRow::label).toList());
        assertEquals(List.of("Hybrid / On-Site"),
                snapshot.workArrangementPerformance().smallSampleRows().stream().map(AnalyticsService.PerformanceRow::label).toList());
        assertEquals(List.of("LinkedIn"),
                snapshot.sourcePerformance().signalRows().stream().map(AnalyticsService.PerformanceRow::label).toList());
        assertEquals(List.of("Ashby"),
                snapshot.sourcePerformance().smallSampleRows().stream().map(AnalyticsService.PerformanceRow::label).toList());
    }

    @Test
    void smallSampleOnlyLeaderIsPresentedAsEarlySignal() {
        FakeAnalyticsRepository repository = new FakeAnalyticsRepository();
        repository.applied = 10;
        repository.responded = 4;
        repository.interviewed = 1;
        repository.careerLanes = List.of(
                new DimensionPerformance("Payments Platform", 1, 1, 1),
                new DimensionPerformance("AI Infrastructure", 1, 1, 0));

        var insight = new AnalyticsService(repository).snapshot().strategyInsights().get(1);

        assertEquals("Payments Platform", insight.leader());
        assertEquals("SMALL", insight.confidence());
        assertEquals("Early signal", insight.metric());
        assertTrue(insight.detail().contains("100.0% interview"));
        assertTrue(insight.detail().contains("very small sample"));
    }

    @Test
    void priorityCoverageExcludesNotSetAndUsesIntentionalDisplayOrder() {
        FakeAnalyticsRepository repository = new FakeAnalyticsRepository();
        repository.applied = 20;
        repository.priorities = List.of(
                new DimensionPerformance("UNSPECIFIED", 5, 1, 0),
                new DimensionPerformance("STRETCH", 5, 2, 1),
                new DimensionPerformance("HIGH", 10, 5, 2));

        var dimension = new AnalyticsService(repository).snapshot().priorityPerformance();

        assertEquals(15L, dimension.taggedApplications());
        assertEquals(75.0, dimension.coverageRate(), 0.0001);
        assertEquals(List.of("High", "Stretch", "Not set"),
                dimension.rows().stream().map(AnalyticsService.PerformanceRow::label).toList());
    }

    @Test
    void emptyDimensionsProduceNoDataInsightInsteadOfFailing() {
        var snapshot = new AnalyticsService(new FakeAnalyticsRepository()).snapshot();

        assertEquals("No data yet", snapshot.strategyInsights().get(0).leader());
        assertEquals("NO_DATA", snapshot.strategyInsights().get(0).confidence());
        assertTrue(snapshot.sourcePerformance().rows().isEmpty());
    }

    private static final class FakeAnalyticsRepository extends AnalyticsRepository {
        long applied;
        long responded;
        long interviewed;
        List<DimensionPerformance> priorities = List.of();
        List<DimensionPerformance> careerLanes = List.of();
        List<DimensionPerformance> workArrangements = List.of();
        List<DimensionPerformance> sources = List.of();

        FakeAnalyticsRepository() {
            super(null);
        }

        @Override
        public long countAppliedApplications() {
            return applied;
        }

        @Override
        public long countRespondedApplications() {
            return responded;
        }

        @Override
        public long countInterviewedApplications() {
            return interviewed;
        }

        @Override
        public long countApplicationsThatReached(String eventType) {
            return 0;
        }

        @Override
        public Double averageDaysToFirstResponse() {
            return null;
        }

        @Override
        public StageTiming averageDaysToStage(String eventType) {
            return new StageTiming(null, 0);
        }

        @Override
        public List<StateCount> stateCounts() {
            return List.of();
        }

        @Override
        public List<OutcomeCount> outcomeCounts() {
            return List.of();
        }

        @Override
        public List<MonthCount> applicationCountsByMonth(LocalDate start, LocalDate end) {
            return List.of();
        }

        @Override
        public List<MonthCount> interviewCountsByMonth(LocalDate start, LocalDate end) {
            return List.of();
        }

        @Override
        public List<DimensionPerformance> priorityPerformance(int limit) {
            return priorities;
        }

        @Override
        public List<DimensionPerformance> careerLanePerformance(int limit) {
            return careerLanes;
        }

        @Override
        public List<DimensionPerformance> workArrangementPerformance(int limit) {
            return workArrangements;
        }

        @Override
        public List<DimensionPerformance> sourcePerformance(int limit) {
            return sources;
        }

        @Override
        public PrepHealth prepHealth() {
            return new PrepHealth(0, null, 0, 0);
        }

        @Override
        public long countPrepNeedsReview() {
            return 0;
        }
    }
}
