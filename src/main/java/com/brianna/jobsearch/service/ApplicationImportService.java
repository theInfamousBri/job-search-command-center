package com.brianna.jobsearch.service;

import com.brianna.jobsearch.model.ApplicationEvent;
import com.brianna.jobsearch.model.ApplicationEventType;
import com.brianna.jobsearch.model.ApplicationState;
import com.brianna.jobsearch.model.ApplicationStatus;
import com.brianna.jobsearch.model.JobApplication;
import com.brianna.jobsearch.model.Priority;
import com.brianna.jobsearch.model.importing.ApplicationImportPreview;
import com.brianna.jobsearch.model.importing.ApplicationImportPreview.StatusMappingSummary;
import com.brianna.jobsearch.model.importing.ApplicationImportResult;
import com.brianna.jobsearch.model.importing.ApplicationImportRow;
import com.brianna.jobsearch.model.importing.DuplicateMatchType;
import com.brianna.jobsearch.model.importing.ImportDecision;
import com.brianna.jobsearch.repository.ApplicationEventRepository;
import com.brianna.jobsearch.repository.JobApplicationRepository;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.DateUtil;
import org.apache.poi.ss.usermodel.FormulaEvaluator;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
public class ApplicationImportService {

    private static final List<String> REQUIRED_HEADERS = List.of("Job Title", "Company", "Status");
    private static final List<DateTimeFormatter> DATE_FORMATTERS = List.of(
            DateTimeFormatter.ofPattern("M/d/uuuu"),
            DateTimeFormatter.ofPattern("M/d/uu"),
            DateTimeFormatter.ofPattern("MM/dd/uuuu"),
            DateTimeFormatter.ofPattern("MM/dd/uu"),
            DateTimeFormatter.ISO_LOCAL_DATE,
            DateTimeFormatter.ofPattern("MMM d, uuuu", Locale.US));

    private final JobApplicationRepository applicationRepository;
    private final ApplicationEventRepository eventRepository;

    public ApplicationImportService(
            JobApplicationRepository applicationRepository,
            ApplicationEventRepository eventRepository) {
        this.applicationRepository = applicationRepository;
        this.eventRepository = eventRepository;
    }

    public ApplicationImportPreview preview(MultipartFile file) throws IOException {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Choose an .xlsx or .csv file to import.");
        }

        String fileName = file.getOriginalFilename() == null ? "applications" : file.getOriginalFilename();
        String lower = fileName.toLowerCase(Locale.ROOT);

        List<ParsedRow> parsedRows;
        String fileType;
        if (lower.endsWith(".xlsx") || lower.endsWith(".xls")) {
            parsedRows = parseExcel(file.getInputStream());
            fileType = "Excel";
        } else if (lower.endsWith(".csv")) {
            parsedRows = parseCsv(file.getInputStream());
            fileType = "CSV";
        } else {
            throw new IllegalArgumentException("Unsupported file type. Use .xlsx, .xls, or .csv.");
        }

        ApplicationImportPreview preview = new ApplicationImportPreview();
        preview.setFileName(fileName);
        preview.setFileType(fileType);

        List<JobApplication> existing = applicationRepository.findAll(null);
        Map<String, Long> statusCounts = new LinkedHashMap<>();
        Map<String, String> mappedStatuses = new LinkedHashMap<>();

        for (ParsedRow parsed : parsedRows) {
            ApplicationImportRow row = mapRow(parsed);
            detectDuplicate(row, existing);
            preview.getRows().add(row);

            String sourceStatus = clean(row.getOriginalStatus());
            if (sourceStatus != null) {
                statusCounts.merge(sourceStatus, 1L, Long::sum);
                mappedStatuses.putIfAbsent(sourceStatus, row.getMappedStatus());
            }
        }

        statusCounts.forEach((source, count) -> preview.getStatusMappings().add(
                new StatusMappingSummary(source, mappedStatuses.get(source), count)));
        return preview;
    }

    @Transactional
    public ApplicationImportResult commit(ApplicationImportPreview preview, Map<Integer, ImportDecision> decisions) {
        int imported = 0;
        int merged = 0;
        int skipped = 0;

        for (ApplicationImportRow row : preview.getRows()) {
            ImportDecision decision = decisions.getOrDefault(row.getSpreadsheetRow(), row.getDefaultDecision());
            if (!row.isImportable() || decision == ImportDecision.SKIP) {
                skipped++;
                continue;
            }

            if (decision == ImportDecision.MERGE && row.getExistingApplicationId() != null) {
                merge(row);
                merged++;
            } else {
                importNew(row, preview.getFileName());
                imported++;
            }
        }

        return new ApplicationImportResult(imported, merged, skipped, 0);
    }

    private List<ParsedRow> parseExcel(InputStream inputStream) throws IOException {
        try (Workbook workbook = WorkbookFactory.create(inputStream)) {
            Sheet sheet = workbook.getSheetAt(0);
            DataFormatter formatter = new DataFormatter(Locale.US);
            FormulaEvaluator evaluator = workbook.getCreationHelper().createFormulaEvaluator();
            Row headerRow = sheet.getRow(sheet.getFirstRowNum());
            if (headerRow == null) {
                throw new IllegalArgumentException("The workbook does not contain a header row.");
            }

            Map<String, Integer> headers = new LinkedHashMap<>();
            for (Cell cell : headerRow) {
                String header = clean(formatter.formatCellValue(cell, evaluator));
                if (header != null) {
                    headers.put(header, cell.getColumnIndex());
                }
            }
            validateHeaders(headers.keySet());

            List<ParsedRow> result = new ArrayList<>();
            for (int rowIndex = headerRow.getRowNum() + 1; rowIndex <= sheet.getLastRowNum(); rowIndex++) {
                Row row = sheet.getRow(rowIndex);
                if (row == null || isBlankRow(row, formatter, evaluator)) {
                    continue;
                }

                Map<String, String> values = new LinkedHashMap<>();
                for (Map.Entry<String, Integer> header : headers.entrySet()) {
                    Cell cell = row.getCell(header.getValue(), Row.MissingCellPolicy.RETURN_BLANK_AS_NULL);
                    values.put(header.getKey(), formattedValue(cell, formatter, evaluator));
                }

                LocalDate appliedDate = dateValue(row, headers.get("Applied On"), formatter, evaluator);
                LocalDate updatedDate = dateValue(row, headers.get("Updated Date"), formatter, evaluator);
                result.add(new ParsedRow(rowIndex + 1, values, appliedDate, updatedDate));
            }
            return result;
        } catch (IllegalArgumentException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new IOException("Could not read the Excel workbook: " + ex.getMessage(), ex);
        }
    }

    private List<ParsedRow> parseCsv(InputStream inputStream) throws IOException {
        try (Reader reader = new InputStreamReader(inputStream, StandardCharsets.UTF_8);
             CSVParser parser = CSVFormat.DEFAULT.builder()
                     .setHeader()
                     .setSkipHeaderRecord(true)
                     .setIgnoreEmptyLines(true)
                     .setTrim(true)
                     .build()
                     .parse(reader)) {

            validateHeaders(parser.getHeaderMap().keySet());
            List<ParsedRow> result = new ArrayList<>();
            int rowNumber = 2;
            for (CSVRecord record : parser) {
                Map<String, String> values = new LinkedHashMap<>();
                for (String header : parser.getHeaderMap().keySet()) {
                    values.put(header, clean(record.get(header)));
                }
                if (values.values().stream().allMatch(Objects::isNull)) {
                    rowNumber++;
                    continue;
                }
                result.add(new ParsedRow(
                        rowNumber,
                        values,
                        parseDate(values.get("Applied On")),
                        parseDate(values.get("Updated Date"))));
                rowNumber++;
            }
            return result;
        }
    }

    private void validateHeaders(java.util.Set<String> headers) {
        List<String> missing = REQUIRED_HEADERS.stream().filter(header -> !headers.contains(header)).toList();
        if (!missing.isEmpty()) {
            throw new IllegalArgumentException("Missing required column(s): " + String.join(", ", missing));
        }
    }

    private ApplicationImportRow mapRow(ParsedRow parsed) {
        ApplicationImportRow row = new ApplicationImportRow();
        row.setSpreadsheetRow(parsed.rowNumber());

        JobApplication application = new JobApplication();
        application.setCompany(value(parsed, "Company"));
        application.setRole(value(parsed, "Job Title"));
        application.setLocation(value(parsed, "Location"));
        application.setWorkArrangement(value(parsed, "Work Arrangement"));
        application.setSalary(value(parsed, "Compensation"));
        application.setYearsExperienceRequired(value(parsed, "YOE Req"));
        application.setCareerLane(value(parsed, "Career Lane"));
        application.setSource(value(parsed, "Source"));
        application.setAppliedDate(parsed.appliedDate());
        application.setNextStep(value(parsed, "Next Step / Follow Up"));
        application.setJobUrl(value(parsed, "Job Link"));
        application.setCoverLetter(parseBoolean(value(parsed, "Cover Letter?")));
        application.setNotes(value(parsed, "Notes"));
        application.setPriority(mapPriority(value(parsed, "Priority"), row));

        String sourceStatus = value(parsed, "Status");
        StatusMapping statusMapping = mapStatus(sourceStatus);
        application.setStatus(statusMapping.status());
        application.setState(statusMapping.state());
        row.setOriginalStatus(sourceStatus);
        row.setMappedStatus(statusMapping.label());
        if (statusMapping.skipByDefault()) {
            row.setDefaultDecision(ImportDecision.SKIP);
        }
        if (statusMapping.unrecognized()) {
            row.getWarnings().add("Unrecognized status: " + safe(sourceStatus) + ". Review the mapped stage before import.");
        }

        if (application.getCompany() == null || application.getCompany().isBlank()) {
            row.getWarnings().add("Company is missing.");
        }
        if (application.getRole() == null || application.getRole().isBlank()) {
            row.getWarnings().add("Job Title is missing.");
        }
        if (application.getStatus() != ApplicationStatus.SAVED && application.getAppliedDate() == null) {
            String raw = value(parsed, "Applied On");
            row.getWarnings().add(raw == null ? "Applied date is missing." : "Could not parse Applied On: " + raw);
        }
        if (parsed.updatedDate() == null && value(parsed, "Updated Date") != null) {
            row.getWarnings().add("Could not parse Updated Date: " + value(parsed, "Updated Date"));
        }

        row.setApplication(application);
        row.setHistoricalUpdatedDate(parsed.updatedDate());
        return row;
    }

    private StatusMapping mapStatus(String rawStatus) {
        String normalized = rawStatus == null ? "" : rawStatus.trim().toLowerCase(Locale.ROOT);
        return switch (normalized) {
            case "applied" -> new StatusMapping(ApplicationStatus.APPLIED, ApplicationState.ACTIVE,
                    "Applied / Active", false, false);
            case "ghosted" -> new StatusMapping(ApplicationStatus.NO_RESPONSE, ApplicationState.CLOSED,
                    "No Response / Closed", false, false);
            case "no longer under consideration" -> new StatusMapping(ApplicationStatus.REJECTED, ApplicationState.CLOSED,
                    "Rejected / Closed", false, false);
            case "final interview" -> new StatusMapping(ApplicationStatus.FINAL_ROUND, ApplicationState.AWAITING_FEEDBACK,
                    "Final Round / Awaiting Feedback", false, false);
            case "technical interveiw", "technical interview" -> new StatusMapping(
                    ApplicationStatus.TECHNICAL_INTERVIEW, ApplicationState.AWAITING_FEEDBACK,
                    "Technical Interview / Awaiting Feedback", false, false);
            case "technical screen", "assessment" -> new StatusMapping(
                    ApplicationStatus.ASSESSMENT, ApplicationState.AWAITING_FEEDBACK,
                    "Assessment / Awaiting Feedback", false, false);
            case "skip" -> new StatusMapping(ApplicationStatus.SAVED, ApplicationState.CLOSED,
                    "Saved / Closed (excluded by default)", true, false);
            case "offer" -> new StatusMapping(ApplicationStatus.OFFER, ApplicationState.ACTIVE,
                    "Offer / Active", false, false);
            default -> new StatusMapping(ApplicationStatus.APPLIED, ApplicationState.ACTIVE,
                    "Applied / Active (unrecognized source status)", false, true);
        };
    }

    private Priority mapPriority(String rawPriority, ApplicationImportRow row) {
        if (rawPriority == null) {
            return Priority.UNSPECIFIED;
        }
        return switch (rawPriority.trim().toLowerCase(Locale.ROOT)) {
            case "low" -> Priority.LOW;
            case "medium" -> Priority.MEDIUM;
            case "high" -> Priority.HIGH;
            case "stretch" -> Priority.STRETCH;
            case "skip" -> Priority.SKIP;
            default -> {
                row.getWarnings().add("Unrecognized priority: " + rawPriority);
                yield Priority.UNSPECIFIED;
            }
        };
    }

    private void detectDuplicate(ApplicationImportRow row, List<JobApplication> existing) {
        JobApplication candidate = row.getApplication();
        if (candidate == null) {
            return;
        }

        JobApplication exact = existing.stream()
                .filter(app -> exactMatch(candidate, app))
                .findFirst()
                .orElse(null);
        if (exact != null) {
            row.setDuplicateType(DuplicateMatchType.EXACT);
            row.setExistingApplicationId(exact.getId());
            row.setExistingApplicationLabel(exact.getCompany() + " · " + exact.getRole());
            if (row.getDefaultDecision() != ImportDecision.SKIP) {
                row.setDefaultDecision(ImportDecision.MERGE);
            }
            return;
        }

        JobApplication possible = existing.stream()
                .filter(app -> sameText(candidate.getCompany(), app.getCompany()))
                .filter(app -> sameText(candidate.getRole(), app.getRole()))
                .findFirst()
                .orElse(null);
        if (possible != null) {
            row.setDuplicateType(DuplicateMatchType.POSSIBLE);
            row.setExistingApplicationId(possible.getId());
            row.setExistingApplicationLabel(possible.getCompany() + " · " + possible.getRole());
        }
    }

    private boolean exactMatch(JobApplication candidate, JobApplication existing) {
        if (candidate.getJobUrl() != null && existing.getJobUrl() != null
                && candidate.getJobUrl().trim().equalsIgnoreCase(existing.getJobUrl().trim())) {
            return true;
        }
        return sameText(candidate.getCompany(), existing.getCompany())
                && sameText(candidate.getRole(), existing.getRole())
                && sameText(candidate.getLocation(), existing.getLocation())
                && Objects.equals(candidate.getAppliedDate(), existing.getAppliedDate());
    }

    private void importNew(ApplicationImportRow row, String importSource) {
        JobApplication application = row.getApplication();
        LocalDate createdDate = application.getAppliedDate() != null
                ? application.getAppliedDate()
                : (row.getHistoricalUpdatedDate() != null ? row.getHistoricalUpdatedDate() : LocalDate.now());
        LocalDate updatedDate = row.getHistoricalUpdatedDate() != null ? row.getHistoricalUpdatedDate() : createdDate;
        LocalDateTime createdAt = createdDate.atTime(9, 0);
        LocalDateTime updatedAt = updatedDate.atTime(17, 0);

        long id = applicationRepository.saveImported(application, createdAt, updatedAt, importSource);
        addImportedLifecycle(id, row, true);
    }

    private void merge(ApplicationImportRow row) {
        long existingId = row.getExistingApplicationId();
        JobApplication existing = applicationRepository.findById(existingId)
                .orElseThrow(() -> new IllegalArgumentException("Existing application not found: " + existingId));
        JobApplication imported = row.getApplication();

        if (looksLikeLegacyCombinedLocation(existing.getLocation(), imported.getLocation(), imported.getWorkArrangement())) {
            existing.setLocation(imported.getLocation());
        } else {
            existing.setLocation(prefer(existing.getLocation(), imported.getLocation()));
        }
        existing.setWorkArrangement(prefer(existing.getWorkArrangement(), imported.getWorkArrangement()));
        existing.setYearsExperienceRequired(prefer(existing.getYearsExperienceRequired(), imported.getYearsExperienceRequired()));
        existing.setCareerLane(prefer(existing.getCareerLane(), imported.getCareerLane()));
        existing.setSource(prefer(existing.getSource(), imported.getSource()));
        existing.setJobUrl(prefer(existing.getJobUrl(), imported.getJobUrl()));
        existing.setSalary(prefer(existing.getSalary(), imported.getSalary()));
        existing.setNextStep(prefer(existing.getNextStep(), imported.getNextStep()));
        if (existing.getCoverLetter() == null) {
            existing.setCoverLetter(imported.getCoverLetter());
        }
        if (existing.getAppliedDate() == null || (imported.getAppliedDate() != null && imported.getAppliedDate().isBefore(existing.getAppliedDate()))) {
            existing.setAppliedDate(imported.getAppliedDate());
        }
        if (existing.getPriority() == Priority.UNSPECIFIED && imported.getPriority() != Priority.UNSPECIFIED) {
            existing.setPriority(imported.getPriority());
        }
        existing.setNotes(mergeNotes(existing.getNotes(), imported.getNotes()));

        boolean importedStatusIsNewer = row.getHistoricalUpdatedDate() != null
                && (existing.getUpdatedAt() == null
                    || row.getHistoricalUpdatedDate().isAfter(existing.getUpdatedAt().toLocalDate()));
        if (importedStatusIsNewer) {
            existing.setStatus(imported.getStatus());
            existing.setState(imported.getState());
        }

        LocalDateTime preservedTimestamp = existing.getUpdatedAt() == null ? LocalDateTime.now() : existing.getUpdatedAt();
        applicationRepository.updatePreservingTimestamp(existing, preservedTimestamp);
        addImportedLifecycle(existingId, row, importedStatusIsNewer || existing.getStatus() == imported.getStatus());
    }

    private void addImportedLifecycle(long applicationId, ApplicationImportRow row, boolean includeCurrentStatus) {
        JobApplication application = row.getApplication();
        if (application.getStatus() == ApplicationStatus.SAVED) {
            LocalDate savedDate = row.getHistoricalUpdatedDate() != null ? row.getHistoricalUpdatedDate() : LocalDate.now();
            addEventIfMissing(applicationId, ApplicationEventType.SAVED, savedDate,
                    "Imported saved role", "Imported from spreadsheet row " + row.getSpreadsheetRow() + ".");
            return;
        }

        if (application.getAppliedDate() != null) {
            addEventIfMissing(applicationId, ApplicationEventType.APPLIED, application.getAppliedDate(),
                    "Applied", null);
        }

        if (includeCurrentStatus
                && application.getStatus() != ApplicationStatus.APPLIED
                && row.getHistoricalUpdatedDate() != null) {
            ApplicationEventType type = ApplicationEventType.fromStatus(application.getStatus());
            addEventIfMissing(applicationId, type, row.getHistoricalUpdatedDate(),
                    importedEventTitle(application.getStatus()),
                    "Imported historical status: " + safe(row.getOriginalStatus()) + ".");
        }
    }

    private void addEventIfMissing(long applicationId, ApplicationEventType type, LocalDate date, String title, String notes) {
        if (date == null || eventRepository.exists(applicationId, type, date)) {
            return;
        }
        ApplicationEvent event = new ApplicationEvent();
        event.setApplicationId(applicationId);
        event.setEventType(type);
        event.setEventDate(date);
        event.setTitle(title);
        event.setNotes(notes);
        eventRepository.save(event);
    }

    private String importedEventTitle(ApplicationStatus status) {
        return switch (status) {
            case NO_RESPONSE -> "Closed · no response";
            case REJECTED -> "No longer under consideration";
            case ASSESSMENT -> "Technical assessment";
            default -> status.getDisplayName();
        };
    }

    private String formattedValue(Cell cell, DataFormatter formatter, FormulaEvaluator evaluator) {
        if (cell == null) {
            return null;
        }
        String value = clean(formatter.formatCellValue(cell, evaluator));
        if ((value == null || !looksLikeUrl(value)) && cell.getHyperlink() != null) {
            String address = clean(cell.getHyperlink().getAddress());
            if (address != null) {
                return address;
            }
        }
        return value;
    }

    private LocalDate dateValue(Row row, Integer column, DataFormatter formatter, FormulaEvaluator evaluator) {
        if (row == null || column == null) {
            return null;
        }
        Cell cell = row.getCell(column, Row.MissingCellPolicy.RETURN_BLANK_AS_NULL);
        if (cell == null) {
            return null;
        }
        try {
            if (cell.getCellType() == org.apache.poi.ss.usermodel.CellType.NUMERIC && DateUtil.isCellDateFormatted(cell)) {
                return DateUtil.getLocalDateTime(cell.getNumericCellValue()).toLocalDate();
            }
        } catch (RuntimeException ignored) {
            // Fall through to parsing the formatted display value.
        }
        return parseDate(formatter.formatCellValue(cell, evaluator));
    }

    private boolean isBlankRow(Row row, DataFormatter formatter, FormulaEvaluator evaluator) {
        for (Cell cell : row) {
            if (clean(formatter.formatCellValue(cell, evaluator)) != null) {
                return false;
            }
        }
        return true;
    }

    private LocalDate parseDate(String value) {
        String cleaned = clean(value);
        if (cleaned == null) {
            return null;
        }
        for (DateTimeFormatter formatter : DATE_FORMATTERS) {
            try {
                return LocalDate.parse(cleaned, formatter);
            } catch (DateTimeParseException ignored) {
                // Try the next supported format.
            }
        }
        return null;
    }

    private Boolean parseBoolean(String value) {
        String cleaned = clean(value);
        if (cleaned == null) {
            return null;
        }
        if (cleaned.equalsIgnoreCase("yes") || cleaned.equalsIgnoreCase("y") || cleaned.equalsIgnoreCase("true")) {
            return true;
        }
        if (cleaned.equalsIgnoreCase("no") || cleaned.equalsIgnoreCase("n") || cleaned.equalsIgnoreCase("false")) {
            return false;
        }
        return null;
    }

    private String value(ParsedRow parsed, String header) {
        return clean(parsed.values().get(header));
    }

    private boolean looksLikeLegacyCombinedLocation(String existingLocation, String importedLocation, String workArrangement) {
        String existing = clean(existingLocation);
        String location = clean(importedLocation);
        String arrangement = clean(workArrangement);
        if (existing == null || location == null || arrangement == null) {
            return false;
        }

        String normalizedExisting = existing.toLowerCase(Locale.ROOT)
                .replace("–", "-")
                .replace("—", "-")
                .replace("·", "-")
                .replaceAll("\\s+", " ")
                .trim();
        String normalizedExpected = (location + " - " + arrangement).toLowerCase(Locale.ROOT)
                .replaceAll("\\s+", " ")
                .trim();
        return normalizedExisting.equals(normalizedExpected);
    }

    private String prefer(String current, String imported) {
        return clean(current) != null ? current : clean(imported);
    }

    private String mergeNotes(String existing, String imported) {
        String current = clean(existing);
        String incoming = clean(imported);
        if (incoming == null) {
            return current;
        }
        if (current == null) {
            return incoming;
        }
        if (current.contains(incoming)) {
            return current;
        }
        return current + "\n\nImported spreadsheet notes:\n" + incoming;
    }

    private boolean sameText(String left, String right) {
        String a = clean(left);
        String b = clean(right);
        return a != null && b != null && a.equalsIgnoreCase(b);
    }

    private boolean looksLikeUrl(String value) {
        return value != null && (value.startsWith("http://") || value.startsWith("https://"));
    }

    private String clean(String value) {
        if (value == null) {
            return null;
        }
        String cleaned = value.trim();
        return cleaned.isEmpty() ? null : cleaned;
    }

    private String safe(String value) {
        return value == null ? "Unknown" : value;
    }

    private record ParsedRow(int rowNumber, Map<String, String> values, LocalDate appliedDate, LocalDate updatedDate) { }
    private record StatusMapping(
            ApplicationStatus status,
            ApplicationState state,
            String label,
            boolean skipByDefault,
            boolean unrecognized) { }
}
