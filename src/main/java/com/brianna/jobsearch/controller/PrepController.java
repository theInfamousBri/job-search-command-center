package com.brianna.jobsearch.controller;

import com.brianna.jobsearch.model.JobApplication;
import com.brianna.jobsearch.model.PrepItem;
import com.brianna.jobsearch.model.PrepItemType;
import com.brianna.jobsearch.service.JobApplicationService;
import com.brianna.jobsearch.service.PrepService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class PrepController {

    private final PrepService prepService;
    private final JobApplicationService applicationService;

    public PrepController(PrepService prepService, JobApplicationService applicationService) {
        this.prepService = prepService;
        this.applicationService = applicationService;
    }

    @ModelAttribute("prepTypes")
    public PrepItemType[] prepTypes() {
        return PrepItemType.values();
    }

    @ModelAttribute("applications")
    public List<JobApplication> applications() {
        return applicationService.findAll();
    }

    @GetMapping("/prep")
    public String prep(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) PrepItemType type,
            @RequestParam(required = false) Long applicationId,
            @RequestParam(required = false) Long reviewed,
            Model model) {

        model.addAttribute("items", prepService.search(q, type, applicationId));
        model.addAttribute("prep", prepService.snapshot());
        model.addAttribute("query", q == null ? "" : q);
        model.addAttribute("selectedType", type);
        model.addAttribute("selectedApplicationId", applicationId);
        model.addAttribute("reviewedItemId", reviewed);
        return "prep/index";
    }

    @GetMapping("/prep/new")
    public String createForm(@RequestParam(required = false) Long applicationId, Model model) {
        PrepItem item = new PrepItem();
        item.setApplicationId(applicationId);
        model.addAttribute("prepItem", item);
        model.addAttribute("editing", false);
        return "prep/form";
    }

    @PostMapping("/prep")
    public String create(
            @Valid @ModelAttribute("prepItem") PrepItem item,
            BindingResult bindingResult,
            Model model) {

        if (bindingResult.hasErrors()) {
            model.addAttribute("editing", false);
            return "prep/form";
        }

        Long applicationId = item.getApplicationId();
        prepService.create(item);
        if (applicationId != null) {
            return "redirect:/applications/" + applicationId + "#interview-prep";
        }
        return "redirect:/prep";
    }

    @GetMapping("/prep/{id}/edit")
    public String editForm(@PathVariable long id, Model model) {
        model.addAttribute("prepItem", prepService.get(id));
        model.addAttribute("editing", true);
        return "prep/form";
    }

    @PostMapping("/prep/{id}")
    public String update(
            @PathVariable long id,
            @Valid @ModelAttribute("prepItem") PrepItem item,
            BindingResult bindingResult,
            Model model) {

        if (bindingResult.hasErrors()) {
            item.setId(id);
            model.addAttribute("editing", true);
            return "prep/form";
        }

        item.setId(id);
        prepService.update(item);
        if (item.getApplicationId() != null) {
            return "redirect:/applications/" + item.getApplicationId() + "#interview-prep";
        }
        return "redirect:/prep";
    }

    @GetMapping("/prep/{id}/review")
    public String review(
            @PathVariable long id,
            @RequestParam(required = false) Long applicationId,
            Model model) {
        model.addAttribute("prepItem", prepService.get(id));
        model.addAttribute("returnApplicationId", applicationId);
        return "prep/review";
    }

    @PostMapping("/prep/{id}/review")
    public String markReviewed(
            @PathVariable long id,
            @RequestParam int confidence,
            @RequestParam(required = false) Long applicationId) {

        prepService.markReviewed(id, confidence);
        if (applicationId != null) {
            return "redirect:/applications/" + applicationId + "#interview-prep";
        }
        return "redirect:/prep?reviewed=" + id + "#review-queue";
    }

    @PostMapping("/prep/{id}/delete")
    public String delete(@PathVariable long id) {
        prepService.delete(id);
        return "redirect:/prep";
    }
}
