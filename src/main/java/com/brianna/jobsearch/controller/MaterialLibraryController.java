package com.brianna.jobsearch.controller;

import com.brianna.jobsearch.model.MaterialApplicationReference;
import com.brianna.jobsearch.model.MaterialType;
import com.brianna.jobsearch.service.MaterialService;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.http.CacheControl;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
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

@Controller
public class MaterialLibraryController {

    private final MaterialService materialService;

    public MaterialLibraryController(MaterialService materialService) {
        this.materialService = materialService;
    }

    @GetMapping("/materials")
    public String library(@RequestParam(required = false) String q, Model model) {
        var materials = materialService.all(q);
        Map<Long, java.util.List<MaterialApplicationReference>> linkedApplications = new LinkedHashMap<>();
        for (var material : materials) {
            if (material.linkedApplicationCount() > 0) {
                linkedApplications.put(material.id(), materialService.applicationsForMaterial(material.id()));
            }
        }
        model.addAttribute("materials", materials);
        model.addAttribute("materialSummary", materialService.summary());
        model.addAttribute("materialTypes", MaterialType.values());
        model.addAttribute("materialQuery", q == null ? "" : q);
        model.addAttribute("linkedApplications", linkedApplications);
        return "materials";
    }

    @PostMapping("/materials")
    public String upload(
            @RequestParam String materialType,
            @RequestParam(required = false) String displayName,
            @RequestParam(required = false) String notes,
            @RequestParam("file") MultipartFile file,
            RedirectAttributes redirectAttributes) {
        try {
            var result = materialService.upload(materialType, displayName, notes, file);
            if (result.created()) {
                redirectAttributes.addFlashAttribute(
                        "materialSuccess", "Added to Materials Library: " + result.material().displayName());
            } else {
                redirectAttributes.addFlashAttribute(
                        "materialNotice", "That exact file is already stored as “" + result.material().displayName() + "”.");
            }
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("materialError", ex.getMessage());
        }
        return "redirect:/materials";
    }

    @PostMapping("/materials/{materialId}")
    public String update(
            @PathVariable long materialId,
            @RequestParam String displayName,
            @RequestParam(required = false) String notes,
            RedirectAttributes redirectAttributes) {
        try {
            var material = materialService.update(materialId, displayName, notes);
            redirectAttributes.addFlashAttribute("materialSuccess", "Updated “" + material.displayName() + "”.");
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("materialError", ex.getMessage());
        }
        return "redirect:/materials#material-" + materialId;
    }

    @PostMapping("/materials/{materialId}/delete")
    public String delete(@PathVariable long materialId, RedirectAttributes redirectAttributes) {
        try {
            String name = materialService.get(materialId).displayName();
            materialService.delete(materialId);
            redirectAttributes.addFlashAttribute("materialSuccess", "Deleted “" + name + "” from the library.");
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("materialError", ex.getMessage());
        }
        return "redirect:/materials";
    }

    @GetMapping("/materials/{materialId}/download")
    public ResponseEntity<byte[]> download(@PathVariable long materialId) {
        var content = materialService.download(materialId);
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
}
