package com.brianna.jobsearch.controller;

import com.brianna.jobsearch.model.ApplicationEvent;
import com.brianna.jobsearch.model.ApplicationEventType;
import com.brianna.jobsearch.model.ApplicationState;
import com.brianna.jobsearch.model.ApplicationStatus;
import com.brianna.jobsearch.model.JobApplication;
import com.brianna.jobsearch.model.Priority;
import com.brianna.jobsearch.service.JobApplicationService;
import com.brianna.jobsearch.service.PrepService;
import jakarta.validation.Valid;
import java.time.LocalDate;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class JobApplicationController {

    private final JobApplicationService service;
    private final PrepService prepService;

    public JobApplicationController(JobApplicationService service, PrepService prepService) {
        this.service = service;
        this.prepService = prepService;
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

    @GetMapping("/applications")
    public String list(@RequestParam(required = false) String q, Model model) {
        model.addAttribute("applications", service.search(q));
        model.addAttribute("query", q == null ? "" : q);
        return "applications/list";
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
