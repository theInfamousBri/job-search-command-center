package com.brianna.jobsearch.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.brianna.jobsearch.model.ApplicationStatus;
import com.brianna.jobsearch.model.JobApplication;
import com.brianna.jobsearch.model.Priority;
import com.brianna.jobsearch.model.importing.ApplicationImportPreview;
import com.brianna.jobsearch.model.importing.DuplicateMatchType;
import com.brianna.jobsearch.model.importing.ImportDecision;
import com.brianna.jobsearch.repository.ApplicationEventRepository;
import com.brianna.jobsearch.repository.JobApplicationRepository;
import java.io.ByteArrayOutputStream;
import java.time.LocalDate;
import java.util.List;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.CreationHelper;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

class ApplicationImportServiceTest {

    private final JobApplicationRepository applications = mock(JobApplicationRepository.class);
    private final ApplicationEventRepository events = mock(ApplicationEventRepository.class);
    private final ApplicationImportService service = new ApplicationImportService(applications, events);

    @Test
    void previewsCsvAndNormalizesHistoricalStatuses() throws Exception {
        when(applications.findAll(null)).thenReturn(List.of());
        String csv = String.join("\n",
                "Job Title,Company,Location,Work Arrangement,Compensation,YOE Req,Priority,Career Lane,Applied On,Updated Date,Status,Next Step / Follow Up,Job Link,Cover Letter?,Notes",
                "Senior Backend Engineer,Northstar Labs,Denver CO,Hybrid,$150k-$175k,5+,Stretch,Backend / Platform,8/7/2026,8/25/2026,Final Interview,Awaiting decision,https://example.com/jobs/123,Yes,Great team");

        ApplicationImportPreview preview = service.preview(new MockMultipartFile(
                "file", "tracker.csv", "text/csv", csv.getBytes()));

        assertThat(preview.getRows()).hasSize(1);
        var row = preview.getRows().getFirst();
        assertThat(row.getApplication().getStatus()).isEqualTo(ApplicationStatus.FINAL_ROUND);
        assertThat(row.getApplication().getPriority()).isEqualTo(Priority.STRETCH);
        assertThat(row.getApplication().getWorkArrangement()).isEqualTo("Hybrid");
        assertThat(row.getApplication().getCareerLane()).isEqualTo("Backend / Platform");
        assertThat(row.getApplication().getCoverLetter()).isTrue();
        assertThat(row.getMappedStatus()).isEqualTo("Final Round / Awaiting Feedback");
        assertThat(row.getHistoricalUpdatedDate()).isEqualTo(LocalDate.of(2026, 8, 25));
    }

    @Test
    void excelPreviewUsesDisplayedValueForDateFormattedYoeCells() throws Exception {
        when(applications.findAll(null)).thenReturn(List.of());

        try (XSSFWorkbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("Sheet1");
            String[] headers = {
                    "Job Title", "Company", "Location", "Work Arrangement", "Compensation", "YOE Req",
                    "Priority", "Career Lane", "Applied On", "Updated Date", "Status", "Next Step / Follow Up",
                    "Job Link", "Cover Letter?", "Notes"
            };
            Row header = sheet.createRow(0);
            for (int i = 0; i < headers.length; i++) {
                header.createCell(i).setCellValue(headers[i]);
            }

            Row row = sheet.createRow(1);
            row.createCell(0).setCellValue("Software Engineer");
            row.createCell(1).setCellValue("Atlas Payments");
            row.createCell(2).setCellValue("Denver, CO");
            row.createCell(3).setCellValue("Hybrid");

            CreationHelper helper = workbook.getCreationHelper();
            CellStyle yoeStyle = workbook.createCellStyle();
            yoeStyle.setDataFormat(helper.createDataFormat().getFormat("m-d"));
            var yoeCell = row.createCell(5);
            yoeCell.setCellValue(LocalDate.of(2026, 3, 5));
            yoeCell.setCellStyle(yoeStyle);

            CellStyle dateStyle = workbook.createCellStyle();
            dateStyle.setDataFormat(helper.createDataFormat().getFormat("m/d/yyyy"));
            var applied = row.createCell(8);
            applied.setCellValue(LocalDate.of(2026, 8, 7));
            applied.setCellStyle(dateStyle);
            var updated = row.createCell(9);
            updated.setCellValue(LocalDate.of(2026, 8, 11));
            updated.setCellStyle(dateStyle);
            row.createCell(10).setCellValue("Technical Screen");

            workbook.write(output);
            ApplicationImportPreview preview = service.preview(new MockMultipartFile(
                    "file", "tracker.xlsx",
                    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                    output.toByteArray()));

            assertThat(preview.getRows()).hasSize(1);
            assertThat(preview.getRows().getFirst().getApplication().getYearsExperienceRequired()).isEqualTo("3-5");
            assertThat(preview.getRows().getFirst().getApplication().getStatus()).isEqualTo(ApplicationStatus.ASSESSMENT);
        }
    }

    @Test
    void exactJobUrlDefaultsToMergeButCompanyRoleOnlyDoesNot() throws Exception {
        JobApplication existing = new JobApplication();
        existing.setId(42L);
        existing.setCompany("Atlas Payments");
        existing.setRole("Software Engineer III");
        existing.setLocation("Denver, CO");
        existing.setAppliedDate(LocalDate.of(2026, 8, 7));
        existing.setJobUrl("https://example.com/jobs/atlas-123");
        when(applications.findAll(null)).thenReturn(List.of(existing));

        String csv = String.join("\n",
                "Job Title,Company,Location,Status,Applied On,Job Link",
                "Software Engineer III,Atlas Payments,Remote,Applied,8/7/2026,https://example.com/jobs/atlas-123",
                "Software Engineer III,Atlas Payments,New York NY,Applied,8/8/2026,https://example.com/jobs/atlas-other");

        ApplicationImportPreview preview = service.preview(new MockMultipartFile(
                "file", "tracker.csv", "text/csv", csv.getBytes()));

        assertThat(preview.getRows()).hasSize(2);
        assertThat(preview.getRows().get(0).getDuplicateType()).isEqualTo(DuplicateMatchType.EXACT);
        assertThat(preview.getRows().get(0).getDefaultDecision()).isEqualTo(ImportDecision.MERGE);
        assertThat(preview.getRows().get(1).getDuplicateType()).isEqualTo(DuplicateMatchType.POSSIBLE);
        assertThat(preview.getRows().get(1).getDefaultDecision()).isEqualTo(ImportDecision.IMPORT);
    }
    @Test
    void importsOptionalCompanyDomainAndCoverLetterTextColumns() throws Exception {
        when(applications.findAll(null)).thenReturn(List.of());
        String csv = String.join("\n",
                "Job Title,Company,Status,Applied On,Company Domain,Cover Letter?,Cover Letter Text",
                "Senior Software Engineer,Atlas Payments,Applied,8/7/2026,https://www.atlas.example/careers,No,Dear Atlas team...");

        ApplicationImportPreview preview = service.preview(new MockMultipartFile(
                "file", "tracker.csv", "text/csv", csv.getBytes()));

        JobApplication imported = preview.getRows().getFirst().getApplication();
        assertThat(imported.getCompanyDomain()).isEqualTo("atlas.example");
        assertThat(imported.getCoverLetterText()).isEqualTo("Dear Atlas team...");
        assertThat(imported.getCoverLetter()).isTrue();
    }

}
