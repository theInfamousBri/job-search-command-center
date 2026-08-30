package com.brianna.jobsearch.service;

import com.brianna.jobsearch.model.ApplicationState;
import com.brianna.jobsearch.model.Priority;
import com.brianna.jobsearch.repository.AnalyticsRepository;
import com.brianna.jobsearch.model.CareerRoleFamily;
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
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.springframework.stereotype.Service;

@Service
public class AnalyticsService {

    private static final DateTimeFormatter MONTH_LABEL = DateTimeFormatter.ofPattern("MMM");
    private static final int MIN_DIRECTIONAL_SAMPLE = 3;
    private static final int STRONG_SAMPLE = 10;

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

        PerformanceDimension priorityPerformance = buildPerformanceDimension(
                "priority",
                "Priority",
                repository.priorityPerformance(8),
                applied,
                responseRate,
                interviewRate,
                true);
        PerformanceDimension careerLanePerformance = buildPerformanceDimension(
                "career-lane",
                "Role family",
                repository.careerLanePerformance(12),
                applied,
                responseRate,
                interviewRate,
                false);
        PerformanceDimension workArrangementPerformance = buildPerformanceDimension(
                "work-arrangement",
                "Work arrangement",
                repository.workArrangementPerformance(10),
                applied,
                responseRate,
                interviewRate,
                false);
        PerformanceDimension sourcePerformance = buildPerformanceDimension(
                "source",
                "Source",
                repository.sourcePerformance(12),
                applied,
                responseRate,
                interviewRate,
                false);

        List<StrategyInsight> strategyInsights = List.of(
                buildStrategyInsight("PRIORITY SIGNAL", "Priority", priorityPerformance),
                buildStrategyInsight("ROLE FAMILY", "Role family", careerLanePerformance),
                buildStrategyInsight("WORK ARRANGEMENT", "Work arrangement", workArrangementPerformance),
                buildStrategyInsight("SOURCE SIGNAL", "Source", sourcePerformance));

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
                priorityPerformance,
                careerLanePerformance,
                workArrangementPerformance,
                sourcePerformance,
                strategyInsights,
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
        List<StateCount> counts = safeList(repository.stateCounts());
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
        for (OutcomeCount count : safeList(repository.outcomeCounts())) {
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

        Map<String, Long> applications = toMonthMap(safeList(repository.applicationCountsByMonth(start, end)));
        Map<String, Long> interviews = toMonthMap(safeList(repository.interviewCountsByMonth(start, end)));

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
        if (timing == null) {
            return new StageSpeed(label, null, 0);
        }
        return new StageSpeed(label, timing.averageDays(), timing.sampleSize());
    }

    private PerformanceDimension buildPerformanceDimension(
            String key,
            String label,
            List<DimensionPerformance> rawRows,
            long applied,
            double overallResponseRate,
            double overallInterviewRate,
            boolean priorityLabels) {

        List<PerformanceRow> rows = new ArrayList<>();
        long taggedApplications = 0;

        for (DimensionPerformance row : safeList(rawRows)) {
            String displayLabel = priorityLabels ? displayPriority(row.label()) : ("career-lane".equals(key) ? CareerRoleFamily.displayNameFor(row.label()) : row.label());
            if (displayLabel == null || displayLabel.isBlank()) {
                continue;
            }

            boolean explicitlyTagged = !priorityLabels || !"Not set".equals(displayLabel);
            if (explicitlyTagged) {
                taggedApplications += row.applications();
            }

            double rowResponseRate = percent(row.responses(), row.applications());
            double rowInterviewRate = percent(row.interviewed(), row.applications());
            double responseToInterviewRate = percent(row.interviewed(), row.responses());

            rows.add(new PerformanceRow(
                    displayLabel,
                    row.applications(),
                    row.responses(),
                    rowResponseRate,
                    row.interviewed(),
                    rowInterviewRate,
                    responseToInterviewRate,
                    percent(row.applications(), applied),
                    rowResponseRate - overallResponseRate,
                    rowInterviewRate - overallInterviewRate,
                    rateBarWidth(rowResponseRate),
                    rateBarWidth(rowInterviewRate),
                    sampleStrength(row.applications()),
                    sampleLabel(row.applications())));
        }

        if (priorityLabels) {
            rows.sort(Comparator.comparingInt(row -> priorityOrder(row.label())));
        }

        List<PerformanceRow> signalRows = rows.stream()
                .filter(row -> !"SMALL".equals(row.sampleStrength()))
                .toList();
        List<PerformanceRow> smallSampleRows = rows.stream()
                .filter(row -> "SMALL".equals(row.sampleStrength()))
                .toList();

        return new PerformanceDimension(
                key,
                label,
                taggedApplications,
                percent(taggedApplications, applied),
                overallResponseRate,
                overallInterviewRate,
                List.copyOf(rows),
                List.copyOf(signalRows),
                List.copyOf(smallSampleRows));
    }

    private StrategyInsight buildStrategyInsight(String eyebrow, String dimensionLabel, PerformanceDimension dimension) {
        if (dimension.rows().isEmpty()) {
            return new StrategyInsight(
                    eyebrow,
                    dimensionLabel,
                    "No data yet",
                    "—",
                    "Add this field to submitted applications to start comparing performance.",
                    "NO_DATA");
        }

        List<PerformanceRow> candidates = dimension.rows().stream()
                .filter(row -> row.applications() >= MIN_DIRECTIONAL_SAMPLE)
                .filter(row -> !"Not set".equals(row.label()))
                .filter(row -> !"Skip".equals(row.label()))
                .toList();

        boolean directional = !candidates.isEmpty();
        if (!directional) {
            candidates = dimension.rows().stream()
                    .filter(row -> !"Not set".equals(row.label()))
                    .filter(row -> !"Skip".equals(row.label()))
                    .toList();
        }
        if (candidates.isEmpty()) {
            candidates = dimension.rows();
        }

        PerformanceRow leader = candidates.stream()
                .max(Comparator
                        .comparingDouble(PerformanceRow::interviewRate)
                        .thenComparingDouble(PerformanceRow::responseRate)
                        .thenComparingLong(PerformanceRow::applications))
                .orElse(dimension.rows().get(0));

        String metric;
        String detail;
        if (leader.interviewed() > 0) {
            metric = formatPercent(leader.interviewRate()) + " interview";
            detail = deltaSentence(leader.interviewDelta(), "interview")
                    + " · " + formatPercent(leader.responseRate()) + " response"
                    + " · " + leader.applications() + " apps";
        } else {
            metric = formatPercent(leader.responseRate()) + " response";
            detail = deltaSentence(leader.responseDelta(), "response")
                    + " · " + leader.applications() + " apps";
        }

        String confidence = leader.sampleStrength();
        if (!directional) {
            String observedMetric = leader.interviewed() > 0
                    ? formatPercent(leader.interviewRate()) + " interview"
                    : formatPercent(leader.responseRate()) + " response";
            metric = "Early signal";
            detail = observedMetric + " · " + detail + " · very small sample";
            confidence = "SMALL";
        }

        return new StrategyInsight(
                eyebrow,
                dimensionLabel,
                leader.label(),
                metric,
                detail,
                confidence);
    }

    private String deltaSentence(double delta, String metric) {
        if (Math.abs(delta) < 0.05) {
            return "At your overall " + metric + " baseline";
        }
        return formatSignedPoints(delta) + " vs overall " + metric;
    }

    private int priorityOrder(String label) {
        return switch (label) {
            case "High" -> 0;
            case "Stretch" -> 1;
            case "Medium" -> 2;
            case "Low" -> 3;
            case "Not set" -> 4;
            case "Skip" -> 5;
            default -> 6;
        };
    }

    private String sampleStrength(long applications) {
        if (applications >= STRONG_SAMPLE) {
            return "STRONG";
        }
        if (applications >= MIN_DIRECTIONAL_SAMPLE) {
            return "DIRECTIONAL";
        }
        return "SMALL";
    }

    private String sampleLabel(long applications) {
        if (applications >= STRONG_SAMPLE) {
            return "Stronger sample";
        }
        if (applications >= MIN_DIRECTIONAL_SAMPLE) {
            return "Directional";
        }
        return "Small sample";
    }

    private double rateBarWidth(double rate) {
        if (rate <= 0) {
            return 0.0;
        }
        return Math.min(100.0, Math.max(3.0, rate));
    }

    private PrepSnapshot buildPrepSnapshot() {
        PrepHealth health = repository.prepHealth();
        if (health == null) {
            health = new PrepHealth(0, null, 0, 0);
        }
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

    private String formatPercent(double value) {
        return String.format(java.util.Locale.US, "%.1f%%", value);
    }

    private String formatSignedPoints(double value) {
        return String.format(java.util.Locale.US, "%+.1f pp", value);
    }

    private <T> List<T> safeList(List<T> values) {
        return Objects.requireNonNullElse(values, List.of());
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
            PerformanceDimension priorityPerformance,
            PerformanceDimension careerLanePerformance,
            PerformanceDimension workArrangementPerformance,
            PerformanceDimension sourcePerformance,
            List<StrategyInsight> strategyInsights,
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

    public record PerformanceDimension(
            String key,
            String label,
            long taggedApplications,
            double coverageRate,
            double overallResponseRate,
            double overallInterviewRate,
            List<PerformanceRow> rows,
            List<PerformanceRow> signalRows,
            List<PerformanceRow> smallSampleRows) {
    }

    public record PerformanceRow(
            String label,
            long applications,
            long responses,
            double responseRate,
            long interviewed,
            double interviewRate,
            double responseToInterviewRate,
            double applicationShare,
            double responseDelta,
            double interviewDelta,
            double responseBarWidth,
            double interviewBarWidth,
            String sampleStrength,
            String sampleLabel) {
    }

    public record StrategyInsight(
            String eyebrow,
            String dimension,
            String leader,
            String metric,
            String detail,
            String confidence) {
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
