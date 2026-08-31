package com.brianna.jobsearch.controller;

import com.brianna.jobsearch.exception.ResourceNotFoundException;
import com.brianna.jobsearch.model.ApplicationEvent;
import com.brianna.jobsearch.model.ApplicationAttachmentType;
import com.brianna.jobsearch.model.ApplicationEventType;
import com.brianna.jobsearch.model.ApplicationPage;
import com.brianna.jobsearch.model.ApplicationSearchCriteria;
import com.brianna.jobsearch.model.ApplicationSort;
import com.brianna.jobsearch.model.ApplicationState;
import com.brianna.jobsearch.model.ApplicationStatus;
import com.brianna.jobsearch.model.CareerRoleFamily;
import com.brianna.jobsearch.model.DataQualityField;
import com.brianna.jobsearch.model.IndustryDomain;
import com.brianna.jobsearch.model.JobApplication;
import com.brianna.jobsearch.model.Priority;
import com.brianna.jobsearch.model.importing.ApplicationImportPreview;
import com.brianna.jobsearch.model.importing.ApplicationImportResult;
import com.brianna.jobsearch.model.importing.ImportDecision;
import com.brianna.jobsearch.service.ApplicationAttachmentService;
import com.brianna.jobsearch.service.ApplicationImportService;
import com.brianna.jobsearch.service.ApplicationContactService;
import com.brianna.jobsearch.service.CompanyLogoService;
import com.brianna.jobsearch.service.CompanyManagementService;
import com.brianna.jobsearch.service.JobApplicationService;
import com.brianna.jobsearch.service.MaterialService;
import com.brianna.jobsearch.service.PrepService;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.CacheControl;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class JobApplicationController {

    private static final String IMPORT_SESSION_KEY = "applicationImportPreview";

    private final JobApplicationService service;
    private final PrepService prepService;
    private final ApplicationImportService importService;
    private final ApplicationAttachmentService attachmentService;
    private final ApplicationContactService applicationContactService;
    private final CompanyLogoService companyLogoService;
    private final CompanyManagementService companyManagementService;
    private final MaterialService materialService;

    public JobApplicationController(
            JobApplicationService service,
            PrepService prepService,
            ApplicationImportService importService,
            ApplicationAttachmentService attachmentService,
            ApplicationContactService applicationContactService,
            CompanyLogoService companyLogoService,
            CompanyManagementService companyManagementService,
            MaterialService materialService) {
        this.service = service;
        this.prepService = prepService;
        this.importService = importService;
        this.attachmentService = attachmentService;
        this.applicationContactService = applicationContactService;
        this.companyLogoService = companyLogoService;
        this.companyManagementService = companyManagementService;
        this.materialService = materialService;
    }

    @ModelAttribute("statuses")
    public ApplicationStatus[] statuses() {
        return ApplicationStatus.values();
    }

    @ModelAttribute("states")
    public ApplicationState[] states() {
        return ApplicationState.values();
    }

    @ModelAttribute("priorities")
    public Priority[] priorities() {
        return Priority.values();
    }

    @ModelAttribute("eventTypes")
    public ApplicationEventType[] eventTypes() {
        return ApplicationEventType.values();
    }

    @ModelAttribute("roleFamilies")
    public CareerRoleFamily[] roleFamilies() {
        return CareerRoleFamily.values();
    }

    @ModelAttribute("industryDomains")
    public IndustryDomain[] industryDomains() {
        return IndustryDomain.values();
    }

    @ModelAttribute("importDecisions")
    public ImportDecision[] importDecisions() {
        return ImportDecision.values();
    }

    @GetMapping("/applications")
    public String list(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) ApplicationStatus status,
            @RequestParam(required = false) ApplicationState state,
            @RequestParam(required = false) Priority priority,
            @RequestParam(required = false) String workArrangement,
            @RequestParam(required = false) String source,
            @RequestParam(required = false) String careerLane,
            @RequestParam(required = false) CareerRoleFamily roleFamily,
            @RequestParam(required = false) IndustryDomain industryDomain,
            @RequestParam(required = false) DataQualityField missing,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate appliedFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate appliedTo,
            @RequestParam(required = false) ApplicationSort sort,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "25") int size,
            Model model) {

        ApplicationSearchCriteria filters = new ApplicationSearchCriteria(
                q, status, state, priority, workArrangement, source, careerLane, roleFamily, industryDomain, missing,
                appliedFrom, appliedTo, sort, Math.max(0, page - 1), size);
        ApplicationPage result = service.search(filters);

        model.addAttribute("applications", result.getItems());
        model.addAttribute("applicationPage", result);
        model.addAttribute("filters", filters);
        model.addAttribute("sortOptions", ApplicationSort.values());
        model.addAttribute("workArrangements", service.workArrangements());
        model.addAttribute("sources", service.sources());
        model.addAttribute("careerLanes", service.careerLanes());
        return "applications/list";
    }

    @GetMapping("/applications/stale")
    public String staleApplications(
            @RequestParam(defaultValue = "21") int days,
            Model model) {
        List<Integer> options = List.of(14, 21, 30, 45, 60);
        int threshold = options.contains(days) ? days : JobApplicationService.DEFAULT_STALE_DAYS;
        model.addAttribute("staleApplications", service.staleApplications(threshold));
        model.addAttribute("staleDays", threshold);
        model.addAttribute("staleDayOptions", options);
        return "applications/stale";
    }

    @PostMapping("/applications/{id}/stale/keep-active")
    public String keepActive(
            @PathVariable long id,
            @RequestParam(defaultValue = "21") int days,
            RedirectAttributes redirectAttributes) {
        service.acknowledgeStillActive(id);
        redirectAttributes.addFlashAttribute("staleSuccess", "Application kept active and its review date was refreshed.");
        return "redirect:/applications/stale?days=" + days;
    }

    @PostMapping("/applications/{id}/stale/no-response")
    public String markNoResponse(
            @PathVariable long id,
            @RequestParam(defaultValue = "21") int days,
            RedirectAttributes redirectAttributes) {
        service.markNoResponse(id);
        redirectAttributes.addFlashAttribute("staleSuccess", "Application marked No Response and closed.");
        return "redirect:/applications/stale?days=" + days;
    }

    @PostMapping("/applications/{id}/stale/follow-up")
    public String markFollowUpDue(
            @PathVariable long id,
            @RequestParam(defaultValue = "21") int days,
            RedirectAttributes redirectAttributes) {
        service.markFollowUpDue(id);
        redirectAttributes.addFlashAttribute("staleSuccess", "Application marked Follow-up Due and added to the calendar.");
        return "redirect:/applications/stale?days=" + days;
    }

    @PostMapping("/applications/stale/bulk")
    public String bulkStaleAction(
            @RequestParam(required = false) List<Long> selectedIds,
            @RequestParam String action,
            @RequestParam(defaultValue = "21") int days,
            RedirectAttributes redirectAttributes) {
        try {
            int changed = service.applyStaleBulkAction(selectedIds, action);
            if (changed == 0) {
                redirectAttributes.addFlashAttribute("staleError", "Select at least one application first.");
            } else {
                redirectAttributes.addFlashAttribute("staleSuccess", "Updated " + changed + " application" + (changed == 1 ? "." : "s."));
            }
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("staleError", ex.getMessage());
        }
        return "redirect:/applications/stale?days=" + days;
    }

    @GetMapping("/applications/import")
    public String importForm(HttpSession session) {
        session.removeAttribute(IMPORT_SESSION_KEY);
        return "applications/import";
    }

    @PostMapping("/applications/import/preview")
    public String importPreview(
            @RequestParam("file") MultipartFile file,
            HttpSession session,
            Model model) {
        try {
            ApplicationImportPreview preview = importService.preview(file);
            session.setAttribute(IMPORT_SESSION_KEY, preview);
            model.addAttribute("preview", preview);
            return "applications/import-preview";
        } catch (Exception ex) {
            model.addAttribute("importError", ex.getMessage());
            return "applications/import";
        }
    }

    @PostMapping("/applications/import/commit")
    public String importCommit(
            @RequestParam Map<String, String> params,
            HttpSession session,
            RedirectAttributes redirectAttributes) {
        Object sessionValue = session.getAttribute(IMPORT_SESSION_KEY);
        if (!(sessionValue instanceof ApplicationImportPreview preview)) {
            redirectAttributes.addFlashAttribute("importError", "The import preview expired. Upload the file again.");
            return "redirect:/applications/import";
        }

        Map<Integer, ImportDecision> decisions = new HashMap<>();
        for (var row : preview.getRows()) {
            String raw = params.get("action-" + row.getSpreadsheetRow());
            if (raw == null || raw.isBlank()) {
                decisions.put(row.getSpreadsheetRow(), row.getDefaultDecision());
            } else {
                try {
                    decisions.put(row.getSpreadsheetRow(), ImportDecision.valueOf(raw));
                } catch (IllegalArgumentException ignored) {
                    decisions.put(row.getSpreadsheetRow(), row.getDefaultDecision());
                }
            }
        }

        try {
            ApplicationImportResult result = importService.commit(preview, decisions);
            session.removeAttribute(IMPORT_SESSION_KEY);
            redirectAttributes.addFlashAttribute("importSuccess", result.summary());
            return "redirect:/applications";
        } catch (RuntimeException ex) {
            redirectAttributes.addFlashAttribute(
                    "importError",
                    "Import failed and was rolled back. No rows were saved. " +
                            (ex.getMessage() == null ? "Check the application logs for details." : ex.getMessage()));
            return "redirect:/applications/import";
        }
    }

    @GetMapping("/applications/new")
    public String createForm(Model model) {
        JobApplication application = new JobApplication();
        application.setAppliedDate(LocalDate.now());
        model.addAttribute("jobApplication", application);
        model.addAttribute("editing", false);
        return "applications/form";
    }

    @PostMapping("/applications")
    public String create(
            @Valid @ModelAttribute("jobApplication") JobApplication application,
            BindingResult bindingResult,
            @RequestParam(defaultValue = "false") boolean saveAnyway,
            Model model,
            RedirectAttributes redirectAttributes) {

        if (bindingResult.hasErrors()) {
            model.addAttribute("editing", false);
            return "applications/form";
        }

        if (!saveAnyway) {
            var duplicate = service.findPotentialDuplicate(application);
            if (duplicate.isPresent()) {
                model.addAttribute("editing", false);
                model.addAttribute("duplicateApplication", duplicate.get());
                return "applications/form";
            }
        }

        long id = service.create(application);
        refreshLogoWhenDomainChanges(null, application.getCompanyDomain(), redirectAttributes);
        return "redirect:/applications/" + id;
    }

    @GetMapping("/applications/{id}")
    public String detail(
            @PathVariable long id,
            @RequestParam(required = false) Long editEvent,
            Model model) {

        JobApplication application = service.get(id);
        model.addAttribute("jobApplication", application);
        model.addAttribute("logoCached", companyLogoService.hasLogo(application.getCompanyDomain()));
        model.addAttribute("companyGroupKey", CompanyManagementService.normalizeCompanyKey(application.getCompany()));
        model.addAttribute("events", service.eventsForApplication(id));
        model.addAttribute("applicationPrepItems", prepService.forApplication(id));
        model.addAttribute("linkablePrepItems", prepService.linkableReusableForApplication(id));
        model.addAttribute("attachments", attachmentService.forApplication(id));
        model.addAttribute("attachmentTypes", List.of(ApplicationAttachmentType.COVER_LETTER, ApplicationAttachmentType.OTHER));
        model.addAttribute("sharedMaterials", materialService.forApplication(id));
        model.addAttribute("linkableMaterials", materialService.linkableForApplication(id));
        model.addAttribute("linkedPeople", applicationContactService.forApplication(id));
        model.addAttribute("linkablePeople", applicationContactService.linkableForApplication(id));

        if (editEvent != null) {
            model.addAttribute("eventForm", service.getEvent(id, editEvent));
            model.addAttribute("editingEvent", true);
        } else {
            ApplicationEvent event = new ApplicationEvent();
            event.setEventDate(LocalDate.now());
            model.addAttribute("eventForm", event);
            model.addAttribute("editingEvent", false);
        }

        return "applications/detail";
    }

    @PostMapping("/applications/{id}/people/link")
    public String linkPerson(
            @PathVariable long id,
            @RequestParam long contactId,
            RedirectAttributes redirectAttributes) {
        service.get(id);
        try {
            boolean linked = applicationContactService.link(id, contactId);
            var person = applicationContactService.forApplication(id).stream()
                    .filter(contact -> contact.id() == contactId)
                    .findFirst()
                    .orElse(null);
            String personName = person == null ? "Person" : person.name();
            redirectAttributes.addFlashAttribute(
                    "peopleSuccess",
                    linked ? personName + " linked to this application." : personName + " was already linked.");
        } catch (ResourceNotFoundException ex) {
            throw ex;
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("peopleError", ex.getMessage());
        }
        return "redirect:/applications/" + id + "#application-people";
    }

    @PostMapping("/applications/{id}/people/{contactId}/unlink")
    public String unlinkPerson(
            @PathVariable long id,
            @PathVariable long contactId,
            RedirectAttributes redirectAttributes) {
        service.get(id);
        try {
            String name = applicationContactService.forApplication(id).stream()
                    .filter(contact -> contact.id() == contactId)
                    .map(com.brianna.jobsearch.model.CompanyContact::name)
                    .findFirst()
                    .orElse("Person");
            boolean unlinked = applicationContactService.unlink(id, contactId);
            redirectAttributes.addFlashAttribute(
                    "peopleSuccess",
                    unlinked ? name + " unlinked from this application." : name + " was not linked to this application.");
        } catch (ResourceNotFoundException ex) {
            throw ex;
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("peopleError", ex.getMessage());
        }
        return "redirect:/applications/" + id + "#application-people";
    }

    @PostMapping("/applications/{id}/attachments")
    public String uploadAttachment(
            @PathVariable long id,
            @RequestParam String attachmentType,
            @RequestParam("file") MultipartFile file,
            RedirectAttributes redirectAttributes) {
        service.get(id);
        try {
            if (ApplicationAttachmentType.fromFormValue(attachmentType) == ApplicationAttachmentType.RESUME) {
                var result = materialService.uploadAndLink(id, "RESUME", null, null, file);
                redirectAttributes.addFlashAttribute(
                        "materialSuccess",
                        result.created()
                                ? "Resume added to your library and linked: " + result.material().displayName()
                                : "Existing library resume linked: " + result.material().displayName());
            } else {
                var attachment = attachmentService.upload(id, attachmentType, file);
                if (attachment.attachmentType() == ApplicationAttachmentType.COVER_LETTER) {
                    service.markCoverLetterUsedForAttachment(id);
                }
                redirectAttributes.addFlashAttribute(
                        "attachmentSuccess",
                        attachment.attachmentType().getDisplayName() + " attached: " + attachment.fileName());
            }
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("attachmentError", ex.getMessage());
        }
        return "redirect:/applications/" + id + "#manage-materials";
    }

    @GetMapping("/applications/{id}/attachments/{attachmentId}")
    public ResponseEntity<byte[]> downloadAttachment(
            @PathVariable long id,
            @PathVariable long attachmentId) {
        service.get(id);
        var content = attachmentService.download(id, attachmentId);
        MediaType mediaType;
        try {
            mediaType = MediaType.parseMediaType(content.metadata().mimeType());
        } catch (IllegalArgumentException ex) {
            mediaType = MediaType.APPLICATION_OCTET_STREAM;
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(mediaType);
        headers.setContentLength(content.data().length);
        headers.setContentDisposition(ContentDisposition.attachment()
                .filename(content.metadata().fileName(), StandardCharsets.UTF_8)
                .build());
        headers.setCacheControl(CacheControl.noStore().getHeaderValue());
        headers.set("X-Content-Type-Options", "nosniff");
        return ResponseEntity.ok().headers(headers).body(content.data());
    }

    @PostMapping("/applications/{id}/attachments/{attachmentId}/delete")
    public String deleteAttachment(
            @PathVariable long id,
            @PathVariable long attachmentId,
            RedirectAttributes redirectAttributes) {
        service.get(id);
        try {
            var attachment = attachmentService.metadata(id, attachmentId);
            attachmentService.delete(id, attachmentId);
            redirectAttributes.addFlashAttribute("attachmentSuccess", "Removed attachment: " + attachment.fileName());
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("attachmentError", ex.getMessage());
        }
        return "redirect:/applications/" + id + "#manage-materials";
    }

    @PostMapping("/applications/{id}/materials")
    public String uploadSharedMaterial(
            @PathVariable long id,
            @RequestParam(defaultValue = "RESUME") String materialType,
            @RequestParam(required = false) String displayName,
            @RequestParam(required = false) String notes,
            @RequestParam("file") MultipartFile file,
            RedirectAttributes redirectAttributes) {
        service.get(id);
        try {
            var result = materialService.uploadAndLink(id, materialType, displayName, notes, file);
            redirectAttributes.addFlashAttribute(
                    "materialSuccess",
                    result.created()
                            ? "Added to your Materials Library and linked: " + result.material().displayName()
                            : "That file was already in your library, so the existing copy was linked: " + result.material().displayName());
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("materialError", ex.getMessage());
        }
        return "redirect:/applications/" + id + "#manage-materials";
    }

    @PostMapping("/applications/{id}/materials/{materialId}/link")
    public String linkSharedMaterial(
            @PathVariable long id,
            @PathVariable long materialId,
            RedirectAttributes redirectAttributes) {
        service.get(id);
        boolean linked = materialService.link(id, materialId);
        var material = materialService.get(materialId);
        redirectAttributes.addFlashAttribute(
                "materialSuccess",
                linked ? "Linked “" + material.displayName() + "”." : "“" + material.displayName() + "” was already linked.");
        return "redirect:/applications/" + id + "#manage-materials";
    }

    @PostMapping("/applications/{id}/materials/{materialId}/unlink")
    public String unlinkSharedMaterial(
            @PathVariable long id,
            @PathVariable long materialId,
            RedirectAttributes redirectAttributes) {
        service.get(id);
        var material = materialService.get(materialId);
        materialService.unlink(id, materialId);
        redirectAttributes.addFlashAttribute("materialSuccess", "Unlinked “" + material.displayName() + "”.");
        return "redirect:/applications/" + id + "#manage-materials";
    }

    @GetMapping("/applications/{id}/logo")
    public ResponseEntity<byte[]> companyLogo(@PathVariable long id) {
        JobApplication application = service.get(id);
        return companyLogoService.find(application.getCompanyDomain())
                .map(logo -> ResponseEntity.ok()
                        .cacheControl(CacheControl.noStore())
                        .contentType(MediaType.parseMediaType(logo.mimeType()))
                        .body(logo.data()))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PostMapping("/applications/{id}/logo/fetch")
    public String fetchCompanyLogo(@PathVariable long id, RedirectAttributes redirectAttributes) {
        JobApplication application = service.get(id);
        try {
            companyLogoService.fetchAndCache(application.getCompanyDomain());
            redirectAttributes.addFlashAttribute("logoSuccess", "Company logo cached locally.");
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("logoError", ex.getMessage());
        }
        return "redirect:/applications/" + id + "#company-branding";
    }

    @PostMapping("/applications/{id}/logo/upload")
    public String uploadCompanyLogo(
            @PathVariable long id,
            @RequestParam("logo") MultipartFile logo,
            RedirectAttributes redirectAttributes) {
        JobApplication application = service.get(id);
        try {
            companyLogoService.storeUpload(application.getCompanyDomain(), logo);
            redirectAttributes.addFlashAttribute("logoSuccess", "Company logo uploaded and cached locally.");
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("logoError", ex.getMessage());
        }
        return "redirect:/applications/" + id + "#company-branding";
    }

    @PostMapping("/applications/{id}/logo/delete")
    public String deleteCompanyLogo(@PathVariable long id, RedirectAttributes redirectAttributes) {
        JobApplication application = service.get(id);
        try {
            companyLogoService.delete(application.getCompanyDomain());
            redirectAttributes.addFlashAttribute("logoSuccess", "Cached company logo removed. Initials will be used instead.");
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("logoError", ex.getMessage());
        }
        return "redirect:/applications/" + id + "#company-branding";
    }

    @PostMapping("/applications/{id}/events")
    public String addEvent(
            @PathVariable long id,
            @ModelAttribute("eventForm") ApplicationEvent event) {

        service.addEvent(id, event);
        return "redirect:/applications/" + id;
    }

    @PostMapping("/applications/{id}/events/{eventId}")
    public String updateEvent(
            @PathVariable long id,
            @PathVariable long eventId,
            @ModelAttribute("eventForm") ApplicationEvent event) {

        service.updateEvent(id, eventId, event);
        return "redirect:/applications/" + id;
    }

    @PostMapping("/applications/{id}/events/{eventId}/delete")
    public String deleteEvent(@PathVariable long id, @PathVariable long eventId) {
        service.deleteEvent(id, eventId);
        return "redirect:/applications/" + id;
    }

    @PostMapping("/applications/{id}/prep/{prepId}/link")
    public String linkPrepItem(@PathVariable long id, @PathVariable long prepId) {
        service.get(id);
        prepService.linkToApplication(prepId, id);
        return "redirect:/applications/" + id + "#interview-prep";
    }

    @PostMapping("/applications/{id}/prep/{prepId}/unlink")
    public String unlinkPrepItem(@PathVariable long id, @PathVariable long prepId) {
        service.get(id);
        prepService.unlinkFromApplication(prepId, id);
        return "redirect:/applications/" + id + "#interview-prep";
    }

    @GetMapping("/applications/{id}/edit")
    public String editForm(@PathVariable long id, Model model) {
        model.addAttribute("jobApplication", service.get(id));
        model.addAttribute("editing", true);
        return "applications/form";
    }

    @PostMapping("/applications/{id}")
    public String update(
            @PathVariable long id,
            @Valid @ModelAttribute("jobApplication") JobApplication application,
            BindingResult bindingResult,
            @RequestParam(defaultValue = "false") boolean saveAnyway,
            Model model,
            RedirectAttributes redirectAttributes) {

        application.setId(id);
        if (bindingResult.hasErrors()) {
            model.addAttribute("editing", true);
            return "applications/form";
        }

        JobApplication previous = service.get(id);
        String previousDomain = previous.getCompanyDomain();

        if (!saveAnyway) {
            var duplicate = service.findPotentialDuplicate(application);
            if (duplicate.isPresent()) {
                model.addAttribute("editing", true);
                model.addAttribute("duplicateApplication", duplicate.get());
                return "applications/form";
            }
        }

        service.update(application);
        refreshLogoWhenDomainChanges(previousDomain, application.getCompanyDomain(), redirectAttributes);
        return "redirect:/applications/" + id;
    }


    /**
     * Domain edits should feel like enough configuration on their own. Save the
     * application first, then refresh the cached logo as a best-effort network
     * operation. A failed logo lookup must never roll back the application edit.
     */
    private void refreshLogoWhenDomainChanges(
            String previousDomain,
            String currentDomain,
            RedirectAttributes redirectAttributes) {

        String before = CompanyLogoService.normalizeDomain(previousDomain);
        String after = CompanyLogoService.normalizeDomain(currentDomain);

        if (after == null || Objects.equals(before, after)) {
            return;
        }

        try {
            companyLogoService.fetchAndCache(after);
            redirectAttributes.addFlashAttribute(
                    "logoAutoSuccess",
                    "Company domain updated and the logo was refreshed automatically.");
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute(
                    "logoAutoWarning",
                    "Company domain saved, but a logo could not be fetched automatically. " + ex.getMessage());
        }
    }

    @PostMapping("/applications/{id}/delete")
    public String delete(@PathVariable long id) {
        service.delete(id);
        return "redirect:/applications";
    }
}
