package com.brianna.jobsearch.model.importing;

import java.util.ArrayList;
import java.util.List;

public class ApplicationImportPreview {
    private String fileName;
    private String fileType;
    private final List<ApplicationImportRow> rows = new ArrayList<>();
    private final List<StatusMappingSummary> statusMappings = new ArrayList<>();

    public long getImportableCount() {
        return rows.stream().filter(ApplicationImportRow::isImportable).count();
    }

    public long getExactDuplicateCount() {
        return rows.stream().filter(ApplicationImportRow::isExactDuplicate).count();
    }

    public long getPossibleDuplicateCount() {
        return rows.stream().filter(ApplicationImportRow::isPossibleDuplicate).count();
    }

    public long getDefaultSkipCount() {
        return rows.stream().filter(row -> row.getDefaultDecision() == ImportDecision.SKIP).count();
    }

    public int getRowCount() {
        return rows.size();
    }

    public String getFileName() { return fileName; }
    public void setFileName(String fileName) { this.fileName = fileName; }

    public String getFileType() { return fileType; }
    public void setFileType(String fileType) { this.fileType = fileType; }

    public List<ApplicationImportRow> getRows() { return rows; }
    public List<StatusMappingSummary> getStatusMappings() { return statusMappings; }

    public static class StatusMappingSummary {
        private final String sourceStatus;
        private final String targetStatus;
        private final long count;

        public StatusMappingSummary(String sourceStatus, String targetStatus, long count) {
            this.sourceStatus = sourceStatus;
            this.targetStatus = targetStatus;
            this.count = count;
        }

        public String getSourceStatus() { return sourceStatus; }
        public String getTargetStatus() { return targetStatus; }
        public long getCount() { return count; }
    }
}
