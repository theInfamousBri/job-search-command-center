package com.brianna.jobsearch.controller;

import com.brianna.jobsearch.service.DataQualityService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class DataQualityController {

    private final DataQualityService dataQualityService;

    public DataQualityController(DataQualityService dataQualityService) {
        this.dataQualityService = dataQualityService;
    }

    @GetMapping("/data-quality")
    public String dataQuality(Model model) {
        model.addAttribute("dataQuality", dataQualityService.snapshot());
        return "data-quality";
    }
}
