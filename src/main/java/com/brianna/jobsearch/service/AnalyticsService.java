package com.brianna.jobsearch.service;

import com.brianna.jobsearch.model.ApplicationState;
import com.brianna.jobsearch.model.Priority;
import com.brianna.jobsearch.repository.AnalyticsRepository;
import com.brianna.jobsearch.repository.AnalyticsRepository.DimensionPerformance;
import com.brianna.jobsearch.repository.AnalyticsRepository.MonthCount;
import com.brianna.jobsearch.repository.AnalyticsRepository.OutcomeCount;
import com.brianna.jobsearch.repository.AnalyticsRepository.PrepHealth;
import com.brianna.jobsearch.repository.AnalyticsRepository.StageTiming;
import com.brianna.jobsearch.repository.AnalyticsRepository.StateCount;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class AnalyticsService {

    private static final DateTimeFormatter MONTH_LABEL = DateTimeFormatter.ofPattern("MMM");

    private final AnalyticsRepository repository;

    public AnalyticsService(AnalyticsRepository repository) {
        this.repository = repository;
    }

    public AnalyticsSnapshot snapshot() {
        long applied = repository.countAppliedApplications();
        long responded = repository.countRespondedApplications();
        long interviewed = repository.countInterviewedApplications();
        long offers = repository.countApplicationsThatReached("OFFER");

        double responseRate = percent(responded, applied);
        double interviewRate = percent(interviewed, applied);
        Double averageFirstResponseDays = repository.averageDaysToFirstResponse();

        List<FunnelStage> funnel = buildFunnel(applied, responded);
        List<StateBreakdown> states = buildStateBreakdown();
        List<OutcomeRow> outcomes = buildOutcomes(applied);
        List<MonthlyActivity> monthlyActivity = buildMonthlyActivity();
        List<StageSpeed> stageSpeeds = buildStageSpeeds();
        List<PerformanceRow> priorities = buildPerformance(repository.priorityPerformance(8), true);
        List<PerformanceRow> careerLanes = buildPerformance(repository.careerLanePerformance(10), false);
        List<PerformanceRow> workArrangements = buildPerformance(repository.workArrangementPerformance(10), false);
        List<PerformanceRow> sources = buildPerformance(repository.sourcePerformance(10), false);
        PrepSnapshot prep = buildPrepSnapshot();

        return new AnalyticsSnapshot(
                applied,
                responseRate,
                interviewRate,
                offers,
                averageFirstResponseDays,
                funnel,
                states,
                outcomes,
                monthlyActivity,
                stageSpeeds,
                priorities,
                careerLanes,
                workArrangements,
                sources,
                prep);
    }

    private List<FunnelStage> buildFunnel(long applied, long responded) {
        List<FunnelStage> stages = new ArrayList<>();
        stages.add(funnelStage("Applied", applied, applied));
        stages.add(funnelStage("Responded", responded, applied));
        stages.add(funnelStage("Recruiter screen", repository.countApplicationsThatReached("RECRUITER_SCREEN"), applied));
        stages.add(funnelStage("Assessment", repository.countApplicationsThatReached("CODING_ASSESSMENT"), applied));
        stages.add(funnelStage("Technical", repository.countApplicationsThatReached("TECHNICAL_INTERVIEW"), applied));
        stages.add(funnelStage("Final round", repository.countApplicationsThatReached("FINAL_ROUND"), applied));
        stages.add(funnelStage("Offer", repository.countApplicationsThatReached("OFFER"), applied));
        return stages;
    }

    private FunnelStage funnelStage(String label, long count, long applied) {
        double rate = percent(count, applied);
        double width = applied == 0 ? 0.0 : Math.max(count > 0 ? 5.0 : 0.0, rate);
        return new FunnelStage(label, count, rate, width);
    }

    private List<StateBreakdown> buildStateBreakdown() {
        List<StateCount> counts = repository.stateCounts();
        long total = counts.stream().mapToLong(StateCount::total).sum();
        List<StateBreakdown> result = new ArrayList<>();

        for (StateCount count : counts) {
            String displayName = displayState(count.state());
            result.add(new StateBreakdown(displayName, count.state(), count.total(), percent(count.total(), total)));
        }
        return result;
    }

    private List<OutcomeRow> buildOutcomes(long applied) {
        List<OutcomeRow> result = new ArrayList<>();
        for (OutcomeCount count : repository.outcomeCounts()) {
            result.add(new OutcomeRow(
                    displayOutcome(count.outcome()),
                    count.outcome(),
                    count.total(),
                    percent(count.total(), applied)));
        }
        return result;
    }

    private List<MonthlyActivity> buildMonthlyActivity() {
        YearMonth endMonth = YearMonth.now();
        YearMonth startMonth = endMonth.minusMonths(5);
        LocalDate start = startMonth.atDay(1);
        LocalDate end = endMonth.atEndOfMonth();

        Map<String, Long> applications = toMonthMap(repository.applicationCountsByMonth(start, end));
        Map<String, Long> interviews = toMonthMap(repository.interviewCountsByMonth(start, end));

        long max = 1;
        for (int i = 0; i < 6; i++) {
            String key = startMonth.plusMonths(i).toString();
            max = Math.max(max, applications.getOrDefault(key, 0L));
            max = Math.max(max, interviews.getOrDefault(key, 0L));
        }

        List<MonthlyActivity> result = new ArrayList<>();
        for (int i = 0; i < 6; i++) {
            YearMonth month = startMonth.plusMonths(i);
            String key = month.toString();
            long applicationCount = applications.getOrDefault(key, 0L);
            long interviewCount = interviews.getOrDefault(key, 0L);
            result.add(new MonthlyActivity(
                    key,
                    month.format(MONTH_LABEL),
                    applicationCount,
                    interviewCount,
                    barHeight(applicationCount, max),
                    barHeight(interviewCount, max)));
        }
        return result;
    }

    private Map<String, Long> toMonthMap(List<MonthCount> rows) {
        Map<String, Long> result = new HashMap<>();
        for (MonthCount row : rows) {
            result.put(row.monthKey(), row.total());
        }
        return result;
    }

    private List<StageSpeed> buildStageSpeeds() {
        List<StageSpeed> speeds = new ArrayList<>();
        speeds.add(stageSpeed("First response", new StageTiming(repository.averageDaysToFirstResponse(),
                repository.countRespondedApplications())));
        speeds.add(stageSpeed("Recruiter screen", repository.averageDaysToStage("RECRUITER_SCREEN")));
        speeds.add(stageSpeed("Assessment", repository.averageDaysToStage("CODING_ASSESSMENT")));
        speeds.add(stageSpeed("Technical interview", repository.averageDaysToStage("TECHNICAL_INTERVIEW")));
        speeds.add(stageSpeed("Final round", repository.averageDaysToStage("FINAL_ROUND")));
        speeds.add(stageSpeed("Offer", repository.averageDaysToStage("OFFER")));
        return speeds;
    }

    private StageSpeed stageSpeed(String label, StageTiming timing) {
        return new StageSpeed(label, timing.averageDays(), timing.sampleSize());
    }

    private List<PerformanceRow> buildPerformance(List<DimensionPerformance> rows, boolean priorityLabels) {
        List<PerformanceRow> result = new ArrayList<>();
        for (DimensionPerformance row : rows) {
            String label = priorityLabels ? displayPriority(row.label()) : row.label();
            result.add(new PerformanceRow(
                    label,
                    row.applications(),
                    row.responses(),
                    percent(row.responses(), row.applications()),
                    row.interviewed(),
                    percent(row.interviewed(), row.applications())));
        }
        return result;
    }

    private PrepSnapshot buildPrepSnapshot() {
        PrepHealth health = repository.prepHealth();
        long needsReview = repository.countPrepNeedsReview();
        double averageConfidence = health.averageConfidence() == null ? 0.0 : health.averageConfidence();
        double reviewedPercent = percent(health.reviewedItems(), health.totalItems());

        return new PrepSnapshot(
                health.totalItems(),
                averageConfidence,
                health.reviewedItems(),
                reviewedPercent,
                needsReview,
                health.completedReviews(),
                Math.min(100.0, Math.max(0.0, averageConfidence * 20.0)));
    }

    private String displayState(String state) {
        if (state == null || state.isBlank()) {
            return "Unknown";
        }
        try {
            return ApplicationState.valueOf(state).getDisplayName();
        } catch (IllegalArgumentException ignored) {
            return state.replace('_', ' ');
        }
    }

    private String displayPriority(String priority) {
        if (priority == null || priority.isBlank()) {
            return "Not set";
        }
        try {
            return Priority.valueOf(priority).getDisplayName();
        } catch (IllegalArgumentException ignored) {
            return priority.replace('_', ' ');
        }
    }

    private String displayOutcome(String outcome) {
        if (outcome == null) {
            return "Other";
        }
        return switch (outcome) {
            case "NO_RESPONSE" -> "No response";
            case "REJECTED" -> "Rejected";
            case "WITHDRAWN" -> "Withdrawn";
            case "OFFER" -> "Offer";
            case "INTERVIEWING" -> "Interviewing";
            case "ACTIVE" -> "Active / applied";
            case "OTHER_CLOSED" -> "Other closed";
            default -> outcome.replace('_', ' ');
        };
    }

    private double percent(long numerator, long denominator) {
        return denominator == 0 ? 0.0 : (numerator * 100.0) / denominator;
    }

    private double barHeight(long value, long max) {
        if (value <= 0 || max <= 0) {
            return 0.0;
        }
        return Math.max(8.0, (value * 100.0) / max);
    }

    public record AnalyticsSnapshot(
            long applications,
            double responseRate,
            double interviewRate,
            long offers,
            Double averageFirstResponseDays,
            List<FunnelStage> funnel,
            List<StateBreakdown> states,
            List<OutcomeRow> outcomes,
            List<MonthlyActivity> monthlyActivity,
            List<StageSpeed> stageSpeeds,
            List<PerformanceRow> priorities,
            List<PerformanceRow> careerLanes,
            List<PerformanceRow> workArrangements,
            List<PerformanceRow> sources,
            PrepSnapshot prep) {
    }

    public record FunnelStage(String label, long count, double rate, double widthPercent) {
    }

    public record StateBreakdown(String label, String key, long count, double percent) {
    }

    public record OutcomeRow(String label, String key, long count, double percent) {
    }

    public record MonthlyActivity(
            String monthKey,
            String label,
            long applications,
            long interviews,
            double applicationsHeight,
            double interviewsHeight) {
    }

    public record StageSpeed(String label, Double averageDays, long sampleSize) {
    }

    public record PerformanceRow(
            String label,
            long applications,
            long responses,
            double responseRate,
            long interviewed,
            double interviewRate) {
    }

    public record PrepSnapshot(
            long totalItems,
            double averageConfidence,
            long reviewedItems,
            double reviewedPercent,
            long needsReview,
            long completedReviews,
            double confidencePercent) {
    }
}
