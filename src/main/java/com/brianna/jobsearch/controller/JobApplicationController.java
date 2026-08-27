package com.brianna.jobsearch.controller;

import com.brianna.jobsearch.model.ApplicationEvent;
import com.brianna.jobsearch.model.ApplicationEventType;
import com.brianna.jobsearch.model.ApplicationPage;
import com.brianna.jobsearch.model.ApplicationSearchCriteria;
import com.brianna.jobsearch.model.ApplicationSort;
import com.brianna.jobsearch.model.ApplicationState;
import com.brianna.jobsearch.model.ApplicationStatus;
import com.brianna.jobsearch.model.JobApplication;
import com.brianna.jobsearch.model.Priority;
import com.brianna.jobsearch.model.importing.ApplicationImportPreview;
import com.brianna.jobsearch.model.importing.ApplicationImportResult;
import com.brianna.jobsearch.model.importing.ImportDecision;
import com.brianna.jobsearch.service.ApplicationImportService;
import com.brianna.jobsearch.service.JobApplicationService;
import com.brianna.jobsearch.service.PrepService;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.format.annotation.DateTimeFormat;
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

    public JobApplicationController(
            JobApplicationService service,
            PrepService prepService,
            ApplicationImportService importService) {
        this.service = service;
        this.prepService = prepService;
        this.importService = importService;
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
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate appliedFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate appliedTo,
            @RequestParam(required = false) ApplicationSort sort,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "25") int size,
            Model model) {

        ApplicationSearchCriteria filters = new ApplicationSearchCriteria(
                q, status, state, priority, workArrangement, source, careerLane,
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
            Model model) {

        if (bindingResult.hasErrors()) {
            model.addAttribute("editing", false);
            return "applications/form";
        }

        long id = service.create(application);
        return "redirect:/applications/" + id;
    }

    @GetMapping("/applications/{id}")
    public String detail(
            @PathVariable long id,
            @RequestParam(required = false) Long editEvent,
            Model model) {

        model.addAttribute("jobApplication", service.get(id));
        model.addAttribute("events", service.eventsForApplication(id));
        model.addAttribute("applicationPrepItems", prepService.forApplication(id));
        model.addAttribute("linkablePrepItems", prepService.linkableReusableForApplication(id));

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
            Model model) {

        if (bindingResult.hasErrors()) {
            model.addAttribute("editing", true);
            return "applications/form";
        }

        application.setId(id);
        service.update(application);
        return "redirect:/applications/" + id;
    }

    @PostMapping("/applications/{id}/delete")
    public String delete(@PathVariable long id) {
        service.delete(id);
        return "redirect:/applications";
    }
}
