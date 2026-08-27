package com.brianna.jobsearch.model.importing;

public record ApplicationImportResult(int imported, int merged, int skipped, int failed) {
    public String summary() {
        return "Import complete: " + imported + " new, " + merged + " merged, " + skipped + " skipped"
                + (failed > 0 ? ", " + failed + " failed." : ".");
    }
}
