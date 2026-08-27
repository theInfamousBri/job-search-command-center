package com.brianna.jobsearch.controller;

import com.brianna.jobsearch.service.AnalyticsService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class AnalyticsController {

    private final AnalyticsService analyticsService;

    public AnalyticsController(AnalyticsService analyticsService) {
        this.analyticsService = analyticsService;
    }

    @GetMapping("/analytics")
    public String analytics(Model model) {
        model.addAttribute("analytics", analyticsService.snapshot());
        return "analytics";
    }
}
