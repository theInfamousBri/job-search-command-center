package com.brianna.jobsearch.model.importing;

import com.brianna.jobsearch.model.JobApplication;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class ApplicationImportRow {
    private int spreadsheetRow;
    private JobApplication application;
    private LocalDate historicalUpdatedDate;
    private String originalStatus;
    private String mappedStatus;
    private DuplicateMatchType duplicateType = DuplicateMatchType.NONE;
    private Long existingApplicationId;
    private String existingApplicationLabel;
    private ImportDecision defaultDecision = ImportDecision.IMPORT;
    private final List<String> warnings = new ArrayList<>();

    public boolean isImportable() {
        return application != null
                && application.getCompany() != null && !application.getCompany().isBlank()
                && application.getRole() != null && !application.getRole().isBlank();
    }

    public boolean isDuplicate() {
        return duplicateType != DuplicateMatchType.NONE;
    }

    public boolean isExactDuplicate() {
        return duplicateType == DuplicateMatchType.EXACT;
    }

    public boolean isPossibleDuplicate() {
        return duplicateType == DuplicateMatchType.POSSIBLE;
    }

    public int getSpreadsheetRow() { return spreadsheetRow; }
    public void setSpreadsheetRow(int spreadsheetRow) { this.spreadsheetRow = spreadsheetRow; }

    public JobApplication getApplication() { return application; }
    public void setApplication(JobApplication application) { this.application = application; }

    public LocalDate getHistoricalUpdatedDate() { return historicalUpdatedDate; }
    public void setHistoricalUpdatedDate(LocalDate historicalUpdatedDate) { this.historicalUpdatedDate = historicalUpdatedDate; }

    public String getOriginalStatus() { return originalStatus; }
    public void setOriginalStatus(String originalStatus) { this.originalStatus = originalStatus; }

    public String getMappedStatus() { return mappedStatus; }
    public void setMappedStatus(String mappedStatus) { this.mappedStatus = mappedStatus; }

    public DuplicateMatchType getDuplicateType() { return duplicateType; }
    public void setDuplicateType(DuplicateMatchType duplicateType) { this.duplicateType = duplicateType; }

    public Long getExistingApplicationId() { return existingApplicationId; }
    public void setExistingApplicationId(Long existingApplicationId) { this.existingApplicationId = existingApplicationId; }

    public String getExistingApplicationLabel() { return existingApplicationLabel; }
    public void setExistingApplicationLabel(String existingApplicationLabel) { this.existingApplicationLabel = existingApplicationLabel; }

    public ImportDecision getDefaultDecision() { return defaultDecision; }
    public void setDefaultDecision(ImportDecision defaultDecision) { this.defaultDecision = defaultDecision; }

    public List<String> getWarnings() { return warnings; }
    public String getWarningDisplay() { return String.join(" · ", warnings); }
}
