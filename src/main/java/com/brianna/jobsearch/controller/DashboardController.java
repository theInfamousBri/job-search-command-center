package com.brianna.jobsearch.controller;

import com.brianna.jobsearch.service.JobApplicationService;
import com.brianna.jobsearch.service.NeedsAttentionService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class DashboardController {

    private final JobApplicationService service;
    private final NeedsAttentionService needsAttentionService;

    public DashboardController(JobApplicationService service, NeedsAttentionService needsAttentionService) {
        this.service = service;
        this.needsAttentionService = needsAttentionService;
    }

    @GetMapping("/")
    public String dashboard(Model model) {
        model.addAttribute("dashboard", service.dashboardSnapshot());
        model.addAttribute("attention", needsAttentionService.snapshot());
        return "dashboard";
    }
}
