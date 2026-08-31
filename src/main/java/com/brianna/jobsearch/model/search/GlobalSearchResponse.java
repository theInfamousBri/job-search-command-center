package com.brianna.jobsearch.model.search;

import java.util.List;

public record GlobalSearchResponse(
        String query,
        int totalResults,
        List<Group> groups) {

    public record Group(
            String type,
            String label,
            List<Result> results) {
    }

    public record Result(
            String type,
            String title,
            String subtitle,
            String meta,
            String url,
            String initials,
            String badge,
            boolean exactMatch) {
    }
}
