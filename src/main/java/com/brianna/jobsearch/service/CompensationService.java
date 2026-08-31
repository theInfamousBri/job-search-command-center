package com.brianna.jobsearch.service;

import com.brianna.jobsearch.model.CareerRoleFamily;
import com.brianna.jobsearch.model.CompensationContext;
import com.brianna.jobsearch.model.JobApplication;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.stereotype.Service;

@Service
public class CompensationService {

    private static final int MIN_COMPARABLE_SAMPLE = 3;
    private static final int MIN_REFINED_SAMPLE = 10;
    private static final long SCALE_STEP = 5_000L;
    private static final Pattern NON_ANNUAL_RATE = Pattern.compile(
            "(?i)(/\\s*(?:hr|hour|day|week|month)|per\\s+(?:hour|day|week|month)|hourly|daily|weekly|monthly)");
    private static final Pattern SHARED_K_RANGE = Pattern.compile(
            "(?i)\\$?\\s*(\\d{2,3}(?:\\.\\d+)?)\\s*(?:-|–|—|to)\\s*\\$?\\s*(\\d{2,3}(?:\\.\\d+)?)\\s*k\\b");
    private static final Pattern AMOUNT = Pattern.compile(
            "(?i)(?:\\$\\s*)?(\\d{1,3}(?:,\\d{3})+|\\d{2,3}(?:\\.\\d+)?\\s*k\\b|\\d{5,7})(?!\\d)");

    private final JobApplicationService applications;

    public CompensationService(JobApplicationService applications) {
        this.applications = applications;
    }

    public CompensationContext contextFor(JobApplication target) {
        Optional<SalaryRange> targetRange = parseAnnualSalary(target == null ? null : target.getSalary());
        if (targetRange.isEmpty()) {
            return unavailable(false, 0,
                    "Add a comparable annual salary or range to see how this role sits against your tracked applications.");
        }

        List<JobApplication> others = applications.findAll().stream()
                .filter(application -> !sameApplication(target, application))
                .toList();

        CareerRoleFamily roleFamily = target.getRoleFamily();
        List<SalaryRange> sameRoleFamily = parseRanges(others.stream()
                .filter(application -> roleFamily != null && application.getRoleFamily() == roleFamily)
                .toList());
        String workArrangement = clean(target.getWorkArrangement());
        List<SalaryRange> sameRoleFamilyAndWorkArrangement = parseRanges(others.stream()
                .filter(application -> roleFamily != null && application.getRoleFamily() == roleFamily)
                .filter(application -> workArrangement != null
                        && sameText(workArrangement, application.getWorkArrangement()))
                .toList());
        List<SalaryRange> allTracked = parseRanges(others);

        List<SalaryRange> comparables;
        String comparisonLabel;
        String comparisonNote = null;

        if (roleFamily != null
                && workArrangement != null
                && sameRoleFamilyAndWorkArrangement.size() >= MIN_REFINED_SAMPLE) {
            comparables = sameRoleFamilyAndWorkArrangement;
            comparisonLabel = roleFamily.getDisplayName() + " · " + workArrangement + " roles";
        } else if (roleFamily != null && sameRoleFamily.size() >= MIN_COMPARABLE_SAMPLE) {
            comparables = sameRoleFamily;
            comparisonLabel = roleFamily.getDisplayName() + " roles";
        } else if (allTracked.size() >= MIN_COMPARABLE_SAMPLE) {
            comparables = allTracked;
            comparisonLabel = "All tracked roles";
            if (roleFamily != null) {
                comparisonNote = "Not enough salary data exists in this role family yet, so this uses your broader tracked history.";
            }
        } else {
            int bestSample = roleFamily == null ? allTracked.size() : Math.max(sameRoleFamily.size(), allTracked.size());
            return unavailable(true, bestSample,
                    "Add salary data to at least " + MIN_COMPARABLE_SAMPLE
                            + " other tracked roles before treating the comparison as useful context.");
        }

        List<Double> midpoints = comparables.stream()
                .map(SalaryRange::midpoint)
                .sorted(Comparator.naturalOrder())
                .toList();
        double q1 = percentile(midpoints, .25);
        double median = percentile(midpoints, .50);
        double q3 = percentile(midpoints, .75);
        SalaryRange current = targetRange.get();

        double rawMin = Math.min(current.min(), q1);
        double rawMax = Math.max(current.max(), q3);
        double rawSpan = Math.max(10_000d, rawMax - rawMin);
        double padding = Math.max(5_000d, rawSpan * .12d);
        long scaleMin = Math.max(0L, floorToStep(rawMin - padding, SCALE_STEP));
        long scaleMax = ceilToStep(rawMax + padding, SCALE_STEP);
        if (scaleMax <= scaleMin) {
            scaleMax = scaleMin + 10_000L;
        }

        double targetLeft = percent(current.min(), scaleMin, scaleMax);
        double targetRight = percent(current.max(), scaleMin, scaleMax);
        double middleLeft = percent(q1, scaleMin, scaleMax);
        double middleRight = percent(q3, scaleMin, scaleMax);

        return new CompensationContext(
                true,
                true,
                comparables.size(),
                sampleStrength(comparables.size()),
                comparisonLabel,
                comparisonNote,
                formatRange(current),
                formatCompact(current.midpoint()),
                midpointDelta(current.midpoint(), median),
                formatCompact(median),
                formatCompact(q1) + " – " + formatCompact(q3),
                positionLabel(current, median),
                positionDescription(current, q1, q3),
                null,
                formatCompact(scaleMin),
                formatCompact(scaleMax),
                roundPercent(middleLeft),
                roundPercent(Math.max(2d, middleRight - middleLeft)),
                roundPercent(percent(median, scaleMin, scaleMax)),
                roundPercent(targetLeft),
                roundPercent(Math.max(2d, targetRight - targetLeft)));
    }

    public Optional<SalaryRange> parseAnnualSalary(String value) {
        if (value == null || value.isBlank() || NON_ANNUAL_RATE.matcher(value).find()) {
            return Optional.empty();
        }

        Matcher sharedK = SHARED_K_RANGE.matcher(value);
        if (sharedK.find()) {
            double first = Double.parseDouble(sharedK.group(1)) * 1_000d;
            double second = Double.parseDouble(sharedK.group(2)) * 1_000d;
            return Optional.of(SalaryRange.of(first, second));
        }

        Matcher matcher = AMOUNT.matcher(value);
        List<Double> amounts = new ArrayList<>(2);
        while (matcher.find() && amounts.size() < 2) {
            Double parsed = parseAmount(matcher.group(1));
            if (parsed != null && parsed >= 10_000d) {
                amounts.add(parsed);
            }
        }
        if (amounts.isEmpty()) {
            return Optional.empty();
        }
        if (amounts.size() == 1) {
            return Optional.of(SalaryRange.of(amounts.get(0), amounts.get(0)));
        }
        return Optional.of(SalaryRange.of(amounts.get(0), amounts.get(1)));
    }

    private List<SalaryRange> parseRanges(List<JobApplication> source) {
        return source.stream()
                .map(JobApplication::getSalary)
                .map(this::parseAnnualSalary)
                .flatMap(Optional::stream)
                .toList();
    }

    private boolean sameApplication(JobApplication target, JobApplication candidate) {
        return target != null
                && target.getId() != null
                && candidate != null
                && target.getId().equals(candidate.getId());
    }

    private Double parseAmount(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        String normalized = raw.toLowerCase(Locale.ROOT).replace(",", "").replaceAll("\\s+", "");
        boolean thousands = normalized.endsWith("k");
        if (thousands) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }
        try {
            double value = Double.parseDouble(normalized);
            return thousands ? value * 1_000d : value;
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private double percentile(List<Double> sorted, double percentile) {
        if (sorted.isEmpty()) {
            return 0d;
        }
        if (sorted.size() == 1) {
            return sorted.get(0);
        }
        double index = (sorted.size() - 1) * percentile;
        int lower = (int) Math.floor(index);
        int upper = (int) Math.ceil(index);
        if (lower == upper) {
            return sorted.get(lower);
        }
        double weight = index - lower;
        return sorted.get(lower) + (sorted.get(upper) - sorted.get(lower)) * weight;
    }

    private String positionLabel(SalaryRange current, double median) {
        if (current.min() <= median && current.max() >= median) {
            return "Spans tracked median";
        }
        return current.min() > median
                ? "Entire range above tracked median"
                : "Entire range below tracked median";
    }

    private String positionDescription(SalaryRange current, double q1, double q3) {
        if (current.min() > q3) {
            return "The full range sits above the tracked middle 50% for comparable roles.";
        }
        if (current.max() < q1) {
            return "The full range sits below the tracked middle 50% for comparable roles.";
        }
        if (current.min() >= q1 && current.max() <= q3) {
            return "The full range sits inside the tracked middle 50% for comparable roles.";
        }
        if (current.min() <= q1 && current.max() >= q3) {
            return "This range covers the full tracked middle 50% for comparable roles.";
        }
        if (current.max() > q3) {
            return "This range overlaps the upper end of the tracked middle 50% for comparable roles.";
        }
        if (current.min() < q1) {
            return "This range overlaps the lower end of the tracked middle 50% for comparable roles.";
        }
        return "This range overlaps the tracked middle 50% for comparable roles.";
    }

    private String sampleStrength(int sampleSize) {
        if (sampleSize >= 10) {
            return "Stronger sample";
        }
        if (sampleSize >= MIN_COMPARABLE_SAMPLE) {
            return "Directional";
        }
        return sampleSize > 0 ? "Small sample" : null;
    }

    private String midpointDelta(double midpoint, double median) {
        if (median <= 0d) {
            return null;
        }
        long percent = Math.round(((midpoint - median) / median) * 100d);
        if (percent == 0L) {
            return "At tracked median";
        }
        return (percent > 0 ? "+" : "") + percent + "% vs median";
    }

    private String formatRange(SalaryRange range) {
        if (Math.abs(range.min() - range.max()) < .01d) {
            return formatCompact(range.min());
        }
        return formatCompact(range.min()) + " – " + formatCompact(range.max());
    }

    private String clean(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String cleaned = value.trim();
        return cleaned.equalsIgnoreCase("Not Specified") ? null : cleaned;
    }

    private boolean sameText(String expected, String actual) {
        return expected != null && actual != null && expected.equalsIgnoreCase(actual.trim());
    }

    private CompensationContext unavailable(boolean targetParsed, int sampleSize, String message) {
        return new CompensationContext(
                targetParsed, false, sampleSize, sampleStrength(sampleSize), null, null, null, null, null,
                null, null, null, null, message, null, null, 0d, 0d, 0d, 0d, 0d);
    }

    private double percent(double value, long min, long max) {
        if (max <= min) {
            return 0d;
        }
        return Math.max(0d, Math.min(100d, ((value - min) / (max - min)) * 100d));
    }

    private double roundPercent(double value) {
        return Math.round(value * 10d) / 10d;
    }

    private long floorToStep(double value, long step) {
        return (long) Math.floor(value / step) * step;
    }

    private long ceilToStep(double value, long step) {
        return (long) Math.ceil(value / step) * step;
    }

    private String formatCompact(double value) {
        double thousands = value / 1_000d;
        double rounded = Math.round(thousands * 10d) / 10d;
        if (Math.abs(rounded - Math.rint(rounded)) < .0001d) {
            return "$" + (long) Math.rint(rounded) + "k";
        }
        return "$" + String.format(Locale.US, "%.1fk", rounded);
    }

    public record SalaryRange(double min, double max) {
        public SalaryRange {
            if (min < 0d || max < 0d) {
                throw new IllegalArgumentException("Salary values cannot be negative.");
            }
        }

        public static SalaryRange of(double first, double second) {
            return new SalaryRange(Math.min(first, second), Math.max(first, second));
        }

        public double midpoint() {
            return (min + max) / 2d;
        }
    }
}
