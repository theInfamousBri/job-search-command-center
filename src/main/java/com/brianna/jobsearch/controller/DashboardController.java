package com.brianna.jobsearch.controller;

import com.brianna.jobsearch.service.JobApplicationService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class DashboardController {

    private final JobApplicationService service;

    public DashboardController(JobApplicationService service) {
        this.service = service;
    }

    @GetMapping("/")
    public String dashboard(Model model) {
        model.addAttribute("dashboard", service.dashboardSnapshot());
        return "dashboard";
    }
}
