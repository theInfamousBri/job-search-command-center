package com.brianna.jobsearch.model;

public record NeedsAttentionItem(
        long applicationId,
        String company,
        String role,
        AttentionUrgency urgency,
        String headline,
        String detail,
        String actionLabel,
        String actionUrl,
        String viewUrl,
        long proximityDays,
        int ruleOrder) {
}
