package com.brianna.jobsearch.model;

public record MaterialApplicationReference(long applicationId, String company, String role) {
    public String label() {
        return company + " · " + role;
    }
}
