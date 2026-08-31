package com.brianna.jobsearch.model;

public record CompensationContext(
        boolean targetParsed,
        boolean benchmarkAvailable,
        int sampleSize,
        String sampleStrength,
        String comparisonLabel,
        String comparisonNote,
        String targetRangeDisplay,
        String targetMidpointDisplay,
        String midpointDeltaDisplay,
        String medianDisplay,
        String middleFiftyDisplay,
        String positionLabel,
        String positionDescription,
        String message,
        String scaleMinDisplay,
        String scaleMaxDisplay,
        double middleLeftPercent,
        double middleWidthPercent,
        double medianPercent,
        double targetLeftPercent,
        double targetWidthPercent) {
}
