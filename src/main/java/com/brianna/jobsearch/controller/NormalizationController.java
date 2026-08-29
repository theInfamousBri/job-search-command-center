package com.brianna.jobsearch.controller;

import com.brianna.jobsearch.model.CareerRoleFamily;
import com.brianna.jobsearch.model.IndustryDomain;
import com.brianna.jobsearch.service.NormalizationService;
import java.util.List;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class NormalizationController {

    private final NormalizationService normalizationService;

    public NormalizationController(NormalizationService normalizationService) {
        this.normalizationService = normalizationService;
    }

    @GetMapping("/data-quality/normalize")
    public String normalizationCenter(
            @RequestParam(required = false) String q,
            @RequestParam(defaultValue = "UNMAPPED") String careerStatus,
            Model model) {

        model.addAttribute("normalization", normalizationService.snapshot(q, careerStatus));
        model.addAttribute("roleFamilies", CareerRoleFamily.values());
        model.addAttribute("industryDomains", IndustryDomain.values());
        return "normalization";
    }

    @PostMapping("/data-quality/normalize/career")
    public String normalizeCareer(
            @RequestParam(name = "legacyTag", required = false) List<String> legacyTags,
            @RequestParam(required = false) CareerRoleFamily roleFamily,
            @RequestParam(required = false) IndustryDomain industryDomain,
            @RequestParam(required = false) String focus,
            @RequestParam(defaultValue = "false") boolean overwriteExisting,
            RedirectAttributes redirectAttributes) {

        try {
            int changed = normalizationService.applyCareerMapping(
                    legacyTags, roleFamily, industryDomain, focus, overwriteExisting);
            redirectAttributes.addFlashAttribute(
                    "normalizationSuccess",
                    changed + " application" + (changed == 1 ? "" : "s") + " reviewed by the career mapping.");
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("normalizationError", ex.getMessage());
        }
        return "redirect:/data-quality/normalize#career-taxonomy";
    }

    @PostMapping("/data-quality/normalize/career/suggestions")
    public String applySuggestedCareerMappings(
            @RequestParam(name = "legacyTag", required = false) List<String> legacyTags,
            RedirectAttributes redirectAttributes) {

        try {
            var result = normalizationService.applySuggestedCareerMappings(legacyTags);
            String skipped = result.groupsSkipped() == 0
                    ? ""
                    : " " + result.groupsSkipped() + " selected group" + (result.groupsSkipped() == 1 ? " had" : "s had") + " no suggestion and was left unchanged.";
            redirectAttributes.addFlashAttribute(
                    "normalizationSuccess",
                    "Suggested mappings reviewed " + result.applicationsReviewed() + " application"
                            + (result.applicationsReviewed() == 1 ? "" : "s")
                            + " across " + result.groupsApplied() + " tag group"
                            + (result.groupsApplied() == 1 ? "" : "s") + "." + skipped);
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("normalizationError", ex.getMessage());
        }
        return "redirect:/data-quality/normalize#career-taxonomy";
    }

    @PostMapping("/data-quality/normalize/source")
    public String normalizeSources(
            @RequestParam(name = "sourceValue", required = false) List<String> sourceValues,
            @RequestParam(required = false) String targetSource,
            RedirectAttributes redirectAttributes) {

        try {
            int changed = normalizationService.normalizeSources(sourceValues, targetSource);
            redirectAttributes.addFlashAttribute(
                    "normalizationSuccess",
                    changed + " application" + (changed == 1 ? "" : "s") + " updated to source “" + targetSource.trim() + "”.");
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("normalizationError", ex.getMessage());
        }
        return "redirect:/data-quality/normalize#source-normalization";
    }

    @PostMapping("/data-quality/normalize/work-arrangement")
    public String normalizeWorkArrangements(
            @RequestParam(name = "workValue", required = false) List<String> workValues,
            @RequestParam(required = false) String targetWorkArrangement,
            RedirectAttributes redirectAttributes) {

        try {
            int changed = normalizationService.normalizeWorkArrangements(workValues, targetWorkArrangement);
            redirectAttributes.addFlashAttribute(
                    "normalizationSuccess",
                    changed + " application" + (changed == 1 ? "" : "s") + " updated to work arrangement “" + targetWorkArrangement.trim() + "”.");
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("normalizationError", ex.getMessage());
        }
        return "redirect:/data-quality/normalize#work-normalization";
    }
}
