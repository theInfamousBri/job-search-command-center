package com.brianna.jobsearch.controller;

import com.brianna.jobsearch.model.CompanyContactRelationship;
import com.brianna.jobsearch.repository.CompanyLogoRepository.CompanyLogo;
import com.brianna.jobsearch.service.CompanyLogoService;
import com.brianna.jobsearch.service.CompanyManagementService;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.util.UriUtils;

@Controller
public class CompanyManagementController {

    private final CompanyManagementService companyService;
    private final CompanyLogoService logoService;

    public CompanyManagementController(
            CompanyManagementService companyService,
            CompanyLogoService logoService) {
        this.companyService = companyService;
        this.logoService = logoService;
    }

    @GetMapping("/companies")
    public String companies(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "20") Integer size,
            Model model) {
        model.addAttribute("companies", companyService.snapshot(q, status, page, size));
        return "companies";
    }

    @GetMapping("/companies/domain-suggestion")
    public ResponseEntity<Map<String, String>> domainSuggestion(@RequestParam String company) {
        var domain = companyService.knownDomainForCompany(company);
        if (domain.isEmpty()) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.ok(Map.of("domain", domain.get()));
    }

    @GetMapping("/companies/logo")
    public ResponseEntity<byte[]> logo(@RequestParam String domain) {
        return logoService.find(domain)
                .map(this::logoResponse)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping("/companies/{groupKey}")
    public String companyDetail(@PathVariable String groupKey, Model model) {
        model.addAttribute("company", companyService.detail(groupKey));
        model.addAttribute("contactRelationships", CompanyContactRelationship.values());
        return "company-detail";
    }

    @GetMapping("/companies/people/{contactId}/photo")
    public ResponseEntity<byte[]> contactPhoto(@PathVariable long contactId) {
        return companyService.contactPhoto(contactId)
                .map(photo -> ResponseEntity.ok()
                        .cacheControl(CacheControl.noStore())
                        .contentType(MediaType.parseMediaType(photo.mimeType()))
                        .body(photo.data()))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PostMapping("/companies/{groupKey}/people")
    public String addPerson(
            @PathVariable String groupKey,
            @RequestParam String name,
            @RequestParam(required = false) String role,
            @RequestParam(required = false) String relationshipType,
            @RequestParam(required = false) String email,
            @RequestParam(required = false) String linkedinUrl,
            @RequestParam(required = false) String notes,
            @RequestParam(required = false) MultipartFile photo,
            RedirectAttributes redirectAttributes) {
        try {
            var contact = companyService.createContact(
                    groupKey, name, role, relationshipType, email, linkedinUrl, notes, photo);
            redirectAttributes.addFlashAttribute("companySuccess", contact.name() + " added to this company.");
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("companyError", ex.getMessage());
        }
        return "redirect:/companies/" + encodePath(groupKey);
    }

    @PostMapping("/companies/{groupKey}/people/{contactId}")
    public String updatePerson(
            @PathVariable String groupKey,
            @PathVariable long contactId,
            @RequestParam String name,
            @RequestParam(required = false) String role,
            @RequestParam(required = false) String relationshipType,
            @RequestParam(required = false) String email,
            @RequestParam(required = false) String linkedinUrl,
            @RequestParam(required = false) String notes,
            @RequestParam(required = false) MultipartFile photo,
            @RequestParam(defaultValue = "false") boolean removePhoto,
            RedirectAttributes redirectAttributes) {
        try {
            var contact = companyService.updateContact(
                    groupKey, contactId, name, role, relationshipType, email, linkedinUrl, notes, photo, removePhoto);
            redirectAttributes.addFlashAttribute("companySuccess", contact.name() + " updated.");
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("companyError", ex.getMessage());
        }
        return "redirect:/companies/" + encodePath(groupKey);
    }

    @PostMapping("/companies/{groupKey}/people/{contactId}/delete")
    public String deletePerson(
            @PathVariable String groupKey,
            @PathVariable long contactId,
            RedirectAttributes redirectAttributes) {
        try {
            companyService.deleteContact(groupKey, contactId);
            redirectAttributes.addFlashAttribute("companySuccess", "Person removed from this company.");
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("companyError", ex.getMessage());
        }
        return "redirect:/companies/" + encodePath(groupKey);
    }

    @PostMapping("/companies/{groupKey}/notes")
    public String saveCompanyNotes(
            @PathVariable String groupKey,
            @RequestParam(required = false) String notes,
            RedirectAttributes redirectAttributes) {
        try {
            companyService.saveNotes(groupKey, notes);
            redirectAttributes.addFlashAttribute("companySuccess", "Company notes saved.");
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("companyError", ex.getMessage());
        }
        return "redirect:/companies/" + encodePath(groupKey);
    }

    @PostMapping("/companies/domain")
    public String setDomain(
            @RequestParam String groupKey,
            @RequestParam String domain,
            @RequestParam(defaultValue = "false") boolean fetchLogo,
            @RequestParam(required = false) String returnTo,
            RedirectAttributes redirectAttributes) {
        try {
            var result = companyService.setDomain(groupKey, domain, fetchLogo);
            String message = result.applicationsUpdated() + " application"
                    + (result.applicationsUpdated() == 1 ? "" : "s")
                    + " now use " + result.domain() + ".";
            if (result.logoFetched()) {
                message += " Logo refreshed locally.";
            }
            redirectAttributes.addFlashAttribute("companySuccess", message);
            if (result.logoWarning() != null) {
                redirectAttributes.addFlashAttribute(
                        "companyWarning",
                        "The domain was saved, but automatic logo lookup failed: " + result.logoWarning());
            }
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("companyError", ex.getMessage());
        }
        return redirectAfterCompanyAction(returnTo, groupKey);
    }

    @PostMapping("/companies/logo/fetch")
    public String refreshLogo(
            @RequestParam String groupKey,
            @RequestParam(required = false) String returnTo,
            RedirectAttributes redirectAttributes) {
        try {
            companyService.refreshLogo(groupKey);
            redirectAttributes.addFlashAttribute("companySuccess", "Shared company logo refreshed locally.");
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("companyError", ex.getMessage());
        }
        return redirectAfterCompanyAction(returnTo, groupKey);
    }

    @PostMapping("/companies/logo/upload")
    public String uploadLogo(
            @RequestParam String groupKey,
            @RequestParam("logo") MultipartFile logo,
            @RequestParam(required = false) String returnTo,
            RedirectAttributes redirectAttributes) {
        try {
            companyService.uploadLogo(groupKey, logo);
            redirectAttributes.addFlashAttribute("companySuccess", "Shared company logo uploaded locally.");
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("companyError", ex.getMessage());
        }
        return redirectAfterCompanyAction(returnTo, groupKey);
    }

    @PostMapping("/companies/logo/delete")
    public String deleteLogo(
            @RequestParam String groupKey,
            @RequestParam(required = false) String returnTo,
            RedirectAttributes redirectAttributes) {
        try {
            companyService.deleteLogo(groupKey);
            redirectAttributes.addFlashAttribute("companySuccess", "Shared company logo removed. Initials will be used instead.");
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("companyError", ex.getMessage());
        }
        return redirectAfterCompanyAction(returnTo, groupKey);
    }

    @PostMapping("/companies/rename")
    public String renameCompany(
            @RequestParam String groupKey,
            @RequestParam String canonicalName,
            @RequestParam(required = false) String returnTo,
            RedirectAttributes redirectAttributes) {
        try {
            var result = companyService.renameGroup(groupKey, canonicalName);
            redirectAttributes.addFlashAttribute(
                    "companySuccess",
                    result.applicationsUpdated() + " application"
                            + (result.applicationsUpdated() == 1 ? "" : "s")
                            + " renamed to " + result.canonicalName() + ".");
            if ("detail".equalsIgnoreCase(returnTo)) {
                return "redirect:/companies/" + encodePath(result.groupKey());
            }
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("companyError", ex.getMessage());
            if ("detail".equalsIgnoreCase(returnTo)) {
                return "redirect:/companies/" + encodePath(groupKey);
            }
        }
        return "redirect:/companies";
    }

    @PostMapping("/companies/merge")
    public String mergeCompanies(
            @RequestParam(required = false) List<String> groupKey,
            @RequestParam String canonicalName,
            RedirectAttributes redirectAttributes) {
        try {
            var result = companyService.mergeGroups(groupKey, canonicalName);
            redirectAttributes.addFlashAttribute(
                    "companySuccess",
                    "Merged " + result.groupsMerged() + " company groups into " + result.canonicalName()
                            + " across " + result.applicationsUpdated() + " applications.");
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("companyError", ex.getMessage());
        }
        return "redirect:/companies";
    }

    private String redirectAfterCompanyAction(String returnTo, String groupKey) {
        if ("detail".equalsIgnoreCase(returnTo)) {
            return "redirect:/companies/" + encodePath(groupKey);
        }
        return "redirect:/companies";
    }

    private String encodePath(String value) {
        return UriUtils.encodePathSegment(value, StandardCharsets.UTF_8);
    }

    private ResponseEntity<byte[]> logoResponse(CompanyLogo logo) {
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .contentType(MediaType.parseMediaType(logo.mimeType()))
                .body(logo.data());
    }
}
