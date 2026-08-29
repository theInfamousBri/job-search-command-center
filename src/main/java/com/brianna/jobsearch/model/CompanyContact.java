package com.brianna.jobsearch.model;

import java.time.LocalDateTime;
import java.util.Locale;

public record CompanyContact(
        long id,
        String companyKey,
        String name,
        String role,
        CompanyContactRelationship relationship,
        String email,
        String linkedinUrl,
        String notes,
        boolean hasPhoto,
        LocalDateTime createdAt,
        LocalDateTime updatedAt) {

    public String initials() {
        if (name == null || name.isBlank()) return "?";
        String[] words = name.trim().split("\\s+");
        if (words.length == 1) {
            return words[0].substring(0, Math.min(2, words[0].length())).toUpperCase(Locale.ROOT);
        }
        return (words[0].substring(0, 1) + words[words.length - 1].substring(0, 1)).toUpperCase(Locale.ROOT);
    }
}
